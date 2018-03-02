package com.example.fansy.audiokeyboard;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.icu.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    final char KEY_NOT_FOUND = 0;

    //voice manager
    HashMap<String, int[]> voice = new HashMap<>();
    ArrayList<Integer> myPlayList = new ArrayList<>();
    MediaPlayer current;

    TextToSpeech textToSpeech;

    final int INIT_MODE_ABSOLUTE = 0;
    final int INIT_MODE_RELATIVE = 1;
    final int INIT_MODE_NOTHING = 2;
    int initMode = INIT_MODE_RELATIVE;

    final int LANG_MODE_ENG = 0;
    final int LANG_MODE_CHN = 1;
    final int LANG_MODE_CHN_PINYIN = 2;
    int languageMode = LANG_MODE_ENG;

    final int CONFIRM_MODE_UP = 0;
    final int CONFIRM_MODE_DOUBLECLICK = 1;
    int confirmMode = CONFIRM_MODE_UP;

    final int UPVOICE_MODE_YES = 0;
    final int UPVOICE_MODE_NO = 1;
    int upvoiceMode = UPVOICE_MODE_YES;

    final int RECORD_MODE_STARTED = 0;
    final int RECORD_MODE_STOPED = 1;
    int recordMode = RECORD_MODE_STOPED;

    int SD_coefficient = 10;

    ImageView keyboard;
    TextView text, candidatesView, readListView, voiceSpeedText, elapsedTimeText;
    Button confirmButton, initModeButton, speedpButton, speedmButton;
    Menu menu;
    String currentInput = "";
    String readList = ""; //current voice list
    String currentWord = ""; //most possible char sequence
    String currentBaseline = "";
    char nowCh = 0; //the most possible char
    char nowChSaved = 0; // for double click
    char nowChBaselineSaved = 0;
    char delete_key='<';
    ArrayList<Word> dict_eng = new ArrayList<>();
    ArrayList<Word> dict_chn = new ArrayList<>();
    ArrayList<Word> dict_chn_pinyin = new ArrayList<>();
    ArrayList<Character> seq = new ArrayList<Character>(); //char sequence during the whole touch
    String keysNearby[] = new String[26];
    double keysNearbyProb[][] = new double[26][26]; //keysNearbyProb[x][y] the possibility when want y but touch x
    String filename = "";

    int voiceSpeed = 50;

    AutoKeyboard autoKeyboard;

    //Fuzzy Input Test Var
    TextView fuzzyInputTestCharShow;
    ProgressBar progressBar;
    ListView listView;
    //String fuzzyInputTestStoragePath="/storage/emulated/0/FuzzyInputTestData";
    boolean ifCalDone=false;
    boolean ifSave=false;
    final int KEYBOARD_MODE=0;
    final int FUZZY_INPUT_TEST_MODE=1;
    int MAX_TURN=8;//eight turn
    int MAX_FUZZY_INPUT_TURN=26*MAX_TURN;
    final int FUZZY_INPUT_SOUND_BEGIN=0;//for sound
    final int FUZZY_INPUT_SOUND_NEXT=1;//for sound
    final int FUZZY_INPUT_SOUND_END=2;//
    int activity_mode=KEYBOARD_MODE;
    final int NORMAL=0;
    final int CANCEL=1;
    final int BORDER=2;
    final int CANCEL_BORDER=3;
    int testmode=NORMAL;
    int fuzzyInputTestTurn=0;
    ArrayList<Integer> fuzzyInputTestList=new ArrayList<>();
    //int fuzzyInputTestData[]=new int[MAX_FUZZY_INPUT_TURN+30];//用来记录第一次手指落下的地方
    //in case of the user type too fast,add a redundancy 30
    //double fuzzyInputKeysNearbyProb[][] = new double[26][26];
    ArrayList<Turn> fuzzyInputTestFigerRecord=new ArrayList<>();//用来记录手指的所有移动
    String DataToSave;//记录每一次触摸事件的准确数据
    String DataToShowAndSave;//计算出概率
    String DataTouchModel;
    //Fuzzy Input test Var
    class Data{
        char target;
        char actual;
        double positionX;
        double positionY;
        int actionType;//ActionMove ActionDown ActionUp
        //ACTION_DOWN==0
        //ACTION_MOVE==2;
        //ACTION_UP==1;
        double timeAfterLastAction;
        int currentSoundNum;//此时正在播放的音频的数量加上等待播放的音频的数量
        Data(char target,char actual,double positionX,double positionY,int actionType,double timeAfterLastAction){
            this.target=target;
            this.actual=actual;
            this.positionX=positionX;
            this.positionY=positionY;
            this.actionType=actionType;
            this.timeAfterLastAction=timeAfterLastAction;
            if(current==null){
                this.currentSoundNum=myPlayList.size();
            }else{
                this.currentSoundNum=1+myPlayList.size();
            }
        }
        Data(){
            this.target='?';
            this.actual='?';
            this.positionX=-1;
            this.positionY=-1;
            this.actionType=-1;
            this.timeAfterLastAction=-1;
            this.currentSoundNum=-1;
        }
        String getData(){
            String data=String.valueOf(target)+" "+String.valueOf(actual)+" "+
                    String.valueOf(positionX)+" "+String.valueOf(positionY)+" "+
                    String.valueOf(actionType)+" "+String.valueOf(timeAfterLastAction)+" "+
                    String.valueOf(currentSoundNum)+"\n";
            return data;
        }
    }
    class Turn {
        Long lastTime;
        int turnIndex;
        char target;
        int upTimes;//用户抬起了多少次手指
        ArrayList<Character> actualTypeIn;//用户实际上键入的值
        ArrayList<Data> data;
        char firstTouch;//除去一开始输到边界外的键入的值
        Turn(int turnIndex,Long lastTime){
            this.lastTime=lastTime;
            this.turnIndex=turnIndex;
            this.target=(char)(fuzzyInputTestList.get(turnIndex%26)+'A');
            this.upTimes=0;
            this.data=new ArrayList<>();
            this.actualTypeIn=new ArrayList<>();
            this.firstTouch=KEY_NOT_FOUND;
        }
        boolean addData(char actual,double positionX,double positionY,int actionType,Long eventTime){
            char upperActual=actual;
            if (upperActual!=KEY_NOT_FOUND)
                upperActual=Character.toUpperCase(upperActual);
            if (this.firstTouch==KEY_NOT_FOUND && upperActual!=KEY_NOT_FOUND)
                this.firstTouch=upperActual;
            this.data.add(new Data(this.target,upperActual,positionX,positionY,actionType,eventTime-this.lastTime));
            this.lastTime=eventTime;
            if (actionType== MotionEvent.ACTION_UP){
                this.upTimes++;
                this.actualTypeIn.add(upperActual);
                if(upperActual==this.target){
                    return true;
                }else
                    return false;
            }
            return false;
        }
        String getTurn(){
            String turnToReturn="turnIndex="+String.valueOf(this.turnIndex)+"\n"+
                    "target="+String.valueOf(this.target)+"\n"+
                    "firstTouch"+String.valueOf(this.firstTouch)+"\n"+
                    "upTimes="+String.valueOf(this.upTimes)+"\n";
            for(int typeIndex=0;typeIndex<this.actualTypeIn.size();typeIndex++){
                turnToReturn+="actualTypeIn "+String.valueOf(typeIndex+1)+"="+this.actualTypeIn.get(typeIndex)+"\n";
            }
            return turnToReturn;
        }
        String getData(){
            String dataToReturn="";
            for (int i=0;i<data.size();i++){
                dataToReturn+=data.get(i).getData();
            }
            return dataToReturn;
        }
    }
    String getTestData(){
        String explain="Data的记录格式为:目标键入字母 实际键入字母 横坐标(相对于键盘) 纵坐标(相对于键盘) 触摸事件类型(按下:0;抬起:1;移动:2) 距离上次触摸事件时间(单位:ms) 目前在播音频数量加上待播音频数量\n";
        String data="MAX_TURN="+String.valueOf(MAX_TURN)+'\n'+
                "screen_width_ratio="+String.valueOf(autoKeyboard.screen_width_ratio)+'\n'+
                "screen_height_ratio="+String.valueOf(autoKeyboard.screen_height_ratio)+'\n'+
                "voiceSpeed="+String.valueOf(voiceSpeed)+'\n';

        return explain+data;
    }
    //Fuzzy Input Test Function
    public void WidgetSet(int activity_mode){
        switch(activity_mode){
            case KEYBOARD_MODE:{
                fuzzyInputTestCharShow.setVisibility(View.GONE);
                text.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                listView.setVisibility(View.GONE);
                //keyboard.setVisibility(View.VISIBLE);
                candidatesView.setVisibility(View.VISIBLE);
                readListView.setVisibility(View.VISIBLE);
                speedmButton.setVisibility(View.VISIBLE);
                voiceSpeedText.setVisibility(View.VISIBLE);
                confirmButton.setText("CONFIRM");
                speedpButton.setText("SPEED+");
                refresh();
                break;
            }
            case FUZZY_INPUT_TEST_MODE:{
                fuzzyInputTestCharShow.setVisibility(View.VISIBLE);
                text.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                listView.setVisibility(View.VISIBLE);
                //keyboard.setVisibility(View.GONE);
                candidatesView.setVisibility(View.GONE);
                readListView.setVisibility(View.GONE);
                speedmButton.setVisibility(View.INVISIBLE);
                voiceSpeedText.setVisibility(View.GONE);
                initModeButton.setText("Back");
                confirmButton.setText("Save");
                speedpButton.setText("BackSpace");
                break;
            }
        }
    }
    public void testListInit() {
        fuzzyInputTestList.clear();
        for (int i = 0; i < 26; i++) {
            fuzzyInputTestList.add(i);
        }
        Collections.shuffle(fuzzyInputTestList);
    }
    public void beginFuzzyInputTest(){
        activity_mode=FUZZY_INPUT_TEST_MODE;
        WidgetSet(activity_mode);
        testListInit();
        fuzzyInputTestTurn=0;
        playMedia("fuzzyInput",FUZZY_INPUT_SOUND_BEGIN,false);
        playMedia("google",fuzzyInputTestList.get(fuzzyInputTestTurn%26),true);
        String nextChar=String.valueOf((char)('A'+fuzzyInputTestList.get(fuzzyInputTestTurn%26)));
        fuzzyInputTestCharShow.setText(String.valueOf(fuzzyInputTestTurn)+" "+nextChar);
        progressBar.setProgress(fuzzyInputTestTurn*100/MAX_FUZZY_INPUT_TURN);
        ifSave=false;
        ifCalDone=false;
        fuzzyInputTestFigerRecord.clear();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void saveFuzzyInputTestData(){
        if(ifSave){
            toast("已经存储过一次了");
        }else {
            if (fuzzyInputTestTurn != MAX_FUZZY_INPUT_TURN) {
                toast("请先完成测试");
            } else if (!ifCalDone) {
                toast("还没算完");
            } else {
                if (isExternalStorageWritable()) {
                    //String directory="/storage/emulated/0/Android/data/FuzzyInput/";
                    //File sdCard = Environment.getExternalStorageDirectory();
                    File DirectoryFolder = this.getExternalFilesDir(null);
                    //File DirectoryFolder = new File(fuzzyInputTestStoragePath);
                    //File DirectoryFolder = this.getExternalStoragePublicDirectory();
                    if (!DirectoryFolder.exists()) { //如果该文件夹不存在，则进行创建
                        DirectoryFolder.mkdirs();//创建文件夹
                    }
                    String time=getTime();
                    File fileInDetail = new File(DirectoryFolder, time+ "_详细数据.txt");
                    File fileInRatio = new File(DirectoryFolder,time+"_概率数据.txt");
                    File fileInTouchModel = new File(DirectoryFolder,time+"_TouchModel.txt");
                    if (!fileInDetail.exists()) {
                        try {
                            fileInDetail.createNewFile();
                            //file is create
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if (!fileInRatio.exists()) {
                        try {
                            fileInRatio.createNewFile();
                            //file is create
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if (!fileInTouchModel.exists()) {
                        try {
                            fileInTouchModel.createNewFile();
                            //file is create
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    try {
                        FileOutputStream fosInDetail = new FileOutputStream(fileInDetail);
                        fosInDetail.write(DataToSave.getBytes());
                        fosInDetail.close();

                        FileOutputStream fosInRatio = new FileOutputStream(fileInRatio);
                        fosInRatio.write(DataToShowAndSave.getBytes());
                        fosInRatio.close();

                        FileOutputStream fosInTouchModel = new FileOutputStream(fileInTouchModel);
                        fosInTouchModel.write(DataTouchModel.getBytes());
                        fosInTouchModel.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    toast("存储完毕！");
                    ifSave = true;

                } else {
                    toast("不能存储文件!");
                }
            }
        }
    }

    public void restartFuzzyInputTest(){
        stopVoice();
        if(!ifSave){
            AlertDialog.Builder dialog=new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle("警告");
            dialog.setMessage("测试还未完成，你确定要重新开始吗？");
            dialog.setCancelable(false);
            dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    fuzzyInputTestTurn=0;
                    playMedia("fuzzyInput",FUZZY_INPUT_SOUND_BEGIN,false);
                    playMedia("google",fuzzyInputTestList.get(fuzzyInputTestTurn%26),true);
                    String nextChar=String.valueOf((char)('A'+fuzzyInputTestList.get(fuzzyInputTestTurn%26)));
                    fuzzyInputTestCharShow.setText(nextChar);
                    progressBar.setProgress(fuzzyInputTestTurn*100/MAX_FUZZY_INPUT_TURN);
                    ifSave=false;
                    ifCalDone=false;
                    listView.setVisibility(View.GONE);
                    fuzzyInputTestFigerRecord.clear();
                }
            });
            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            dialog.show();
        }else{
            fuzzyInputTestTurn=0;
            playMedia("fuzzyInput",FUZZY_INPUT_SOUND_BEGIN,false);
            playMedia("google",fuzzyInputTestList.get(fuzzyInputTestTurn%26),false);
            String nextChar=String.valueOf((char)('A'+fuzzyInputTestList.get(fuzzyInputTestTurn%26)));
            fuzzyInputTestCharShow.setText(nextChar);
            progressBar.setProgress(fuzzyInputTestTurn*100/MAX_FUZZY_INPUT_TURN);
            ifSave=false;
            ifCalDone=false;
            listView.setVisibility(View.GONE);
            fuzzyInputTestFigerRecord.clear();
        }

    }

    public void stopFuzzyInputTest(){
        stopVoice();
        if(!ifSave){
            AlertDialog.Builder dialog=new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle("警告");
            dialog.setMessage("测试还未完成，你确定要返回吗？");
            dialog.setCancelable(false);
            dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    activity_mode = KEYBOARD_MODE;
                    WidgetSet(activity_mode);
                    ifSave=false;
                    ifCalDone=false;
                    fuzzyInputTestFigerRecord.clear();
                }
            });
            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            dialog.show();
        }else {
            activity_mode = KEYBOARD_MODE;
            WidgetSet(activity_mode);
            refresh();
            fuzzyInputTestFigerRecord.clear();
        }
    }
    int sum(int x[],int length){
        int ans=0;
        for (int i=0;i<length;i++){
            ans+=x[i];
        }
        return ans;
    }
    public void calculateFuzzyInputData(){
        if(fuzzyInputTestTurn!=MAX_FUZZY_INPUT_TURN){
            toast("不应该在这时计算!");
        }
        else{
            int keyNearByData[][]=new int[26][26];
            for (int i=0;i<26;i++){
                for(int j=0;j<26;j++){
                    keyNearByData[i][j]=0;
                }
            }
            DataToSave =getTestData();
            DataToShowAndSave="";
            DataTouchModel="a,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z\n";
            ArrayList<String> listViewData=new ArrayList<String>();
            for(int i=0;i<MAX_FUZZY_INPUT_TURN;i++) {
                Turn turn = fuzzyInputTestFigerRecord.get(i);
                String eachTurn = turn.getTurn();
                keyNearByData[turn.target - 'A'][turn.firstTouch - 'A']++;
                //listViewData.add(eachTurn);
                DataToSave += eachTurn;
                DataToSave += turn.getData();
            }
            float keyNearByRatio[][]=new float[26][26];
            for (int i=0;i<26;i++){
                int totalTimes=sum(keyNearByData[i],26);
                if (totalTimes!=MAX_TURN){
                    String temp="计算"+String.valueOf((char)(i+'A'))+"的键入概率时总次数为"+String.valueOf(totalTimes);
                    listViewData.add(temp);
                    toast(temp);
                }
                DataTouchModel+=String.valueOf((char)(i+'a'));
                for (int j=0;j<26;j++){
                    keyNearByRatio[i][j]=(float)keyNearByData[i][j]/(float)totalTimes;
                    String temp=String.valueOf((char)(i+'A'))+" "+String.valueOf((char)(j+'A'))+" "+String.valueOf(keyNearByRatio[i][j]);
                    DataToShowAndSave+=temp+'\n';
                    DataTouchModel+=","+String.valueOf(keyNearByRatio[i][j]);
                    if (keyNearByRatio[i][j]!=0)
                        listViewData.add(temp);
                }
                DataTouchModel+="\n";
            }
            ifCalDone=true;
            ArrayAdapter<String> adapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,listViewData);
            listView.setVisibility(View.VISIBLE);
            listView.setAdapter(adapter);

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public String getTime()
    {
        long time=System.currentTimeMillis();//long now = android.os.SystemClock.uptimeMillis();
        SimpleDateFormat format=new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date d1=new Date(time);
        return format.format(d1);
    }
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    public void toast(String textForToast){
        Toast mytoast = Toast.makeText(getApplicationContext(),
                textForToast, Toast.LENGTH_SHORT);
        mytoast.setGravity(Gravity.CENTER, 0, 0);
        mytoast.show();
    }

    public String getFilename(){
        Calendar starttime = Calendar.getInstance();
        int YY = starttime.get(Calendar.YEAR);
        int MM = starttime.get(Calendar.MONTH) + 1;
        int DD = starttime.get(Calendar.DATE);
        int HH = starttime.get(Calendar.HOUR_OF_DAY);
        int mm = starttime.get(Calendar.MINUTE);
        int SS = starttime.get(Calendar.SECOND);
        int MI = starttime.get(Calendar.MILLISECOND);
        return "" + YY + MM + DD + HH + mm + SS + MI + "_" + initMode + "_" + languageMode + "_" + confirmMode + "_" + SD_coefficient + ".txt";
    }

    public void write(String content){
        if (recordMode != RECORD_MODE_STARTED)
            return;
        Log.i("write", content);
        try{
            String path = Environment.getExternalStorageDirectory().getPath() + "/Android/data/com.example.fansy.audiokeyboard/files";
            File path1 = new File(path);
            if (!path1.exists()) {
                Log.i("write", path + " doesn't exist!");
                try {
                    path1.mkdirs();
                }catch (Exception e){
                    Log.i("write", "mkdirs failed");
                }
            }
            PrintWriter logger = new PrintWriter(new OutputStreamWriter(new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Android/data/com.example.fansy.audiokeyboard/files/" + filename, true)), true);
            logger.println(System.currentTimeMillis() + " " + content);
            logger.flush();
            logger.close();
        } catch (Exception e){
            Log.i("write", "failed");
            Log.i("write", Log.getStackTraceString(e));
        }
    }

    //redraw the views
    public void refresh(){
        text.setText(currentInput + "\n" + currentWord + "\n" + currentBaseline);
        String str = "";
        for (int i = 0; i < candidates.size(); ++i)
            str += candidates.get(i).alias + "\n";
        candidatesView.setText(str);
        readListView.setText(readList);
        if (initMode == INIT_MODE_ABSOLUTE)
            initModeButton.setText("absolute");
        else if(initMode == INIT_MODE_RELATIVE)
            initModeButton.setText("relative");
        else
            initModeButton.setText("nothing");
    }

    final int MAX_CANDIDATE = 5;
    int currentCandidate;
    public ArrayList<Word> candidates = new ArrayList<Word>();

    //predict the candidates according the currentWord and currentWord2 and refresh
    public void predict(String currentWord){
        candidates.clear();

        if (currentWord == "")
            return;
        ArrayList<Word> dict = new ArrayList<>();
        if (languageMode == LANG_MODE_ENG)
            dict = dict_eng;
        else if (languageMode == LANG_MODE_CHN)
            dict = dict_chn_pinyin;

        for (int i = 0; i < dict.size(); ++i){
            if (candidates.size() >= MAX_CANDIDATE)
                break;
            Word candidate = dict.get(i);
            if (candidate.text.length() < currentWord.length())
                continue;

            boolean flag = true;
            for (int j = 0; j < currentWord.length(); ++j)
                if (candidate.text.charAt(j) != currentWord.charAt(j)){
                    flag = false;
                    break;
                }
            if (flag)
                candidates.add(candidate);
        }
        refresh();
    }

    public void stopVoice(){
        textToSpeech.stop();
        if (current != null) {
            current.stop();
            current.reset();
            current.release();
            current = null;
        }
        myPlayList.clear();
    }

    public void stopInput(){
        seq.clear();
        stopVoice();
    }

    public void finishWord(){
        currentWord = "";
        currentBaseline = "";
        readList = "";
        predict(currentWord);
        refresh();
    }

    //new touch point in keyboard area
    public void newPoint(int x, int y){
        if (seq.size() == 0){//first touch
            if (initMode == INIT_MODE_ABSOLUTE) {
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                addToSeq(autoKeyboard.getKeyByPosition(x, y, autoKeyboard.INIT_LAYOUT));
            }
            else if(initMode == INIT_MODE_RELATIVE){
                char ch = autoKeyboard.getKeyByPosition(x, y, autoKeyboard.CURR_LAYOUT);
                if (ch == upKey){
                    addToSeq(ch);
                }
                else{
                    autoKeyboard.resetLayout();
                    autoKeyboard.drawLayout();
                    write("reset");
                    ch = autoKeyboard.getKeyByPosition(x, y, autoKeyboard.CURR_LAYOUT);
                    char best = getMostPossibleKey(x, y);
                    if (autoKeyboard.tryLayout(best, x, y)){
                        write("try " + best + " " + x + " " + y);
                        autoKeyboard.drawLayout();
                        addToSeq(best);
                    }else{
                        if (autoKeyboard.tryLayout(ch, x, y)){
                            write("try " + ch + " " + x + " " + y);
                            autoKeyboard.drawLayout();
                        }
                        addToSeq(ch);
                    }
                }
            }else{
                char ch = autoKeyboard.getKeyByPosition(x, y, autoKeyboard.INIT_LAYOUT);
                addToSeq(ch);
            }
        }
        else{
            addToSeq(autoKeyboard.getKeyByPosition(x, y, autoKeyboard.CURR_LAYOUT));
        }
    }

    //init keysNearby and keysNearbyProb
    //import touch model
    public void initKeyboard(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.touchmodel)));
        String line;
        try{
            String[] secondKeys = reader.readLine().split(",");
            for (int i = 0; i < 26; ++i){
                keysNearby[i] = "";
            }
            while ((line = reader.readLine()) != null){
                String[] firstKeyAndPs = line.split(",");
                char firstKey = firstKeyAndPs[0].charAt(0);
                for(int i=0;i!=26;i++){
                    if(firstKeyAndPs[i+1]!="0"){
                        int tmp = secondKeys[i + 1].charAt(0)-'a';
                        keysNearby[tmp] += firstKey;
                        keysNearbyProb[tmp][keysNearby[tmp].length() - 1] = Double.valueOf(firstKeyAndPs[i + 1]);
                    }
                }
            }
            reader.close();
            Log.i("init", "read touch model finished ");
        } catch (Exception e){
            Log.i("init", "read touch model failed");
        }
    }

    final int DICT_SIZE[] = {50000, 38991, 65105};
    //read dict from file
    public void initDict(){
        Log.i("init", "start loading dict_eng");
        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.dict_eng)));
        String line;
        try{
            int lineNo = 0;
            while ((line = reader.readLine()) != null){
                lineNo++;
                String[] ss = line.split(" ");
                dict_eng.add(new Word(ss[0], Double.valueOf(ss[1])));
                if (lineNo == DICT_SIZE[LANG_MODE_ENG])
                    break;
            }
            reader.close();
            Log.i("init", "read dict_eng finished " + dict_eng.size());
        } catch (Exception e){
            Log.i("init", "read dict_eng failed");
        }

        reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.dict_chn)));
        try{
            int lineNo = 0;
            while ((line = reader.readLine()) != null){
                lineNo++;
                String[] ss = line.split(" ");
                dict_chn.add(new Word(ss[0], Double.valueOf(ss[1])));
                if (lineNo == DICT_SIZE[LANG_MODE_CHN])
                    break;
            }
            reader.close();
            Log.i("init", "read dict_chn finished" + dict_chn.size());
        } catch (Exception e){
            Log.i("init", "read dict_chn failed");
        }

        reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.dict_chn_pinyin)));
        try{
            int lineNo = 0;
            while ((line = reader.readLine()) != null){
                lineNo++;
                String[] ss = line.split(" ");
                dict_chn_pinyin.add(new Word(ss[0], Double.valueOf(ss[1])));
                dict_chn_pinyin.get(dict_chn_pinyin.size() - 1).alias = ss[2];
                if (lineNo == DICT_SIZE[LANG_MODE_CHN_PINYIN])
                    break;
            }
            reader.close();
            Log.i("init", "read dict_chn_pinyin finished" + dict_chn_pinyin.size());
        } catch (Exception e){
            Log.i("init", "read dict_chn_pinyin failed");
        }
    }

    public void initVoice() {
        voice.put("ios11_50", new int[26]);
        voice.put("ios11_60", new int[26]);
        voice.put("ios11_70", new int[26]);
        voice.put("ios11_80", new int[26]);
        voice.put("ios11_90", new int[26]);
        voice.put("ios11_100", new int[26]);
        voice.put("ios11da", new int[1]);
        voice.put("delete", new int[1]);
        voice.put("blank", new int[1]);
        voice.put("delete_word", new int[1]);
        voice.put("fuzzyInput",new int[3]);
        voice.put("google",new int[26]);

        voice.get("fuzzyInput")[0]=R.raw.fuzzy_input_test_begin;
        voice.get("fuzzyInput")[1]=R.raw.fuzzy_input_test_next;
        voice.get("fuzzyInput")[2]=R.raw.fuzzy_input_test_end;

        voice.get("google")[ 0] = R.raw.google_a;
        voice.get("google")[ 1] = R.raw.google_b;
        voice.get("google")[ 2] = R.raw.google_c;
        voice.get("google")[ 3] = R.raw.google_d;
        voice.get("google")[ 4] = R.raw.google_e;
        voice.get("google")[ 5] = R.raw.google_f;
        voice.get("google")[ 6] = R.raw.google_g;
        voice.get("google")[ 7] = R.raw.google_h;
        voice.get("google")[ 8] = R.raw.google_i;
        voice.get("google")[ 9] = R.raw.google_j;
        voice.get("google")[10] = R.raw.google_k;
        voice.get("google")[11] = R.raw.google_l;
        voice.get("google")[12] = R.raw.google_m;
        voice.get("google")[13] = R.raw.google_n;
        voice.get("google")[14] = R.raw.google_o;
        voice.get("google")[15] = R.raw.google_p;
        voice.get("google")[16] = R.raw.google_q;
        voice.get("google")[17] = R.raw.google_r;
        voice.get("google")[18] = R.raw.google_s;
        voice.get("google")[19] = R.raw.google_t;
        voice.get("google")[20] = R.raw.google_u;
        voice.get("google")[21] = R.raw.google_v;
        voice.get("google")[22] = R.raw.google_w;
        voice.get("google")[23] = R.raw.google_x;
        voice.get("google")[24] = R.raw.google_y;
        voice.get("google")[25] = R.raw.google_z;
        voice.get("ios11_50")[ 0] = R.raw.ios11_50_a;
        voice.get("ios11_50")[ 1] = R.raw.ios11_50_b;
        voice.get("ios11_50")[ 2] = R.raw.ios11_50_c;
        voice.get("ios11_50")[ 3] = R.raw.ios11_50_d;
        voice.get("ios11_50")[ 4] = R.raw.ios11_50_e;
        voice.get("ios11_50")[ 5] = R.raw.ios11_50_f;
        voice.get("ios11_50")[ 6] = R.raw.ios11_50_g;
        voice.get("ios11_50")[ 7] = R.raw.ios11_50_h;
        voice.get("ios11_50")[ 8] = R.raw.ios11_50_i;
        voice.get("ios11_50")[ 9] = R.raw.ios11_50_j;
        voice.get("ios11_50")[10] = R.raw.ios11_50_k;
        voice.get("ios11_50")[11] = R.raw.ios11_50_l;
        voice.get("ios11_50")[12] = R.raw.ios11_50_m;
        voice.get("ios11_50")[13] = R.raw.ios11_50_n;
        voice.get("ios11_50")[14] = R.raw.ios11_50_o;
        voice.get("ios11_50")[15] = R.raw.ios11_50_p;
        voice.get("ios11_50")[16] = R.raw.ios11_50_q;
        voice.get("ios11_50")[17] = R.raw.ios11_50_r;
        voice.get("ios11_50")[18] = R.raw.ios11_50_s;
        voice.get("ios11_50")[19] = R.raw.ios11_50_t;
        voice.get("ios11_50")[20] = R.raw.ios11_50_u;
        voice.get("ios11_50")[21] = R.raw.ios11_50_v;
        voice.get("ios11_50")[22] = R.raw.ios11_50_w;
        voice.get("ios11_50")[23] = R.raw.ios11_50_x;
        voice.get("ios11_50")[24] = R.raw.ios11_50_y;
        voice.get("ios11_50")[25] = R.raw.ios11_50_z;

        voice.get("ios11_60")[ 0] = R.raw.ios11_60_a;
        voice.get("ios11_60")[ 1] = R.raw.ios11_60_b;
        voice.get("ios11_60")[ 2] = R.raw.ios11_60_c;
        voice.get("ios11_60")[ 3] = R.raw.ios11_60_d;
        voice.get("ios11_60")[ 4] = R.raw.ios11_60_e;
        voice.get("ios11_60")[ 5] = R.raw.ios11_60_f;
        voice.get("ios11_60")[ 6] = R.raw.ios11_60_g;
        voice.get("ios11_60")[ 7] = R.raw.ios11_60_h;
        voice.get("ios11_60")[ 8] = R.raw.ios11_60_i;
        voice.get("ios11_60")[ 9] = R.raw.ios11_60_j;
        voice.get("ios11_60")[10] = R.raw.ios11_60_k;
        voice.get("ios11_60")[11] = R.raw.ios11_60_l;
        voice.get("ios11_60")[12] = R.raw.ios11_60_m;
        voice.get("ios11_60")[13] = R.raw.ios11_60_n;
        voice.get("ios11_60")[14] = R.raw.ios11_60_o;
        voice.get("ios11_60")[15] = R.raw.ios11_60_p;
        voice.get("ios11_60")[16] = R.raw.ios11_60_q;
        voice.get("ios11_60")[17] = R.raw.ios11_60_r;
        voice.get("ios11_60")[18] = R.raw.ios11_60_s;
        voice.get("ios11_60")[19] = R.raw.ios11_60_t;
        voice.get("ios11_60")[20] = R.raw.ios11_60_u;
        voice.get("ios11_60")[21] = R.raw.ios11_60_v;
        voice.get("ios11_60")[22] = R.raw.ios11_60_w;
        voice.get("ios11_60")[23] = R.raw.ios11_60_x;
        voice.get("ios11_60")[24] = R.raw.ios11_60_y;
        voice.get("ios11_60")[25] = R.raw.ios11_60_z;

        voice.get("ios11_70")[ 0] = R.raw.ios11_70_a;
        voice.get("ios11_70")[ 1] = R.raw.ios11_70_b;
        voice.get("ios11_70")[ 2] = R.raw.ios11_70_c;
        voice.get("ios11_70")[ 3] = R.raw.ios11_70_d;
        voice.get("ios11_70")[ 4] = R.raw.ios11_70_e;
        voice.get("ios11_70")[ 5] = R.raw.ios11_70_f;
        voice.get("ios11_70")[ 6] = R.raw.ios11_70_g;
        voice.get("ios11_70")[ 7] = R.raw.ios11_70_h;
        voice.get("ios11_70")[ 8] = R.raw.ios11_70_i;
        voice.get("ios11_70")[ 9] = R.raw.ios11_70_j;
        voice.get("ios11_70")[10] = R.raw.ios11_70_k;
        voice.get("ios11_70")[11] = R.raw.ios11_70_l;
        voice.get("ios11_70")[12] = R.raw.ios11_70_m;
        voice.get("ios11_70")[13] = R.raw.ios11_70_n;
        voice.get("ios11_70")[14] = R.raw.ios11_70_o;
        voice.get("ios11_70")[15] = R.raw.ios11_70_p;
        voice.get("ios11_70")[16] = R.raw.ios11_70_q;
        voice.get("ios11_70")[17] = R.raw.ios11_70_r;
        voice.get("ios11_70")[18] = R.raw.ios11_70_s;
        voice.get("ios11_70")[19] = R.raw.ios11_70_t;
        voice.get("ios11_70")[20] = R.raw.ios11_70_u;
        voice.get("ios11_70")[21] = R.raw.ios11_70_v;
        voice.get("ios11_70")[22] = R.raw.ios11_70_w;
        voice.get("ios11_70")[23] = R.raw.ios11_70_x;
        voice.get("ios11_70")[24] = R.raw.ios11_70_y;
        voice.get("ios11_70")[25] = R.raw.ios11_70_z;

        voice.get("ios11_80")[ 0] = R.raw.ios11_80_a;
        voice.get("ios11_80")[ 1] = R.raw.ios11_80_b;
        voice.get("ios11_80")[ 2] = R.raw.ios11_80_c;
        voice.get("ios11_80")[ 3] = R.raw.ios11_80_d;
        voice.get("ios11_80")[ 4] = R.raw.ios11_80_e;
        voice.get("ios11_80")[ 5] = R.raw.ios11_80_f;
        voice.get("ios11_80")[ 6] = R.raw.ios11_80_g;
        voice.get("ios11_80")[ 7] = R.raw.ios11_80_h;
        voice.get("ios11_80")[ 8] = R.raw.ios11_80_i;
        voice.get("ios11_80")[ 9] = R.raw.ios11_80_j;
        voice.get("ios11_80")[10] = R.raw.ios11_80_k;
        voice.get("ios11_80")[11] = R.raw.ios11_80_l;
        voice.get("ios11_80")[12] = R.raw.ios11_80_m;
        voice.get("ios11_80")[13] = R.raw.ios11_80_n;
        voice.get("ios11_80")[14] = R.raw.ios11_80_o;
        voice.get("ios11_80")[15] = R.raw.ios11_80_p;
        voice.get("ios11_80")[16] = R.raw.ios11_80_q;
        voice.get("ios11_80")[17] = R.raw.ios11_80_r;
        voice.get("ios11_80")[18] = R.raw.ios11_80_s;
        voice.get("ios11_80")[19] = R.raw.ios11_80_t;
        voice.get("ios11_80")[20] = R.raw.ios11_80_u;
        voice.get("ios11_80")[21] = R.raw.ios11_80_v;
        voice.get("ios11_80")[22] = R.raw.ios11_80_w;
        voice.get("ios11_80")[23] = R.raw.ios11_80_x;
        voice.get("ios11_80")[24] = R.raw.ios11_80_y;
        voice.get("ios11_80")[25] = R.raw.ios11_80_z;

        voice.get("ios11_90")[ 0] = R.raw.ios11_90_a;
        voice.get("ios11_90")[ 1] = R.raw.ios11_90_b;
        voice.get("ios11_90")[ 2] = R.raw.ios11_90_c;
        voice.get("ios11_90")[ 3] = R.raw.ios11_90_d;
        voice.get("ios11_90")[ 4] = R.raw.ios11_90_e;
        voice.get("ios11_90")[ 5] = R.raw.ios11_90_f;
        voice.get("ios11_90")[ 6] = R.raw.ios11_90_g;
        voice.get("ios11_90")[ 7] = R.raw.ios11_90_h;
        voice.get("ios11_90")[ 8] = R.raw.ios11_90_i;
        voice.get("ios11_90")[ 9] = R.raw.ios11_90_j;
        voice.get("ios11_90")[10] = R.raw.ios11_90_k;
        voice.get("ios11_90")[11] = R.raw.ios11_90_l;
        voice.get("ios11_90")[12] = R.raw.ios11_90_m;
        voice.get("ios11_90")[13] = R.raw.ios11_90_n;
        voice.get("ios11_90")[14] = R.raw.ios11_90_o;
        voice.get("ios11_90")[15] = R.raw.ios11_90_p;
        voice.get("ios11_90")[16] = R.raw.ios11_90_q;
        voice.get("ios11_90")[17] = R.raw.ios11_90_r;
        voice.get("ios11_90")[18] = R.raw.ios11_90_s;
        voice.get("ios11_90")[19] = R.raw.ios11_90_t;
        voice.get("ios11_90")[20] = R.raw.ios11_90_u;
        voice.get("ios11_90")[21] = R.raw.ios11_90_v;
        voice.get("ios11_90")[22] = R.raw.ios11_90_w;
        voice.get("ios11_90")[23] = R.raw.ios11_90_x;
        voice.get("ios11_90")[24] = R.raw.ios11_90_y;
        voice.get("ios11_90")[25] = R.raw.ios11_90_z;

        voice.get("ios11_100")[ 0] = R.raw.ios11_100_a;
        voice.get("ios11_100")[ 1] = R.raw.ios11_100_b;
        voice.get("ios11_100")[ 2] = R.raw.ios11_100_c;
        voice.get("ios11_100")[ 3] = R.raw.ios11_100_d;
        voice.get("ios11_100")[ 4] = R.raw.ios11_100_e;
        voice.get("ios11_100")[ 5] = R.raw.ios11_100_f;
        voice.get("ios11_100")[ 6] = R.raw.ios11_100_g;
        voice.get("ios11_100")[ 7] = R.raw.ios11_100_h;
        voice.get("ios11_100")[ 8] = R.raw.ios11_100_i;
        voice.get("ios11_100")[ 9] = R.raw.ios11_100_j;
        voice.get("ios11_100")[10] = R.raw.ios11_100_k;
        voice.get("ios11_100")[11] = R.raw.ios11_100_l;
        voice.get("ios11_100")[12] = R.raw.ios11_100_m;
        voice.get("ios11_100")[13] = R.raw.ios11_100_n;
        voice.get("ios11_100")[14] = R.raw.ios11_100_o;
        voice.get("ios11_100")[15] = R.raw.ios11_100_p;
        voice.get("ios11_100")[16] = R.raw.ios11_100_q;
        voice.get("ios11_100")[17] = R.raw.ios11_100_r;
        voice.get("ios11_100")[18] = R.raw.ios11_100_s;
        voice.get("ios11_100")[19] = R.raw.ios11_100_t;
        voice.get("ios11_100")[20] = R.raw.ios11_100_u;
        voice.get("ios11_100")[21] = R.raw.ios11_100_v;
        voice.get("ios11_100")[22] = R.raw.ios11_100_w;
        voice.get("ios11_100")[23] = R.raw.ios11_100_x;
        voice.get("ios11_100")[24] = R.raw.ios11_100_y;
        voice.get("ios11_100")[25] = R.raw.ios11_100_z;

        voice.get("ios11da")[0] = R.raw.ios11_da;

        voice.get("delete")[0] = R.raw.delete;
        voice.get("delete_word")[0] = R.raw.delete_word;

        voice.get("blank")[0] = R.raw.blank;
    }

    public void initButtons(){
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                switch(activity_mode) {
                    case KEYBOARD_MODE:{
                        stopInput();
                        finishWord();
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        break;
                    }
                    case FUZZY_INPUT_TEST_MODE:{
                        saveFuzzyInputTestData();
                        break;
                    }
                    default:
                }

            }
        });

        initModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(activity_mode){
                    case KEYBOARD_MODE:{
                        if (initMode == INIT_MODE_ABSOLUTE)
                            initMode = INIT_MODE_RELATIVE;
                        else if(initMode == INIT_MODE_RELATIVE)
                            initMode = INIT_MODE_NOTHING;
                        else
                            initMode = INIT_MODE_ABSOLUTE;
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        refresh();
                        break;
                    }
                    case FUZZY_INPUT_TEST_MODE:{
                        stopFuzzyInputTest();
                        break;
                    }
                    default:
                }

            }
        });

        speedmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(voiceSpeed>=60){
                    voiceSpeed -= 10;
                }
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                voiceSpeedText.setText(voiceSpeed+"");
            }
        });

        speedpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(activity_mode){
                    case KEYBOARD_MODE:{
                        if(voiceSpeed<=90){
                            voiceSpeed += 10;
                        }
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        voiceSpeedText.setText(voiceSpeed+"");
                        break;
                    }
                    case FUZZY_INPUT_TEST_MODE:{// 回退一轮
                        if(fuzzyInputTestFigerRecord.size()>fuzzyInputTestTurn)
                            fuzzyInputTestFigerRecord.remove(fuzzyInputTestTurn);//将本轮清空
                        if(fuzzyInputTestTurn>0)
                            fuzzyInputTestTurn--;
                        if(fuzzyInputTestFigerRecord.size()>fuzzyInputTestTurn)
                            fuzzyInputTestFigerRecord.remove(fuzzyInputTestTurn);//将上一轮清空
                        stopVoice();
                        if (fuzzyInputTestTurn < 10) {//10轮熟悉之后加快测试速度
                            playMedia("fuzzyInput", FUZZY_INPUT_SOUND_NEXT,false );
                        }
                        playMedia("google", fuzzyInputTestList.get(fuzzyInputTestTurn%26),true);
                        String nextChar = String.valueOf((char) ('A' + fuzzyInputTestList.get(fuzzyInputTestTurn%26)));
                        fuzzyInputTestCharShow.setText(String.valueOf(fuzzyInputTestTurn)+" "+nextChar);
                        progressBar.setProgress(fuzzyInputTestTurn * 100 / MAX_FUZZY_INPUT_TURN);
                        break;
                    }
                    default:
                }
            }
        });
    }

    public void playFirstVoice(){
        if (current == null && !myPlayList.isEmpty()){
            current = MediaPlayer.create(this, myPlayList.get(0));
            myPlayList.remove(0);
            current.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    current = null;
                    playFirstVoice();
                }
            });
            current.start();
        }
    }

    public void playMedia(String tag, int index, boolean isChar){
        myPlayList.add(voice.get(tag)[index]);
        playFirstVoice();
    }

    public double Normal(double x, double miu, double sigma){
        return 1.0 / Math.sqrt(2.0 * Math.PI) / sigma  * Math.pow(Math.E, -(x - miu) * (x - miu) / 2.0 / sigma / sigma);
    }

    public char getMostPossibleKey(int x, int y){
        ArrayList<Word> letters = new ArrayList<Word>();
        for (int i = 0; i < 26; ++i)
            letters.add(new Word((char)(i + 'a') + "", 0));

        ArrayList<Word> dict = new ArrayList<>();
        if (languageMode == LANG_MODE_ENG)
            dict = dict_eng;
        else if (languageMode == LANG_MODE_CHN)
            dict = dict_chn;

        for (int i = 0; i < dict.size(); ++i)
            if (dict.get(i).text.length() >= currentWord.length() + 1){
                boolean flag = true;
                Word word = dict.get(i);
                for (int j = 0; j < currentWord.length(); ++j)
                    if (word.text.charAt(j) != currentWord.charAt(j)){
                        flag = false;
                        break;
                    }
                if (flag){
                    for (int j = 0; j < letters.size(); ++j)
                        if (letters.get(j).text.charAt(0) == word.text.charAt(currentWord.length()))
                            letters.get(j).freq += word.freq;
                }
            }
        for (int i = 0; i < letters.size(); ++i){
            letters.get(i).freq += 0.01;
            int tmp = letters.get(i).text.charAt(0) - 'a';
            letters.get(i).freq *= Normal(x, autoKeyboard.keys[autoKeyboard.keyPos[tmp]].init_x, autoKeyboard.keys[0].init_width * SD_coefficient / 10);
            letters.get(i).freq *= Normal(y, autoKeyboard.keys[autoKeyboard.keyPos[tmp]].init_y, autoKeyboard.keys[0].init_width * SD_coefficient / 10);
        }
        Collections.sort(letters);
        return letters.get(0).text.charAt(0);
    }

    //if write=false, just return the most possible key
    public void addToSeq(char ch){
        if (ch != KEY_NOT_FOUND && (seq.size() == 0 || seq.get(seq.size() - 1) != ch)) {
            seq.add(ch);
            stopVoice();
            readList = "";
            nowCh = ch;
            playMedia("ios11_" + voiceSpeed, nowCh - 'a', true);
            readList += nowCh;
            refresh();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main_relative);

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status){
                if (status == textToSpeech.SUCCESS){
                    int result = textToSpeech.setLanguage(Locale.CHINA);
                    if (result != TextToSpeech.LANG_COUNTRY_AVAILABLE && result != TextToSpeech.LANG_AVAILABLE){
                        Toast.makeText(MainActivity.this, "TTS暂时不支持这种语音的朗读！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        keyboard = (ImageView)findViewById(R.id.keyboard);
        text = (TextView)findViewById(R.id.text);
        elapsedTimeText = (TextView)findViewById(R.id.elapsedtime);
        candidatesView = (TextView)findViewById(R.id.candidates);
        confirmButton = (Button)findViewById(R.id.confirm_button);
        readListView = (TextView)findViewById(R.id.readList);
        initModeButton = (Button)findViewById(R.id.init_mode_button);
        speedmButton = (Button)findViewById(R.id.speed_m_button);
        speedpButton = (Button)findViewById(R.id.speed_p_button);
        voiceSpeedText = (TextView)findViewById(R.id.voice_speed_text);
        fuzzyInputTestCharShow=(TextView)findViewById(R.id.fuzzyInputTestCharShow);
        fuzzyInputTestCharShow.setVisibility(View.GONE);
        progressBar=(ProgressBar)findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        listView=(ListView)findViewById(R.id.list_view);
        listView.setVisibility(View.GONE);
        ViewTreeObserver vto2 = keyboard.getViewTreeObserver();
        autoKeyboard=new AutoKeyboard(keyboard);
        vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                keyboard.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                autoKeyboard.drawLayout();
            }
        });
        initDict();
        initVoice();
        initButtons();
        initKeyboard();
    }
    @Override
    public void onDestroy(){
        if (current != null){
            current.release();
            current = null;
        }
        if (textToSpeech != null)
            textToSpeech.shutdown();
        super.onDestroy();
    }

    public void deleteLastChar() {
        if (currentWord.length() > 0) {
            currentWord = currentWord.substring(0, currentWord.length() - 1);
            currentBaseline = currentBaseline.substring(0, currentBaseline.length() - 1);
        }
        readList = "";
        predict(currentWord);
        refresh();
    }

    public void deleteAllChar(){
        currentWord = "";
        currentBaseline = "";
        readList = "";
        predict(currentWord);
        refresh();
    }

    int downX, downY, downX2, downY2;
    long downTime = 0, downTime2 = 0;
    long wordDownTime = 0;
    boolean TwoFingersFlag = false;
    long firstDownTime = 0, lastDownTime = 0; // used for check double-click
    final long STAY_TIME = 400;
    final int SLIP_DIST = 90;
    char upKey = KEY_NOT_FOUND;
    boolean isOverScreen=false;
    char lastChar='A';
    public boolean checkLeftwipe(int x, int y, long tempTime) {
        return x < downX - SLIP_DIST && Math.abs(downX - x) > Math.abs(downY - y) + 50 && tempTime < downTime + STAY_TIME;
    }
    public boolean checkRightwipe(int x, int y, long tempTime) {
        return x > downX + SLIP_DIST && Math.abs(downX - x) > Math.abs(downY - y) + 50 && tempTime < downTime + STAY_TIME;
    }
    public boolean checkUpwipe(int x, int y, long tempTime){
        return y < downY - SLIP_DIST && Math.abs(downY - y) > Math.abs(downX - x) + 50 && tempTime < downTime + STAY_TIME;
    }
    public boolean checkDownwipe(int x, int y, long tempTime){
        return y > downY + SLIP_DIST && Math.abs(downY - y) > Math.abs(downX - x) + 50 && tempTime < downTime + STAY_TIME;
    }
    public boolean checkUpwipe2(int x, int y, long tempTime){
        return y < downY2 - SLIP_DIST && Math.abs(downY2 - y) > Math.abs(downX2 - x) + 50 && tempTime < downTime2 + STAY_TIME;
    }
    public boolean onTouchEvent(MotionEvent event){
        int[] location = new int[2];
        keyboard.getLocationOnScreen(location);
        int x = (int)event.getX();
        int y = (int)event.getY();
        switch(activity_mode){
            case KEYBOARD_MODE:{
                if (event.getPointerCount() == 2){
                    TwoFingersFlag = true;
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_POINTER_DOWN:
                            downX2 = (int)event.getX(0);
                            downY2 = (int)event.getY(0);
                            downTime2 = event.getEventTime();
                            break;
                        case MotionEvent.ACTION_POINTER_UP:
                            stopInput();
                            if (checkUpwipe2((int)event.getX(0), (int)event.getY(0), event.getEventTime())){
                                if (currentCandidate < candidates.size()) {
                                    write("up2wipe");
                                    currentInput = candidates.get(currentCandidate).alias;
                                    refresh();
                                    textToSpeech.speak("确认输入 " + currentInput, TextToSpeech.QUEUE_ADD, null);
                                }
                            }
                            autoKeyboard.resetLayout();
                            autoKeyboard.drawLayout();
                            break;
                    }
                }
                if (!TwoFingersFlag && event.getPointerCount() == 1 && (autoKeyboard.getKeyByPosition(x, y - location[1], autoKeyboard.CURR_LAYOUT) == upKey
                        || autoKeyboard.getKeyByPosition(x, y - location[1], autoKeyboard.INIT_LAYOUT) != KEY_NOT_FOUND)){ // in the keyboard area
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_POINTER_DOWN:
                            write("down " + x + " " + y);
                            downX = x;
                            downY = y;
                            downTime = System.currentTimeMillis();
                            if (currentWord.length() == 0){
                                wordDownTime = downTime;
                            }
                            if (downTime - firstDownTime > 400){
                                firstDownTime = downTime;
                            }
                            else{
                                lastDownTime = downTime;
                            }
                            newPoint(x, y - location[1]);
                            //action down
                            break;

                        case MotionEvent.ACTION_MOVE:
                            write("move " + x + " " + y);
                            newPoint(x, y - location[1]);
                            //action move
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_POINTER_UP:
                            write("up " + x + " " + y);
                            long tempTime = System.currentTimeMillis();
                            elapsedTimeText.setText(String.valueOf(tempTime - wordDownTime));
                            stopInput();
                            if (confirmMode == CONFIRM_MODE_UP) {
                                if (checkLeftwipe(x, y, tempTime)) {
                                    deleteLastChar();
                                    write("leftwipe");
                                    currentCandidate = 0;
                                    textToSpeech.speak("删除", TextToSpeech.QUEUE_ADD, null);
                                    autoKeyboard.resetLayout();
                                    autoKeyboard.drawLayout();
                                } else if (checkRightwipe(x, y, tempTime)) {
                                    deleteAllChar();
                                    write("rightwipe");
                                    currentCandidate = 0;
                                    elapsedTimeText.setText("0");
                                    upKey = KEY_NOT_FOUND;
                                    textToSpeech.speak("清空", TextToSpeech.QUEUE_ADD, null);
                                    currentInput = "";
                                    refresh();
                                    autoKeyboard.resetLayout();
                                    autoKeyboard.drawLayout();
                                } else if (checkUpwipe(x, y, tempTime)) {
                                    if (currentCandidate > 0)
                                        --currentCandidate;
                                    autoKeyboard.resetLayout();
                                    autoKeyboard.drawLayout();
                                    //todo
                                } else if (checkDownwipe(x, y, tempTime)) {
                                    if (currentCandidate + 1 < candidates.size())
                                        ++currentCandidate;
                                    autoKeyboard.resetLayout();
                                    autoKeyboard.drawLayout();
                                    //todo
                                } else {
                                    currentCandidate = 0;
                                    upKey = autoKeyboard.getKeyByPosition(x, y - location[1], autoKeyboard.CURR_LAYOUT);
                                    //if (upvoiceMode == UPVOICE_MODE_YES && nowCh >= 'a' && nowCh <= 'z') {
                                    //    playMedia("ios11_" + voiceSpeed, nowCh - 'a', true);
                                    //}
                                    currentWord += nowCh;
                                    currentBaseline += autoKeyboard.getKeyByPosition(x, y - location[1],autoKeyboard.INIT_LAYOUT);
                                    predict(currentWord);
                                    write("enter " + nowCh);
                                    refresh();
                                }
                            }
                            else{
                                if (checkLeftwipe(x, y, tempTime)) {
                                    deleteLastChar();
                                    write("leftwipe");
                                    nowChSaved = '*';
                                    textToSpeech.speak("删除", TextToSpeech.QUEUE_ADD, null);
                                    autoKeyboard.resetLayout();
                                    autoKeyboard.drawLayout();
                                } else if (checkRightwipe(x, y, tempTime)) {
                                    deleteAllChar();
                                    write("rightwipe");
                                    elapsedTimeText.setText("0");
                                    upKey = KEY_NOT_FOUND;
                                    nowChSaved = '*';
                                    textToSpeech.speak("清空", TextToSpeech.QUEUE_ADD, null);
                                    autoKeyboard.resetLayout();
                                    autoKeyboard.drawLayout();
                                } else if (checkUpwipe(x, y, tempTime)) {
                                    //todo
                                } else if (checkDownwipe(x, y, tempTime)) {
                                    //todo
                                } else if (downTime == lastDownTime && tempTime - firstDownTime < 800) {
                                    //double click
                                    if (nowChSaved != '*'){
                                        write("doubleclick");
                                        currentWord += nowChSaved;
                                        if (upvoiceMode == UPVOICE_MODE_YES) {
                                            playMedia("ios11_" + voiceSpeed, nowChSaved - 'a', true);
                                        }
                                        else{
                                            playMedia("delete", 0, false);
                                        }
                                        currentBaseline += nowChBaselineSaved;
                                        predict(currentWord);
                                        refresh();
                                    }
                                }
                                else{
                                    if (tempTime - downTime > 300) {
                                        upKey = autoKeyboard.getKeyByPosition(x, y - location[1], autoKeyboard.CURR_LAYOUT);
                                        nowChSaved = nowCh;
                                        nowChBaselineSaved = autoKeyboard.getKeyByPosition(x, y - location[1],autoKeyboard.INIT_LAYOUT);
                                        write("enter " + nowCh);
                                    }
                                }
                            }
                            if (currentCandidate < candidates.size())
                                textToSpeech.speak(candidates.get(currentCandidate).alias, TextToSpeech.QUEUE_ADD, null);
                            write("word " + currentWord);
                            nowCh = 0;
                            break;
                    }
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP){
                    TwoFingersFlag = false;
                }
                break;
            }
            case FUZZY_INPUT_TEST_MODE: {
                y=y-location[1];
                if (fuzzyInputTestTurn < MAX_FUZZY_INPUT_TURN && autoKeyboard.inKeyboard(x,y,autoKeyboard.CURR_LAYOUT)&& isOverScreen==false) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN: {
                            char ch = autoKeyboard.getKeyByPosition(x,y,autoKeyboard.CURR_LAYOUT);
                            if(ch==KEY_NOT_FOUND){
                                isOverScreen=true;
                                break;
                            }
                            if(fuzzyInputTestTurn>=fuzzyInputTestFigerRecord.size()){
                                fuzzyInputTestFigerRecord.add(new Turn(fuzzyInputTestTurn,event.getDownTime()));
                            }
                            fuzzyInputTestFigerRecord.get(fuzzyInputTestTurn).addData(ch,x,y,MotionEvent.ACTION_DOWN,event.getEventTime());
                            stopVoice();
                            playMedia("ios11_"+voiceSpeed, ch - 'a',true);
                            lastChar=ch;
                            break;
                        }
                        case MotionEvent.ACTION_MOVE: {
                            char ch = autoKeyboard.getKeyByPosition(x,y,autoKeyboard.CURR_LAYOUT);
                            if(ch==KEY_NOT_FOUND){
                                break;
                            }
                            fuzzyInputTestFigerRecord.get(fuzzyInputTestTurn).addData(ch,x,y,MotionEvent.ACTION_MOVE,event.getEventTime());
                            if(ch!=lastChar){
                                stopVoice();
                                playMedia("ios11da", 0,false);
                                Vibrator vibrator =  (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                long[] pattern = {0, 30};
                                vibrator.vibrate(pattern, -1);
                                playMedia("ios11_"+voiceSpeed, ch - 'a',true);
                            }
                            lastChar=ch;
                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            char ch = autoKeyboard.getKeyByPosition(x,y,autoKeyboard.CURR_LAYOUT);
                            boolean isDone=fuzzyInputTestFigerRecord.get(fuzzyInputTestTurn).addData(ch,x,y,MotionEvent.ACTION_UP,event.getEventTime());
                            stopVoice();
                            if(isDone){
                                fuzzyInputTestTurn++;
                                if (fuzzyInputTestTurn >= MAX_FUZZY_INPUT_TURN) {
                                    fuzzyInputTestCharShow.setText("DONE!");
                                    playMedia("fuzzyInput", FUZZY_INPUT_SOUND_END,false);
                                    calculateFuzzyInputData();
                                    break;
                                }
                            }
                            if (fuzzyInputTestTurn < 10) {//10轮熟悉之后加快测试速度
                                playMedia("fuzzyInput", FUZZY_INPUT_SOUND_NEXT,false);
                            }
                            playMedia("google", fuzzyInputTestList.get(fuzzyInputTestTurn%26),true);
                            String nextChar = String.valueOf((char) ('A' + fuzzyInputTestList.get(fuzzyInputTestTurn%26)));
                            fuzzyInputTestCharShow.setText(String.valueOf(fuzzyInputTestTurn)+" "+nextChar);
                            progressBar.setProgress(fuzzyInputTestTurn * 100 / MAX_FUZZY_INPUT_TURN);
                            break;
                        }
                        default:
                    }
                }
                else if(isOverScreen||autoKeyboard.inKeyboard(x,y,autoKeyboard.CURR_LAYOUT)==false){
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN: {
                            isOverScreen=true;
                            break;
                        }
                        case MotionEvent.ACTION_UP:{
                            isOverScreen=false;
                            break;
                        }
                        default:
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    class Word implements Comparable<Word>{
        String text, alias;
        double freq;
        Word(String text, double freq){
            this.text = this.alias = text;
            this.freq = freq;
        }

        @Override
        public int compareTo(Word o){
            if (this.freq > o.freq) return -1;
            if (this.freq < o.freq) return 1;
            return 0;
        }
    }

    class AutoKeyboard{
        ImageView keyboard;
        Canvas canvas;
        Bitmap baseBitmap;
        Paint backgroundPaint;
        Paint textPaint;
        Paint linePaint;
        boolean Visibility=true;
        float screen_width_ratio = 1F;
        float screen_height_ratio = 1F;
        //Fuzzy Input Test Var
        float keyboardHeight;// the height of the keyboard
        float keyboardWidth;// the width of the keyboard
        float deltaY;// the distance between the first line and the upper bound of the keyboard
        float topThreshold;// the upper bound
        float bottomThreshold;// the lower bound
        float minWidth;// the minimum width of a key
        float minHetight;// the minimum height of a key
        int keyPos[];// the position of each key
        int[] location;// the coordinate of the left top corner of the keyboard
        int[] firstLine={0,1,2,3,4,5,6,7,8,9};// the index of the first line of the keyboard
        int[] secondLine={10,11,12,13,14,15,16,17,18};
        int[] thirdLine={19,20,21,22,23,24,25};
        int[] firstSecondLine={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18};
        int[] secondThirdLine={10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25};
        int[] firstThirdLine={0,1,2,3,4,5,6,7,8,9,19,20,21,22,23,24,25};
        int[] allLine={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25};
        final int TL=0;// top left key
        final int TM=5;// top middle key
        final int TR=9;// top right key
        final int ML=10;// middle left key
        final int MM=15;// middle middle key
        final int MR=18;// middle right key
        final int BL=19;// bottom left key
        final int BM=22;// bottom middle key
        final int BR=25;// bottom right key
        final int INIT_LAYOUT=0;
        final int CURR_LAYOUT=1;
        final int TEST_LAYOUT=2;
        final int BODILY_MOVEMENT=0;
        final int RESPECTIVELY_MOVEMENT=1;
        final int STRICT_MODE=1;
        final int LOOSE_MODE=0;
        final int LINEAR_MODE=0;
        final int EXPONENT_MODE=1;
        final int TOP=0;
        final int MIDDLE=1;
        final int BOTTOM=2;
        int scalingMode;// LINEAR_MODE EXPONENT_MODE
        double[] exponent_array={0.05,0.1,0.15,0.2,0.25,0.3,0.35,0.4,0.45,0.5};
        int exponent_index;
        float exponent;//
        int scalingNum;// left and shift scaling number of keys
        int try_layout_mode;// BODILY_MOVEMENT RESPECTIVELY_MOVEMENT
        int getKey_mode;// STRICT_MODE LOOSE_MODE
        double[] tap_range_array={0.95,0.90,0.85,0.80,0.75,0.70,0.65,0.60,0.55,0.50};
        float[][][] expRatio=new float[6][3][6];//scalingNum,Line,index
        int tap_range_index;
        float tap_range;
        class KEY{
            char ch;
            float init_x;
            float init_y;
            float curr_x;
            float curr_y;
            float test_x;
            float test_y;
            float init_width;
            float init_height;
            float curr_width;
            float curr_height;
            float test_width;
            float test_height;
            float getDist(float x,float y,int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                // mode==2 test_layout
                if(mode==INIT_LAYOUT){
                    return (init_x-x)*(init_x-x)+(init_y-y)*(init_y-y);
                }
                else if(mode==CURR_LAYOUT){
                    return (curr_x-x)*(curr_x-x)+(curr_y-y)*(curr_y-y);
                }else {
                    return (test_x-x)*(test_x-x)+(test_y-y)*(test_y-y);
                }
            }
            float getBottom(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_y+init_height/2;
                }else if(mode==CURR_LAYOUT){
                    return curr_y+curr_height/2;
                }else{
                    return test_y+test_height/2;
                }
            }
            float getTop(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_y-init_height/2;
                }else if(mode==CURR_LAYOUT){
                    return curr_y-curr_height/2;
                }else{
                    return test_y-test_height/2;
                }
            }
            float getLeft(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_x-init_width/2;
                }else if(mode==CURR_LAYOUT){
                    return curr_x-curr_width/2;
                }else{
                    return test_x-test_width/2;
                }
            }
            float getRight(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_x+init_width/2;
                }else if(mode==CURR_LAYOUT){
                    return curr_x+curr_width/2;
                }else{
                    return test_x+test_width/2;
                }
            }
            boolean contain(float x,float y,int mode){
                return x>=getLeft(mode)&&x<=getRight(mode)&&y>=getTop(mode)&&y<=getBottom(mode);
            }
            int contain_tap(float x,float y,int mode){
                int[][] quadrant={{1,2,3},{4,5,6},{7,8,9}};
                // 1: left top
                // 2: moddle top
                // 3: right top
                // 4: left middle
                // 5: middle middle
                // 6: right middle
                // 7: left bottom
                // 8: middle bottom
                // 9: right bottom
                int row=0;
                int col=0;
                if(x<getLeft_tap(mode)){
                    col=0;
                }else if(x>getRight_tap(mode)){
                    col=2;
                }else{
                    col=1;
                }
                if(y<getTop_tap(mode)){
                    row=0;
                }else if(y>getBottom_tap(mode)){
                    row=2;
                }else{
                    row=1;
                }
                return quadrant[row][col];

            }
            float getBottom_tap(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_y+init_height*tap_range/2;
                }else if(mode==CURR_LAYOUT){
                    return curr_y+curr_height*tap_range/2;
                }else{
                    return test_y+test_height*tap_range/2;
                }
            }
            float getTop_tap(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_y-init_height*tap_range/2;
                }else if(mode==CURR_LAYOUT){
                    return curr_y-curr_height*tap_range/2;
                }else{
                    return test_y-test_height*tap_range/2;
                }
            }
            float getLeft_tap(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_x-init_width*tap_range/2;
                }else if(mode==CURR_LAYOUT){
                    return curr_x-curr_width*tap_range/2;
                }else{
                    return test_x-test_width*tap_range/2;
                }
            }
            float getRight_tap(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_x+init_width*tap_range/2;
                }else if(mode==CURR_LAYOUT){
                    return curr_x+curr_width*tap_range/2;
                }else{
                    return test_x+test_width*tap_range/2;
                }
            }

            KEY(){
                ch='a';
                init_x=0;
                init_y=0;
                init_height=0;
                init_width=0;
                curr_height=0;
                curr_width=0;
                curr_x=0;
                curr_y=0;
                test_height=0;
                test_width=0;
                test_x=0;
                test_y=0;
            }

        }
        KEY keys[];
        void defaultPara(){// set the parameters to default value
            keyboardHeight=680;
            keyboardWidth=1440;
            deltaY=0;
            topThreshold=0;
            bottomThreshold=680;
            minWidth=72;
            minHetight=110;
            scalingNum=3;
            try_layout_mode=RESPECTIVELY_MOVEMENT;
            getKey_mode=LOOSE_MODE;
            tap_range_index=3;
            tap_range=(float)tap_range_array[tap_range_index];
            scalingMode=LINEAR_MODE;
            exponent_index=1;
            exponent=(float)exponent_array[exponent_index];
            calExpRatio();
        }
        void calExpRatio(){
            float e_0=1;
            float e_1=(float)Math.exp(exponent);
            float e_2=(float)Math.exp(exponent*2);
            float e_3=(float)Math.exp(exponent*3);
            float e_4=(float)Math.exp(exponent*4);
            final int SCALINGNUM_1=1;
            final int SCALINGNUM_2=2;
            final int SCALINGNUM_3=3;
            final int SCALINGNUM_4=4;
            final int SCALINGNUM_5=5;
            expRatio[SCALINGNUM_1][TOP][0]=1;

            expRatio[SCALINGNUM_2][TOP][0]=e_1/(e_0+e_1);
            expRatio[SCALINGNUM_2][TOP][1]=e_0/(e_0+e_1);

            expRatio[SCALINGNUM_3][TOP][0]=e_2/(e_0+e_1+e_2);
            expRatio[SCALINGNUM_3][TOP][1]=e_1/(e_0+e_1+e_2);
            expRatio[SCALINGNUM_3][TOP][2]=e_0/(e_0+e_1+e_2);

            expRatio[SCALINGNUM_4][TOP][0]=e_3/(e_0+e_1+e_2+e_3);
            expRatio[SCALINGNUM_4][TOP][1]=e_2/(e_0+e_1+e_2+e_3);
            expRatio[SCALINGNUM_4][TOP][2]=e_1/(e_0+e_1+e_2+e_3);
            expRatio[SCALINGNUM_4][TOP][3]=e_0/(e_0+e_1+e_2+e_3);

            expRatio[SCALINGNUM_5][TOP][0]=e_4/(e_0+e_1+e_2+e_3+e_4);
            expRatio[SCALINGNUM_5][TOP][1]=e_3/(e_0+e_1+e_2+e_3+e_4);
            expRatio[SCALINGNUM_5][TOP][2]=e_2/(e_0+e_1+e_2+e_3+e_4);
            expRatio[SCALINGNUM_5][TOP][3]=e_1/(e_0+e_1+e_2+e_3+e_4);
            expRatio[SCALINGNUM_5][TOP][4]=e_0/(e_0+e_1+e_2+e_3+e_4);

            expRatio[SCALINGNUM_1][MIDDLE][0]=(float)(2.0/3.0);
            expRatio[SCALINGNUM_1][MIDDLE][1]=(float)(1.0/3.0);

            expRatio[SCALINGNUM_2][MIDDLE][0]=e_1/(e_0+e_1+e_0/2);
            expRatio[SCALINGNUM_2][MIDDLE][1]=e_0/(e_0+e_1+e_0/2);
            expRatio[SCALINGNUM_2][MIDDLE][2]=e_0/2/(e_0+e_1+e_0/2);

            expRatio[SCALINGNUM_3][MIDDLE][0]=e_2/(e_0+e_1+e_2+e_0/2);
            expRatio[SCALINGNUM_3][MIDDLE][1]=e_1/(e_0+e_1+e_2+e_0/2);
            expRatio[SCALINGNUM_3][MIDDLE][2]=e_0/(e_0+e_1+e_2+e_0/2);
            expRatio[SCALINGNUM_3][MIDDLE][3]=e_0/2/(e_0+e_1+e_2+e_0/2);

            expRatio[SCALINGNUM_4][MIDDLE][0]=e_3/(e_0+e_1+e_2+e_3+e_0/2);
            expRatio[SCALINGNUM_4][MIDDLE][1]=e_2/(e_0+e_1+e_2+e_3+e_0/2);
            expRatio[SCALINGNUM_4][MIDDLE][2]=e_1/(e_0+e_1+e_2+e_3+e_0/2);
            expRatio[SCALINGNUM_4][MIDDLE][3]=e_0/(e_0+e_1+e_2+e_3+e_0/2);
            expRatio[SCALINGNUM_4][MIDDLE][4]=e_0/2/(e_0+e_1+e_2+e_3+e_0/2);

            expRatio[SCALINGNUM_5][MIDDLE][0]=e_4/(e_0+e_1+e_2+e_3+e_4+e_0/2);
            expRatio[SCALINGNUM_5][MIDDLE][1]=e_3/(e_0+e_1+e_2+e_3+e_4+e_0/2);
            expRatio[SCALINGNUM_5][MIDDLE][2]=e_2/(e_0+e_1+e_2+e_3+e_4+e_0/2);
            expRatio[SCALINGNUM_5][MIDDLE][3]=e_1/(e_0+e_1+e_2+e_3+e_4+e_0/2);
            expRatio[SCALINGNUM_5][MIDDLE][4]=e_0/(e_0+e_1+e_2+e_3+e_4+e_0/2);
            expRatio[SCALINGNUM_5][MIDDLE][5]=e_0/2/(e_0+e_1+e_2+e_3+e_4+e_0/2);

            expRatio[SCALINGNUM_1][BOTTOM][0]=(float)(2.0/3.0);
            expRatio[SCALINGNUM_1][BOTTOM][1]=(float)(1.0/3.0);

            expRatio[SCALINGNUM_2][BOTTOM][0]=e_1/(e_0+e_1+e_0*3/2);
            expRatio[SCALINGNUM_2][BOTTOM][1]=e_0/(e_0+e_1+e_0*3/2);
            expRatio[SCALINGNUM_2][BOTTOM][2]=e_0*3/2/(e_0+e_1+e_0*3/2);

            expRatio[SCALINGNUM_3][BOTTOM][0]=e_2/(e_0+e_1+e_2+e_0*3/2);
            expRatio[SCALINGNUM_3][BOTTOM][1]=e_1/(e_0+e_1+e_2+e_0*3/2);
            expRatio[SCALINGNUM_3][BOTTOM][2]=e_0/(e_0+e_1+e_2+e_0*3/2);
            expRatio[SCALINGNUM_3][BOTTOM][3]=e_0*3/2/(e_0+e_1+e_2+e_0*3/2);

            expRatio[SCALINGNUM_4][BOTTOM][0]=e_3/(e_0+e_1+e_2+e_3+e_0*3/2);
            expRatio[SCALINGNUM_4][BOTTOM][1]=e_2/(e_0+e_1+e_2+e_3+e_0*3/2);
            expRatio[SCALINGNUM_4][BOTTOM][2]=e_1/(e_0+e_1+e_2+e_3+e_0*3/2);
            expRatio[SCALINGNUM_4][BOTTOM][3]=e_0/(e_0+e_1+e_2+e_3+e_0*3/2);
            expRatio[SCALINGNUM_4][BOTTOM][4]=e_0*3/2/(e_0+e_1+e_2+e_3+e_0*3/2);

            expRatio[SCALINGNUM_5][BOTTOM][0]=e_4/(e_0+e_1+e_2+e_3+e_4+e_0*3/2);
            expRatio[SCALINGNUM_5][BOTTOM][1]=e_3/(e_0+e_1+e_2+e_3+e_4+e_0*3/2);
            expRatio[SCALINGNUM_5][BOTTOM][2]=e_2/(e_0+e_1+e_2+e_3+e_4+e_0*3/2);
            expRatio[SCALINGNUM_5][BOTTOM][3]=e_1/(e_0+e_1+e_2+e_3+e_4+e_0*3/2);
            expRatio[SCALINGNUM_5][BOTTOM][4]=e_0/(e_0+e_1+e_2+e_3+e_4+e_0*3/2);
            expRatio[SCALINGNUM_5][BOTTOM][5]=e_0*3/2/(e_0+e_1+e_2+e_3+e_4+e_0*3/2);

        }
        float getBottom(int mode){
            return this.keys[BR].getBottom(mode);
        }
        float getTop(int mode){
            return this.keys[TL].getTop(mode);
        }
        float getLeft(int mode){
            return this.keys[TL].getLeft(mode);
        }
        float getRight(int mode){
            return this.keys[TR].getRight(mode);
        }
        void setKeyboardHeight(float newkeyBoardHeight){
            this.keyboardHeight=newkeyBoardHeight*screen_height_ratio;
        }
        void setKeyboardWidth(float newkeyBoardWidth){
            this.keyboardWidth=newkeyBoardWidth*screen_width_ratio;
        }
        void setTopThreshold(float newTopThreshold){
            this.topThreshold=newTopThreshold*screen_height_ratio;
        }
        void setBottomThreshold(float newBottomThreshold){
            this.bottomThreshold=newBottomThreshold*screen_height_ratio;
        }
        void setMinWidth(float newMinWidth){
            this.minWidth=newMinWidth*screen_width_ratio;
        }
        void setMinHetight(float newMinHeight){
            this.minHetight=newMinHeight*screen_height_ratio;
        }
        void getScreenSizeRatio(){
            DisplayMetrics metrics =new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            this.screen_width_ratio = metrics.widthPixels/1440F;
            this.screen_height_ratio = metrics.heightPixels/2560F;
        }
        boolean inKeyboard(float x,float y, int mode){
            return x>=getLeft(mode)&&x<=getRight(mode)&&y>=getTop(mode)&&y<=getBottom(mode);
        }
        public char getKeyByPosition(float x, float y, int mode){
            // mode==0 init_layout
            // mode==1 current_layout
            char key = KEY_NOT_FOUND;
            if(y<topThreshold || y>bottomThreshold)
                return key;
            float min_dist = Float.MAX_VALUE;
            for (int i: allLine){
                float dist_temp=keys[i].getDist(x,y,mode);
                if (dist_temp<min_dist){
                    key=keys[i].ch;
                    min_dist=dist_temp;
                }
            }
            if(getKey_mode==LOOSE_MODE){
                return key;
            }else if(key!=KEY_NOT_FOUND && getKey_mode==STRICT_MODE){
                int pos = this.keyPos[key-'a'];
                if(this.keys[pos].contain(x,y,mode)){
                    return key;
                }else{
                    return KEY_NOT_FOUND;
                }
            }else{
                return key;
            }

        };
        boolean shift_y(int pos,float dY){
            // mode==0 bodily movement
            // mode==1 respectively movement
            if(dY==0){
                for(int i:allLine){
                    this.keys[i].curr_y=this.keys[i].init_y;
                    this.keys[i].curr_height=this.keys[i].init_height;
                }
                return true;
            }
            else if(dY>0){// downward movement
                if(this.keys[BM].getBottom(INIT_LAYOUT)+dY>this.bottomThreshold){// compress
                    if(pos>=BL) // in the third line
                        return false;
                    else if(pos>=ML){//in the second line
                        for (int i: firstSecondLine){
                            this.keys[i].test_y=this.keys[i].init_y+dY;
                            this.keys[i].test_height=this.keys[i].init_height;
                        }
                        float bottomHeight=this.bottomThreshold-this.keys[MM].getBottom(TEST_LAYOUT);
                        for (int i: thirdLine){
                            this.keys[i].test_y=this.bottomThreshold-bottomHeight/2;
                            this.keys[i].test_height=bottomHeight;
                        }
                    }else{// in the first line
                        for (int i:firstLine){
                            this.keys[i].test_y=this.keys[i].init_y+dY;
                            this.keys[i].test_height=this.keys[i].init_height;
                        }
                        float bottomHeight=this.bottomThreshold-this.keys[TM].getBottom(TEST_LAYOUT);
                        for (int i:secondLine){
                            this.keys[i].test_y=this.bottomThreshold-bottomHeight*3/4;
                            this.keys[i].test_height=bottomHeight/2;
                        }
                        for (int i: thirdLine){
                            this.keys[i].test_y=this.bottomThreshold-bottomHeight/4;
                            this.keys[i].test_height=bottomHeight/2;
                        }
                    }
                }
                else{// no compress
                    for (int i:allLine){
                        this.keys[i].test_y=this.keys[i].init_y+dY;
                        this.keys[i].test_height=this.keys[i].init_height;
                    }
                }
            }else{// upward movement
                if(this.keys[TM].getTop(INIT_LAYOUT)+dY<this.topThreshold){// compress
                    if(pos<=TR)// compress nothing
                        return false;
                    else if(pos<=MR){// compress the first line
                        for (int i:secondThirdLine){
                            this.keys[i].test_y=this.keys[i].init_y+dY;
                            this.keys[i].test_height=this.keys[i].init_height;
                        }
                        float topHeight=this.keys[MM].getTop(TEST_LAYOUT);
                        for (int i:firstLine){
                            this.keys[i].test_y=topHeight/2;
                            this.keys[i].test_height=topHeight;
                        }
                    }else{//compress the first and the second line
                        for (int i:thirdLine){
                            this.keys[i].test_y=this.keys[i].init_y+dY;
                            this.keys[i].test_height=this.keys[i].init_height;
                        }
                        float topHeight=this.keys[BM].getTop(TEST_LAYOUT);
                        for (int i:firstLine){
                            this.keys[i].test_y=topHeight/4;
                            this.keys[i].test_height=topHeight/2;
                        }
                        for (int i:secondLine){
                            this.keys[i].test_y=topHeight*3/4;
                            this.keys[i].test_height=topHeight/2;
                        }
                    }
                }else{// no compress
                    for (int i:allLine){
                        this.keys[i].test_y=this.keys[i].init_y+dY;
                        this.keys[i].test_height=this.keys[i].init_height;
                    }
                }
            }

            // check the height of each key
            for (int i:allLine){
                if(this.keys[i].test_height<this.minHetight){
                    return false;
                }
            }
            for (int i:allLine){
                this.keys[i].curr_y=this.keys[i].test_y;
                this.keys[i].curr_height=this.keys[i].test_height;
            }
            return true;

        }
        boolean shift_x(int pos,float dX){
            // mode==0 bodily movement
            // mode==1 respectively movement
            if(dX==0){
                for(int i:allLine){
                    this.keys[i].curr_x=this.keys[i].init_x;
                    this.keys[i].curr_width=this.keys[i].init_width;
                }
                return true;
            }
            if(try_layout_mode==BODILY_MOVEMENT){
                // BODILY MOVEMENT
                if(dX>=0) {// right shift
                    if (pos ==TR || pos == MR || pos == BR)
                        return false;
                }else {// left shift
                    if (pos == TL || pos == ML || pos == BL)
                        return false;
                }

                if(pos<=TR){
                    // left shift the first line
                    int begin=Math.min(pos,TL+scalingNum);
                    int end=Math.max(pos,TR-scalingNum);
                    // move the first line
                    for(int i=begin;i<=end;i++){
                        this.keys[i].test_x=this.keys[i].init_x+dX;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }
                    // scale the first line
                    if(scalingMode==LINEAR_MODE){
                        float leftRatio=this.keys[begin].getLeft(TEST_LAYOUT)/this.keys[begin].getLeft(INIT_LAYOUT);
                        for(int i=TL;i<begin;i++) {
                            this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                            this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                        }
                        float rightRatio=(keyboardWidth-this.keys[end].getRight(TEST_LAYOUT))/(keyboardWidth-this.keys[end].getRight(INIT_LAYOUT));
                        for (int i=end+1;i<=TR;i++){
                            this.keys[i].test_x=keyboardWidth-(keyboardWidth-this.keys[i].init_x)*rightRatio;
                            this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                        }
                    }else{
                        int leftScalingNum=begin-TL;
                        int rightScalingNum=TR-end;
                        if(leftScalingNum>scalingNum || rightScalingNum>scalingNum || leftScalingNum<=0||rightScalingNum<=0){
                            Log.e("scaling num error","top pos:"+String.valueOf(pos));
                        }
                        float leftLength=this.keys[begin].getLeft(TEST_LAYOUT);
                        float rightLength=keyboardWidth-this.keys[end].getRight(TEST_LAYOUT);
                        for (int i=begin-1;i>=TL;i--){
                            int index=begin-1-i;
                            this.keys[i].test_width=leftLength*expRatio[leftScalingNum][TOP][index];
                            this.keys[i].test_x=this.keys[i+1].getLeft(TEST_LAYOUT)-this.keys[i].test_width/2;
                        }
                        for (int i=end+1;i<=TR;i++){
                            int index=i-end-1;
                            this.keys[i].test_width=rightLength*expRatio[rightScalingNum][TOP][index];
                            this.keys[i].test_x=this.keys[i-1].getRight(TEST_LAYOUT)+this.keys[i].test_width/2;
                        }
                    }
                    // match the second line
                    for (int i:secondLine){
                        this.keys[i].test_x=(this.keys[i-10].test_x+this.keys[i-9].test_x)/2;
                        this.keys[i].test_width=this.keys[i-9].test_x-this.keys[i-10].test_x;
                    }
                    // match the third line
                    for (int i:thirdLine){
                        this.keys[i].test_x=this.keys[i-8].test_x;
                        this.keys[i].test_width=this.keys[i-8].test_width;
                    }

                }else if(pos<=MR){// left shift the second line
                    //this.keys[pos].test_x=x;
                    // this.keys[pos].test_width=this.keys[pos].init_width;

                    int begin=Math.min(pos,ML+scalingNum);
                    int end=Math.max(pos,MR-scalingNum);
                    // move the second line
                    for(int i=begin;i<=end;i++){
                        this.keys[i].test_x=this.keys[i].init_x+dX;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }

                    // scale the second line
                    if(scalingMode==LINEAR_MODE){
                        float leftRatio=this.keys[begin].getLeft(TEST_LAYOUT)/this.keys[begin].getLeft(INIT_LAYOUT);
                        for(int i=ML;i<begin;i++) {
                            this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                            this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                        }
                        float rightRatio=(keyboardWidth-this.keys[end].getRight(TEST_LAYOUT))/(keyboardWidth-this.keys[end].getRight(INIT_LAYOUT));
                        for (int i=end+1;i<=MR;i++){
                            this.keys[i].test_x=keyboardWidth-(keyboardWidth-this.keys[i].init_x)*rightRatio;
                            this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                        }
                    }else{
                        int leftScalingNum=begin-ML;
                        int rightScalingNum=MR-end;
                        if(leftScalingNum>scalingNum || rightScalingNum>scalingNum || leftScalingNum<=0||rightScalingNum<=0){
                            Log.e("scaling num error","middle pos:"+String.valueOf(pos));
                        }
                        float leftLength=this.keys[begin].getLeft(TEST_LAYOUT);
                        float rightLength=keyboardWidth-this.keys[end].getRight(TEST_LAYOUT);
                        for (int i=begin-1;i>=ML;i--){
                            int index=begin-1-i;
                            this.keys[i].test_width=leftLength*expRatio[leftScalingNum][MIDDLE][index];
                            this.keys[i].test_x=this.keys[i+1].getLeft(TEST_LAYOUT)-this.keys[i].test_width/2;
                        }
                        for (int i=end+1;i<=MR;i++){
                            int index=i-end-1;
                            this.keys[i].test_width=rightLength*expRatio[rightScalingNum][MIDDLE][index];
                            this.keys[i].test_x=this.keys[i-1].getRight(TEST_LAYOUT)+this.keys[i].test_width/2;
                        }
                    }

                    //  match the first line
                    this.keys[TL].test_x=this.keys[ML].test_x/2;
                    this.keys[TL].test_width=this.keys[ML].test_x;
                    this.keys[TR].test_width=this.keyboardWidth-this.keys[MR].test_x;
                    this.keys[TR].test_x=this.keyboardWidth-this.keys[MR].test_width/2;
                    for (int i=TL+1;i<TR;i++){
                        this.keys[i].test_x=(this.keys[i+9].test_x+this.keys[i+10].test_x)/2;
                        this.keys[i].test_width=this.keys[i+10].test_x-this.keys[i+9].test_x;
                    }
                    // match the third line
                    for (int i:thirdLine){
                        this.keys[i].test_x=this.keys[i-8].test_x;
                        this.keys[i].test_width=this.keys[i-8].test_width;
                    }
                }else{
                    // left shift the third line
                    int begin=Math.min(pos,BL+scalingNum);
                    int end=Math.max(pos,BR-scalingNum);

                    if(scalingMode==LINEAR_MODE){
                        // move the third line
                        for(int i=begin;i<=end;i++){
                            this.keys[i].test_x=this.keys[i].init_x+dX;
                            this.keys[i].test_width=this.keys[i].init_width;
                        }
                        // scale the third line
                        float leftRatio=this.keys[begin].getLeft(TEST_LAYOUT)/this.keys[begin].getLeft(INIT_LAYOUT);
                        for(int i=BL;i<begin;i++) {
                            this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                            this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                        }
                        float rightRatio=(keyboardWidth-this.keys[end].getRight(TEST_LAYOUT))/(keyboardWidth-this.keys[end].getRight(INIT_LAYOUT));
                        for (int i=end+1;i<=BR;i++){
                            this.keys[i].test_x=keyboardWidth-(keyboardWidth-this.keys[i].init_x)*rightRatio;
                            this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                        }
                        // match the second line
                        for(int i=ML+1;i<=MR-1;i++){
                            this.keys[i].test_x=this.keys[i+8].test_x;
                            this.keys[i].test_width=this.keys[i+8].test_width;
                        }
                        this.keys[ML].test_width=this.keys[ML].init_width*leftRatio;
                        this.keys[ML].test_x=this.keys[ML].init_x*leftRatio;
                        this.keys[MR].test_x=keyboardWidth-(keyboardWidth-this.keys[MR].init_x)*rightRatio;
                        this.keys[MR].test_width=this.keys[MR].init_width*rightRatio;
                    }else{
                        begin=begin-8;
                        end=end-8;
                        // move the second line
                        for(int i=begin;i<=end;i++){
                            this.keys[i].test_x=this.keys[i].init_x+dX;
                            this.keys[i].test_width=this.keys[i].init_width;
                        }
                        // scale the second line
                        int leftScalingNum=begin-ML;
                        int rightScalingNum=MR-end;
                        if(leftScalingNum>scalingNum || rightScalingNum>scalingNum || leftScalingNum<=0||rightScalingNum<=0){
                            Log.e("scaling num error","middle pos:"+String.valueOf(pos));
                        }
                        float leftLength=this.keys[begin].getLeft(TEST_LAYOUT);
                        float rightLength=keyboardWidth-this.keys[end].getRight(TEST_LAYOUT);
                        for (int i=begin-1;i>=ML;i--){
                            int index=begin-1-i;
                            this.keys[i].test_width=leftLength*expRatio[leftScalingNum][MIDDLE][index];
                            this.keys[i].test_x=this.keys[i+1].getLeft(TEST_LAYOUT)-this.keys[i].test_width/2;
                        }
                        for (int i=end+1;i<=MR;i++){
                            int index=i-end-1;
                            this.keys[i].test_width=rightLength*expRatio[rightScalingNum][MIDDLE][index];
                            this.keys[i].test_x=this.keys[i-1].getRight(TEST_LAYOUT)+this.keys[i].test_width/2;
                        }

                        // match the third line
                        for (int i:thirdLine){
                            this.keys[i].test_x=this.keys[i-8].test_x;
                            this.keys[i].test_width=this.keys[i-8].test_width;
                        }
                    }


                    // match first line
                    this.keys[TL].test_x=this.keys[ML].test_x/2;
                    this.keys[TL].test_width=this.keys[ML].test_x;
                    this.keys[TR].test_width=this.keyboardWidth-this.keys[MR].test_x;
                    this.keys[TR].test_x=this.keyboardWidth-this.keys[MR].test_width/2;
                    for (int i=TL+1;i<TR;i++){
                        this.keys[i].test_x=(this.keys[i+9].test_x+this.keys[i+10].test_x)/2;
                        this.keys[i].test_width=this.keys[i+10].test_x-this.keys[i+9].test_x;
                    }
                }

                // check the width of each key
                for (int i:allLine){
                    if(this.keys[i].test_width<this.minWidth){
                        return false;
                    }
                }
                for (int i:allLine){
                    this.keys[i].curr_x=this.keys[i].test_x;
                    this.keys[i].curr_width=this.keys[i].test_width;
                }
                return true;
            }else{
                // RESPECTIVELY MOVEMENT
                if(pos<=TR){// left shift the first line
                    if (pos ==TR && dX>=0)
                        return false;
                    else if (pos ==TL && dX<0)
                        return false;
                    else{
                        int begin=Math.min(pos,TL+scalingNum);
                        int end=Math.max(pos,TR-scalingNum);
                        // move the first line
                        for(int i=begin;i<=end;i++){
                            this.keys[i].test_x=this.keys[i].init_x+dX;
                            this.keys[i].test_width=this.keys[i].init_width;
                        }
                        // scale the first line
                        if(scalingMode==LINEAR_MODE){
                            float leftRatio=this.keys[begin].getLeft(TEST_LAYOUT)/this.keys[begin].getLeft(INIT_LAYOUT);
                            for(int i=TL;i<begin;i++) {
                                this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                                this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                            }
                            float rightRatio=(keyboardWidth-this.keys[end].getRight(TEST_LAYOUT))/(keyboardWidth-this.keys[end].getRight(INIT_LAYOUT));
                            for (int i=end+1;i<=TR;i++){
                                this.keys[i].test_x=keyboardWidth-(keyboardWidth-this.keys[i].init_x)*rightRatio;
                                this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                            }
                        }else{
                            int leftScalingNum=begin-TL;
                            int rightScalingNum=TR-end;
                            if(leftScalingNum>scalingNum || rightScalingNum>scalingNum || leftScalingNum<=0||rightScalingNum<=0){
                                Log.e("scaling num error","top pos:"+String.valueOf(pos));
                            }
                            float leftLength=this.keys[begin].getLeft(TEST_LAYOUT);
                            float rightLength=keyboardWidth-this.keys[end].getRight(TEST_LAYOUT);
                            for (int i=begin-1;i>=TL;i--){
                                int index=begin-1-i;
                                this.keys[i].test_width=leftLength*expRatio[leftScalingNum][TOP][index];
                                this.keys[i].test_x=this.keys[i+1].getLeft(TEST_LAYOUT)-this.keys[i].test_width/2;
                            }
                            for (int i=end+1;i<=TR;i++){
                                int index=i-end-1;
                                this.keys[i].test_width=rightLength*expRatio[rightScalingNum][TOP][index];
                                this.keys[i].test_x=this.keys[i-1].getRight(TEST_LAYOUT)+this.keys[i].test_width/2;
                            }
                        }
                    }
                    // reset the second and the third line
                    for(int i:secondThirdLine){
                        this.keys[i].test_x=this.keys[i].init_x;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }
                }else if(pos<=MR){// left shift the second line
                    //this.keys[pos].test_x=x;
                    // this.keys[pos].test_width=this.keys[pos].init_width;
                    if(pos==MR && dX>=0) {
                        if (this.keys[MR].getRight(INIT_LAYOUT) + dX > this.keyboardWidth) {
                            return false;
                        } else {
                            for (int i : secondLine) {
                                this.keys[i].test_x = this.keys[i].init_x + dX;
                                this.keys[i].test_width = this.keys[i].init_width;
                            }
                        }
                    }
                    else if(pos==ML && dX<0) {
                        if (this.keys[ML].getLeft(INIT_LAYOUT) + dX < 0) {
                            return false;
                        } else {
                            for (int i : secondLine) {
                                this.keys[i].test_x = this.keys[i].init_x + dX;
                                this.keys[i].test_width = this.keys[i].init_width;
                            }
                        }
                    }
                    else{
                        int begin=Math.min(pos,ML+scalingNum);
                        int end=Math.max(pos,MR-scalingNum);
                        // move the second line
                        for(int i=begin;i<=end;i++){
                            this.keys[i].test_x=this.keys[i].init_x+dX;
                            this.keys[i].test_width=this.keys[i].init_width;
                        }

                        // scale the second line
                        if(scalingMode==LINEAR_MODE){
                            float leftRatio=this.keys[begin].getLeft(TEST_LAYOUT)/this.keys[begin].getLeft(INIT_LAYOUT);
                            for(int i=ML;i<begin;i++) {
                                this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                                this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                            }
                            float rightRatio=(keyboardWidth-this.keys[end].getRight(TEST_LAYOUT))/(keyboardWidth-this.keys[end].getRight(INIT_LAYOUT));
                            for (int i=end+1;i<=MR;i++){
                                this.keys[i].test_x=keyboardWidth-(keyboardWidth-this.keys[i].init_x)*rightRatio;
                                this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                            }
                        }else{
                            int leftScalingNum=begin-ML;
                            int rightScalingNum=MR-end;
                            if(leftScalingNum>scalingNum || rightScalingNum>scalingNum || leftScalingNum<=0||rightScalingNum<=0){
                                Log.e("scaling num error","middle pos:"+String.valueOf(pos));
                            }
                            float leftLength=this.keys[begin].getLeft(TEST_LAYOUT);
                            float rightLength=keyboardWidth-this.keys[end].getRight(TEST_LAYOUT);
                            for (int i=begin-1;i>=ML;i--){
                                int index=begin-1-i;
                                this.keys[i].test_width=leftLength*expRatio[leftScalingNum][MIDDLE][index];
                                this.keys[i].test_x=this.keys[i+1].getLeft(TEST_LAYOUT)-this.keys[i].test_width/2;
                            }
                            for (int i=end+1;i<=MR;i++){
                                int index=i-end-1;
                                this.keys[i].test_width=rightLength*expRatio[rightScalingNum][MIDDLE][index];
                                this.keys[i].test_x=this.keys[i-1].getRight(TEST_LAYOUT)+this.keys[i].test_width/2;
                            }
                        }
                    }

                    // reset the first and the third line
                    for(int i:firstThirdLine){
                        this.keys[i].test_x=this.keys[i].init_x;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }
                }else{// left shift the third line
                    if(pos==BR && dX>=0){
                        if(this.keys[BR].getRight(INIT_LAYOUT)+dX>this.keyboardWidth){
                            return false;
                        }else{
                            for (int i:thirdLine){
                                this.keys[i].test_x=this.keys[i].init_x+dX;
                                this.keys[i].test_width=this.keys[i].init_width;
                            }
                        }
                    }else if(pos==BL && dX<0){
                        if(this.keys[BL].getLeft(INIT_LAYOUT)+dX<0){
                            return false;
                        }else{
                            for (int i:thirdLine){
                                this.keys[i].test_x=this.keys[i].init_x+dX;
                                this.keys[i].test_width=this.keys[i].init_width;
                            }
                        }
                    }
                    else{
                        int begin=Math.min(pos,BL+scalingNum);
                        int end=Math.max(pos,BR-scalingNum);
                        // move the third line
                        for(int i=begin;i<=end;i++){
                            this.keys[i].test_x=this.keys[i].init_x+dX;
                            this.keys[i].test_width=this.keys[i].init_width;
                        }

                        // scale the third line
                        if(scalingMode==LINEAR_MODE){
                            float leftRatio=this.keys[begin].getLeft(TEST_LAYOUT)/this.keys[begin].getLeft(INIT_LAYOUT);
                            for(int i=BL;i<begin;i++) {
                                this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                                this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                            }
                            float rightRatio=(keyboardWidth-this.keys[end].getRight(TEST_LAYOUT))/(keyboardWidth-this.keys[end].getRight(INIT_LAYOUT));
                            for (int i=end+1;i<=BR;i++){
                                this.keys[i].test_x=keyboardWidth-(keyboardWidth-this.keys[i].init_x)*rightRatio;
                                this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                            }
                        }else{
                            int leftScalingNum=begin-BL;
                            int rightScalingNum=BR-end;
                            if(leftScalingNum>scalingNum || rightScalingNum>scalingNum || leftScalingNum<=0||rightScalingNum<=0){
                                Log.e("scaling num error","bottom pos:"+String.valueOf(pos));
                            }
                            float leftLength=this.keys[begin].getLeft(TEST_LAYOUT);
                            float rightLength=keyboardWidth-this.keys[end].getRight(TEST_LAYOUT);
                            for (int i=begin-1;i>=BL;i--){
                                int index=begin-1-i;
                                this.keys[i].test_width=leftLength*expRatio[leftScalingNum][BOTTOM][index];
                                this.keys[i].test_x=this.keys[i+1].getLeft(TEST_LAYOUT)-this.keys[i].test_width/2;
                            }
                            for (int i=end+1;i<=BR;i++){
                                int index=i-end-1;
                                this.keys[i].test_width=rightLength*expRatio[leftScalingNum][BOTTOM][index];
                                this.keys[i].test_x=this.keys[i-1].getRight(TEST_LAYOUT)+this.keys[i].test_width/2;
                            }
                        }

                    }
                    // reset the first and the second line
                    for(int i:firstSecondLine){
                        this.keys[i].test_x=this.keys[i].init_x;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }
                }
                // check the width of each key
                for (int i:allLine){
                    if(this.keys[i].test_width<this.minWidth){
                        return false;
                    }
                }
                for (int i:allLine){
                    this.keys[i].curr_x=this.keys[i].test_x;
                    this.keys[i].curr_width=this.keys[i].test_width;
                }
                return true;
            }
        }

        public  boolean tryLayout(char ch,float x,float y){
            // mode==0 bodily movement
            // mode==1 respectively movement
            ch=Character.toLowerCase(ch);
            int pos = this.keyPos[ch-'a'];
            int qua = this.keys[pos].contain_tap(x,y,INIT_LAYOUT);
            float dX=0;
            float dY=0;
            //竖直上的平移不能太大
            /*if (Math.abs(dY) >= MAX_DELTAY){
                return false;
            }*/
            switch(qua){
                case 1:{
                    dX=x-this.keys[pos].getLeft_tap(INIT_LAYOUT);
                    dY=y-this.keys[pos].getTop_tap(INIT_LAYOUT);
                    break;
                }
                case 2:{
                    dX=0;
                    dY=y-this.keys[pos].getTop_tap(INIT_LAYOUT);
                    break;
                }case 3:{
                    dX=x-this.keys[pos].getRight_tap(INIT_LAYOUT);
                    dY=y-this.keys[pos].getTop_tap(INIT_LAYOUT);
                    break;
                }case 4:{
                    dX=x-this.keys[pos].getLeft_tap(INIT_LAYOUT);
                    dY=0;
                    break;
                }case 5:{
                    dX=0;
                    dY=0;
                    break;
                }case 6:{
                    dX=x-this.keys[pos].getRight_tap(INIT_LAYOUT);
                    dY=0;
                    break;
                }
                case 7:{
                    dX=x-this.keys[pos].getLeft_tap(INIT_LAYOUT);
                    dY=y-this.keys[pos].getBottom_tap(INIT_LAYOUT);
                    break;
                }case 8:{
                    dX=0;
                    dY=y-this.keys[pos].getBottom_tap(INIT_LAYOUT);
                    break;
                }case 9:{
                    dX=x-this.keys[pos].getRight_tap(INIT_LAYOUT);
                    dY=y-this.keys[pos].getBottom_tap(INIT_LAYOUT);
                    break;
                }
            }
            return shift_x(pos,dX)&&shift_y(pos,dY);
        }

        public void drawLayout(){ // curr_x,curr_y
            if(Visibility){
                float left=this.location[0]+getLeft(CURR_LAYOUT);
                float top=this.location[1]+getTop(CURR_LAYOUT);
                float right=this.location[0]+getRight(CURR_LAYOUT);
                float bottom=this.location[1]+getBottom(CURR_LAYOUT);
                this.baseBitmap = Bitmap.createBitmap(this.keyboard.getWidth(),this.keyboard.getHeight(), Bitmap.Config.ARGB_8888);
                this.canvas=new Canvas(this.baseBitmap);
                //RectF rect = new RectF(left, top, right, bottom);
                //this.canvas.drawRect(rect, this.backgroundPaint);

                Paint.FontMetrics fontMetrics = this.textPaint.getFontMetrics();
                float fonttop = fontMetrics.top;//为基线到字体上边框的距离
                float fontbottom = fontMetrics.bottom;//为基线到字体下边框的距离
                for (int i:allLine){
                    RectF rect = new RectF(this.keys[i].getLeft(CURR_LAYOUT)+5,this.keys[i].getTop(CURR_LAYOUT)+10,this.keys[i].getRight(CURR_LAYOUT)-5,this.keys[i].getBottom(CURR_LAYOUT)-10);
                    this.canvas.drawRoundRect(rect,10,10,this.backgroundPaint);
                    this.canvas.drawText(String.valueOf(this.keys[i].ch).toUpperCase(),this.keys[i].curr_x+this.location[0],this.keys[i].curr_y+this.location[1]-fonttop/2-fontbottom/2,this.textPaint);
                }
                this.keyboard.setImageBitmap(this.baseBitmap);
            }else{
                this.baseBitmap = Bitmap.createBitmap(this.keyboard.getWidth(),this.keyboard.getHeight(), Bitmap.Config.ARGB_8888);
                this.keyboard.setImageBitmap(this.baseBitmap);
            }

        }

        public void resetLayout(){
            if(this.keys==null){
                this.keys=new KEY[BR+1];
            }
            this.keys[0].ch='q';
            this.keys[1].ch='w';
            this.keys[2].ch='e';
            this.keys[3].ch='r';
            this.keys[4].ch='t';
            this.keys[5].ch='y';
            this.keys[6].ch='u';
            this.keys[7].ch='i';
            this.keys[8].ch='o';
            this.keys[9].ch='p';
            this.keys[10].ch='a';
            this.keys[11].ch='s';
            this.keys[12].ch='d';
            this.keys[13].ch='f';
            this.keys[14].ch='g';
            this.keys[15].ch='h';
            this.keys[16].ch='j';
            this.keys[17].ch='k';
            this.keys[18].ch='l';
            this.keys[19].ch='z';
            this.keys[20].ch='x';
            this.keys[21].ch='c';
            this.keys[22].ch='v';
            this.keys[23].ch='b';
            this.keys[24].ch='n';
            this.keys[25].ch='m';
            //this.keys[26].ch=delete_key;

            for (int i:firstLine){
                this.keys[i].init_x=this.keyboardWidth*(2*i+1)/20;
                this.keys[i].init_y=this.keyboardHeight/6+this.deltaY;
            }
            for (int i:secondLine){
                this.keys[i].init_x=(this.keys[i-10].init_x+this.keys[i-9].init_x)/2;
                this.keys[i].init_y=this.keyboardHeight/2+this.deltaY;
            }
            for (int i:thirdLine){
                this.keys[i].init_x=this.keys[i-8].init_x;
                this.keys[i].init_y=this.keyboardHeight*5/6+this.deltaY;
            }

            for (int i:allLine) {
                this.keys[i].init_height=this.keyboardHeight/3;
                this.keys[i].init_width=this.keyboardWidth/10;
                this.keys[i].curr_width=this.keys[i].init_width;
                this.keys[i].curr_height=this.keys[i].init_height;
                this.keys[i].curr_x = this.keys[i].init_x;
                this.keys[i].curr_y = this.keys[i].init_y;
            }
            //this.drawLayout();
        }
        public AutoKeyboard(ImageView keyBoard){
            defaultPara();
            this.backgroundPaint=new Paint();
            this.textPaint=new Paint();
            this.backgroundPaint.setColor(Color.rgb(239,239,239));
            this.backgroundPaint.setStrokeJoin(Paint.Join.ROUND);
            this.backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
            this.backgroundPaint.setStrokeWidth(3);

            this.textPaint.setColor(Color.BLACK);
            this.textPaint.setStrokeJoin(Paint.Join.ROUND);
            this.textPaint.setStrokeCap(Paint.Cap.ROUND);
            this.textPaint.setStrokeWidth(3);
            this.textPaint.setTextSize(Math.round(70*screen_height_ratio));
            this.textPaint.setTextAlign(Paint.Align.CENTER);

            this.linePaint=new Paint();
            this.linePaint.setColor(Color.BLACK);
            this.textPaint.setStrokeJoin(Paint.Join.ROUND);
            this.textPaint.setStrokeCap(Paint.Cap.ROUND);
            this.backgroundPaint.setStrokeWidth(3);

            this.keyboard=keyBoard;

            getScreenSizeRatio();
            this.keyboardHeight=this.keyboardHeight*this.screen_height_ratio;
            this.keyboardWidth=this.keyboardWidth*this.screen_width_ratio;
            this.topThreshold=this.screen_height_ratio*this.topThreshold;
            this.bottomThreshold=this.screen_width_ratio*this.bottomThreshold;
            this.minWidth=this.screen_width_ratio*this.minWidth;
            this.minHetight=this.screen_height_ratio*this.minHetight;


            this.location=new int[2];
            this.keyPos=new int[]{10,23,21,12,2,13,14,15,7,16,17,18,25,24,8,9,0,3,11,4,6,22,1,20,5,19};// A-Z ��Ӧ Q-M
            this.keyboard.getLocationOnScreen(this.location);
            this.keys=new KEY[BR+1];
            for (int i:allLine){
                this.keys[i]=new KEY();
            }
            this.resetLayout();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu_temp) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_menu, menu_temp);
        menu=menu_temp;
        setMenuTitle();
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        /*
         * 将actionBar的HomeButtonEnabled设为ture，
         *
         * 将会执行此case
         */
            case R.id.recordModeItem:{
                switch (recordMode){
                    case RECORD_MODE_STOPED:{
                        recordMode = RECORD_MODE_STARTED;
                        filename = getFilename();
                        break;
                    }
                    case RECORD_MODE_STARTED:{
                        recordMode = RECORD_MODE_STOPED;
                        break;
                    }
                }
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                refresh();
                break;
            }
            case R.id.langModeItem:{
                switch (languageMode){
                    case LANG_MODE_ENG:{
                        languageMode = LANG_MODE_CHN;
                        break;
                    }
                    case LANG_MODE_CHN:{
                        languageMode = LANG_MODE_ENG;
                        break;
                    }
                }
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                refresh();
                break;
            }
            case R.id.confirmModeItem:{
                switch (confirmMode){
                    case CONFIRM_MODE_UP:{
                        confirmMode = CONFIRM_MODE_DOUBLECLICK;
                        break;
                    }
                    case CONFIRM_MODE_DOUBLECLICK:{
                        confirmMode = CONFIRM_MODE_UP;
                        break;
                    }
                }
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                refresh();
                break;
            }
            case R.id.upVoiceModeItem:{
                switch (upvoiceMode){
                    case UPVOICE_MODE_YES:{
                        upvoiceMode = UPVOICE_MODE_NO;
                        break;
                    }
                    case UPVOICE_MODE_NO:{
                        upvoiceMode = UPVOICE_MODE_YES;
                        break;
                    }
                }
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                refresh();
                break;
            }
            case R.id.SDPlusItem:{
                SD_coefficient += 5;
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                refresh();
                break;
            }
            case R.id.SDMinusItem:{
                if (SD_coefficient > 5)
                    SD_coefficient -= 5;
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                refresh();
                break;
            }
            case R.id.fuzzyInputTestBegin:{
                switch (activity_mode){
                    case KEYBOARD_MODE:{
                        beginFuzzyInputTest();
                        break;
                    }
                    case FUZZY_INPUT_TEST_MODE:{
                        restartFuzzyInputTest();
                        break;
                    }
                }
                break;
            }
            case R.id.testTurnChange:{
                if(activity_mode==KEYBOARD_MODE){
                    MAX_TURN=(MAX_TURN+1)%8+1;
                    MAX_FUZZY_INPUT_TURN=MAX_TURN*26;
                }
                break;
            }
            case R.id.testMode:{
                /*
                testmode=(testmode+1)%4;
                */
                break;
            }
            case R.id.keyBoardVisibility:{
                autoKeyboard.Visibility=!autoKeyboard.Visibility;
                if(autoKeyboard.Visibility)
                    Toast.makeText(this, "Keyboard Visible", Toast.LENGTH_LONG).show();
                else{
                    Toast.makeText(this, "Keyboard Invisible", Toast.LENGTH_LONG).show();
                }
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
            }
            case R.id.reset_keyboard:{
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }
            case R.id.try_layout_mode:{
                if(autoKeyboard.try_layout_mode==autoKeyboard.BODILY_MOVEMENT){
                    autoKeyboard.try_layout_mode=autoKeyboard.RESPECTIVELY_MOVEMENT;
                    Toast.makeText(this, "respectively movement", Toast.LENGTH_LONG).show();
                }else{
                    autoKeyboard.try_layout_mode=autoKeyboard.BODILY_MOVEMENT;
                    Toast.makeText(this, "bodily movement", Toast.LENGTH_LONG).show();
                }
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }
            case R.id.scalingNum:{
                autoKeyboard.scalingNum=(autoKeyboard.scalingNum+1)%3+1;
                Toast.makeText(this, "scalingNum="+String.valueOf(autoKeyboard.scalingNum), Toast.LENGTH_LONG).show();
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }
            case R.id.getKey_mode:{
                if(autoKeyboard.getKey_mode==autoKeyboard.LOOSE_MODE){
                    autoKeyboard.getKey_mode=autoKeyboard.STRICT_MODE;
                    Toast.makeText(this, "getKey_mode:STRICT_MODE", Toast.LENGTH_LONG).show();
                }else{
                    autoKeyboard.getKey_mode=autoKeyboard.LOOSE_MODE;
                    Toast.makeText(this, "getKey_mode:LOOSE_MODE", Toast.LENGTH_LONG).show();
                }
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.deltaY:{
                autoKeyboard.deltaY=(autoKeyboard.deltaY-50+10)%200+50;
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.topThreshold:{
                autoKeyboard.topThreshold=(autoKeyboard.topThreshold+5)%50;
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.bottomThreshold:{
                autoKeyboard.bottomThreshold=(autoKeyboard.bottomThreshold-900+5)%50+900;
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.minWidth:{
                autoKeyboard.minWidth=(autoKeyboard.minWidth-30+3)%90+30;
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.minHetight:{
                autoKeyboard.minHetight=(autoKeyboard.minHetight-30+3)%90+30;
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.tap_range:{
                autoKeyboard.tap_range_index=(autoKeyboard.tap_range_index+1)%autoKeyboard.tap_range_array.length;
                autoKeyboard.tap_range=(float)autoKeyboard.tap_range_array[autoKeyboard.tap_range_index];
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.keyboardHeight:{
                autoKeyboard.keyboardHeight=(autoKeyboard.keyboardHeight-500+5)%200+500;
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.scalingMode:{
                if(autoKeyboard.scalingMode==autoKeyboard.LINEAR_MODE){
                    autoKeyboard.scalingMode=autoKeyboard.EXPONENT_MODE;
                }else if(autoKeyboard.scalingMode==autoKeyboard.EXPONENT_MODE){
                    autoKeyboard.scalingMode=autoKeyboard.LINEAR_MODE;
                }
                break;
            }case R.id.exponent:{
                autoKeyboard.exponent_index=(autoKeyboard.exponent_index+1)%autoKeyboard.exponent_array.length;
                autoKeyboard.exponent=(float)autoKeyboard.exponent_array[autoKeyboard.exponent_index];
                autoKeyboard.calExpRatio();
                break;
            }case R.id.default_keyboard:{
                autoKeyboard.defaultPara();
                break;
            }
            default:
                break;
        }
        setMenuTitle();
        return super.onOptionsItemSelected(item);
    }
    void setMenuTitle(){
        switch (activity_mode){
            case KEYBOARD_MODE:{
                menu.findItem(R.id.fuzzyInputTestBegin).setTitle("Start Test");
                break;
            }
            case FUZZY_INPUT_TEST_MODE:{
                menu.findItem(R.id.fuzzyInputTestBegin).setTitle("Restart Test");
                break;
            }
        }
        switch (recordMode){
            case RECORD_MODE_STOPED:{
                menu.findItem(R.id.recordModeItem).setTitle("recordMode:stoped");
                break;
            }
            case RECORD_MODE_STARTED:{
                menu.findItem(R.id.recordModeItem).setTitle("recordMode:started");
                break;
            }
        }
        switch (languageMode){
            case LANG_MODE_ENG:{
                menu.findItem(R.id.langModeItem).setTitle("langMode:ENG");
                break;
            }
            case LANG_MODE_CHN:{
                menu.findItem(R.id.langModeItem).setTitle("langMode:CHN");
                break;
            }
        }
        switch (confirmMode){
            case CONFIRM_MODE_UP:{
                menu.findItem(R.id.confirmModeItem).setTitle("confirmMode:UP");
                break;
            }
            case CONFIRM_MODE_DOUBLECLICK:{
                menu.findItem(R.id.confirmModeItem).setTitle("confirmMode:CLICK");
                break;
            }
        }
        switch (upvoiceMode){
            case UPVOICE_MODE_YES:{
                menu.findItem(R.id.upVoiceModeItem).setTitle("upVoiceMode:YES");
                break;
            }
            case UPVOICE_MODE_NO:{
                menu.findItem(R.id.upVoiceModeItem).setTitle("upVoiceMode:NO");
                break;
            }
        }
        menu.findItem(R.id.SDPlusItem).setTitle("SDPlus:" + SD_coefficient);
        menu.findItem(R.id.SDMinusItem).setTitle("SDMinus:" + SD_coefficient);
        menu.findItem(R.id.testTurnChange).setTitle("TestTurn:"+String.valueOf(MAX_TURN));
        if(activity_mode==KEYBOARD_MODE){
            menu.findItem(R.id.testTurnChange).setTitle("TestTurn:"+String.valueOf(MAX_TURN));
        }
        switch(testmode){
            case NORMAL:{
                menu.findItem(R.id.testMode).setTitle("TestMode:NORMAL");
                break;
            }
            case CANCEL:{
                menu.findItem(R.id.testMode).setTitle("TestMode:CANCEL");
                break;
            }
            case BORDER:{
                menu.findItem(R.id.testMode).setTitle("TestMode:BORDER");
                break;
            }
            case CANCEL_BORDER:{
                menu.findItem(R.id.testMode).setTitle("TestMode:CANBOR");
                break;
            }
        }
        if(autoKeyboard.Visibility){
            menu.findItem(R.id.keyBoardVisibility).setTitle("KeyboardVisibility:True");
        }else{
            menu.findItem(R.id.keyBoardVisibility).setTitle("KeyboardVisibility:False");
        }
        menu.findItem(R.id.reset_keyboard).setTitle("Reset Keyboard");
        if(autoKeyboard.try_layout_mode==autoKeyboard.BODILY_MOVEMENT){
            menu.findItem(R.id.try_layout_mode).setTitle("try layout mode:Bodily");
        }else if(autoKeyboard.try_layout_mode==autoKeyboard.RESPECTIVELY_MOVEMENT){
            menu.findItem(R.id.try_layout_mode).setTitle("try layout mode:Respectively");
        }
        menu.findItem(R.id.scalingNum).setTitle("scalingNum:"+String.valueOf(autoKeyboard.scalingNum));
        if(autoKeyboard.getKey_mode==autoKeyboard.LOOSE_MODE){
            menu.findItem(R.id.getKey_mode).setTitle("getKey mode:LOOSE MODE");
        }else if(autoKeyboard.getKey_mode==autoKeyboard.STRICT_MODE){
            menu.findItem(R.id.getKey_mode).setTitle("getKey mode:STRICT MODE");
        }
        menu.findItem(R.id.deltaY).setTitle("deltaY:"+String.valueOf(autoKeyboard.deltaY));
        menu.findItem(R.id.topThreshold).setTitle("topThreshold:"+String.valueOf(autoKeyboard.topThreshold));
        menu.findItem(R.id.bottomThreshold).setTitle("bottomThreshold:"+String.valueOf(autoKeyboard.bottomThreshold));
        menu.findItem(R.id.minWidth).setTitle("minWidth:"+String.valueOf(autoKeyboard.minWidth));
        menu.findItem(R.id.minHetight).setTitle("minHetight:"+String.valueOf(autoKeyboard.minHetight));
        menu.findItem(R.id.tap_range).setTitle("tap range:"+String.valueOf(autoKeyboard.tap_range));
        menu.findItem(R.id.keyboardHeight).setTitle("keyboardHeight:"+String.valueOf(autoKeyboard.keyboardHeight));
        if(autoKeyboard.scalingMode==autoKeyboard.LINEAR_MODE){
            menu.findItem(R.id.scalingMode).setTitle("scalingMode:LINEAR_MODE");
        }else if(autoKeyboard.scalingMode==autoKeyboard.EXPONENT_MODE){
            menu.findItem(R.id.scalingMode).setTitle("scalingMode:EXPONENT_MODE");
        }
        menu.findItem(R.id.exponent).setTitle("exponent:"+String.valueOf(autoKeyboard.exponent));

    }
}
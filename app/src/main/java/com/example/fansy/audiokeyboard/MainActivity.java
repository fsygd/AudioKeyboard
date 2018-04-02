package com.example.fansy.audiokeyboard;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.MovementMethod;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    class Parameters{
        final long TIME_TOUCH_BY_MISTAKE = 100;
        final int MAX_PREDICT_LENGTH = 7;
        final int SHOW_RANK = 5;
    };
    Parameters mpara = new Parameters();
    class TouchInfo{
        long lastReadTime = 0;
        int voiceLength = 0;
        boolean TwoFingersFlag = false;
        boolean inKeyboard = true;
        char upKey = KEY_NOT_FOUND;
        String ranking = "";
        boolean lastReadFlag = false;
        long lastChChangeTime = 0;
    };
    TouchInfo mtouchinfo = new TouchInfo();

    class PinyinDecoderServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIPinyinDecoderService = IPinyinDecoderService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    }
    public IPinyinDecoderService mIPinyinDecoderService = null;
    public PinyinDecoderServiceConnection mPinyinDecoderServiceConnection = null;

    final char KEY_NOT_FOUND = '*';

    //voice manager
    HashMap<String, int[]> voice = new HashMap<>();
    ArrayList<Integer> myPlayList = new ArrayList<>();
    MediaPlayer current;

    TextToSpeech ttsENG,ttsCHN;

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

    final int PREDICT_ON = 0;
    final int PREDICT_OFF = 1;
    int predictEnableMode = PREDICT_ON;

    final int JP_ON = 0;
    final int JP_OFF = 1;
    int pinyinjpMode = JP_ON;

    final int AUTOREAD_ON = 0;
    final int AUTOREAD_OFF = -1;
    int autoreadMode = AUTOREAD_ON;

    float SD_coefficient_X = 6;
    float SD_coefficient_Y = 6;

    final int MAX_CANDIDATE = 5;
    int currentCandidate = autoreadMode;
    public ArrayList<Word> candidates = new ArrayList<Word>();

    ImageView keyboard;
    TextView text, candidatesView, readListView, voiceSpeedText, elapsedTimeText;
    Button confirmButton, initModeButton, speedButton;
    String currentInput = "";
    String readList = ""; //current voice list
    String currentWord = ""; //most possible char sequence
    String currentBaseline = "";
    char nowCh = KEY_NOT_FOUND; //the most possible char
    char nowChSaved = KEY_NOT_FOUND; // for double click
    char nowChBaselineSaved = KEY_NOT_FOUND;
    String currentBaselineDown = "";
    String currentPredictDown = "";
    char nowBaseLineDownSaved = KEY_NOT_FOUND;
    char nowPredictDownSaved = KEY_NOT_FOUND;
    char nowBaseLineDown = KEY_NOT_FOUND;
    char nowPredictDown = KEY_NOT_FOUND;

    final int TESTCASE_ALL = 6;
    final int TESTCASE_TURN = 3;
    int currentTestcase;
    ArrayList<String> testcases = new ArrayList<>();

    ArrayList<Word> dict_eng = new ArrayList<>();
    ArrayList<Word> dict_chn_jianpin = new ArrayList<>();
    ArrayList<Word> dict_chn_pinyin = new ArrayList<>();
    ArrayList<Word> dict_chn_quanpin = new ArrayList<>();
    HashMap<String,String> dict_chn_hint = new HashMap<>();
    ArrayList<Character> seq = new ArrayList<Character>(); //char sequence during the whole touch
    String keysNearby[] = new String[26];
    double keysNearbyProb[][] = new double[26][26]; //keysNearbyProb[x][y] the possibility when want y but touch x
    String filename = "";
    // 50-1.0f 60-1.5f 70-2.0f 80-2.5f 90-3.0f 100-3.5f
    int voiceSpeed = 60;
    float ttsVoiceSpeed=1.5f;
    AutoKeyboard autoKeyboard;
    // Screen
    Screen screen;
    float fontRatio = 1.0f;
    enum ActivityMode{KEYBOARD_MODE,FUZZY_INPUT_TEST_MODE};
    ActivityMode activity_mode=ActivityMode.KEYBOARD_MODE;
    public void tts(String text){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switch(languageMode){
                case LANG_MODE_CHN:{
                    ttsCHN.speak(text,TextToSpeech.QUEUE_FLUSH,null,null);
                    break;
                }case LANG_MODE_ENG:{
                    ttsENG.speak(text,TextToSpeech.QUEUE_FLUSH,null,null);
                    break;
                }
            }
        } else {
            switch(languageMode){
                case LANG_MODE_CHN:{
                    ttsCHN.speak(text,TextToSpeech.QUEUE_FLUSH,null,null);
                    break;
                }case LANG_MODE_ENG:{
                    ttsENG.speak(text,TextToSpeech.QUEUE_FLUSH,null,null);
                    break;
                }
            }
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
        return "" + YY + MM + DD + HH + mm + SS + MI + "_" + initMode + "_" + languageMode + "_" + confirmMode + "_" + SD_coefficient_X + ".txt";
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
        String testcase = "";
        if (recordMode == RECORD_MODE_STARTED)
            testcase = currentTestcase + ":" + testcases.get(currentTestcase) + "\n";

        String textContent = "";
        if (languageMode == LANG_MODE_CHN){
            try {
                String composingWord = mIPinyinDecoderService.imGetChoice(0).substring(0, mIPinyinDecoderService.imGetFixedLen());
                composingWord += currentWord.substring(mIPinyinDecoderService.imGetSplStart()[mIPinyinDecoderService.imGetFixedLen() + 1]);
                textContent += testcase + currentInput + "|\n" + composingWord + "\n";
            }catch (RemoteException e){
                e.printStackTrace();
            }
        }
        else {
            textContent += testcase + currentInput + "|\n" + currentWord + "\n";
        }
        int diff = 0;
        for (int i = 0; i < currentWord.length(); ++i)
            if (currentWord.charAt(i) != currentPredictDown.charAt(i))
                diff += 1;
        textContent += currentPredictDown + " diff = " + diff + "\n";
        diff = 0;
        for (int i = 0; i < currentWord.length(); ++i)
            if (currentWord.charAt(i) != currentBaselineDown.charAt(i))
                diff += 1;
        textContent += currentBaselineDown + " diff = " + diff + "\n";
        text.setText(textContent);
        String str = "";
        int temp = Math.max(currentCandidate, 0);
        for (int i = temp; i < temp + MAX_CANDIDATE; ++i)
            if (i >= 0 && i < candidates.size())
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

    //predict the candidates according the currentWord and currentWord2 and refresh
    public void predict(String currentWord){
        candidates.clear();
        if (currentWord.length() == 0)
            return;
        if (languageMode == LANG_MODE_CHN){
            try {
                int listlen = mIPinyinDecoderService.imSearch(currentWord.getBytes(), currentWord.length());
                List<String> wordlist = mIPinyinDecoderService.imGetChoiceList(0, listlen, mIPinyinDecoderService.imGetFixedLen());
                for (int i = 0; i < wordlist.size(); ++i)
                    candidates.add(new Word(wordlist.get(i), 0));
            }catch (RemoteException e){

            }
            refresh();
            return;
        }
        ArrayList<Word> dict = new ArrayList<>();
        if (languageMode == LANG_MODE_ENG)
            dict = dict_eng;
        else if (languageMode == LANG_MODE_CHN)
            dict = dict_chn_pinyin;

        for (int i = 0; i < dict.size(); ++i){
            //if (candidates.size() >= MAX_CANDIDATE)
            //    break;
            Word candidate = dict.get(i);
            if (candidate.text.length() < currentWord.length())
                continue;

            boolean flag = true;
            for (int j = 0; j < currentWord.length(); ++j)
                if (candidate.text.charAt(j) != currentWord.charAt(j)){
                    flag = false;
                    break;
                }
            if (flag) {
                candidates.add(new Word(candidate));
            }
        }

        double delta = dict.get(0).freq;
        for (int i = 0; i < candidates.size(); ++i)
            if (candidates.get(i).text.length() > currentWord.length())
                candidates.get(i).freq -= delta;
        Collections.sort(candidates);
        if (candidates.size() == 0){
            candidates.add(new Word(currentWord, 0));
        }
        else if (!candidates.get(0).text.equals(currentWord)){
            candidates.add(0, new Word(currentWord, 0));
        }
        refresh();
    }

    public void predictBylastch(char lastCh){
        candidates.clear();
        ArrayList<Word> dict = new ArrayList<>();
        dict = dict_chn_pinyin;

        for (int i = 0; i < dict.size(); ++i){
            //if (candidates.size() >= MAX_CANDIDATE)
            //    break;
            Word candidate = dict.get(i);
            if (candidate.alias.charAt(0) == lastCh && candidate.alias.length() >= 2){
                candidates.add(new Word(candidate.alias.substring(1), candidate.freq));
            }
        }
        Collections.sort(candidates);

        refresh();
    }

    public void printCallStatck() {
        RuntimeException here = new RuntimeException("here");
        here.fillInStackTrace();
        Log.i("fsy", "Called: ", here);
    }
    public void stopVoice(){
        Log.i("fsy", "stopVoice!");
        //printCallStatck();
        ttsENG.stop();
        ttsCHN.stop();
        if (current != null) {
            current.stop();
            current.reset();
            current.release();
            current = null;
        }
        myPlayList.clear();
    }

    public void stopInput(boolean stopvoice){
        seq.clear();
        mtouchinfo.lastReadFlag = false;
        if (stopvoice)
            stopVoice();
    }

    public void finishWord(){
        currentWord = "";
        currentBaseline = "";
        currentBaselineDown = "";
        currentPredictDown = "";
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
                addToSeq(autoKeyboard.getKeyByPosition(x, y, autoKeyboard.INIT_LAYOUT), x, y);
            }
            else if(initMode == INIT_MODE_RELATIVE){
                char ch = autoKeyboard.getKeyByPosition(x, y, autoKeyboard.CURR_LAYOUT);
                if (ch == mtouchinfo.upKey){
                    addToSeq(ch, x, y);
                }
                else{
                    autoKeyboard.resetLayout();
                    autoKeyboard.drawLayout();
                    write("reset");
                    ch = autoKeyboard.getKeyByPosition(x, y, autoKeyboard.CURR_LAYOUT);
                    char best = getMostPossibleKey(x, y);
                    if (autoKeyboard.tryLayout(best, x, y)){
                        write("try " + best + " " + x + " " + y);
                        autoKeyboard.drawLayout(mtouchinfo.ranking);
                        addToSeq(best, x, y);
                    }else{
                        if (ch != KEY_NOT_FOUND && autoKeyboard.tryLayout(ch, x, y)){
                            write("try " + ch + " " + x + " " + y);
                            autoKeyboard.drawLayout(mtouchinfo.ranking);
                        }
                        addToSeq(ch, x, y);
                    }
                }
            }else{
                char ch = autoKeyboard.getKeyByPosition(x, y, autoKeyboard.INIT_LAYOUT);
                addToSeq(ch, x, y);
            }
        }
        else{
            addToSeq(autoKeyboard.getKeyByPosition(x, y, autoKeyboard.CURR_LAYOUT), x, y);
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

    final int DICT_SIZE[] = {50000, 49588, 65105};
    final int DICT_SIZE_QUANPIN = 38991;
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

        reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.dict_chn_jianpin)));
        try{
            int lineNo = 0;
            while ((line = reader.readLine()) != null){
                lineNo++;
                String[] ss = line.split(" ");
                dict_chn_jianpin.add(new Word(ss[0], Double.valueOf(ss[1])));
                if (lineNo == DICT_SIZE[LANG_MODE_CHN])
                    break;
            }
            reader.close();
            Log.i("init", "read dict_chn_jianpin finished" + dict_chn_jianpin.size());
        } catch (Exception e){
            Log.i("init", "read dict_chn_jianpin failed");
        }

        reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.dict_chn_quanpin)));
        try{
            int lineNo = 0;
            while ((line = reader.readLine()) != null){
                lineNo++;
                String[] ss = line.split(" ");
                dict_chn_quanpin.add(new Word(ss[0], Double.valueOf(ss[1])));
                if (lineNo == DICT_SIZE_QUANPIN)
                    break;
            }
            reader.close();
            Log.i("init", "read dict_chn_quanpin finished" + dict_chn_quanpin.size());
        } catch (Exception e){
            Log.i("init", "read dict_chn_quanpin failed");
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

        reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.dict_chn_hint)));
        try{
            while((line = reader.readLine()) != null){
                String[] ss = line.split(" ");
                if(ss.length==2) {
                    dict_chn_hint.put(ss[0], ss[1]);
                }else{
                    dict_chn_hint.put(ss[0],"");
                }
            }
            reader.close();
            Log.i("init", "read dict_chn_hint finished" + dict_chn_hint.size());
        }catch (Exception e){
            Log.i("init", "read dict_chn_hint failed");
        }

        reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.testcases)));
        try{
            int lineNo = 0;
            while ((line = reader.readLine()) != null){
                lineNo++;
                testcases.add(line);
                if (lineNo == TESTCASE_ALL)
                    break;
            }
            reader.close();
            Log.i("init", "read testcases finished" + testcases.size());
        } catch (Exception e){
            Log.i("init", "read testcases failed");
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
    public void playFirstVoice(){
        if (current == null && !myPlayList.isEmpty()){
            current = MediaPlayer.create(this, myPlayList.get(0));
            myPlayList.remove(0);
            current.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (activity_mode == ActivityMode.FUZZY_INPUT_TEST_MODE) {
                        //current.reset();
                        current.release();
                        current = null;
                    }
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
        else if (languageMode == LANG_MODE_CHN) {
            if (pinyinjpMode == JP_ON)
                dict = dict_chn_jianpin;
            else
                dict = dict_chn_quanpin;
        }

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
                        if (letters.get(j).text.charAt(0) == word.text.charAt(currentWord.length())) {
                            letters.get(j).freq = Math.max(letters.get(j).freq, word.freq);
                        }
                }
            }
        for (int i = 0; i < letters.size(); ++i){
            letters.get(i).freq += 0.01;
            int tmp = letters.get(i).text.charAt(0) - 'a';
            letters.get(i).freq *= Normal(x, autoKeyboard.keys[autoKeyboard.keyPos[tmp]].init_x, autoKeyboard.keys[0].init_width * SD_coefficient_X / 10);
            letters.get(i).freq *= Normal(y, autoKeyboard.keys[autoKeyboard.keyPos[tmp]].init_y, autoKeyboard.keys[0].init_width * SD_coefficient_Y / 10);
        }
        Collections.sort(letters);
        mtouchinfo.ranking = "";
        for (int i = 0; i < mpara.SHOW_RANK; ++i)
            mtouchinfo.ranking += letters.get(i).text.charAt(0);
        return letters.get(0).text.charAt(0);
    }

    //if write=false, just return the most possible key
    public void addToSeq(char ch, int x, int y){
        if (ch == KEY_NOT_FOUND){
            if (mtouchinfo.inKeyboard && System.currentTimeMillis() > downTime + STAY_TIME){
                tts("出界");
                nowCh = KEY_NOT_FOUND;
                mtouchinfo.lastReadFlag = false;
                mtouchinfo.inKeyboard = false;
            }
        }
        else{
            mtouchinfo.inKeyboard = true;
        }
        if (ch != KEY_NOT_FOUND && (seq.size() == 0 || seq.get(seq.size() - 1) != ch)) {
            if (seq.size() == 0){
                if (confirmMode == CONFIRM_MODE_UP){
                    nowPredictDownSaved = ch;
                    nowBaseLineDownSaved = autoKeyboard.getKeyByPosition(x, y, autoKeyboard.INIT_LAYOUT);
                }
                else{
                    nowPredictDown = ch;
                    nowBaseLineDown = autoKeyboard.getKeyByPosition(x, y, autoKeyboard.INIT_LAYOUT);
                }
            }
            seq.add(ch);
            mtouchinfo.lastReadFlag = false;
            mtouchinfo.lastChChangeTime = System.currentTimeMillis();
        }
        if (ch != KEY_NOT_FOUND && !mtouchinfo.lastReadFlag && (seq.size() == 1 || System.currentTimeMillis() - mtouchinfo.lastChChangeTime >= mpara.TIME_TOUCH_BY_MISTAKE)){
            stopVoice();
            readList = "";
            mtouchinfo.lastReadTime = System.currentTimeMillis();
            playMedia("ios11_" + voiceSpeed, ch - 'a', true);
            mtouchinfo.lastReadFlag = true;
            //added
            nowCh = ch;
            mtouchinfo.voiceLength = current.getDuration();
            readList += ch;
            refresh();
        }
        /*if (ch != KEY_NOT_FOUND){
            if (seq.size() == 1 || System.currentTimeMillis() - mtouchinfo.lastReadTime >= mtouchinfo.voiceLength * 0.3){
                nowCh = ch;
            }
        }*/
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        layoutInit();
        initDict();
        initVoice();
        initKeyboard();

        ttsENG = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status){
                if (status == ttsENG.SUCCESS){
                    int result = ttsENG.setLanguage(Locale.ENGLISH);
                    if (result != TextToSpeech.LANG_COUNTRY_AVAILABLE && result != TextToSpeech.LANG_AVAILABLE){
                        Toast.makeText(MainActivity.this, "TTS暂时不支持英文的朗读！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        ttsENG.setSpeechRate(ttsVoiceSpeed);
        ttsENG.setPitch(1.0f);

        ttsCHN = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status){
                if (status == ttsCHN.SUCCESS){
                    int result = ttsCHN.setLanguage(Locale.CHINA);
                    if (result != TextToSpeech.LANG_COUNTRY_AVAILABLE && result != TextToSpeech.LANG_AVAILABLE){
                        Toast.makeText(MainActivity.this, "TTS暂时不支持中文的朗读！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        ttsCHN.setSpeechRate(ttsVoiceSpeed);
        ttsCHN.setPitch(1.0f);


        //startPinyinDecoderService
        if (mIPinyinDecoderService == null) {
            Intent serviceIntent = new Intent();
            serviceIntent.setClass(this, PinyinDecoderService.class);

            if (mPinyinDecoderServiceConnection == null) {
                mPinyinDecoderServiceConnection = new PinyinDecoderServiceConnection();
            }

            if (bindService(serviceIntent, mPinyinDecoderServiceConnection, Context.BIND_AUTO_CREATE))
                Log.i("fsy", "true");
        }
    }

    void layoutInit(){
        switch(activity_mode){
            case KEYBOARD_MODE:{
                setContentView(R.layout.activity_main_relative);
                text = (TextView)findViewById(R.id.text);
                elapsedTimeText = (TextView)findViewById(R.id.elapsedtime);
                candidatesView = (TextView)findViewById(R.id.candidates);
                confirmButton = (Button)findViewById(R.id.confirm_button);
                readListView = (TextView)findViewById(R.id.readList);
                initModeButton = (Button)findViewById(R.id.init_mode_button);
                speedButton = (Button)findViewById(R.id.speedButton);
                voiceSpeedText = (TextView)findViewById(R.id.voice_speed_text);
                voiceSpeedText.setText(String.valueOf(voiceSpeed));
                languageMode=LANG_MODE_ENG;
                break;
            }
            case FUZZY_INPUT_TEST_MODE:{
                setContentView(R.layout.fuzzy_input_test_relative);
                fuzzyInputTestCharShow=(TextView)findViewById(R.id.fuzzyInputTestCharShow);
                fuzzyInputTestCharShow.setVisibility(View.VISIBLE);
                listView=(ListView)findViewById(R.id.list_view);
                listView.setVisibility(View.VISIBLE);
                progressBar=(ProgressBar)findViewById(R.id.progress_bar);
                progressBar.setVisibility(View.VISIBLE);
                BackButton=(Button)findViewById(R.id.back_button);
                BackSpaceButton=(Button)findViewById(R.id.backspace_button);
                SaveButton=(Button)findViewById(R.id.save_button);
                RestartButton=(Button)findViewById(R.id.restart_button);
                languageMode=LANG_MODE_CHN;
                break;
            }
        }
        keyboard = (ImageView)findViewById(R.id.keyboard);
        ViewTreeObserver vto2 = keyboard.getViewTreeObserver();
        vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                keyboard.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                autoKeyboard=new AutoKeyboard(keyboard);
                fitOnScreen();
            }
        });
        screen=new Screen(this);
        seekBar=(SeekBar)findViewById(R.id.seekBar);
        seekBar.setVisibility(View.GONE);
        seekBarInit();
        seekBarText=(TextView)findViewById(R.id.seekBarText);
        seekBarText.setVisibility(View.GONE);
        fontRatio = 4.0f/screen.dmDensity;
        initButtons();
    }
    void initButtons(){
        switch(activity_mode){
            case KEYBOARD_MODE:{
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onClick(View v) {
                        stopInput(true);
                        finishWord();
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                    }
                });

                initModeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (initMode == INIT_MODE_ABSOLUTE)
                            initMode = INIT_MODE_RELATIVE;
                        else if(initMode == INIT_MODE_RELATIVE)
                            initMode = INIT_MODE_NOTHING;
                        else
                            initMode = INIT_MODE_ABSOLUTE;
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        refresh();
                    }
                });

                speedButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currentMenuVar=MENUVAR.SPEED;
                        seekBar.setVisibility(View.VISIBLE);
                        float voiceSpeedMax=100f;
                        float voiceSpeedMin=50f;
                        int progress=Math.round(100f*(voiceSpeed-voiceSpeedMin)/(voiceSpeedMax-voiceSpeedMin));
                        seekBar.setProgress(progress);
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        voiceSpeedText.setText(voiceSpeed+"");
                        seekBarText.setVisibility(View.VISIBLE);
                        seekBarText.setText("voiceSpeed:"+String.valueOf(voiceSpeed));
                    }
                });
                break;
            }
            case FUZZY_INPUT_TEST_MODE:{
                BackButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stopVoice();
                        if(!fu.ifSave){
                            AlertDialog.Builder dialog=new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle("警告");
                            tts("测试还未完成，你确定要返回吗？");
                            dialog.setMessage("测试还未完成，你确定要返回吗？");
                            dialog.setCancelable(false);
                            dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    activity_mode = ActivityMode.KEYBOARD_MODE;
                                    fu.sentence.clear();
                                    fu=null;
                                    layoutInit();
                                    setMenuTitle();
                                }
                            });
                            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            });
                            dialog.show();
                        }else {
                            activity_mode = ActivityMode.KEYBOARD_MODE;
                            fu.sentence.clear();
                            fu=null;
                            layoutInit();
                            setMenuTitle();
                            refresh();
                        }
                    }
                });
                SaveButton.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onClick(View v) {
                        stopVoice();
                        fu.saveFuzzyInputTestData();
                    }
                });
                BackSpaceButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stopVoice();
                        fu.backSpace();
                    }
                });
                RestartButton.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        if(!fu.ifSave){
                            AlertDialog.Builder dialog=new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle("警告");
                            tts("测试还未完成，你确定要返回吗？");
                            dialog.setMessage("测试还未完成，你确定要重新开始吗？");
                            dialog.setCancelable(false);
                            dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if(fu.sentence!=null){
                                        fu.sentence.clear();
                                    }
                                    fu=new FuzzyInputTest();
                                    listView.setVisibility(View.GONE);
                                }
                            });
                            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            dialog.show();
                        }else{
                            if(fu!=null){
                                fu.sentence.clear();
                            }
                            fu=new FuzzyInputTest();
                            listView.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }
    }
    public void fitOnScreen(){

        // fit text size
        int textSize=18;
        int buttonTextSize=12;
        switch (activity_mode){
            case KEYBOARD_MODE:{
                text.setTextSize((int)(textSize*fontRatio));
                candidatesView.setTextSize((int)(textSize*fontRatio));
                readListView.setTextSize((int)(textSize*fontRatio));
                voiceSpeedText.setTextSize((int)(textSize*fontRatio));
                elapsedTimeText.setTextSize((int)(textSize*fontRatio));
                seekBarText.setTextSize((int)(textSize*fontRatio));
                confirmButton.setTextSize((int)(buttonTextSize*fontRatio));
                initModeButton.setTextSize((int)(buttonTextSize*fontRatio));
                speedButton.setTextSize((int)(buttonTextSize*fontRatio));
                break;
            }
            case FUZZY_INPUT_TEST_MODE:{
                fuzzyInputTestCharShow.setTextSize((int)(textSize*fontRatio));
            }
        }
    }
    @Override
    public void onDestroy(){
        if (current != null){
            current.release();
            current = null;
        }
        if (ttsCHN != null)
            ttsCHN.shutdown();

        if (ttsENG != null)
            ttsENG.shutdown();
        unbindService(mPinyinDecoderServiceConnection);
        super.onDestroy();
    }

    public String deleteLastChar() {
        String str = "";
        if (currentWord.length() > 0) {
            write("deletechar");
            str += currentWord.charAt(currentWord.length() - 1);
            currentWord = currentWord.substring(0, currentWord.length() - 1);
            currentBaselineDown = currentBaselineDown.substring(0, currentBaselineDown.length() - 1);
            currentPredictDown = currentPredictDown.substring(0, currentPredictDown.length() - 1);
            currentBaseline = currentBaseline.substring(0, currentBaseline.length() - 1);
        }
        else if (currentInput.length() > 0){
            str += currentInput.charAt(currentInput.length() - 1);
            currentInput = currentInput.substring(0, currentInput.length() - 1);
        }
        readList = "";
        predict(currentWord);
        refresh();
        if (str.equals(" ")){
            str = "空格";
        }
        return str;
    }

    public void deleteAllChar(){
        currentWord = "";
        currentBaseline = "";
        currentBaselineDown = "";
        currentPredictDown = "";
        readList = "";
        predict(currentWord);
        refresh();
    }

    int downX, downY, downX2, downY2;
    long downTime = 0, downTime2 = 0;
    long wordDownTime = 0;
    long firstDownTime = 0, lastDownTime = 0; // used for check double-click
    final long STAY_TIME = 200;
    final int SLIP_DIST = 70;
    boolean isOverScreen=false;
    char lastChar='A';
    public boolean checkLeftwipe(int x, int y, long tempTime) {
        return x < downX - SLIP_DIST && Math.abs(downX - x) > Math.abs(downY - y) && tempTime < downTime + STAY_TIME;
    }
    public boolean checkRightwipe(int x, int y, long tempTime) {
        return x > downX + SLIP_DIST && Math.abs(downX - x) > Math.abs(downY - y) && tempTime < downTime + STAY_TIME;
    }
    public boolean checkUpwipe(int x, int y, long tempTime){
        return y < downY - SLIP_DIST && Math.abs(downY - y) > Math.abs(downX - x) && tempTime < downTime + STAY_TIME;
    }
    public boolean checkDownwipe(int x, int y, long tempTime){
        return y > downY + SLIP_DIST && Math.abs(downY - y) > Math.abs(downX - x) && tempTime < downTime + STAY_TIME;
    }
    public boolean checkLeftwipe2(int x, int y, long tempTime){
        return x < downX2 - SLIP_DIST && Math.abs(downX2 - x) > Math.abs(downY2 - y) && tempTime < downTime2 + STAY_TIME;
    }
    public boolean checkUpwipe2(int x, int y, long tempTime){
        return y < downY2 - SLIP_DIST && Math.abs(downY2 - y) > Math.abs(downX2 - x) && tempTime < downTime2 + STAY_TIME;
    }
    public boolean checkRightwipe2(int x, int y, long tempTime){
        return x > downX2 + SLIP_DIST && Math.abs(downX2 - x) > Math.abs(downY2 - y) && tempTime < downTime2 + STAY_TIME;
    }

    public String getChnHint(String sentence){
        String ans = "";
        if (languageMode == LANG_MODE_CHN) {
            for (int i = 0; i < sentence.length(); ++i)
                if (dict_chn_hint.get(sentence.charAt(i) + "").length() > 0) {
                    ans += "，" + dict_chn_hint.get(sentence.charAt(i) + "") + "的" + sentence.charAt(i);
                }
        }
        return ans + "，";
    }

    public void nextTestcase(){
        ++currentTestcase;
        write("sentence " + testcases.get(currentTestcase));
        tts("请输入:" + testcases.get(currentTestcase));
    }

    public void actionRightwipe(){
        stopInput(true);
        autoKeyboard.resetLayout();
        autoKeyboard.drawLayout();
        write("rightwipe");
        mtouchinfo.upKey = KEY_NOT_FOUND;
        if (currentCandidate >= 0 && currentCandidate < candidates.size()) {
            if (languageMode == LANG_MODE_CHN){
                String delta = candidates.get(currentCandidate).alias;
                tts("确认输入");
                try {
                    int temp;
                    if (currentWord.length() == 0)
                        temp = 1;
                    else
                        temp = mIPinyinDecoderService.imChoose(currentCandidate);
                    if (temp == 1){
                        if (currentWord.length() == 0)
                            currentInput += delta;
                        else
                            currentInput += mIPinyinDecoderService.imGetChoice(0);
                        currentWord = "";
                        currentBaseline = "";
                        currentBaselineDown = "";
                        currentPredictDown = "";
                        mIPinyinDecoderService.imResetSearch();
                        if (predictEnableMode == PREDICT_OFF){
                            predict(currentWord);
                        }
                        else {
                            String predictString = "";
                            if (currentInput.length() > mpara.MAX_PREDICT_LENGTH){
                                predictString = currentInput.substring(currentInput.length() - mpara.MAX_PREDICT_LENGTH);
                            }
                            else{
                                predictString = new String(currentInput);
                            }
                            //int len = mIPinyinDecoderService.imGetPredictsNum(currentInput);
                            int len = mIPinyinDecoderService.imGetPredictsNum(predictString);
                            List<String> templist = mIPinyinDecoderService.imGetPredictList(0, len);
                            candidates.clear();
                            for (int i = 0; i < len; ++i)
                                candidates.add(new Word(templist.get(i), 0));
                            if (autoreadMode == AUTOREAD_ON)
                                tts("推荐候选");
                        }
                        currentCandidate = autoreadMode;
                        refresh();
                        return;
                    }
                }catch (RemoteException e){
                    e.printStackTrace();
                }
                predict(currentWord);
                currentCandidate = autoreadMode;
                refresh();
                return;
            }
            String delta = candidates.get(currentCandidate).alias;
            char lastCh = delta.charAt(delta.length() - 1);
            currentInput += delta + " ";
            currentWord = "";
            currentBaseline = "";
            currentBaselineDown = "";
            currentPredictDown = "";
            predict(currentWord);
            currentCandidate = autoreadMode;
            refresh();
            tts("确认输入");
        }
        else if (currentWord.length() == 0){
            currentInput += " ";
            tts("空格");
        }
        refresh();
    }

    public void actionLeftwipe(){
        autoKeyboard.resetLayout();
        autoKeyboard.drawLayout();
        stopInput(true);
        write("leftwipe");
        mtouchinfo.upKey = KEY_NOT_FOUND;
        if (languageMode == LANG_MODE_CHN){
            try{
                if (mIPinyinDecoderService.imGetFixedLen() > 0){
                    nowChSaved = '*';
                    currentCandidate = autoreadMode;
                    tts("删除");
                    autoKeyboard.resetLayout();
                    autoKeyboard.drawLayout();
                    mIPinyinDecoderService.imCancelLastChoice();
                    predict(currentWord);
                    refresh();
                }
                else{
                    String deleted = deleteLastChar();
                    nowChSaved = '*';
                    currentCandidate = autoreadMode;
                    tts("删除");
                    if (deleted.length() > 0 && deleted.charAt(0) >= 'a' && deleted.charAt(0) <= 'z' && currentWord.length() == 0){
                        tts("拼音已清空");
                    }
                    autoKeyboard.resetLayout();
                    autoKeyboard.drawLayout();
                    predict(currentWord);
                    refresh();
                }
            }catch (RemoteException e){
            }
            return;
        }
        String deleted = deleteLastChar();
        nowChSaved = '*';
        currentCandidate = autoreadMode;
        tts("删除");
        refresh();
    }

    public void actionPointerDown(MotionEvent event){
        downX2 = (int)event.getX(0);
        downY2 = (int)event.getY(0);
        downTime2 = event.getEventTime();
        mtouchinfo.upKey = KEY_NOT_FOUND;
    }

    public void actionPointerUp(MotionEvent event){
        stopInput(true);
        if (checkUpwipe2((int)event.getX(0), (int)event.getY(0), event.getEventTime())){
            write("up2wipe");
            if (currentInput.length() == 0){
                tts("当前还没有输入");
            }
            else{
                tts("当前输入" + currentInput);
            }
        }
        else if (checkRightwipe2((int)event.getX(0), (int)event.getY(0), event.getEventTime())) {
            write("right2wipe");
            if (recordMode == RECORD_MODE_STARTED && currentInput.equals(testcases.get(currentTestcase))){
                if (currentTestcase + 1 < TESTCASE_TURN)
                    nextTestcase();
                else{
                    recordMode = RECORD_MODE_STOPED;
                    setMenuTitle();
                    tts("实验结束，谢谢您的配合");
                }
                deleteAllChar();
                currentCandidate = autoreadMode;
                elapsedTimeText.setText("0");
                mtouchinfo.upKey = KEY_NOT_FOUND;
                nowChSaved = '*';
                currentInput = "";
                refresh();
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
            }
            else{
                tts("当前输入 " + currentInput);
            }
        }
        else if (checkLeftwipe2((int)event.getX(0), (int)event.getY(0), event.getEventTime())){
            write("left2wipe");
            deleteAllChar();
            currentCandidate = autoreadMode;
            elapsedTimeText.setText("0");
            mtouchinfo.upKey = KEY_NOT_FOUND;
            nowChSaved = '*';
            tts("清空");
            currentInput = "";
            refresh();
            autoKeyboard.resetLayout();
            autoKeyboard.drawLayout();
        }
        autoKeyboard.resetLayout();
        autoKeyboard.drawLayout();
        nowCh = KEY_NOT_FOUND;
    }

    public void actionUpwipe(){
        stopInput(true);
        write("upwipe");
        currentCandidate = Math.max(currentCandidate - 1, 0);
        if (currentWord.length() > 0 && candidates.size() == 0){
            tts("无候选词");
        }
        autoKeyboard.resetLayout();
        autoKeyboard.drawLayout();
        mtouchinfo.upKey = KEY_NOT_FOUND;
        refresh();
    }

    public void actionDownwipe(){
        stopInput(true);
        write("downwipe");
        if (currentCandidate + 1 < candidates.size())
            ++currentCandidate;
        if (currentWord.length() > 0 && candidates.size() == 0){
            tts("无候选词");
        }
        autoKeyboard.resetLayout();
        autoKeyboard.drawLayout();
        mtouchinfo.upKey = KEY_NOT_FOUND;
        refresh();
    }

    public boolean onTouchEvent(MotionEvent event){
        int[] location = new int[2];
        keyboard.getLocationOnScreen(location);
        int x = (int)event.getX();
        int y = (int)event.getY();
        HideSeekBar(event);
        switch(activity_mode){
            case KEYBOARD_MODE:{
                if (event.getPointerCount() == 2){
                    mtouchinfo.TwoFingersFlag = true;
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_POINTER_DOWN:
                            actionPointerDown(event);
                            break;
                        case MotionEvent.ACTION_POINTER_UP:
                            actionPointerUp(event);
                            break;
                    }
                }
                if (!mtouchinfo.TwoFingersFlag && event.getPointerCount() == 1){
                //if (!TwoFingersFlag && event.getPointerCount() == 1 && (autoKeyboard.getKeyByPosition(x, y - location[1], autoKeyboard.CURR_LAYOUT) == upKey
                //        || autoKeyboard.getKeyByPosition(x, y - location[1], autoKeyboard.INIT_LAYOUT) != KEY_NOT_FOUND)){ // in the keyboard area
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
                            stopInput(false);
                            if (confirmMode == CONFIRM_MODE_UP) {
                                if (checkLeftwipe(x, y, tempTime)) {
                                    actionLeftwipe();
                                } else if (checkRightwipe(x, y, tempTime)) {
                                    actionRightwipe();
                                } else if (checkUpwipe(x, y, tempTime)) {
                                    actionUpwipe();
                                } else if (checkDownwipe(x, y, tempTime)) {
                                    actionDownwipe();
                                } else {
                                    currentCandidate = autoreadMode;
                                    mtouchinfo.upKey = autoKeyboard.getKeyByPosition(x, y - location[1], autoKeyboard.CURR_LAYOUT);
                                    //if (upvoiceMode == UPVOICE_MODE_YES && nowCh >= 'a' && nowCh <= 'z') {
                                    //    playMedia("ios11_" + voiceSpeed, nowCh - 'a', true);
                                    //}
                                    if (nowCh != KEY_NOT_FOUND) {
                                        write("enter " + nowCh + " " + ((nowPredictDownSaved != KEY_NOT_FOUND) ? nowPredictDownSaved : '*') + " " + ((nowBaseLineDownSaved != KEY_NOT_FOUND) ? nowBaseLineDownSaved : '*'));
                                        currentWord += nowCh;
                                        currentPredictDown += (nowPredictDownSaved != KEY_NOT_FOUND) ? nowPredictDownSaved : '*';
                                        currentBaselineDown += (nowBaseLineDownSaved != KEY_NOT_FOUND) ? nowBaseLineDownSaved : '*';
                                        currentBaseline += autoKeyboard.getKeyByPosition(x, y - location[1], autoKeyboard.INIT_LAYOUT);
                                    }
                                    predict(currentWord);
                                    refresh();
                                }
                            }
                            else{
                                if (checkLeftwipe(x, y, tempTime)) {
                                    actionLeftwipe();
                                } else if (checkRightwipe(x, y, tempTime)) {
                                    actionRightwipe();
                                } else if (checkUpwipe(x, y, tempTime)) {
                                    actionUpwipe();
                                    //todo
                                } else if (checkDownwipe(x, y, tempTime)) {
                                    actionDownwipe();
                                    //todo
                                } else if (downTime == lastDownTime && tempTime - firstDownTime < 800) {
                                    //double click
                                    currentCandidate = autoreadMode;
                                    if (nowChSaved != '*'){
                                        write("doubleclick");
                                        currentWord += nowChSaved;
                                        currentPredictDown += (nowPredictDownSaved != KEY_NOT_FOUND) ? nowPredictDownSaved : '*';
                                        currentBaselineDown += (nowBaseLineDownSaved != KEY_NOT_FOUND) ? nowBaseLineDownSaved : '*';
                                        write("enter " + nowChSaved + " " + ((nowPredictDownSaved != KEY_NOT_FOUND) ? nowPredictDownSaved : '*') + " " + ((nowBaseLineDownSaved != KEY_NOT_FOUND) ? nowBaseLineDownSaved : '*'));
                                        /*if (upvoiceMode == UPVOICE_MODE_YES) {
                                            playMedia("ios11_" + voiceSpeed, nowChSaved - 'a', true);
                                        }
                                        else{
                                            playMedia("delete", 0, false);
                                        }*/
                                        currentBaseline += nowChBaselineSaved;
                                        predict(currentWord);
                                        refresh();
                                    }
                                }
                                else{
                                    if (tempTime - downTime > 300 && nowCh != KEY_NOT_FOUND) {
                                        mtouchinfo.upKey = autoKeyboard.getKeyByPosition(x, y - location[1], autoKeyboard.CURR_LAYOUT);
                                        nowChSaved = nowCh;
                                        nowPredictDownSaved = nowPredictDown;
                                        nowBaseLineDownSaved = nowBaseLineDown;
                                        nowChBaselineSaved = autoKeyboard.getKeyByPosition(x, y - location[1],autoKeyboard.INIT_LAYOUT);
                                    }
                                }
                            }
                            if (currentCandidate >= 0 && currentCandidate < candidates.size())
                                tts(candidates.get(currentCandidate).alias + getChnHint(candidates.get(currentCandidate).alias));
                            write("word " + currentWord);
                            nowCh = KEY_NOT_FOUND;
                            break;
                    }
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP){
                    mtouchinfo.TwoFingersFlag = false;
                    mtouchinfo.inKeyboard = true;
                }
                break;
            }
            case FUZZY_INPUT_TEST_MODE: {
                y=y-location[1];
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN: {
                            if(!fu.ifBegin){
                                fu.ifBegin=true;
                            }
                            char ch = KEY_NOT_FOUND;
                            switch (fuMode) {
                                case Target:{
                                    ch = fu.getTarget();
                                    break;
                                }
                                case Current:{
                                    ch = autoKeyboard.getKeyByPosition(x,y,autoKeyboard.CURR_LAYOUT);
                                    break;
                                }
                            }
                            stopVoice();
                            if(ch==KEY_NOT_FOUND){
                                tts("出界");
                            }else {
                                playMedia("ios11_"+voiceSpeed, ch - 'a',true);
                            }
                            if(fu.index<fu.chineseNum) {
                                fu.addSentence(event);
                            }
                            lastChar=ch;
                            break;
                        }
                        case MotionEvent.ACTION_MOVE: {
                            char ch = KEY_NOT_FOUND;
                            switch (fuMode) {
                                case Target:{
                                    ch = fu.getTarget();
                                    break;
                                }
                                case Current:{
                                    ch = autoKeyboard.getKeyByPosition(x,y,autoKeyboard.CURR_LAYOUT);
                                    break;
                                }
                            }
                            if(fu.index<fu.chineseNum)
                                fu.addSentence(event);
                            if(ch!=lastChar ){
                                stopVoice();
                                if(ch==KEY_NOT_FOUND){
                                    tts("出界");
                                }else{
                                    playMedia("ios11da", 0,false);
                                    Vibrator vibrator =  (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                    long[] pattern = {0, 30};
                                    vibrator.vibrate(pattern, -1);
                                    playMedia("ios11_"+voiceSpeed, ch - 'a',true);
                                }
                            }
                            lastChar=ch;
                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            if(fu.ifBegin){
                                if(fu.index<fu.chineseNum)
                                    fu.addSentence(event);
                            }
                            break;
                        }
                        default:
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

        Word(Word tmp){
            this.text = tmp.text;
            this.alias = tmp.alias;
            this.freq = tmp.freq;
        }

        @Override
        public int compareTo(Word o){
            if (this.freq > o.freq) return -1;
            if (this.freq < o.freq) return 1;
            return 0;
        }
    }
    // AutoKeyboard
    class AutoKeyboard{
        ImageView keyboard;
        Canvas canvas;
        Bitmap baseBitmap;
        Paint backgroundPaint,textPaint,boundPaint,movePaint,originPaint,rankingPaint;
        boolean Visibility=true;
        boolean Outline = false;
        boolean Ranking = true;
        boolean FunctionKeys = false;
        float screen_width_ratio = 1F;
        float screen_height_ratio = 1F;
        //Fuzzy Input Test Var
        float keyboardHeight;// the height of the keyboard
        float keyboardWidth;// the width of the keyboard
        float baseImageWidth;
        float baseImageHeight;
        float topThreshold;// the upper bound
        float bottomThreshold;// the lower bound
        float minWidthRatio;// the minimum width of a key
        float minHeightRatio;// the minimum height of a key
        int[] keyPos={10,24,22,12,2,13,14,15,7,16,17,18,26,25,8,9,0,3,11,4,6,23,1,21,5,20};// A-Z to the position in the keyboard
        int[] location;// the coordinate of the left top corner of the keyboard
        int[] allChar={0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,20,21,22,23,24,25,26};
        final int KEYNUM=33;
        final int Q=0;
        final int W=1;
        final int O=8;
        final int P=9;
        final int A=10;
        final int S=11;
        final int K=17;
        final int L=18;
        final int SHIFT=19;
        final int Z=20;
        final int X=21;
        final int N=25;
        final int M=26;
        final int BACKSPACE=27;
        final int SYMBOL=28;
        final int LANGUAGE=29;
        final int SPACE=30;
        final int COMMA=31;
        final int PERIOD=32;

        final char shiftCh=KEY_NOT_FOUND;
        final char symbolCh=KEY_NOT_FOUND;
        final char languageCh=KEY_NOT_FOUND;
        final char spaceCh=KEY_NOT_FOUND;
        final char commaCh=KEY_NOT_FOUND;
        final char periodCh=KEY_NOT_FOUND;
        final char backspaceCh=KEY_NOT_FOUND;


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
        float exponent;// 0.05 ~ 0.5
        int scalingNum;// left and shift scaling number of keys
        int try_layout_mode;// BODILY_MOVEMENT RESPECTIVELY_MOVEMENT
        int getKey_mode;// STRICT_MODE LOOSE_MODE
        float[][][] expRatio=new float[6][3][6];//scalingNum,Line,index
        float tapRange;// 0.5 ~ 1
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
                    return init_y+init_height/2F;
                }else if(mode==CURR_LAYOUT){
                    return curr_y+curr_height/2F;
                }else{
                    return test_y+test_height/2F;
                }
            }
            float getTop(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_y-init_height/2F;
                }else if(mode==CURR_LAYOUT){
                    return curr_y-curr_height/2F;
                }else{
                    return test_y-test_height/2F;
                }
            }
            float getLeft(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_x-init_width/2F;
                }else if(mode==CURR_LAYOUT){
                    return curr_x-curr_width/2F;
                }else{
                    return test_x-test_width/2F;
                }
            }
            float getRight(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_x+init_width/2F;
                }else if(mode==CURR_LAYOUT){
                    return curr_x+curr_width/2F;
                }else{
                    return test_x+test_width/2F;
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
                    return init_y+init_height*tapRange/2F;
                }else if(mode==CURR_LAYOUT){
                    return curr_y+curr_height*tapRange/2F;
                }else{
                    return test_y+test_height*tapRange/2F;
                }
            }
            float getTop_tap(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_y-init_height*tapRange/2F;
                }else if(mode==CURR_LAYOUT){
                    return curr_y-curr_height*tapRange/2F;
                }else{
                    return test_y-test_height*tapRange/2F;
                }
            }
            float getLeft_tap(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_x-init_width*tapRange/2F;
                }else if(mode==CURR_LAYOUT){
                    return curr_x-curr_width*tapRange/2F;
                }else{
                    return test_x-test_width*tapRange/2F;
                }
            }
            float getRight_tap(int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==INIT_LAYOUT){
                    return init_x+init_width*tapRange/2F;
                }else if(mode==CURR_LAYOUT){
                    return curr_x+curr_width*tapRange/2F;
                }else{
                    return test_x+test_width*tapRange/2F;
                }
            }
            void reset(){
                curr_x=init_x;
                curr_y=init_y;
                curr_height=init_height;
                curr_width=init_width;
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
        KEY keys[]; // A-Z

        void defaultPara(){// set the parameters to default value
            keyboardHeight=800*screen_height_ratio;
            keyboardWidth=1440*screen_width_ratio;
            topThreshold=0*screen_height_ratio;
            bottomThreshold=907*screen_height_ratio;
            minWidthRatio=1F/2F;
            minHeightRatio=1F/2F;
            scalingNum=3;
            try_layout_mode=RESPECTIVELY_MOVEMENT;
            getKey_mode=LOOSE_MODE;
            tapRange=8F/10F;
            scalingMode=LINEAR_MODE;
            exponent=1F/10F;
            FunctionKeys = false;
            Outline = false;
            Ranking = true;
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

            expRatio[SCALINGNUM_1][MIDDLE][0]=2F/3F;
            expRatio[SCALINGNUM_1][MIDDLE][1]=1F/3F;

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

            expRatio[SCALINGNUM_1][BOTTOM][0]=2F/3F;
            expRatio[SCALINGNUM_1][BOTTOM][1]=1F/3F;

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
            return this.keys[SPACE].getBottom(mode);
        }
        float getTop(int mode){
            return this.keys[Q].getTop(mode);
        }
        float getLeft(int mode){
            return this.keys[Q].getLeft(mode);
        }
        float getRight(int mode){
            return this.keys[P].getRight(mode);
        }
        void getScreenSizeRatio(){
            this.screen_width_ratio = keyboard.getWidth()/1440F;
            this.screen_height_ratio = keyboard.getHeight()/907F;
            /*
            DisplayMetrics metrics =new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            this.screen_width_ratio = metrics.widthPixels/1440F;
            this.screen_height_ratio = metrics.heightPixels/2560F;
            */
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
            for (int i=0;i<KEYNUM;i++){
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
        public char getKeyByPosition(MotionEvent event,int mode){
            return getKeyByPosition(event.getX()-location[0],event.getY()-location[1],mode);
        }
        public String toString(){
            return "AutoKeyboard,keyboardHeight:"+keyboardHeight+",keyboardWidth:"+keyboardWidth+"\n";
        }
        boolean shift_y(int pos,float dY){
            // mode==0 bodily movement
            // mode==1 respectively movement
            if(pos==SHIFT||pos==BACKSPACE||pos>=SYMBOL){
                return false;
            }
            if(dY==0){
                for(int i=0;i<KEYNUM;i++){
                    this.keys[i].curr_y=this.keys[i].init_y;
                    this.keys[i].curr_height=this.keys[i].init_height;
                }
                return true;
            }
            else if(dY>0){// downward movement, only compress
                if( keys[SYMBOL].init_y+dY>=bottomThreshold){
                    return false;
                }else{
                    if(pos>=SHIFT) //the third line
                    {
                        for(int i=Q;i<=BACKSPACE;i++){
                            this.keys[i].test_y=this.keys[i].init_y+dY;
                            this.keys[i].test_height=this.keys[i].init_height;
                        }
                        for(int i=SYMBOL;i<=PERIOD;i++){
                            this.keys[i].test_height=bottomThreshold-this.keys[SHIFT].getBottom(TEST_LAYOUT);
                            this.keys[i].test_y=this.keys[SHIFT].getBottom(TEST_LAYOUT)+this.keys[i].test_height/2F;

                        }
                    }else if(pos>=A)// the second line
                    {
                        for(int i=Q;i<=L;i++) {
                            this.keys[i].test_y = this.keys[i].init_y + dY;
                            this.keys[i].test_height = this.keys[i].init_height;
                        }
                        for(int i=SHIFT;i<=BACKSPACE;i++){
                            this.keys[i].test_height=(bottomThreshold-this.keys[A].getBottom(TEST_LAYOUT))/2F;
                            this.keys[i].test_y=this.keys[A].getBottom(TEST_LAYOUT)+this.keys[i].test_height/2F;
                        }
                        for(int i=SYMBOL;i<=PERIOD;i++){
                            this.keys[i].test_height=this.keys[SHIFT].test_height;
                            this.keys[i].test_y=this.keys[Z].getBottom(TEST_LAYOUT)+this.keys[i].test_height/2F;
                        }
                    }else // the first line
                    {
                        for(int i=Q;i<=P;i++) {
                            this.keys[i].test_y = this.keys[i].init_y + dY;
                            this.keys[i].test_height = this.keys[i].init_height;
                        }
                        for(int i=A;i<=L;i++){
                            this.keys[i].test_height=(bottomThreshold-this.keys[Q].getBottom(TEST_LAYOUT))/3F;
                            this.keys[i].test_y=this.keys[Q].getBottom(TEST_LAYOUT)+this.keys[i].test_height/2F;
                        }
                        for(int i=SHIFT;i<=BACKSPACE;i++){
                            this.keys[i].test_height=this.keys[A].test_height;
                            this.keys[i].test_y=this.keys[A].getBottom(TEST_LAYOUT)+this.keys[i].test_height/2F;
                        }
                        for(int i=SYMBOL;i<=PERIOD;i++){
                            this.keys[i].test_height=this.keys[SHIFT].test_height;
                            this.keys[i].test_y=this.keys[SHIFT].getBottom(TEST_LAYOUT)+this.keys[i].test_height/2F;
                        }
                    }
                }
            }else {// upward movement, move or compress
                if (keys[Q].init_y + dY <= topThreshold) {
                    return false;
                }
                if (keys[Q].getTop(INIT_LAYOUT) + dY > topThreshold) {// no compress
                    for (int i = 0; i < KEYNUM; i++) {
                        keys[i].test_y = keys[i].init_y + dY;
                        keys[i].test_height = keys[i].init_height;
                    }
                } else {// compress
                    if (pos <=L ){ // the first and the second line
                        for (int i = A; i <= PERIOD; i++) {
                            this.keys[i].test_y = this.keys[i].init_y + dY;
                            this.keys[i].test_height = this.keys[i].init_height;
                        }
                        for (int i = Q; i <= P; i++) {
                            this.keys[i].test_height =this.keys[A].getTop(TEST_LAYOUT)-topThreshold;
                            this.keys[i].test_y =topThreshold+this.keys[i].test_height/2F;
                        }
                    }else // the third line
                        for (int i = SHIFT; i <= PERIOD; i++) {
                            this.keys[i].test_y = this.keys[i].init_y + dY;
                            this.keys[i].test_height = this.keys[i].init_height;
                        }
                    for (int i = Q; i <= P; i++) {
                        this.keys[i].test_height = (this.keys[SHIFT].getTop(TEST_LAYOUT)-topThreshold)/2F;
                        this.keys[i].test_y =topThreshold+keys[i].test_height / 2F;
                    }
                    for (int i = A; i <= L; i++) {
                        this.keys[i].test_height = this.keys[Q].test_height;
                        this.keys[i].test_y =this.keys[Q].getBottom(TEST_LAYOUT)+this.keys[i].test_height / 2F;
                    }
                }
            }
            // check the height of each key
            float minHeight=minHeightRatio*(keyboardHeight/4F);
            for (int i=0;i<KEYNUM;i++){
                if(this.keys[i].test_height<minHeight){
                    return false;
                }
            }
            for (int i=0;i<KEYNUM;i++){
                this.keys[i].curr_y=this.keys[i].test_y;
                this.keys[i].curr_height=this.keys[i].test_height;
            }
            return true;
        }
        boolean shift_x(int pos,float dX){
            // mode==0 bodily movement
            // mode==1 respectively movement
            if(pos==SHIFT|| pos==BACKSPACE|| pos>=SYMBOL){
                return false;
            }
            if(dX==0){
                for(int i=0;i<KEYNUM;i++){
                    this.keys[i].curr_x=this.keys[i].init_x;
                    this.keys[i].curr_width=this.keys[i].init_width;
                }
                return true;
            }
            if(dX>=0) {// right shift
                if (pos ==P || pos == L || pos == BACKSPACE|| pos==PERIOD)
                    return false;
            }else {// left shift
                if (pos == Q || pos == A || pos == SHIFT || pos ==SYMBOL)
                    return false;
            }

            if(pos<=P){ // the first line
                int begin=Math.min(pos,Q+scalingNum);
                int end=Math.max(pos,P-scalingNum);
                // move the first line
                for(int i=begin;i<=end;i++){
                    this.keys[i].test_x=this.keys[i].init_x+dX;
                    this.keys[i].test_width=this.keys[i].init_width;
                }
                // scale the first line
                if(scalingMode==LINEAR_MODE){
                    float leftRatio=this.keys[begin].getLeft(TEST_LAYOUT)/this.keys[begin].getLeft(INIT_LAYOUT);
                    for(int i=Q;i<begin;i++) {
                        this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                        this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                    }
                    float rightRatio=(keyboardWidth-this.keys[end].getRight(TEST_LAYOUT))/(keyboardWidth-this.keys[end].getRight(INIT_LAYOUT));
                    for (int i=end+1;i<=P;i++){
                        this.keys[i].test_x=keyboardWidth-(keyboardWidth-this.keys[i].init_x)*rightRatio;
                        this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                    }
                }else{
                    int leftScalingNum=begin-Q;
                    int rightScalingNum=P-end;
                    if(leftScalingNum>scalingNum || rightScalingNum>scalingNum || leftScalingNum<=0||rightScalingNum<=0){
                        Log.e("scaling num error","top pos:"+String.valueOf(pos));
                    }
                    float leftLength=this.keys[begin].getLeft(TEST_LAYOUT);
                    float rightLength=keyboardWidth-this.keys[end].getRight(TEST_LAYOUT);
                    for (int i=begin-1;i>=Q;i--){
                        int index=begin-1-i;
                        this.keys[i].test_width=leftLength*expRatio[leftScalingNum][TOP][index];
                        this.keys[i].test_x=this.keys[i+1].getLeft(TEST_LAYOUT)-this.keys[i].test_width/2F;
                    }
                    for (int i=end+1;i<=P;i++){
                        int index=i-end-1;
                        this.keys[i].test_width=rightLength*expRatio[rightScalingNum][TOP][index];
                        this.keys[i].test_x=this.keys[i-1].getRight(TEST_LAYOUT)+this.keys[i].test_width/2F;
                    }
                }
                if(try_layout_mode==BODILY_MOVEMENT){// BODILY MOVEMENT
                    // match the second line
                    for (int i=A;i<=L;i++){
                        this.keys[i].test_x=(this.keys[i-(A-Q)].test_x+this.keys[i-(A-W)].test_x)/2F;
                        this.keys[i].test_width=this.keys[i-(A-W)].test_x-this.keys[i-(A-Q)].test_x;
                    }
                    // match the third line
                    for (int i=Z;i<=M;i++){
                        this.keys[i].test_x=this.keys[i-(Z-S)].test_x;
                        this.keys[i].test_width=this.keys[i-(Z-S)].test_width;
                    }
                }else{// RESPECTIVELY MOVEMENT
                    // match the second line
                    for (int i=A;i<=L;i++){
                        this.keys[i].test_x=this.keys[i].init_x;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }
                    // match the third line
                    for (int i=Z;i<=M;i++){
                        this.keys[i].test_x=this.keys[i].init_x;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }
                }
            }else if(pos<=L){// the second line
                //this.keys[pos].test_x=x;
                // this.keys[pos].test_width=this.keys[pos].init_width;

                int begin=Math.min(pos,A+scalingNum);
                int end=Math.max(pos,L-scalingNum);
                // move the second line
                for(int i=begin;i<=end;i++){
                    this.keys[i].test_x=this.keys[i].init_x+dX;
                    this.keys[i].test_width=this.keys[i].init_width;
                }

                // scale the second line
                if(scalingMode==LINEAR_MODE){
                    float leftRatio=this.keys[begin].getLeft(TEST_LAYOUT)/this.keys[begin].getLeft(INIT_LAYOUT);
                    for(int i=A;i<begin;i++) {
                        this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                        this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                    }
                    float rightRatio=(keyboardWidth-this.keys[end].getRight(TEST_LAYOUT))/(keyboardWidth-this.keys[end].getRight(INIT_LAYOUT));
                    for (int i=end+1;i<=L;i++){
                        this.keys[i].test_x=keyboardWidth-(keyboardWidth-this.keys[i].init_x)*rightRatio;
                        this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                    }
                }else{
                    int leftScalingNum=begin-A;
                    int rightScalingNum=L-end;
                    if(leftScalingNum>scalingNum || rightScalingNum>scalingNum || leftScalingNum<=0||rightScalingNum<=0){
                        Log.e("scaling num error","middle pos:"+String.valueOf(pos));
                    }
                    float leftLength=this.keys[begin].getLeft(TEST_LAYOUT);
                    float rightLength=keyboardWidth-this.keys[end].getRight(TEST_LAYOUT);
                    for (int i=begin-1;i>=A;i--){
                        int index=begin-1-i;
                        this.keys[i].test_width=leftLength*expRatio[leftScalingNum][MIDDLE][index];
                        this.keys[i].test_x=this.keys[i+1].getLeft(TEST_LAYOUT)-this.keys[i].test_width/2;
                    }
                    for (int i=end+1;i<=L;i++){
                        int index=i-end-1;
                        this.keys[i].test_width=rightLength*expRatio[rightScalingNum][MIDDLE][index];
                        this.keys[i].test_x=this.keys[i-1].getRight(TEST_LAYOUT)+this.keys[i].test_width/2F;
                    }
                }
                if(try_layout_mode==BODILY_MOVEMENT) {// BODILY MOVEMENT
                    //  match the first line
                    this.keys[Q].test_x = this.keys[A].test_x / 2F;
                    this.keys[Q].test_width = this.keys[A].test_x;
                    this.keys[P].test_width = this.keyboardWidth - this.keys[L].test_x;
                    this.keys[P].test_x = this.keyboardWidth - this.keys[L].test_width / 2F;
                    for (int i = W; i <=O ; i++) {
                        this.keys[i].test_x = (this.keys[i + (A - W)].test_x + this.keys[i + (S - W)].test_x) / 2F;
                        this.keys[i].test_width = this.keys[i + (S - W)].test_x - this.keys[i + (A - W)].test_x;
                    }
                    // match the third line
                    for (int i = Z; i <= M; i++) {
                        this.keys[i].test_x = this.keys[i - (Z - S)].test_x;
                        this.keys[i].test_width = this.keys[i - (Z - S)].test_width;
                    }
                }else{// RESPECTIVELY MOVEMENT
                    // match the first line
                    for (int i=Q;i<=P;i++){
                        this.keys[i].test_x=this.keys[i].init_x;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }
                    // match the third line
                    for (int i=Z;i<=M;i++){
                        this.keys[i].test_x=this.keys[i].init_x;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }

                }
            }else{// the third line
                int begin=Math.min(pos,SHIFT+scalingNum);
                int end=Math.max(pos,BACKSPACE-scalingNum);

                if(scalingMode==LINEAR_MODE){
                    // move the third line
                    for(int i=begin;i<=end;i++){
                        this.keys[i].test_x=this.keys[i].init_x+dX;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }
                    // scale the third line
                    float leftRatio=this.keys[begin].getLeft(TEST_LAYOUT)/this.keys[begin].getLeft(INIT_LAYOUT);
                    for(int i=SHIFT;i<begin;i++) {
                        this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                        this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                    }
                    float rightRatio=(keyboardWidth-this.keys[end].getRight(TEST_LAYOUT))/(keyboardWidth-this.keys[end].getRight(INIT_LAYOUT));
                    for (int i=end+1;i<=BACKSPACE;i++){
                        this.keys[i].test_x=keyboardWidth-(keyboardWidth-this.keys[i].init_x)*rightRatio;
                        this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                    }
                }else{
                    int leftScalingNum=begin-SHIFT;
                    int rightScalingNum=BACKSPACE-end;
                    if(leftScalingNum>scalingNum || rightScalingNum>scalingNum || leftScalingNum<=0||rightScalingNum<=0){
                        Log.e("scaling num error","bottom pos:"+String.valueOf(pos));
                    }
                    float leftLength=this.keys[begin].getLeft(TEST_LAYOUT);
                    float rightLength=keyboardWidth-this.keys[end].getRight(TEST_LAYOUT);
                    for (int i=begin-1;i>=SHIFT;i--){
                        int index=begin-1-i;
                        this.keys[i].test_width=leftLength*expRatio[leftScalingNum][BOTTOM][index];
                        this.keys[i].test_x=this.keys[i+1].getLeft(TEST_LAYOUT)-this.keys[i].test_width/2F;
                    }
                    for (int i=end+1;i<=BACKSPACE;i++){
                        int index=i-end-1;
                        this.keys[i].test_width=rightLength*expRatio[leftScalingNum][BOTTOM][index];
                        this.keys[i].test_x=this.keys[i-1].getRight(TEST_LAYOUT)+this.keys[i].test_width/2F;
                    }
                }
                if(try_layout_mode==BODILY_MOVEMENT) {// BODILY MOVEMENT
                    // match the second line
                    for (int i = S; i <= K; i++) {
                        this.keys[i].test_x = this.keys[i + (Z - S)].test_x;
                        this.keys[i].test_width = this.keys[i + (Z - S)].test_width;
                    }
                    this.keys[A].test_width = this.keys[S].getLeft(TEST_LAYOUT)*2F/3F;
                    this.keys[A].test_x = this.keys[A].test_width;
                    this.keys[L].test_width =(this.keyboardWidth - this.keys[K].getRight(TEST_LAYOUT))*2F/3F;
                    this.keys[L].test_x = this.keys[K].getRight(TEST_LAYOUT)+this.keys[L].test_width/2F;

                    // match first line
                    this.keys[Q].test_x = this.keys[A].test_x / 2F;
                    this.keys[Q].test_width = this.keys[A].test_x;
                    this.keys[P].test_width = this.keyboardWidth - this.keys[L].test_x;
                    this.keys[P].test_x = this.keyboardWidth - this.keys[L].test_width / 2F;
                    for (int i = W; i <= O; i++) {
                        this.keys[i].test_x = (this.keys[i + (A - W)].test_x + this.keys[i + (S - W)].test_x) / 2F;
                        this.keys[i].test_width = this.keys[i + (S - W)].test_x - this.keys[i + (A - W)].test_x;
                    }
                }else{// RESPECTIVELY MOVEMENT
                    // match the first line
                    for (int i=Q;i<=P;i++){
                        this.keys[i].test_x=this.keys[i].init_x;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }
                    // match the second line
                    for (int i=A;i<=L;i++){
                        this.keys[i].test_x=this.keys[i].init_x;
                        this.keys[i].test_width=this.keys[i].init_width;
                    }

                }

            }
            if(try_layout_mode==BODILY_MOVEMENT) {// BODILY MOVEMENT
                // SHIFT
                this.keys[SHIFT].test_width = this.keys[Z].getLeft(TEST_LAYOUT);
                this.keys[SHIFT].test_x = this.keys[SHIFT].test_width / 2F;

                // BACKSPACE
                this.keys[BACKSPACE].test_width = keyboardWidth - this.keys[M].getRight(TEST_LAYOUT);
                this.keys[BACKSPACE].test_x = this.keys[M].getRight(TEST_LAYOUT) + this.keys[BACKSPACE].test_width / 2F;

                // match the fourth line

                // SYMBOL
                this.keys[SYMBOL].test_width = this.keys[SHIFT].test_width;
                this.keys[SYMBOL].test_x = this.keys[SHIFT].test_x;

                // LANGUAGE
                this.keys[LANGUAGE].test_width = this.keys[X].test_x - this.keys[Z].getLeft(TEST_LAYOUT);
                this.keys[LANGUAGE].test_x = this.keys[SYMBOL].getRight(TEST_LAYOUT) + this.keys[LANGUAGE].test_width / 2F;

                // PERIOD
                this.keys[PERIOD].test_width = this.keys[BACKSPACE].test_width;
                this.keys[PERIOD].test_x = this.keys[BACKSPACE].test_x;

                // COMMA
                this.keys[COMMA].test_width = this.keys[M].getRight(TEST_LAYOUT) - this.keys[N].test_x;
                this.keys[COMMA].test_x = this.keys[PERIOD].getLeft(TEST_LAYOUT) - this.keys[COMMA].test_width / 2F;

                // SPACE
                this.keys[SPACE].test_width = this.keys[COMMA].getLeft(TEST_LAYOUT) - this.keys[LANGUAGE].getRight(TEST_LAYOUT);
                this.keys[SPACE].test_x = this.keys[LANGUAGE].getRight(TEST_LAYOUT) + this.keys[SPACE].test_width / 2F;
            }else {// RESPECTIVELY MOVEMENT
                // match the fourth line
                for (int i = SYMBOL; i <= PERIOD; i++) {
                    this.keys[i].test_x = this.keys[i].init_x;
                    this.keys[i].test_width = this.keys[i].init_width;
                }
                // SHIFT
                this.keys[SHIFT].test_width = this.keys[Z].getLeft(TEST_LAYOUT);
                this.keys[SHIFT].test_x = this.keys[SHIFT].test_width / 2F;

                // BACKSPACE
                this.keys[BACKSPACE].test_width = keyboardWidth - this.keys[M].getRight(TEST_LAYOUT);
                this.keys[BACKSPACE].test_x = this.keys[M].getRight(TEST_LAYOUT) + this.keys[BACKSPACE].test_width / 2F;
            }
            // check the width of each key
            float minWidth=minWidthRatio*keyboardWidth/10F;
            for (int i=0;i<KEYNUM;i++){
                if(this.keys[i].test_width<minWidth){
                    return false;
                }
            }
            for (int i=0;i<KEYNUM;i++){
                this.keys[i].curr_x=this.keys[i].test_x;
                this.keys[i].curr_width=this.keys[i].test_width;
            }
            return true;
        }
        public  boolean tryLayout(char ch,float x,float y){
            // mode==0 bodily movement
            // mode==1 respectively movement
            if(ch==KEY_NOT_FOUND){
                return false;
            }
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
        public void drawLayout(){
            this.drawLayout("");
        }
        public void resetLayout(){

            if(this.keys==null){
                this.keys=new KEY[KEYNUM];
                for (int i=0;i<KEYNUM;i++){
                    this.keys[i]=new KEY();
                }
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
            this.keys[19].ch=shiftCh;
            this.keys[20].ch='z';
            this.keys[21].ch='x';
            this.keys[22].ch='c';
            this.keys[23].ch='v';
            this.keys[24].ch='b';
            this.keys[25].ch='n';
            this.keys[26].ch='m';
            this.keys[27].ch=backspaceCh;
            this.keys[28].ch=symbolCh;
            this.keys[29].ch=languageCh;
            this.keys[30].ch=spaceCh;
            this.keys[31].ch=commaCh;
            this.keys[32].ch=periodCh;

            for (int i=Q;i<=P;i++){
                this.keys[i].init_x=this.keyboardWidth*(2*i+1)/20F;
                this.keys[i].init_y=this.keyboardHeight/8F+(this.bottomThreshold-this.keyboardHeight);
            }
            for (int i=A;i<=L;i++){
                this.keys[i].init_x=(this.keys[i-(A-Q)].init_x+this.keys[i-(A-W)].init_x)/2F;
                this.keys[i].init_y=this.keyboardHeight*3F/8F+(this.bottomThreshold-this.keyboardHeight);
            }
            for (int i=Z;i<=M;i++){
                this.keys[i].init_x=this.keys[i-(Z-S)].init_x;
                this.keys[i].init_y=this.keyboardHeight*5F/8F+(this.bottomThreshold-this.keyboardHeight);
            }
            for (int i:allChar) {
                this.keys[i].init_height=this.keyboardHeight/4F;
                this.keys[i].init_width=this.keyboardWidth/10F;
            }
            // SHIFT
            this.keys[SHIFT].init_height=this.keyboardHeight/4F;
            this.keys[SHIFT].init_width=this.keys[Z].getLeft(INIT_LAYOUT);
            this.keys[SHIFT].init_x=this.keys[SHIFT].init_width/2F;
            this.keys[SHIFT].init_y=this.keys[Z].init_y;

            // BACKSPACE
            this.keys[BACKSPACE].init_height=this.keyboardHeight/4F;
            this.keys[BACKSPACE].init_width=this.keyboardWidth-this.keys[M].getRight(INIT_LAYOUT);
            this.keys[BACKSPACE].init_x=this.keys[M].getRight(INIT_LAYOUT)+this.keys[BACKSPACE].init_width/2F;
            this.keys[BACKSPACE].init_y=this.keys[M].init_y;

            // SYMBOL
            this.keys[SYMBOL].init_height=this.keyboardHeight/4F;
            this.keys[SYMBOL].init_width=this.keys[SHIFT].init_width;
            this.keys[SYMBOL].init_x=this.keys[SHIFT].init_x;
            this.keys[SYMBOL].init_y=this.keys[SHIFT].getBottom(INIT_LAYOUT)+this.keys[SYMBOL].init_height/2F;

            // LANGUAGE
            this.keys[LANGUAGE].init_height=this.keyboardHeight/4F;
            this.keys[LANGUAGE].init_width=this.keys[SYMBOL].init_width;
            this.keys[LANGUAGE].init_x=this.keys[SYMBOL].getRight(INIT_LAYOUT)+this.keys[LANGUAGE].init_width/2F;
            this.keys[LANGUAGE].init_y=this.keys[SYMBOL].init_y;

            // PERIOD
            this.keys[PERIOD].init_height=this.keyboardHeight/4F;
            this.keys[PERIOD].init_width=this.keys[BACKSPACE].init_width;
            this.keys[PERIOD].init_x=this.keys[BACKSPACE].init_x;
            this.keys[PERIOD].init_y=this.keys[SYMBOL].init_y;

            // COMMA
            this.keys[COMMA].init_height=this.keyboardHeight/4F;
            this.keys[COMMA].init_width=this.keys[BACKSPACE].init_width;
            this.keys[COMMA].init_x=this.keys[PERIOD].getLeft(INIT_LAYOUT)-this.keys[COMMA].init_width/2F;
            this.keys[COMMA].init_y=this.keys[SYMBOL].init_y;

            // SPACE
            this.keys[SPACE].init_height=this.keyboardHeight/4F;
            this.keys[SPACE].init_width=this.keys[COMMA].getLeft(INIT_LAYOUT)-this.keys[LANGUAGE].getRight(INIT_LAYOUT);
            this.keys[SPACE].init_x=this.keys[LANGUAGE].getRight(INIT_LAYOUT)+this.keys[SPACE].init_width/2F;
            this.keys[SPACE].init_y=this.keys[SYMBOL].init_y;
            // reset all the keys
            for (int i=0;i<KEYNUM;i++){
                this.keys[i].reset();
            }
        }
        public void drawLayout(String rank){ // curr_x,curr_y
            if(Visibility){
                this.baseBitmap = Bitmap.createBitmap(this.keyboard.getWidth(),this.keyboard.getHeight(), Bitmap.Config.ARGB_8888);
                this.canvas=new Canvas(this.baseBitmap);
                //RectF rect = new RectF(left, top, right, bottom);
                //this.canvas.drawRect(rect, this.backgroundPaint);

                Paint.FontMetrics fontMetrics = this.textPaint.getFontMetrics();
                float fonttop = fontMetrics.top;//为基线到字体上边框的距离
                float fontbottom = fontMetrics.bottom;//为基线到字体下边框的距离
                // 画底图上边界
                RectF upperBoundRect = new RectF(0,0,keyboardWidth,topThreshold);
                this.canvas.drawRect(upperBoundRect,this.boundPaint);
                // 画上可移动边界
                RectF upperMoveBoundRect = new RectF(0,topThreshold,keyboardWidth,keys[P].getTop(CURR_LAYOUT));
                this.canvas.drawRect(upperMoveBoundRect,this.movePaint);
                // 画下可移动边界
                RectF lowerMoveBound = new RectF(0,this.keys[SYMBOL].getBottom(CURR_LAYOUT),keyboardWidth,bottomThreshold);
                this.canvas.drawRect(lowerMoveBound,this.movePaint);
                // 画下边界
                RectF lowerBound = new RectF(0,bottomThreshold,keyboardWidth,baseImageHeight);
                this.canvas.drawRect(lowerBound,this.boundPaint);

                // 画边框
                for (int i=0;i<KEYNUM;i++){
                    RectF rect = new RectF(this.keys[i].getLeft(CURR_LAYOUT)+5*screen_width_ratio,this.keys[i].getTop(CURR_LAYOUT)+10*screen_height_ratio,this.keys[i].getRight(CURR_LAYOUT)-5*screen_width_ratio,this.keys[i].getBottom(CURR_LAYOUT)-10*screen_height_ratio);
                    this.canvas.drawRoundRect(rect,10,10,this.backgroundPaint);
                }
                // 画原始边框
                if(Outline){
                    for (int i=0;i<KEYNUM;i++){

                        float left=this.keys[i].getLeft(INIT_LAYOUT)+5*screen_width_ratio;
                        float top=this.keys[i].getTop(INIT_LAYOUT)+10*screen_height_ratio;
                        float right=this.keys[i].getRight(INIT_LAYOUT)-5*screen_width_ratio;
                        float bottom=this.keys[i].getBottom(INIT_LAYOUT)-10*screen_height_ratio;
                        this.canvas.drawLine(left,top,right,top,this.originPaint);
                        this.canvas.drawLine(left,top,left,bottom,this.originPaint);
                        this.canvas.drawLine(right,top,right,bottom,this.originPaint);
                        this.canvas.drawLine(left,bottom,right,bottom,this.originPaint);
                    /*
                    RectF rect = new RectF(this.keys[i].getLeft(INIT_LAYOUT)+5*screen_width_ratio,this.keys[i].getTop(INIT_LAYOUT)+10*screen_height_ratio,this.keys[i].getRight(INIT_LAYOUT)-5*screen_width_ratio,this.keys[i].getBottom(INIT_LAYOUT)-10*screen_height_ratio);
                    this.canvas.drawRoundRect(rect,10,10,this.originPaint);
                    */
                    }
                }

                // 画A-Z
                for (int i:allChar){
                    this.canvas.drawText(String.valueOf(this.keys[i].ch).toUpperCase(),this.keys[i].curr_x,this.keys[i].curr_y-fonttop/2F-fontbottom/2F,this.textPaint);
                }
                if(FunctionKeys){
                    // SHIFT

                    float shiftX=keys[SHIFT].curr_x;
                    float shiftY=keys[SHIFT].curr_y;
                    float shiftH=keys[SHIFT].curr_height;
                    float shiftW=keys[SHIFT].curr_width;
                    float[] shiftA={shiftX-shiftW/4,shiftY};
                    float[] shiftB={shiftX,shiftY-shiftH*3/8};
                    float[] shiftC={shiftX+shiftW/4,shiftY};
                    float[] shiftD={shiftX-shiftW/8,shiftY};
                    float[] shiftE={shiftX+shiftW/8,shiftY};
                    float[] shiftF={shiftX-shiftW/8,shiftY+shiftH*3/8};
                    float[] shiftG={shiftX+shiftW/8,shiftY+shiftH*3/8};
                    this.canvas.drawLine(shiftA[0],shiftA[1],shiftB[0],shiftB[1],textPaint);
                    this.canvas.drawLine(shiftB[0],shiftB[1],shiftC[0],shiftC[1],textPaint);
                    this.canvas.drawLine(shiftA[0],shiftA[1],shiftD[0],shiftD[1],textPaint);
                    this.canvas.drawLine(shiftE[0],shiftE[1],shiftC[0],shiftC[1],textPaint);
                    this.canvas.drawLine(shiftD[0],shiftD[1],shiftF[0],shiftF[1],textPaint);
                    this.canvas.drawLine(shiftE[0],shiftE[1],shiftG[0],shiftG[1],textPaint);
                    this.canvas.drawLine(shiftF[0],shiftF[1],shiftG[0],shiftG[1],textPaint);

                    // SYMBOL
                    this.canvas.drawText("123",this.keys[SYMBOL].curr_x,this.keys[SYMBOL].curr_y-fonttop/2-fontbottom/2,this.textPaint);

                    // LANGUAGE
                    this.canvas.drawText("C/E",this.keys[LANGUAGE].curr_x,this.keys[LANGUAGE].curr_y-fonttop/2-fontbottom/2,this.textPaint);

                    // COMMA
                    this.canvas.drawText(",",this.keys[COMMA].curr_x,this.keys[COMMA].curr_y-fonttop/2-fontbottom/2,this.textPaint);

                    // PERIOD
                    this.canvas.drawText(".",this.keys[PERIOD].curr_x,this.keys[PERIOD].curr_y-fonttop/2-fontbottom/2,this.textPaint);

                    // BACKSPACE
                    float backspaceX=keys[BACKSPACE].curr_x;
                    float backspaceY=keys[BACKSPACE].curr_y;
                    float backspaceH=keys[BACKSPACE].curr_height;
                    float backspaceW=keys[BACKSPACE].curr_width;
                    float[] backspaceA={backspaceX,backspaceY+backspaceH/4};
                    float[] backspaceB={backspaceX-backspaceW*3/8,backspaceY};
                    float[] backspaceC={backspaceX,backspaceY-backspaceH/4};
                    float[] backspaceD={backspaceX,backspaceY+backspaceH/8};
                    float[] backspaceE={backspaceX,backspaceY-backspaceH/8};
                    float[] backspaceF={backspaceX+backspaceW*3/8,backspaceY+backspaceH/8};
                    float[] backspaceG={backspaceX+backspaceW*3/8,backspaceY-backspaceH/8};
                    this.canvas.drawLine(backspaceA[0],backspaceA[1],backspaceB[0],backspaceB[1],textPaint);
                    this.canvas.drawLine(backspaceB[0],backspaceB[1],backspaceC[0],backspaceC[1],textPaint);
                    this.canvas.drawLine(backspaceA[0],backspaceA[1],backspaceD[0],backspaceD[1],textPaint);
                    this.canvas.drawLine(backspaceE[0],backspaceE[1],backspaceC[0],backspaceC[1],textPaint);
                    this.canvas.drawLine(backspaceD[0],backspaceD[1],backspaceF[0],backspaceF[1],textPaint);
                    this.canvas.drawLine(backspaceE[0],backspaceE[1],backspaceG[0],backspaceG[1],textPaint);
                    this.canvas.drawLine(backspaceF[0],backspaceF[1],backspaceG[0],backspaceG[1],textPaint);
                }

                // ranking
                if(Ranking){
                    int num = rank.length();
                    for(int i=0;i<num;i++){
                        int ranking=i+1;
                        char key=rank.charAt(i);
                        key=Character.toUpperCase(key);
                        int pos=keyPos[key-'A'];
                        this.canvas.drawText(String.valueOf(ranking),keys[pos].getLeft_tap(CURR_LAYOUT),keys[pos].getTop_tap(CURR_LAYOUT)-fonttop/2-fontbottom/2,rankingPaint);
                    }
                }
                this.keyboard.setImageBitmap(this.baseBitmap);

            }else{
                this.baseBitmap = Bitmap.createBitmap(this.keyboard.getWidth(),this.keyboard.getHeight(), Bitmap.Config.ARGB_8888);
                this.keyboard.setImageBitmap(this.baseBitmap);
            }

        }
        public AutoKeyboard(ImageView keyBoard){
            this.keyboard=keyBoard;
            getScreenSizeRatio();
            defaultPara();
            this.location=new int[2];
            this.keyboard.getLocationOnScreen(this.location);
            this.keys=new KEY[KEYNUM];
            for (int i=0;i<KEYNUM;i++){
                this.keys[i]=new KEY();
            }
            this.backgroundPaint=new Paint();
            this.backgroundPaint.setColor(Color.rgb(239,239,239));
            this.backgroundPaint.setStrokeJoin(Paint.Join.ROUND);
            this.backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
            this.backgroundPaint.setStrokeWidth(3);
            this.backgroundPaint.setStrokeWidth(3);

            this.originPaint=new Paint();
            this.originPaint.setColor(Color.rgb(231,88,113));
            //this.originPaint.setColor(Color.rgb(239,239,239));
            this.originPaint.setStrokeJoin(Paint.Join.ROUND);
            this.originPaint.setStrokeCap(Paint.Cap.ROUND);
            this.originPaint.setStrokeWidth(3);
            this.originPaint.setStrokeWidth(3);
            this.originPaint.setAlpha(50);

            this.textPaint=new Paint();
            this.textPaint.setColor(Color.BLACK);
            this.textPaint.setStrokeJoin(Paint.Join.ROUND);
            this.textPaint.setStrokeCap(Paint.Cap.ROUND);
            this.textPaint.setStrokeWidth(3);
            this.textPaint.setTextSize(Math.round(70*screen_height_ratio));
            this.textPaint.setTextAlign(Paint.Align.CENTER);
            this.textPaint.setStrokeJoin(Paint.Join.ROUND);
            this.textPaint.setStrokeCap(Paint.Cap.ROUND);

            this.boundPaint=new Paint();
            this.boundPaint.setColor(Color.rgb(201,184,201));
            this.boundPaint.setStrokeJoin(Paint.Join.ROUND);
            this.boundPaint.setStrokeCap(Paint.Cap.ROUND);
            this.boundPaint.setStrokeWidth(3);

            this.movePaint=new Paint();
            this.movePaint.setColor(Color.rgb(255,201,14));
            this.movePaint.setStrokeJoin(Paint.Join.ROUND);
            this.movePaint.setStrokeCap(Paint.Cap.ROUND);
            this.movePaint.setStrokeWidth(3);

            this.rankingPaint=new Paint();
            this.rankingPaint.setColor(Color.BLUE);
            this.rankingPaint.setStrokeJoin(Paint.Join.ROUND);
            this.rankingPaint.setStrokeCap(Paint.Cap.ROUND);
            this.rankingPaint.setStrokeWidth(3);
            this.rankingPaint.setTextSize(Math.round(30*screen_height_ratio));
            this.rankingPaint.setTextAlign(Paint.Align.CENTER);
            this.rankingPaint.setStrokeJoin(Paint.Join.ROUND);
            this.rankingPaint.setStrokeCap(Paint.Cap.ROUND);

            baseImageHeight=keyboard.getHeight();
            baseImageWidth=keyboard.getWidth();
            this.resetLayout();
            this.drawLayout();
        }

    }

    // Menu var
    // ---- edited by WangTong ----- 2018.3.11
    Menu menu;
    SeekBar seekBar;
    TextView seekBarText;
    enum MENUVAR{
        SD,TESTTURN,EXPONENT,SCALINGNUM,KEYBOARDHEIGHT,TOPTHRESHOLD,BOTTOMTHRESHOLD,MINWIDTH,MINHEIGHT,TAPRANGE,SPEED,FONTRATIO
    }
    MENUVAR currentMenuVar=MENUVAR.SD;

    // Menu function
    // ----- edited by WangTong ---- 2018.03.11
    void seekBarInit(){
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float progressF=(float)(progress);
                float ratio=progressF/100F;
                // 当拖动条的滑块位置发生改变时触发该方法,在这里直接使用参数progress，即当前滑块代表的进度值
                switch(currentMenuVar){
                    case SD:{
                        // max:20
                        float SDMax=20F;
                        float SDMin=1F;
                        SD_coefficient_X =Math.round (ratio*(SDMax-SDMin)+SDMin);
                        seekBarText.setText("SD:"+String.valueOf(SD_coefficient_X));
                        refresh();
                        break;
                    }case EXPONENT:{
                        float expMax=1F/2F;
                        float expMin=5F/100F;
                        autoKeyboard.exponent=ratio*(expMax-expMin)+expMin;
                        seekBarText.setText("Exponent:"+String.valueOf(autoKeyboard.exponent));
                        autoKeyboard.calExpRatio();
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        break;
                    }case MINWIDTH:{
                        autoKeyboard.minWidthRatio=ratio;
                        seekBarText.setText("minWidthRatio:"+String.valueOf(progress)+"%");
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        break;
                    }case TAPRANGE:{
                        autoKeyboard.tapRange=ratio;
                        seekBarText.setText("tapRange:"+String.valueOf(progress)+"%");
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        break;
                    }case MINHEIGHT:{
                        autoKeyboard.minHeightRatio=ratio;
                        seekBarText.setText("minHeightRatio:"+String.valueOf(progress)+"%");
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        break;
                    }case SCALINGNUM:{
                        float scalingNumMax=3F;
                        float scalingNumMin=1F;
                        autoKeyboard.scalingNum=Math.round(ratio*(scalingNumMax-scalingNumMin)+scalingNumMin);
                        seekBarText.setText("scalingNum:"+String.valueOf(autoKeyboard.scalingNum));
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        break;
                    }case TOPTHRESHOLD:{
                        float topThresholdMax=autoKeyboard.bottomThreshold-autoKeyboard.keyboardHeight;
                        float topThresholdMin=0;
                        autoKeyboard.topThreshold=ratio*(topThresholdMax-topThresholdMin)+topThresholdMin;
                        seekBarText.setText("topThreshold:"+String.valueOf(autoKeyboard.topThreshold));
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        break;
                    }case KEYBOARDHEIGHT:{
                        float keyBoardHeightMax=autoKeyboard.baseImageHeight;
                        float keyBoardHeightMin=autoKeyboard.baseImageHeight/2F;
                        autoKeyboard.keyboardHeight=ratio*(keyBoardHeightMax-keyBoardHeightMin)+keyBoardHeightMin;
                        seekBarText.setText("keyboardHeight:"+String.valueOf(autoKeyboard.keyboardHeight));
                        autoKeyboard.bottomThreshold=autoKeyboard.baseImageHeight;
                        autoKeyboard.topThreshold=Math.min(autoKeyboard.topThreshold,autoKeyboard.bottomThreshold-autoKeyboard.keyboardHeight);
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        break;
                    }case BOTTOMTHRESHOLD:{
                        float bottomThresholdMax=autoKeyboard.baseImageHeight;
                        float bottomThresholdMin=autoKeyboard.topThreshold+autoKeyboard.keyboardHeight;
                        autoKeyboard.bottomThreshold=ratio*(bottomThresholdMax-bottomThresholdMin)+bottomThresholdMin;
                        seekBarText.setText("bottomThreshold:"+String.valueOf(autoKeyboard.bottomThreshold));
                        autoKeyboard.resetLayout();
                        autoKeyboard.drawLayout();
                        break;
                    }case SPEED:{
                        int[] speedArray={50,60,70,80,90,100};
                        float[] ttsSpeedArray={1.0f,1.5f,2.0f,2.5f,3.0f,3.5f};
                        int speedMin=0;
                        int speedMax=5;
                        int voiceSpeedIndex=Math.round(ratio*(speedMax-speedMin)+speedMin);
                        voiceSpeed=speedArray[voiceSpeedIndex];
                        voiceSpeedText.setText(String.valueOf(voiceSpeed));
                        seekBarText.setText("voiceSpeed:"+String.valueOf(voiceSpeed));
                        ttsVoiceSpeed=ttsSpeedArray[voiceSpeedIndex];
                        ttsENG.setSpeechRate(ttsVoiceSpeed);
                        ttsCHN.setSpeechRate(ttsVoiceSpeed);
                        break;
                    }case FONTRATIO:{
                        float fontRatioMax = 3.0f;
                        float fontRatioMin = 0.5f;
                        fontRatio=ratio*(fontRatioMax-fontRatioMin)+fontRatioMin;
                        seekBarText.setText("Font ratio:"+String.valueOf(fontRatio));
                        fitOnScreen();
                        break;
                    }
                }
                // seekBarText.setText("Value:" + Integer.toString(progress));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setMenuTitle();
            }
        });
    }
    public void HideSeekBar(MotionEvent event) {
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN && seekBar.getVisibility()==View.VISIBLE) {
            int[] leftTop = {0, 0};
            seekBar.getLocationInWindow(leftTop);
            int left = leftTop[0]-10, top = leftTop[1]-20, bottom = leftTop[1] + seekBar.getHeight()+20, right = leftTop[0]
                    + seekBar.getWidth()+10;
            if (event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom) {
                // 保留seekBar
            } else {
                seekBar.setVisibility(View.GONE);
                seekBarText.setVisibility(View.GONE);
                setMenuTitle();
            }
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
            case R.id.fontRatio:{
                currentMenuVar=MENUVAR.FONTRATIO;
                float fontRatioMax=3.0f;
                float fontRatioMin=0.5f;
                seekBar.setVisibility(View.VISIBLE);
                seekBarText.setVisibility(View.VISIBLE);
                seekBarText.setText("Font ratio:"+String.valueOf(fontRatio));
                seekBar.setProgress(Math.round((fontRatio-fontRatioMin)/(fontRatioMax-fontRatioMin)*100));

                fitOnScreen();
                break;
            }
            case R.id.recordModeItem:{
                switch (recordMode){
                    case RECORD_MODE_STOPED:{
                        recordMode = RECORD_MODE_STARTED;
                        filename = getFilename();
                        Collections.shuffle(testcases);
                        currentTestcase = -1;
                        nextTestcase();
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
            case R.id.pinyinjpModeItem:{
                switch (pinyinjpMode) {
                    case JP_ON:
                        pinyinjpMode = JP_OFF;
                        try {
                            mIPinyinDecoderService.EnableSzm(false);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                    case JP_OFF:
                        pinyinjpMode = JP_ON;
                        try {
                            mIPinyinDecoderService.EnableSzm(true);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                refresh();
                break;
            }
            case R.id.predictEnableItem:{
                switch (predictEnableMode) {
                    case PREDICT_ON:
                        predictEnableMode = PREDICT_OFF;
                        break;
                    case PREDICT_OFF:
                        predictEnableMode = PREDICT_ON;
                        break;
                }
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                refresh();
                break;
            }
            case R.id.autoreadModeItem:{
                switch (autoreadMode) {
                    case AUTOREAD_ON:
                        autoreadMode = AUTOREAD_OFF;
                        break;
                    case AUTOREAD_OFF:
                        autoreadMode = AUTOREAD_ON;
                        break;
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
            case R.id.SDItem:{
                // SD MAX=20 , MIN=1
                currentMenuVar=MENUVAR.SD;
                float SDMax=20F;
                float SDMin=1F;
                seekBar.setVisibility(View.VISIBLE);
                seekBarText.setVisibility(View.VISIBLE);
                seekBarText.setText("SD:"+String.valueOf(SD_coefficient_X));
                seekBar.setProgress(Math.round((SD_coefficient_X -SDMin)/(SDMax-SDMin)*100));
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                refresh();
                break;
            }
            case R.id.fuzzyInputTestBegin:{
                switch (activity_mode){
                    case KEYBOARD_MODE:{
                        activity_mode=ActivityMode.FUZZY_INPUT_TEST_MODE;
                        languageMode=LANG_MODE_CHN;
                        layoutInit();
                        setMenuTitle();
                        if(fu!=null){
                            fu.sentence.clear();
                        }
                        fu=new FuzzyInputTest();
                        break;
                    }
                    case FUZZY_INPUT_TEST_MODE:{
                        activity_mode=ActivityMode.KEYBOARD_MODE;
                        fu.sentence.clear();
                        fu=null;
                        layoutInit();
                        setMenuTitle();
                        break;
                    }
                }
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
                currentMenuVar=MENUVAR.SCALINGNUM;
                float scalingNumMax=3F;
                float scalingNumMin=1F;
                seekBar.setVisibility(View.VISIBLE);
                seekBarText.setVisibility(View.VISIBLE);
                seekBar.setProgress(Math.round((autoKeyboard.scalingNum-scalingNumMin)/(scalingNumMax-scalingNumMin)*100));
                seekBarText.setText("scalingNum:"+String.valueOf(autoKeyboard.scalingNum));
                //Toast.makeText(this, "scalingNum="+String.valueOf(autoKeyboard.scalingNum), Toast.LENGTH_LONG).show();
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
            }case R.id.topThreshold:{
                currentMenuVar=MENUVAR.TOPTHRESHOLD;
                float topThresholdMax=autoKeyboard.bottomThreshold-autoKeyboard.keyboardHeight;
                float topThresholdMin=0F;
                seekBar.setVisibility(View.VISIBLE);
                seekBarText.setVisibility(View.VISIBLE);
                seekBar.setProgress(Math.round((autoKeyboard.topThreshold-topThresholdMin)/(topThresholdMax-topThresholdMin)*100));
                seekBarText.setText("topThreshold:"+String.valueOf(autoKeyboard.topThreshold));
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.bottomThreshold:{
                currentMenuVar=MENUVAR.BOTTOMTHRESHOLD;
                float bottomThresholdMax=autoKeyboard.baseImageHeight;
                float bottomThresholdMin=autoKeyboard.topThreshold+autoKeyboard.keyboardHeight;
                seekBar.setVisibility(View.VISIBLE);
                seekBarText.setVisibility(View.VISIBLE);
                seekBar.setProgress(Math.round((autoKeyboard.bottomThreshold-bottomThresholdMin)/(bottomThresholdMax-bottomThresholdMin)*100));
                seekBarText.setText("bottomThreshold:"+String.valueOf(autoKeyboard.bottomThreshold));
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.minWidthRatio:{
                currentMenuVar=MENUVAR.MINWIDTH;
                seekBar.setVisibility(View.VISIBLE);
                seekBarText.setVisibility(View.VISIBLE);
                int progress = Math.min(100,Math.round(autoKeyboard.minWidthRatio*100));
                seekBar.setProgress(progress);
                seekBarText.setText("minWidthRatio:"+String.valueOf(progress)+"%");
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.minHeightRatio:{
                currentMenuVar=MENUVAR.MINHEIGHT;
                seekBar.setVisibility(View.VISIBLE);
                seekBarText.setVisibility(View.VISIBLE);
                int progress=Math.min(100,Math.round(autoKeyboard.minHeightRatio*100));
                seekBar.setProgress(progress);
                seekBarText.setText("minHeightRatio:"+String.valueOf(progress)+"%");
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.tapRange:{
                currentMenuVar=MENUVAR.TAPRANGE;
                seekBar.setVisibility(View.VISIBLE);
                seekBarText.setVisibility(View.VISIBLE);
                int progress=Math.min(100,Math.round(autoKeyboard.tapRange*100));
                seekBar.setProgress(progress);
                seekBarText.setText("tapRange:"+String.valueOf(progress)+"%");
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.keyboardHeight:{
                currentMenuVar=MENUVAR.KEYBOARDHEIGHT;
                float keyBoardHeightMax=autoKeyboard.baseImageHeight;
                float keyBoardHeightMin=autoKeyboard.baseImageHeight/2F;
                seekBar.setVisibility(View.VISIBLE);
                seekBarText.setVisibility(View.VISIBLE);
                seekBar.setProgress(Math.round((autoKeyboard.keyboardHeight-keyBoardHeightMin)/(keyBoardHeightMax-keyBoardHeightMin)*100));
                seekBarText.setText("keyBoardHeight:"+String.valueOf(autoKeyboard.keyboardHeight));
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
                currentMenuVar=MENUVAR.EXPONENT;
                float expMax=1F/2F;
                float expMin=5F/100F;
                seekBar.setVisibility(View.VISIBLE);
                seekBarText.setVisibility(View.VISIBLE);
                seekBar.setProgress(Math.round((autoKeyboard.exponent-expMin)/(expMax-expMin)*100));
                seekBarText.setText("Exponent:"+String.valueOf(autoKeyboard.exponent));
                autoKeyboard.calExpRatio();
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.Outline:{
                autoKeyboard.Outline=!autoKeyboard.Outline;
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }
            case R.id.FunctionKeys:{
                autoKeyboard.FunctionKeys=!autoKeyboard.FunctionKeys;
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }case R.id.Ranking:{
                autoKeyboard.Ranking = !autoKeyboard.Ranking;
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }
            case R.id.default_keyboard:{
                autoKeyboard.defaultPara();
                autoKeyboard.resetLayout();
                autoKeyboard.drawLayout();
                break;
            }
            case R.id.fuzzyInputTestMode:{
                switch (fuMode){
                    case Current:{
                        fuMode=FuzzyInputTestMode.Target;
                        break;
                    }
                    case Target:{
                        fuMode=FuzzyInputTestMode.Current;
                        break;
                    }
                }
                break;
            }
            default:
                break;
        }
        setMenuTitle();
        return super.onOptionsItemSelected(item);
    }
    void setMenuTitle(){
        menu.findItem(R.id.fuzzyInputTestBegin).setTitle("Start Test");
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
        switch (pinyinjpMode){
            case JP_ON:{
                menu.findItem(R.id.pinyinjpModeItem).setTitle("简拼：开启");
                break;
            }
            case JP_OFF:{
                menu.findItem(R.id.pinyinjpModeItem).setTitle("简拼：关闭");
                break;
            }
        }
        switch (predictEnableMode){
            case PREDICT_ON:{
                menu.findItem(R.id.predictEnableItem).setTitle("预测：开启");
                break;
            }
            case PREDICT_OFF:{
                menu.findItem(R.id.predictEnableItem).setTitle("预测：关闭");
                break;
            }
        }
        switch (autoreadMode){
            case AUTOREAD_ON:{
                menu.findItem(R.id.autoreadModeItem).setTitle("自动朗读：开启");
                break;
            }
            case AUTOREAD_OFF:{
                menu.findItem(R.id.autoreadModeItem).setTitle("自动朗读：关闭");
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
        menu.findItem(R.id.SDItem).setTitle("SD:" + String.valueOf(SD_coefficient_X));

        menu.findItem(R.id.reset_keyboard).setTitle("Reset Keyboard");

        try {
            if(autoKeyboard.Visibility){
                menu.findItem(R.id.keyBoardVisibility).setTitle("KeyboardVisibility:True");
            }else{
                menu.findItem(R.id.keyBoardVisibility).setTitle("KeyboardVisibility:False");
            }
            menu.findItem(R.id.scalingNum).setTitle("scalingNum:"+String.valueOf(autoKeyboard.scalingNum));
            if(autoKeyboard.try_layout_mode==autoKeyboard.BODILY_MOVEMENT){
                menu.findItem(R.id.try_layout_mode).setTitle("try layout mode:Bodily");
            }else if(autoKeyboard.try_layout_mode==autoKeyboard.RESPECTIVELY_MOVEMENT){
                menu.findItem(R.id.try_layout_mode).setTitle("try layout mode:Respectively");
            }

            if (autoKeyboard.getKey_mode == autoKeyboard.LOOSE_MODE) {
                menu.findItem(R.id.getKey_mode).setTitle("getKey mode:LOOSE MODE");
            } else if (autoKeyboard.getKey_mode == autoKeyboard.STRICT_MODE) {
                menu.findItem(R.id.getKey_mode).setTitle("getKey mode:STRICT MODE");
            }
            if(autoKeyboard.scalingMode==autoKeyboard.LINEAR_MODE){
                menu.findItem(R.id.scalingMode).setTitle("scalingMode:LINEAR_MODE");
            }else if(autoKeyboard.scalingMode==autoKeyboard.EXPONENT_MODE){
                menu.findItem(R.id.scalingMode).setTitle("scalingMode:EXPONENT_MODE");
            }
            menu.findItem(R.id.topThreshold).setTitle("topThreshold:"+String.valueOf(autoKeyboard.topThreshold));
            menu.findItem(R.id.bottomThreshold).setTitle("bottomThreshold:"+String.valueOf(autoKeyboard.bottomThreshold));
            menu.findItem(R.id.minWidthRatio).setTitle("minWidthRatio:"+String.valueOf(Math.round(autoKeyboard.minWidthRatio*100))+"%");
            menu.findItem(R.id.minHeightRatio).setTitle("minHeightRatio:"+String.valueOf(Math.round(autoKeyboard.minHeightRatio*100))+"%");
            menu.findItem(R.id.tapRange).setTitle("tap range:"+String.valueOf(Math.round(autoKeyboard.tapRange*100))+"%");
            menu.findItem(R.id.keyboardHeight).setTitle("keyboardHeight:"+String.valueOf(autoKeyboard.keyboardHeight));
            menu.findItem(R.id.exponent).setTitle("exponent:"+String.valueOf(autoKeyboard.exponent));
            menu.findItem(R.id.Outline).setTitle("Outline:"+String.valueOf(autoKeyboard.Outline));
            menu.findItem(R.id.FunctionKeys).setTitle("Function keys:"+String.valueOf(autoKeyboard.FunctionKeys));
            menu.findItem(R.id.Ranking).setTitle("Ranking:"+String.valueOf(autoKeyboard.Ranking));

        }catch (Exception e){
            Log.i("Error:","AutoKeyboard is not set up");
        }

        try{
            menu.findItem(R.id.dmDensityDpi).setTitle("DensityDpi:"+String.valueOf(screen.dmDensityDpi));
            menu.findItem(R.id.dmDensity).setTitle("Density:"+String.valueOf(screen.dmDensity));
            menu.findItem(R.id.dmWidthPixels).setTitle("Width pixels:"+String.valueOf(screen.dmWidthPixels));
            menu.findItem(R.id.dmHeightPixels).setTitle("Height pixels:"+String.valueOf(screen.dmHeightPixels));
        }catch (Exception e){
            Log.i("Error:","Screen is not set up");
        }
        menu.findItem(R.id.fontRatio).setTitle("Font ratio:"+String.valueOf(fontRatio));
        switch (fuMode){
            case Current:{
                menu.findItem(R.id.fuzzyInputTestMode).setTitle("Test Mode:Current");
                break;
            }
            case Target:{
                menu.findItem(R.id.fuzzyInputTestMode).setTitle("Test Mode:Target");
                break;
            }
        }

    }
    // Screen
    public class Screen {

        final String TAG = Screen.class.getSimpleName();
        DisplayMetrics dm;
        // 当前屏幕的参数
        int dmDensityDpi = 0;
        int dmWidthPixels = 1024;
        int dmHeightPixels = 1980;
        float dmDensity = 0.0f;
        float scale = 0.0f;
        /**
         *
         * 根据构造函数获得当前手机的屏幕系数
         *
         * */
        public Screen(Context context) {
            // 获取当前屏幕
            dm = new DisplayMetrics();
            dm = context.getApplicationContext().getResources().getDisplayMetrics();
            // 设置DensityDpi
            dmDensityDpi = dm.densityDpi;
            // 密度因子
            scale = (float)(dmDensityDpi)/ 160f;
            // 宽度
            dmWidthPixels = dm.widthPixels;
            // 高度
            dmHeightPixels = dm.heightPixels;
            // Density
            dmDensity = dm.density;

        }


        /**
         * 密度转换像素
         * */
        public int dip2px(float dipValue) {

            return (int) (dipValue * scale + 0.5f);

        }

        /**
         * 像素转换密度
         * */
        public int px2dip(float pxValue) {
            return (int) (pxValue / scale + 0.5f);
        }

        @Override
        public String toString() {
            return "Screen,dmDensityDpi:"+dmDensityDpi+",dmDensity:"+dmDensity+",dmWidthPixels:"+dmWidthPixels+",dmHeightPixels:"+dmHeightPixels+"\n";
        }
    }
    // FuzzyinputTest
    FuzzyInputTest fu;
    Button BackButton,BackSpaceButton,SaveButton,RestartButton;
    TextView fuzzyInputTestCharShow;
    ProgressBar progressBar;
    ListView listView;
    enum FuzzyInputTestMode{Target,Current};
    FuzzyInputTestMode fuMode=FuzzyInputTestMode.Target;
    class FuzzyInputTest{
        class Sentence{
            class Letter{// 每输入一个字母视为1轮
                class Char{
                    char actual;
                    double positionX;
                    double positionY;
                    Char(char actual_in,double positionX_in,double positionY_in) {
                        this.actual = actual_in;
                        this.positionX = positionX_in;
                        this.positionY = positionY_in;
                    }
                    Char(MotionEvent event){
                        this.actual = autoKeyboard.getKeyByPosition(event,autoKeyboard.CURR_LAYOUT);
                        this.positionX = event.getX();
                        this.positionY = event.getY();
                    }
                    Char(){
                        this.actual='?';
                        this.positionX=-1;
                        this.positionY=-1;
                    }
                    public String toString(){
                        return "Char,actual:"+actual+",X:"+ String.valueOf(positionX) +",Y:"+ String.valueOf(positionY)+'\n';
                    }
                }
                char target;
                ArrayList<Char> ch=new ArrayList<>();
                Long downTime;
                Long upTime;
                Letter(char target_in,MotionEvent event){
                    this.target = target_in;
                    this.ch.add(new Char(event));
                    this.downTime = event.getDownTime();
                    this.upTime = event.getEventTime();
                }
                public void addChar(MotionEvent event){
                    this.ch.add(new Char(event));
                    this.upTime = event.getEventTime();
                }
                public String toString(){
                    String data = "Letter,target:"+target+",downTime:"+downTime+",upTime:"+upTime+"\n";
                    for(Char perChar:this.ch){
                        data+=perChar;
                    }
                    return data;
                }
                public char getTarget(){
                    return this.target;
                }
            }
            String pinyin;
            String chinese;
            String typeIn;
            ArrayList<Letter> letter=new ArrayList<>();
            int index=0;
            boolean isRecord=false;
            Sentence(String chinese_in, String pinyin_in){
                this.pinyin = pinyin_in;
                this.chinese = chinese_in;
                index = 0;
                typeIn="";
            }
            public void clear(){
                this.letter.clear();
                typeIn="";
                index=0;
                isRecord=false;
            }
            public char getTarget(){
                return Character.toLowerCase(this.pinyin.charAt(this.index));
            }
            public boolean addLetter(MotionEvent event){
                try {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_UP: {
                            if (isRecord) {
                                this.letter.get(index).addChar(event);
                                typeIn+=autoKeyboard.getKeyByPosition(event,autoKeyboard.CURR_LAYOUT);
                                fuzzyInputTestCharShow.setText(chinese+" "+pinyin+" "+typeIn);
                                index++;
                                isRecord = false;
                            }
                            break;
                        }
                        case MotionEvent.ACTION_DOWN: {
                            isRecord = true;
                            this.letter.add(new Letter(pinyin.charAt(index), event));
                            break;
                        }
                        case MotionEvent.ACTION_MOVE: {
                            if (isRecord) {
                                this.letter.get(index).addChar(event);
                            }
                            break;
                        }
                    }
                }catch (Exception e){
                    Log.i("addLetter", "index out of range");
                    return true; // 单词输入结束
                }
                return index>=pinyin.length(); // 单词输入结束：true  单词还没输完：false
            };
            public String getPinYin(){
                return this.pinyin;
            }
            public String getChinese(){
                return this.chinese;
            }
            public int getIndex(){
                return this.index;
            }
            public String getTypeIn(){
                return this.typeIn;
            }
            public String toString(){
                return "Sentence,Chinese:"+chinese+",PinYin:"+pinyin+",TypeIn:"+typeIn+"\n";
            }
        }
        ArrayList<Sentence> sentence=new ArrayList<>();
        int chineseNum;
        boolean ifBegin;
        FuzzyInputTest(){
            BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.pilotdict)));
            String line;
            chineseNum=0;
            try{
                while ((line = reader.readLine()) != null){
                    String[] lineArray = line.split(" ");
                    sentence.add(new Sentence(lineArray[0],lineArray[1]));
                }
                reader.close();
                Log.i("init", "read touch model finished ");
            } catch (Exception e){
                Log.i("init", "read touch model failed");
            }
            chineseNum=sentence.size();
            activity_mode=ActivityMode.FUZZY_INPUT_TEST_MODE;
            index=0;
            ifBegin=false;
            progressBar.setMax(chineseNum);
            showAndSpeak();
            ifSave=false;
            ifCalDone=false;
        }
        public String toString(){
            return "FuzzyInputTest,chineseNum:"+String.valueOf(chineseNum)+"\n";
        }
        int index = 0;
        String DataToSave;
        String DataToShowAndSave;
        public boolean ifSave;
        boolean ifCalDone;
        boolean ifTypeIn=false;
        public char getTarget(){
            return this.sentence.get(this.index).getTarget();
        }
        public boolean addSentence(MotionEvent event) {
            if(index<sentence.size())
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        ifTypeIn=true;
                        if(this.isInRange())
                            sentence.get(index).addLetter(event);
                        Log.i("addSentence","ACTION_DOWN");
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        if(this.isInRange())
                            sentence.get(index).addLetter(event);
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        if(ifTypeIn){
                            if(this.isInRange())
                                if (sentence.get(index).addLetter(event)) {
                                    index++;
                                    showAndSpeak();
                                }
                            ifTypeIn = false;
                        }
                        break;
                    }
                }
            if(index>=chineseNum){
                fuzzyInputTestCharShow.setText("测试结束，非常感谢\n");
                tts("测试结束，非常感谢\n");
                calculateFuzzyInputData();
                return true;
            }else{
                return false;
            }
        };
        public void backSpace(){
            if(this.isInRange())
                sentence.get(index).clear();
            if(index>0&&index<this.chineseNum){
                index--;
                sentence.get(index).clear();
            }
            stopVoice();
            showAndSpeak();
        }
        @RequiresApi(api = Build.VERSION_CODES.N)
        public void saveFuzzyInputTestData(){
            if(ifSave){
                String text = "已经存储过一次了";
                toast(text);
                tts(text);
            }else if (index < chineseNum) {
                String text = "请先完成测试";
                toast(text);
                tts(text);
            }else if(!ifCalDone){
                String text = "请等待计算完成";
                toast(text);
                tts(text);
            }
            else {
                if (isExternalStorageWritable()) {
                    //String directory="/storage/emulated/0/Android/data/FuzzyInput/";
                    //File sdCard = Environment.getExternalStorageDirectory();
                    File DirectoryFolder = getExternalFilesDir(null);
                    //File DirectoryFolder = new File(fuzzyInputTestStoragePath);
                    //File DirectoryFolder = this.getExternalStoragePublicDirectory();
                    if (!DirectoryFolder.exists()) { //如果该文件夹不存在，则进行创建
                        DirectoryFolder.mkdirs();//创建文件夹
                    }
                    String time=getTime();
                    File fileInDetail = new File(DirectoryFolder, time+ "_详细数据.txt");
                    File fileInBrief= new File(DirectoryFolder,time+"_键入数据.txt");
                    if (!fileInDetail.exists()) {
                        try {
                            fileInDetail.createNewFile();
                            //file is create
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if (!fileInBrief.exists()) {
                        try {
                            fileInBrief.createNewFile();
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        FileOutputStream fosInBrief = new FileOutputStream(fileInBrief);
                        fosInBrief.write(DataToShowAndSave.getBytes());
                        fosInBrief.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String text = "存储完毕";
                    toast(text);
                    tts(text);
                    ifSave = true;
                } else {
                    String text = "不能存储文件";
                    toast(text);
                    tts(text);
                }
            }
        }
        private void showAndSpeak(){
            if(index==0){
                tts("模糊输入测试现在开始，接下来请输入："+sentence.get(index).getChinese());
            }else if(this.isInRange()){
                tts(sentence.get(index).getChinese());
                fuzzyInputTestCharShow.setText(sentence.get(index).getChinese()+" "+sentence.get(index).getPinYin()+" "+sentence.get(index).getTypeIn());
            }
            progressBar.setProgress(index);
        }
        private void calculateFuzzyInputData(){
            if(index < chineseNum){
                toast("不应该在这时计算!");
            }
            else{
                DataToSave = screen.toString()+autoKeyboard.toString()+this.toString();
                DataToShowAndSave = "";
                ArrayList<String> listViewData=new ArrayList<String>();
                for (Sentence s:sentence){
                    DataToShowAndSave+=s.getChinese()+","+s.getPinYin()+","+s.getTypeIn()+"\n";
                    listViewData.add(s.getPinYin()+" "+s.getTypeIn());
                    DataToSave+=s.toString();
                    for (Sentence.Letter l:s.letter){
                        DataToSave+=l.toString();
                        for(Sentence.Letter.Char c:l.ch){
                            DataToSave+=c.toString();
                        }
                    }
                }
                ifCalDone=true;
                ArrayAdapter<String> adapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,listViewData);
                listView.setVisibility(View.VISIBLE);
                listView.setAdapter(adapter);
            }
        }
        public boolean isInRange(){
            return this.index>=0 && index<this.chineseNum;
        }
    }

}
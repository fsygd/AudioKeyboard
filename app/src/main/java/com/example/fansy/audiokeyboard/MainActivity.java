package com.example.fansy.audiokeyboard;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    final char KEY_NOT_FOUND = 0;

    //voice manager
    HashMap<String, int[]> voice = new HashMap<>();
    ArrayList<Integer> myPlayList = new ArrayList<>();
    MediaPlayer current;

    final int INIT_MODE_ABSOLUTE = 0;
    final int INIT_MODE_RELATIVE = 1;
    final int INIT_MODE_NOTHING = 2;
    int initMode = INIT_MODE_RELATIVE;
    final int CONFIRM_MODE_UP = 0;
    final int CONFIRM_MODE_DOUBLECLICK = 1;
    int confirmMode = CONFIRM_MODE_UP;
    ImageView keyboard;
    TextView text, candidatesView, readListView, voiceSpeedText, predictionRepeatText;
    Button confirmButton, initModeButton, confirmModeButton, speedpbutton, speedmbutton, predictionRepeatPButton, predictionRepeatMButton;
    String readList = ""; //current voice list
    String currentWord = ""; //most possible char sequence
    String currentWord2 = ""; //second possible char sequence
    String currentBaseline = "";
    char nowCh = 0; //the most possible char
    char nowCh2 = 0; //the second possible char, '*' if less than 1/10 of the most possible char
    char nowChSaved = 0;
    char nowCh2Saved = 0;
    char nowChBaselineSaved = 0;
    char firstTouchSaved1 = KEY_NOT_FOUND;
    char firstTouchSaved2 = KEY_NOT_FOUND;
    ArrayList<Word> dict = new ArrayList();
    ArrayList<Character> seq = new ArrayList<Character>(); //char sequence during the whole touch
    String keysNearby[] = new String[26];
    double keysNearbyProb[][] = new double[26][26];


    int voiceSpeed = 50;


    boolean playDaFlag = false;
    boolean slideFlag = false;

    String charsPlayed = "";
    String charsInPlaylist = "";
    int predictionCount = 0;
    int predictionRepeatTime = 1;

    AutoKeyboard autoKeyboard;


    //redraw the views
    public void refresh(){
        text.setText(currentWord + "\n" + currentWord2 + "\n" + currentBaseline);
        String str = "";
        for (int i = 0; i < candidates.size(); ++i)
            str += candidates.get(i).text + "\n";
        candidatesView.setText(str);
        readListView.setText(readList);
        if (initMode == INIT_MODE_ABSOLUTE)
            initModeButton.setText("absolute");
        else if(initMode == INIT_MODE_RELATIVE)
            initModeButton.setText("relative");
        else
            initModeButton.setText("nothing");
        if (confirmMode == CONFIRM_MODE_UP)
            confirmModeButton.setText("up");
        else
            confirmModeButton.setText("double click");
    }

    final int MAX_CANDIDATE = 5;
    public ArrayList<Word> candidates = new ArrayList<Word>();

    //predict the candidates according the currentWord and currentWord2 and refresh
    public void predict(String currentWord, String currentWord2){
        candidates.clear();
        for (int i = 0; i < dict.size(); ++i){
            if (candidates.size() >= MAX_CANDIDATE)
                break;
            Word candidate = dict.get(i);
            if (candidate.text.length() != currentWord.length())
                continue;

            boolean flag = true;
            for (int j = 0; j < currentWord.length(); ++j)
                if (candidate.text.charAt(j) != currentWord.charAt(j) && candidate.text.charAt(j) != currentWord2.charAt(j)){
                    flag = false;
                    break;
                }
            if (flag)
                candidates.add(candidate);
        }
        refresh();
    }

    //todo reconstruct
    public void stopVoice(){
        if (current != null) {
            current.stop();
            current.reset();
            current.release();
            current = null;
        }
        myPlayList.clear();
        charsInPlaylist = "";
    }

    public void stopInput(){
        seq.clear();
        stopVoice();
    }

    public void finishWord(){
        currentWord = "";
        currentWord2 = "";
        currentBaseline = "";
        readList = "";
        predict(currentWord, currentWord2);
        refresh();
    }

    //new touch point in keyboard area
    public void newPoint(int x, int y){
        if (seq.size() == 0){//first touch
            if (initMode == INIT_MODE_ABSOLUTE) {
                autoKeyboard.resetLayout();
                addToSeq(autoKeyboard.getKeyByPosition(x, y, 0), true,true);
            }
            else if(initMode == INIT_MODE_RELATIVE){
                char ch = autoKeyboard.getKeyByPosition(x, y, 1);
                if (ch == upKey){
                    addToSeq(ch, true, false);
                }
                else
                if (ch != upKey){
                    autoKeyboard.resetLayout();
                    ch = autoKeyboard.getKeyByPosition(x, y, 1);
                    char best = addToSeq(ch, false,true);
                    if (autoKeyboard.tryLayout(best, x, y)){
                        autoKeyboard.drawLayout();
                    }
                    addToSeq(best, true,true);
                }
                firstTouchSaved1 = KEY_NOT_FOUND;
                firstTouchSaved2 = KEY_NOT_FOUND;
            }else{
                char ch = autoKeyboard.getKeyByPosition(x, y, 0);
                addToSeq(ch,true,false);
            }
        }
        else{
            addToSeq(autoKeyboard.getKeyByPosition(x, y, 1), true,true);
        }
    }

    //init keysNearby and keysNearbyProb
    //import touch model
    public void initKeyboard(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.touchmodel)));
        String line;
        try{
            String[] secondKeys = reader.readLine().split(",");
            while ((line = reader.readLine()) != null){
                String[] firstKeyAndPs = line.split(",");
                char firstKey = firstKeyAndPs[0].charAt(0);
                keysNearby[firstKey-'a'] = "";
                for(int i=0;i!=26;i++){
                    if(firstKeyAndPs[i+1]!="0"){
                        keysNearby[firstKey-'a'] += secondKeys[i+1];
                        keysNearbyProb[firstKey-'a'][keysNearby[firstKey-'a'].length()-1] = Double.valueOf(firstKeyAndPs[i+1]);
                    }
                }
            }
            reader.close();
            Log.i("init", "read touch model finished ");
        } catch (Exception e){
            Log.i("init", "read touch model failed");
        }

    }

    final int DICT_SIZE = 50000;
    //read dict from file
    public void initDict(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.dict)));
        String line;
        try{
            int lineNo = 0;
            while ((line = reader.readLine()) != null){
                lineNo++;
                String[] ss = line.split(" ");
                dict.add(new Word(ss[0], Double.valueOf(ss[1])));
                if (lineNo == DICT_SIZE)
                    break;
            }
            reader.close();
            Log.i("init", "read dictionary finished " + dict.size());
        } catch (Exception e){
            Log.i("init", "read dictionary failed");
        }
    }

    //todo reconstruct
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

        voice.get("blank")[0] = R.raw.blank;
    }

    public void initButtons(){
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopInput();
                finishWord();
            }
        });

        initModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (initMode == INIT_MODE_ABSOLUTE)
                    initMode = INIT_MODE_RELATIVE;
                else if(initMode == INIT_MODE_RELATIVE) {
                    initMode = INIT_MODE_NOTHING;
                    autoKeyboard.resetLayout();
                    autoKeyboard.drawLayout();
                }else
                    initMode = INIT_MODE_ABSOLUTE;
                    autoKeyboard.resetLayout();
                    autoKeyboard.drawLayout();
                refresh();
            }
        });

        confirmModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (confirmMode == CONFIRM_MODE_UP)
                    confirmMode = CONFIRM_MODE_DOUBLECLICK;
                else
                    confirmMode = CONFIRM_MODE_UP;
                refresh();
            }
        });

        speedmbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(voiceSpeed>=60){
                    voiceSpeed -= 10;
                }
                voiceSpeedText.setText(voiceSpeed+"");
            }
        });

        speedpbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(voiceSpeed<=90){
                    voiceSpeed += 10;
                }
                voiceSpeedText.setText(voiceSpeed+"");
            }
        });

        predictionRepeatPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                predictionRepeatTime++;
                predictionRepeatText.setText(predictionRepeatTime+"");
            }
        });

        predictionRepeatMButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(predictionRepeatTime>1) predictionRepeatTime--;
                predictionRepeatText.setText(predictionRepeatTime+"");
            }
        });

    }

    public void playFirstVoice(){
        if (current == null && !myPlayList.isEmpty()){
            current = MediaPlayer.create(this, myPlayList.get(0));
            myPlayList.remove(0);
            if(myPlayList.isEmpty()) {
                if(!charsInPlaylist.equals("")){
                    charsPlayed += charsInPlaylist;
                    predictionCount += 1;
                }
            }
            current.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playDaFlag = false;
                    slideFlag = true;
                    mp.release();
                    current = null;
                    playFirstVoice();
                }
            });
            current.start();
        }
    }

    //todo reconstruct
    public void playMedia(String tag, int index,boolean isChar){
        myPlayList.add(voice.get(tag)[index]);
        if(isChar) charsInPlaylist += (char)('a'+index);
        playFirstVoice();
    }

    //if write=false, just return the most possible key
    public char addToSeq(char ch, boolean write, boolean predictMode){
        if (ch != KEY_NOT_FOUND){
            if (seq.size() == 0 || seq.get(seq.size() - 1) != ch ) {
                //make a vibrate

                Vibrator vibrator =  (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0, 30};
                vibrator.vibrate(pattern, -1);

                ArrayList<Word> letters = new ArrayList<Word>();
                if(predictMode){
                    for (int i = 0; i < keysNearby[ch - 'a'].length(); ++i){
                        if((!charsPlayed.contains(keysNearby[ch-'a'].charAt(i)+"")) && (predictionCount<predictionRepeatTime)) {
                            letters.add(new Word(keysNearby[ch - 'a'].charAt(i) + "", 0));
                        }else if(predictionCount >= predictionRepeatTime){
                            letters.add(new Word(keysNearby[ch - 'a'].charAt(i) + "", 0));
                        }
                    }
                    for (int i = 0; i < dict.size(); ++i)
                        if (dict.get(i).text.length() >= currentWord.length() + 1){
                            boolean flag = true;
                            Word word = dict.get(i);
                            for (int j = 0; j < currentWord.length(); ++j)
                                if (word.text.charAt(j) != currentWord.charAt(j) && word.text.charAt(j) != currentWord2.charAt(j)){
                                    flag = false;
                                    break;
                                }
                            if (flag){
                                for (int j = 0; j < letters.size(); ++j)
                                    if (letters.get(j).text.charAt(0) == word.text.charAt(currentWord.length()))
                                        letters.get(j).freq += word.freq;
                            }
                        }
                    for (int i = 0; i < letters.size(); ++i)
                        letters.get(i).freq = (letters.get(i).freq + 0.01) * keysNearbyProb[ch - 'a'][keysNearby[ch-'a'].indexOf(letters.get(i).text)];
                    Collections.sort(letters);
                    if (!write) {
                        firstTouchSaved1 = letters.get(0).text.charAt(0);
                        if (letters.get(1).freq * 10 >= letters.get(0).freq){
                            firstTouchSaved2 = letters.get(1).text.charAt(0);
                        }
                        else{
                            firstTouchSaved2 = '*';
                        }
                        if (letters.get(0).freq > 0)
                            return letters.get(0).text.charAt(0);
                        else
                            return ch;
                    }
                }

                //write=true
                seq.add(ch);
                Log.i("seq", ch + "");
                stopVoice();
                readList = "";
                nowCh = '*';
                nowCh2 = '*';

                playMedia("ios11da", 0,false);
                if (predictMode && initMode != INIT_MODE_NOTHING){
                    if (seq.size() == 1 ||(playDaFlag&&!slideFlag)||(predictionCount<predictionRepeatTime)){
                        if (firstTouchSaved1 != KEY_NOT_FOUND){
                            nowCh = firstTouchSaved1;
                            playMedia("ios11_"+voiceSpeed, firstTouchSaved1 - 'a',true);
                            readList += firstTouchSaved1;

                            if (firstTouchSaved2 != '*') {
                                nowCh2 = firstTouchSaved2;
                                playMedia("ios11_" + voiceSpeed, firstTouchSaved2 - 'a', true);
                                readList += firstTouchSaved2;
                            }
                        }
                        //prob top 2
                        else if (letters.size() >= 1 && letters.get(0).freq > 0) {
                            nowCh = letters.get(0).text.charAt(0);
                            playMedia("ios11_"+voiceSpeed, nowCh - 'a',true);
                            readList += nowCh;
                            if (letters.size() >= 2 && letters.get(1).freq * 10 > letters.get(0).freq){
                                nowCh2 = letters.get(1).text.charAt(0);
                                playMedia("ios11_"+voiceSpeed, nowCh2 - 'a',true);
                                readList += nowCh2;
                            }
                        }
                        else {
                            //current key
                            nowCh = ch;
                            playMedia("ios11_"+voiceSpeed, nowCh - 'a',true);
                            readList += nowCh;
                        }
                    }
                    else{
                        //current key
                        nowCh = ch;
                        playMedia("ios11_"+voiceSpeed, nowCh - 'a',true);
                        readList += nowCh;
                    }
                }else{
                    nowCh = ch;
                    playMedia("ios11_"+voiceSpeed, nowCh - 'a',true);
                    readList += nowCh;
                }
                playDaFlag = true;
                refresh();
            }
        }
        return 'a';
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main_relative);

        keyboard = (ImageView)findViewById(R.id.keyboard);
        text = (TextView)findViewById(R.id.text);
        candidatesView = (TextView)findViewById(R.id.candidates);
        confirmButton = (Button)findViewById(R.id.confirm_button);
        readListView = (TextView)findViewById(R.id.readList);
        initModeButton = (Button)findViewById(R.id.init_mode_button);
        confirmModeButton = (Button)findViewById(R.id.confirm_mode_button);
        speedmbutton = (Button)findViewById(R.id.speed_m_button);
        speedpbutton = (Button)findViewById(R.id.speed_p_button);
        voiceSpeedText = (TextView)findViewById(R.id.voice_speed_text);
        predictionRepeatPButton = (Button)findViewById(R.id.predictionRepeat_p);
        predictionRepeatMButton = (Button)findViewById(R.id.predictionRepeat_m);
        predictionRepeatText = (TextView)findViewById(R.id.predictionReoeatText);

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
        //left 0 right 1440 top 1554 bottom 2320
    }

    @Override
    public void onDestroy(){
        if (current != null){
            current.release();
            current = null;
        }
        super.onDestroy();
    }
    public void deleteLastChar() {
        if (currentWord.length() > 0) {
            currentWord = currentWord.substring(0, currentWord.length() - 1);
            currentWord2 = currentWord2.substring(0, currentWord2.length() - 1);
            currentBaseline = currentBaseline.substring(0, currentBaseline.length() - 1);
        }
        readList = "";
        predict(currentWord, currentWord2);
        refresh();
    }

    public void deleteAllChar(){
        currentWord = "";
        currentWord2 = "";
        currentBaseline = "";
        readList = "";
        predict(currentWord, currentWord2);
        refresh();
    }

    int downX, downY;
    long downTime = 0;
    long firstDownTime = 0, lastDownTime = 0; // used for check double-click
    final long STAY_TIME = 400;
    final int SLIP_DIST = 90;
    char upKey = KEY_NOT_FOUND;

    public boolean onTouchEvent(MotionEvent event){
        int[] location = new int[2];
        keyboard.getLocationOnScreen(location);
        int x = (int)event.getX();
        int y = (int)event.getY();

        if (event.getPointerCount() == 1 && (autoKeyboard.getKeyByPosition(x, y - location[1], 1) == upKey || autoKeyboard.getKeyByPosition(x, y - location[1],0) != KEY_NOT_FOUND)){ // in the keyboard area
        //if (event.getPointerCount() == 1 && event.getY() - location[1] + deltaY >= key_top['q' - 'a'] && event.getY() - location[1] + deltaY <= key_bottom['z' - 'a']) {//in the keyboard area
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    downX = x;
                    downY = y;
                    downTime = System.currentTimeMillis();
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
                    newPoint(x, y - location[1]);
                    //action move
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    slideFlag = false;
                    charsPlayed = "";
                    predictionCount = 0;
                    long tempTime = System.currentTimeMillis();
                    boolean predictLetterFlag = (myPlayList.size() == 0); // if the predict letter is considered
                    if (predictLetterFlag == false){
                        nowCh2 = '*';
                    }
                    stopInput();
                    if (confirmMode == CONFIRM_MODE_UP) {
                        if (x < downX - SLIP_DIST && tempTime < downTime + STAY_TIME) {
                            deleteLastChar();
                            playMedia("delete", 0,false);
                            autoKeyboard.resetLayout();
                            autoKeyboard.drawLayout();
                        } else if (x > downX + SLIP_DIST && tempTime < downTime + STAY_TIME) {
                            deleteAllChar();
                            playMedia("delete", 0,false);
                            autoKeyboard.resetLayout();
                            autoKeyboard.drawLayout();
                        } else {
                            upKey = autoKeyboard.getKeyByPosition(x, y - location[1], 1);
                            currentWord += nowCh;
                            currentWord2 += nowCh2;
                            currentBaseline += autoKeyboard.getKeyByPosition(x, y - location[1],0);
                            predict(currentWord, currentWord2);
                            refresh();
                        }
                    }
                    else{
                        if (x < downX - SLIP_DIST && tempTime < downTime + STAY_TIME) {
                            deleteLastChar();
                            nowChSaved = '*';
                            nowCh2Saved = '*';
                            playMedia("delete", 0,false);
                            autoKeyboard.resetLayout();
                            autoKeyboard.drawLayout();
                        } else if (x > downX + SLIP_DIST && tempTime < downTime + STAY_TIME) {
                            deleteAllChar();
                            nowChSaved = '*';
                            nowCh2Saved = '*';
                            playMedia("delete", 0,false);
                            autoKeyboard.resetLayout();
                            autoKeyboard.drawLayout();
                        } else if (downTime == lastDownTime && tempTime - firstDownTime < 800) {
                            //double click
                            if (nowChSaved != '*'){
                                currentWord += nowChSaved;
                                currentWord2 += nowCh2Saved;
                                currentBaseline += nowChBaselineSaved;
                                predict(currentWord, currentWord2);
                                playMedia("delete", 0,false);
                                refresh();
                                nowChSaved = '*';
                                nowCh2Saved = '*';
                            }
                        }
                        else{
                            if (tempTime - downTime > 500) {
                                upKey = autoKeyboard.getKeyByPosition(x, y - location[1], 1);
                                nowChSaved = nowCh;
                                nowCh2Saved = nowCh2;
                                nowChBaselineSaved = autoKeyboard.getKeyByPosition(x, y - location[1],0);
                            }
                        }
                    }
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    class Word implements Comparable<Word>{
        String text;
        double freq;
        Word(String text, double freq){
            this.text = text;
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

        float screen_width_ratio = 1F;
        float screen_height_ratio = 1F;
        //Fuzzy Input Test Var
        float keyboardHeight=570;
        float keyboardWidth=1438;
        float deltaY=190;
        float topThreshold=0;// �Ͻ�
        float bottomThreshold=955;// �½�
        float minWidth=72;// ��С����
        float minHetight=95;//��С����
        int keyPos[];
        int[] location;
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
                if(mode==0){
                    return (init_x-x)*(init_x-x)+(init_y-y)*(init_y-y);
                }
                else{
                    return (curr_x-x)*(curr_x-x)+(curr_y-y)*(curr_y-y);
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

        public char getKeyByPosition(float x, float y, int mode){
            // mode==0 init_layout
            // mode==1 current_layout
            char key = KEY_NOT_FOUND;
            if(y<topThreshold || y>bottomThreshold)
                return key;
            float min_dist = Float.MAX_VALUE;
            for (int i = 0; i < 26; ++i){
                float dist_temp=keys[i].getDist(x,y,mode);
                if (dist_temp<min_dist){
                    key=keys[i].ch;
                    min_dist=dist_temp;
                }
            }
            return key;
        };
        public  boolean tryLayout(char ch,float x,float y){
            int pos = this.keyPos[ch-'a'];
            float dX=x-this.keys[pos].init_x;
            float dY=y-this.keys[pos].init_y;
            if(dY>=0){// ����ƽ��
                if(this.keys[19].init_y+this.keys[19].init_height/2+dY>this.bottomThreshold){
                    if(pos>18)
                        return false;
                    else if(pos>9){//�ڶ�������ѹ��
                        for (int i=0;i<19;i++){
                            this.keys[i].test_y=this.keys[i].init_y+dY;
                            this.keys[i].test_height=this.keys[i].init_height;
                        }
                        float bottomHeight=this.bottomThreshold-this.keys[15].test_y-this.keys[15].test_height/2;
                        for (int i=19;i<26;i++){
                            this.keys[i].test_y=this.bottomThreshold-bottomHeight/2;
                            this.keys[i].test_height=bottomHeight;
                        }
                    }else{// ��һ������ѹ��
                        for (int i=0;i<9;i++){
                            this.keys[i].test_y=this.keys[i].init_y+dY;
                            this.keys[i].test_height=this.keys[i].init_height;
                        }
                        float bottomHeight=this.bottomThreshold-this.keys[0].test_y-this.keys[0].test_height/2;
                        for (int i=10;i<19;i++){
                            this.keys[i].test_y=this.bottomThreshold-bottomHeight*3/4;
                            this.keys[i].test_height=bottomHeight/2;
                        }
                        for (int i=19;i<26;i++){
                            this.keys[i].test_y=this.bottomThreshold-bottomHeight/4;
                            this.keys[i].test_height=bottomHeight/2;
                        }
                    }
                }
                else{
                    for (int i=0;i<26;i++){
                        this.keys[i].test_y=this.keys[i].init_y+dY;
                        this.keys[i].test_height=this.keys[i].init_height;
                    }
                }
            }else{// ����ƽ��
                if(this.keys[0].init_y-this.keys[0].init_height/2+dY<this.topThreshold){
                    if(pos<10)
                        return false;
                    else if(pos<19){//�ڶ�������ѹ
                        for (int i=10;i<26;i++){
                            this.keys[i].test_y=this.keys[i].init_y+dY;
                            this.keys[i].test_height=this.keys[i].init_height;
                        }
                        float topHeight=this.keys[15].test_y-this.keys[15].test_height/2;
                        for (int i=0;i<10;i++){
                            this.keys[i].test_y=topHeight/2;
                            this.keys[i].test_height=topHeight;
                        }
                    }else{//����������ѹ
                        for (int i=19;i<26;i++){
                            this.keys[i].test_y=this.keys[i].init_y+dY;
                            this.keys[i].test_height=this.keys[i].init_height;
                        }
                        float topHeight=this.keys[19].test_y-this.keys[19].test_height/2;
                        for (int i=0;i<10;i++){
                            this.keys[i].test_y=topHeight/4;
                            this.keys[i].test_height=topHeight/2;
                        }
                        for (int i=10;i<19;i++){
                            this.keys[i].test_y=topHeight*3/4;
                            this.keys[i].test_height=topHeight/2;
                        }
                    }
                }else{
                    for (int i=0;i<26;i++){
                        this.keys[i].test_y=this.keys[i].init_y+dY;
                        this.keys[i].test_height=this.keys[i].init_height;
                    }
                }
            }
            for (int i=0;i<26;i++){
                if(this.keys[i].test_height<this.minHetight){
                    return false;
                }
            }
            if(dX>=0) {// ����ƽ��
                if (pos == 9 || pos == 18 || pos == 25)
                    return false;
            }else {// ����ƽ��
                if (pos == 0 || pos == 10 || pos == 19)
                    return false;
            }

            if(pos<10 ){// ��һ��
                this.keys[pos].test_x=x;
                this.keys[pos].test_width=this.keys[pos].init_width;

                float rightRatio=(this.keyboardWidth-this.keys[pos].test_x-this.keys[pos].test_width/2)/(this.keyboardWidth-this.keys[pos].init_x-this.keys[pos].init_width/2);
                float leftRatio=(this.keys[pos].test_x-this.keys[pos].test_width)/(this.keys[pos].init_x-this.keys[pos].init_width);

                for(int i=0;i<pos;i++){
                    this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                    this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                }
                for (int i=pos+1;i<10;i++){
                    this.keys[i].test_x=keyboardWidth-(keyboardWidth-this.keys[i].init_x)*rightRatio;
                    this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                }

                for (int i=10;i<19;i++){
                    this.keys[i].test_x=(this.keys[i-10].test_x+this.keys[i-9].test_x)/2;
                    this.keys[i].test_width=this.keys[i-9].test_x-this.keys[i-10].test_x;
                }
                for (int i=19;i<26;i++){
                    this.keys[i].test_x=this.keys[i-8].test_x;
                    this.keys[i].test_width=this.keys[i-8].test_width;
                }

            }else if(pos<19){// �ڶ���
                this.keys[pos].test_x=x;
                this.keys[pos].test_width=this.keys[pos].init_width;

                float rightRatio=(this.keyboardWidth-this.keys[pos].test_x-this.keys[pos].test_width/2)/(this.keyboardWidth-this.keys[pos].init_x-this.keys[pos].init_width/2);
                float leftRatio=(this.keys[pos].test_x-this.keys[pos].test_width)/(this.keys[pos].init_x-this.keys[pos].init_width);

                for(int i=10;i<pos;i++){
                    this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                    this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                }
                for (int i=pos+1;i<19;i++){
                    this.keys[i].test_x=this.keyboardWidth-(this.keyboardWidth-this.keys[i].init_x)*rightRatio;
                    this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                }
                this.keys[0].test_x=this.keys[10].test_x/2;
                this.keys[0].test_width=this.keys[10].test_x;
                this.keys[9].test_width=this.keyboardWidth-this.keys[18].test_x;
                this.keys[9].test_x=this.keyboardWidth-this.keys[9].test_width/2;
                for (int i=1;i<9;i++){
                    this.keys[i].test_x=(this.keys[i+9].test_x+this.keys[i+10].test_x)/2;
                    this.keys[i].test_width=this.keys[i+10].test_x-this.keys[i+9].test_x;
                }
                for (int i=19;i<26;i++){
                    this.keys[i].test_x=this.keys[i-8].test_x;
                    this.keys[i].test_width=this.keys[i-8].test_width;
                }
            }else{// ������
                this.keys[pos].test_x=x;
                this.keys[pos].test_width=this.keys[pos].init_width;

                float rightRatio=(this.keyboardWidth-this.keys[pos].test_x-this.keys[pos].test_width/2)/(this.keyboardWidth-this.keys[pos].init_x-this.keys[pos].init_width/2);
                float leftRatio=(this.keys[pos].test_x-this.keys[pos].test_width)/(this.keys[pos].init_x-this.keys[pos].init_width);

                for(int i=19;i<pos;i++){
                    this.keys[i].test_x=this.keys[i].init_x*leftRatio;
                    this.keys[i].test_width=this.keys[i].init_width*leftRatio;
                }
                for (int i=pos+1;i<26;i++){
                    this.keys[i].test_x=this.keyboardWidth-(this.keyboardWidth-this.keys[i].init_x)*rightRatio;
                    this.keys[i].test_width=this.keys[i].init_width*rightRatio;
                }

                for (int i=11;i<18;i++){
                    this.keys[i].test_x=this.keys[i+8].test_x;
                    this.keys[i].test_width=this.keys[i+8].test_width;
                }
                this.keys[10].test_width=this.keys[11].test_x-this.keys[11].test_width/2;
                this.keys[10].test_x=this.keys[10].test_width/2;
                this.keys[18].test_width=this.keyboardWidth-this.keys[17].test_x-this.keys[17].test_width/2;
                this.keys[18].test_x=this.keyboardWidth-this.keys[18].test_width/2;

                this.keys[0].test_x=this.keys[10].test_x/2;
                this.keys[0].test_width=this.keys[10].test_x;
                this.keys[9].test_width=this.keyboardWidth-this.keys[18].test_x;
                this.keys[9].test_x=this.keyboardWidth-this.keys[9].test_width/2;
                for (int i=1;i<9;i++){
                    this.keys[i].test_x=(this.keys[i+9].test_x+this.keys[i+10].test_x)/2;
                    this.keys[i].test_width=this.keys[i+10].test_x-this.keys[i+9].test_x;
                }
            }


            for (int i=0;i<26;i++){
                if(this.keys[i].test_width<this.minWidth){
                    return false;
                }
            }
            for (int i=0;i<26;i++){
                this.keys[i].curr_x=this.keys[i].test_x;
                this.keys[i].curr_y=this.keys[i].test_y;
                this.keys[i].curr_height=this.keys[i].test_height;
                this.keys[i].curr_width=this.keys[i].test_width;
            }
            return true;
        }

        public void drawLayout(){ // curr_x,curr_y
            float left=this.location[0]+this.keys[0].curr_x-this.keys[0].curr_width/2;
            float top=this.location[1]+this.keys[0].curr_y-this.keys[0].curr_height/2;
            float right=this.location[0]+this.keys[9].curr_x+this.keys[9].curr_width/2;
            float bottom=this.location[1]+this.keys[25].curr_y+this.keys[25].curr_height/2;
            this.baseBitmap = Bitmap.createBitmap(this.keyboard.getWidth(),this.keyboard.getHeight(), Bitmap.Config.ARGB_8888);
            this.canvas=new Canvas(this.baseBitmap);
            RectF rect = new RectF(left, top, right, bottom);
            this.canvas.drawRect(rect, this.backgroundPaint);
            for (int i=0;i<26;i++){
                this.canvas.drawText(String.valueOf(this.keys[i].ch),this.keys[i].curr_x+this.location[0],this.keys[i].curr_y+this.location[1],this.textPaint);
            }
            this.keyboard.setImageBitmap(this.baseBitmap);
        }

        public void resetLayout(){
            if(this.keys==null){
                this.keys=new KEY[26];
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

            for (int i=0;i<10;i++){
                this.keys[i].init_x=this.keyboardWidth*(2*i+1)/20;
                this.keys[i].init_y=this.keyboardHeight/6+this.deltaY;
            }
            for (int i=10;i<19;i++){
                this.keys[i].init_x=(this.keys[i-10].init_x+this.keys[i-9].init_x)/2;
                this.keys[i].init_y=this.keyboardHeight/2+this.deltaY;
            }
            for (int i=19;i<26;i++){
                this.keys[i].init_x=this.keys[i-8].init_x;
                this.keys[i].init_y=this.keyboardHeight*5/6+this.deltaY;
            }

            for (int i=0;i<26;i++) {
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
            this.backgroundPaint=new Paint();
            this.textPaint=new Paint();
            this.backgroundPaint.setColor(Color.rgb(230,255,255));
            this.backgroundPaint.setStrokeJoin(Paint.Join.ROUND);
            this.backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
            this.backgroundPaint.setStrokeWidth(3);

            this.textPaint.setColor(Color.BLACK);
            this.textPaint.setStrokeJoin(Paint.Join.ROUND);
            this.textPaint.setStrokeCap(Paint.Cap.ROUND);
            this.textPaint.setStrokeWidth(3);
            this.textPaint.setTextSize(Math.round(40*screen_height_ratio));
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
            this.keys=new KEY[26];
            for (int i=0;i<26;i++){
                this.keys[i]=new KEY();
            }
            this.resetLayout();
        }
    }

}
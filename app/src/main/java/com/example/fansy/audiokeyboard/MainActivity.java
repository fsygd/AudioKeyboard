package com.example.fansy.audiokeyboard;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
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
    TextView text, candidatesView, readListView,voiceSpeedText;
    Button confirmButton, initModeButton, confirmModeButton, speedpbutton, speedmbutton;
    String readList = ""; //current voice list
    String currentWord = ""; //most possible char sequence
    String currentWord2 = ""; //second possible char sequence
    String currentBaseline = "";
    char nowCh = 0; //the most possible char
    char nowCh2 = 0; //the second possible char, '*' if less than 1/10 of the most possible char
    char nowChSaved = 0;
    char nowCh2Saved = 0;
    char nowChBaselineSaved = 0;
    ArrayList<Word> dict = new ArrayList();
    ArrayList<Character> seq = new ArrayList<Character>(); //char sequence during the whole touch
    String keys[] = new String[] {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    String keysNearby[] = new String[26];
    double keysNearbyProb[][] = new double[26][26];
    int key_left[] = new int[26];
    int key_right[] = new int[26];
    int key_top[] = new int[26];
    int key_bottom[] = new int[26];
    int deltaX = 0, deltaY = 0; //translation of XY coordinate

    int voiceSpeed = 50;

    float screen_width_ratio = 1F;
    float screen_height_ratio = 1F;

    public void getScreenSizeRatio(){
        DisplayMetrics metrics =new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        screen_width_ratio = metrics.widthPixels/1440F;
        screen_height_ratio = metrics.heightPixels/2560F;
    }

    // init the coordinates of the key a-z on phone
    public void initKeyPosition(){
        key_left['q' - 'a'] = (int)(15F*screen_width_ratio);
        key_right['q' - 'a'] = (int)(137F*screen_width_ratio);
        key_bottom['q' - 'a'] = (int)(167F*screen_width_ratio);
        key_top['q' - 'a'] = (int)(0F*screen_width_ratio);
        for (int i = 1; i < keys[0].length(); ++i){
            key_left[keys[0].charAt(i) - 'a'] = key_left[keys[0].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_right[keys[0].charAt(i) - 'a'] = key_right[keys[0].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_top[keys[0].charAt(i) - 'a'] = key_top[keys[0].charAt(i - 1) - 'a'];
            key_bottom[keys[0].charAt(i) - 'a'] = key_bottom[keys[0].charAt(i - 1) - 'a'];
        }

        key_left['a' - 'a'] = (int)(142F*screen_width_ratio);
        key_right['a' - 'a'] = (int)(209F*screen_width_ratio);
        key_bottom['a' - 'a'] = (int)(354F*screen_width_ratio);
        key_top['a' - 'a'] = (int)(187F*screen_width_ratio);
        for (int i = 1; i < keys[1].length(); ++i){
            key_left[keys[1].charAt(i) - 'a'] = key_left[keys[1].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_right[keys[1].charAt(i) - 'a'] = key_right[keys[1].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_top[keys[1].charAt(i) - 'a'] = key_top[keys[1].charAt(i - 1) - 'a'];
            key_bottom[keys[1].charAt(i) - 'a'] = key_bottom[keys[1].charAt(i - 1) - 'a'];
        }

        key_left['z' - 'a'] = (int)(230F*screen_width_ratio);
        key_right['z' - 'a'] = (int)(352F*screen_width_ratio);
        key_bottom['z' - 'a'] = (int)(541F*screen_width_ratio);
        key_top['z' - 'a'] = (int)(374F*screen_width_ratio);
        for (int i = 1; i < keys[2].length(); ++i){
            key_left[keys[2].charAt(i) - 'a'] = key_left[keys[2].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_right[keys[2].charAt(i) - 'a'] = key_right[keys[2].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_top[keys[2].charAt(i) - 'a'] = key_top[keys[2].charAt(i - 1) - 'a'];
            key_bottom[keys[2].charAt(i) - 'a'] = key_bottom[keys[2].charAt(i - 1) - 'a'];
        }
    }

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
                deltaX = 0;
                deltaY = 0;
                char ch = getKeyByPosition(x, y);
                deltaX = (key_left[ch - 'a'] + key_right[ch - 'a']) / 2 - x; //move to the centre of the key
                deltaY = (key_top[ch - 'a'] + key_bottom[ch - 'a']) / 2 - y;
                addToSeq(getKeyByPosition(x, y), true,true);
            }
            else if(initMode == INIT_MODE_RELATIVE){
                deltaX = 0;
                deltaY = 0;
                char ch = getKeyByPosition(x, y);
                char best = addToSeq(ch, false,true);
                deltaX = (key_left[best - 'a'] + key_right[best - 'a']) / 2 - x; //move to the centre of the most possible key
                deltaY = (key_top[best - 'a'] + key_bottom[best - 'a']) / 2 - y;
                addToSeq(best, true,true);
            }else{
                deltaX = 0;
                deltaY = 0;
                char ch = getKeyByPosition(x,y);
                addToSeq(ch,true,false);
            }
        }
        else{
            addToSeq(getKeyByPosition(x, y), true,true);
        }
    }

    //init keysNearby and keysNearbyProb
    public void initKeyboard(){
        int delta[][] = new int [][]{{-1, -1, 0, 0, 0, 1, 1},{0, 1, -1, 0, 1, -1, 0}};
        double prob[] = new double[]{0.042, 0.042, 0.192, 0.376, 0.192, 0.044, 0.023};
        for (int  i= 0; i < 3; ++i)
            for (int j = 0; j < keys[i].length(); ++j){
                char ch = keys[i].charAt(j);
                keysNearby[ch - 'a'] = "";
                for (int k = 0; k < 7; ++k){
                    int _i = i + delta[0][k], _j = j + delta[1][k];
                    if (_i >= 0 && _i < 3 && _j >= 0 && _j < keys[_i].length()) {
                        keysNearby[ch - 'a'] += keys[_i].charAt(_j);
                        keysNearbyProb[ch - 'a'][keysNearby[ch - 'a'].length() - 1] = prob[k];
                    }
                }
            }
    }

    final int DICT_SIZE = 10000;
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
                else if(initMode == INIT_MODE_RELATIVE)
                    initMode = INIT_MODE_NOTHING;
                else
                    initMode = INIT_MODE_ABSOLUTE;
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

    //todo reconstruct
    public void playMedia(String tag, int index){
        myPlayList.add(voice.get(tag)[index]);
        playFirstVoice();
    }

    //if write=false, just return the most possible key
    public char addToSeq(char ch, boolean write, boolean predictMode){
        if (ch != KEY_NOT_FOUND){
            if (seq.size() == 0 || seq.get(seq.size() - 1) != ch) {
                //make a vibrate
                Vibrator vibrator =  (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0, 30};
                vibrator.vibrate(pattern, -1);
                ArrayList<Word> letters = new ArrayList<Word>();
                if(predictMode){
                    for (int i = 0; i < keysNearby[ch - 'a'].length(); ++i)
                        letters.add(new Word(keysNearby[ch - 'a'].charAt(i) + "", 0));
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
                    for (int i = 0; i < keysNearby[ch - 'a'].length(); ++i)
                        letters.get(i).freq *= keysNearbyProb[ch - 'a'][i];
                    Collections.sort(letters);
                    if (!write) {
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

                playMedia("ios11da", 0);
                if (predictMode){
                    if (seq.size() == 1){
                        //prob top 2
                        if (letters.get(0).freq > 0) {
                            nowCh = letters.get(0).text.charAt(0);
                            playMedia("ios11_"+voiceSpeed, nowCh - 'a');
                            readList += nowCh;
                            if (letters.get(1).freq * 10 > letters.get(0).freq){
                                nowCh2 = letters.get(1).text.charAt(0);
                                playMedia("ios11_"+voiceSpeed, nowCh2 - 'a');
                                readList += nowCh2;
                            }
                        }
                        else {
                            //current key
                            nowCh = ch;
                            playMedia("ios11_"+voiceSpeed, nowCh - 'a');
                            readList += nowCh;
                        }
                    }
                    else{
                        //current key
                        nowCh = ch;
                        playMedia("ios11_"+voiceSpeed, nowCh - 'a');
                        readList += nowCh;
                    }
                }else{
                    nowCh = ch;
                    playMedia("ios11_"+voiceSpeed, nowCh - 'a');
                    readList += nowCh;
                }

                refresh();
            }
        }
        return 'a';
    }

    final char KEY_NOT_FOUND = 0;
    //get key with translation
    char getKeyByPosition(int x, int y){
        x += deltaX;
        y += deltaY;
        char key = KEY_NOT_FOUND;
        int min_dist = Integer.MAX_VALUE;
        for (int i = 0; i < 26; ++i){
            int _x = (key_left[i] + key_right[i]) / 2;
            int _y = (key_top[i] + key_bottom[i]) / 2;
            if ((x - _x) * (x - _x) + (y - _y) * (y - _y) < min_dist){
                key = (char)('a' + i);
                min_dist = (x - _x) * (x - _x) + (y - _y) * (y - _y);
            }
        }
        return key;
    }

    //get key without translation
    char getKeyBaseLine(int x, int y){
        char key = KEY_NOT_FOUND;
        int min_dist = Integer.MAX_VALUE;
        for (int i = 0; i < 26; ++i){
            int _x = (key_left[i] + key_right[i]) / 2;
            int _y = (key_top[i] + key_bottom[i]) / 2;
            if ((x - _x) * (x - _x) + (y - _y) * (y - _y) < min_dist){
                key = (char)('a' + i);
                min_dist = (x - _x) * (x - _x) + (y - _y) * (y - _y);
            }
        }
        return key;
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

        getScreenSizeRatio();
        initKeyPosition();
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

    public boolean onTouchEvent(MotionEvent event){
        if (event.getPointerCount() == 1 && event.getY() >= (int)(1500F*screen_width_ratio)) {//in the keyboard area
            int x = (int) event.getX();
            int y = (int) event.getY();
            int[] location = new int[2];
            keyboard.getLocationOnScreen(location);
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
                    long tempTime = System.currentTimeMillis();
                    stopInput();
                    if (confirmMode == CONFIRM_MODE_UP) {
                        if (x < downX - SLIP_DIST && tempTime < downTime + STAY_TIME) {
                            deleteLastChar();
                            playMedia("delete", 0);
                        } else if (x > downX + SLIP_DIST && tempTime < downTime + STAY_TIME) {
                            deleteAllChar();
                            playMedia("delete", 0);
                        } else {
                            currentWord += nowCh;
                            currentWord2 += nowCh2;
                            currentBaseline += getKeyBaseLine(x, y - location[1]);
                            predict(currentWord, currentWord2);
                            refresh();
                        }
                        break;
                    }
                    else{
                        if (x < downX - SLIP_DIST && tempTime < downTime + STAY_TIME) {
                            deleteLastChar();
                            nowChSaved = '*';
                            nowCh2Saved = '*';
                            playMedia("delete", 0);
                        } else if (x > downX + SLIP_DIST && tempTime < downTime + STAY_TIME) {
                            deleteAllChar();
                            nowChSaved = '*';
                            nowCh2Saved = '*';
                            playMedia("delete", 0);
                        } else if (downTime == lastDownTime && tempTime - firstDownTime < 800) {
                            //double click
                            if (nowChSaved != '*'){
                                currentWord += nowChSaved;
                                currentWord2 += nowCh2Saved;
                                currentBaseline += nowChBaselineSaved;
                                predict(currentWord, currentWord2);
                                playMedia("delete", 0);
                                refresh();
                                nowChSaved = '*';
                                nowCh2Saved = '*';
                            }
                        }
                        else{
                            if (tempTime - downTime > 500) {
                                nowChSaved = nowCh;
                                nowCh2Saved = nowCh2;
                                nowChBaselineSaved = getKeyBaseLine(x, y - location[1]);
                            }
                        }
                        break;
                    }
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
}
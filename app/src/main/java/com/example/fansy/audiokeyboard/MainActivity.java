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
    int initMode = INIT_MODE_ABSOLUTE;
    ImageView keyboard;
    TextView text, candidatesView, readListView;
    Button confirmButton, initModeButton;
    String readList = ""; //current voice list
    String currentWord = ""; //most possible char sequence
    String currentWord2 = ""; //second possible char sequence
    String currentBaseline = "";
    char nowCh = 0; //the most possible char
    char nowCh2 = 0; //the second possible char, '*' if less than 1/10 of the most possible char
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
        else
            initModeButton.setText("relative");
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
                addToSeq(getKeyByPosition(x, y), true);
            }
            else {
                deltaX = 0;
                deltaY = 0;
                char ch = getKeyByPosition(x, y);
                char best = addToSeq(ch, false);
                deltaX = (key_left[best - 'a'] + key_right[best - 'a']) / 2 - x; //move to the centre of the most possible key
                deltaY = (key_top[best - 'a'] + key_bottom[best - 'a']) / 2 - y;
                addToSeq(best, true);
            }
        }
        else{
            addToSeq(getKeyByPosition(x, y), true);
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
        voice.put("first", new int[26]);
        voice.put("second", new int[26]);
        voice.put("ios11_50", new int[26]);
        voice.put("ios11da", new int[1]);
        voice.put("delete", new int[1]);

        voice.get("first")[ 0] = R.raw.voiceover_a;
        voice.get("first")[ 1] = R.raw.voiceover_b;
        voice.get("first")[ 2] = R.raw.voiceover_c;
        voice.get("first")[ 3] = R.raw.voiceover_d;
        voice.get("first")[ 4] = R.raw.voiceover_e;
        voice.get("first")[ 5] = R.raw.voiceover_f;
        voice.get("first")[ 6] = R.raw.voiceover_g;
        voice.get("first")[ 7] = R.raw.voiceover_h;
        voice.get("first")[ 8] = R.raw.voiceover_i;
        voice.get("first")[ 9] = R.raw.voiceover_j;
        voice.get("first")[10] = R.raw.voiceover_k;
        voice.get("first")[11] = R.raw.voiceover_l;
        voice.get("first")[12] = R.raw.voiceover_m;
        voice.get("first")[13] = R.raw.voiceover_n;
        voice.get("first")[14] = R.raw.voiceover_o;
        voice.get("first")[15] = R.raw.voiceover_p;
        voice.get("first")[16] = R.raw.voiceover_q;
        voice.get("first")[17] = R.raw.voiceover_r;
        voice.get("first")[18] = R.raw.voiceover_s;
        voice.get("first")[19] = R.raw.voiceover_t;
        voice.get("first")[20] = R.raw.voiceover_u;
        voice.get("first")[21] = R.raw.voiceover_v;
        voice.get("first")[22] = R.raw.voiceover_w;
        voice.get("first")[23] = R.raw.voiceover_x;
        voice.get("first")[24] = R.raw.voiceover_y;
        voice.get("first")[25] = R.raw.voiceover_z;

        voice.get("second")[ 0] = R.raw.second_a;
        voice.get("second")[ 1] = R.raw.second_b;
        voice.get("second")[ 2] = R.raw.second_c;
        voice.get("second")[ 3] = R.raw.second_d;
        voice.get("second")[ 4] = R.raw.second_e;
        voice.get("second")[ 5] = R.raw.second_f;
        voice.get("second")[ 6] = R.raw.second_g;
        voice.get("second")[ 7] = R.raw.second_h;
        voice.get("second")[ 8] = R.raw.second_i;
        voice.get("second")[ 9] = R.raw.second_j;
        voice.get("second")[10] = R.raw.second_k;
        voice.get("second")[11] = R.raw.second_l;
        voice.get("second")[12] = R.raw.second_m;
        voice.get("second")[13] = R.raw.second_n;
        voice.get("second")[14] = R.raw.second_o;
        voice.get("second")[15] = R.raw.second_p;
        voice.get("second")[16] = R.raw.second_q;
        voice.get("second")[17] = R.raw.second_r;
        voice.get("second")[18] = R.raw.second_s;
        voice.get("second")[19] = R.raw.second_t;
        voice.get("second")[20] = R.raw.second_u;
        voice.get("second")[21] = R.raw.second_v;
        voice.get("second")[22] = R.raw.second_w;
        voice.get("second")[23] = R.raw.second_x;
        voice.get("second")[24] = R.raw.second_y;
        voice.get("second")[25] = R.raw.second_z;

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
                else
                    initMode = INIT_MODE_ABSOLUTE;
                refresh();
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
    public char addToSeq(char ch, boolean write){
        if (ch != KEY_NOT_FOUND){
            if (seq.size() == 0 || seq.get(seq.size() - 1) != ch) {
                //make a vibrate
                Vibrator vibrator =  (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0, 30};
                vibrator.vibrate(pattern, -1);

                ArrayList<Word> letters = new ArrayList<Word>();
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
                //write=true
                seq.add(ch);
                Log.i("seq", ch + "");
                stopVoice();
                readList = "";
                nowCh = '*';
                nowCh2 = '*';

                playMedia("ios11da", 0);
                if (seq.size() == 1){
                    //prob top 2
                    if (letters.get(0).freq > 0) {
                        nowCh = letters.get(0).text.charAt(0);
                        playMedia("ios11_50", nowCh - 'a');
                        readList += nowCh;
                        if (letters.get(1).freq * 10 > letters.get(0).freq){
                            nowCh2 = letters.get(1).text.charAt(0);
                            playMedia("ios11_50", nowCh2 - 'a');
                            readList += nowCh2;
                        }
                    }
                    else {
                        //current key
                        nowCh = ch;
                        playMedia("ios11_50", nowCh - 'a');
                        readList += nowCh;
                    }
                }
                else{
                    //current key
                    nowCh = ch;
                    playMedia("ios11_50", nowCh - 'a');
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
    long downTime;
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
                    newPoint(x, y - location[1]);
                    //action down
                    break;

                case MotionEvent.ACTION_MOVE:
                    newPoint(x, y - location[1]);
                    //action move
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    stopInput();
                    if (x < downX - SLIP_DIST && System.currentTimeMillis() < downTime + STAY_TIME) {
                        deleteLastChar();
                        playMedia("delete", 0);
                    }
                    else if (x > downX + SLIP_DIST && System.currentTimeMillis() < downTime + STAY_TIME) {
                        deleteAllChar();
                        playMedia("delete", 0);
                    }
                    else {
                        currentWord += nowCh;
                        currentWord2 += nowCh2;
                        currentBaseline += getKeyBaseLine(x, y - location[1]);
                        predict(currentWord, currentWord2);
                        refresh();
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
}
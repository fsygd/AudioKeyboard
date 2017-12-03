package com.example.fansy.audiokeyboard;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {
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
    MediaPlayer voices[] = new MediaPlayer[26];
    MediaPlayer voices_second[] = new MediaPlayer[26];
    MediaPlayer voiceEmpty = new MediaPlayer();
    ArrayList<MediaPlayer> mediaPlayers = new ArrayList<>(); //mediaPlayer
    ArrayList<Character> seq = new ArrayList<Character>(); //char sequence during the whole touch
    String keys[] = new String[] {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    String keysNearby[] = new String[26];
    double keysNearbyProb[][] = new double[26][26];
    int key_left[] = new int[26];
    int key_right[] = new int[26];
    int key_top[] = new int[26];
    int key_bottom[] = new int[26];
    int deltaX = 0, deltaY = 0; //translation of XY coordinate
    // init the coordinates of the key a-z on phone
    public void initKeyPosition(){
        key_left['q' - 'a'] = 15;
        key_right['q' - 'a'] = 137;
        key_bottom['q' - 'a'] = 167;
        key_top['q' - 'a'] = 0;
        for (int i = 1; i < keys[0].length(); ++i){
            key_left[keys[0].charAt(i) - 'a'] = key_left[keys[0].charAt(i - 1) - 'a'] + 142;
            key_right[keys[0].charAt(i) - 'a'] = key_right[keys[0].charAt(i - 1) - 'a'] + 142;
            key_top[keys[0].charAt(i) - 'a'] = key_top[keys[0].charAt(i - 1) - 'a'];
            key_bottom[keys[0].charAt(i) - 'a'] = key_bottom[keys[0].charAt(i - 1) - 'a'];
        }

        key_left['a' - 'a'] = 87;
        key_right['a' - 'a'] = 209;
        key_bottom['a' - 'a'] = 354;
        key_top['a' - 'a'] = 187;
        for (int i = 1; i < keys[1].length(); ++i){
            key_left[keys[1].charAt(i) - 'a'] = key_left[keys[1].charAt(i - 1) - 'a'] + 142;
            key_right[keys[1].charAt(i) - 'a'] = key_right[keys[1].charAt(i - 1) - 'a'] + 142;
            key_top[keys[1].charAt(i) - 'a'] = key_top[keys[1].charAt(i - 1) - 'a'];
            key_bottom[keys[1].charAt(i) - 'a'] = key_bottom[keys[1].charAt(i - 1) - 'a'];
        }

        key_left['z' - 'a'] = 230;
        key_right['z' - 'a'] = 352;
        key_bottom['z' - 'a'] = 541;
        key_top['z' - 'a'] = 374;
        for (int i = 1; i < keys[2].length(); ++i){
            key_left[keys[2].charAt(i) - 'a'] = key_left[keys[2].charAt(i - 1) - 'a'] + 142;
            key_right[keys[2].charAt(i) - 'a'] = key_right[keys[2].charAt(i - 1) - 'a'] + 142;
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
        if (mediaPlayers.size() > 0)
            mediaPlayers.get(0).pause();
        mediaPlayers.clear();
    }

    public void stopInput(){
        seq.clear();
        stopVoice();
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
        voices[0] = MediaPlayer.create(this, R.raw.voiceover_a);
        voices[1] = MediaPlayer.create(this, R.raw.voiceover_b);
        voices[2] = MediaPlayer.create(this, R.raw.voiceover_c);
        voices[3] = MediaPlayer.create(this, R.raw.voiceover_d);
        voices[4] = MediaPlayer.create(this, R.raw.voiceover_e);
        voices[5] = MediaPlayer.create(this, R.raw.voiceover_f);
        voices[6] = MediaPlayer.create(this, R.raw.voiceover_g);
        voices[7] = MediaPlayer.create(this, R.raw.voiceover_h);
        voices[8] = MediaPlayer.create(this, R.raw.voiceover_i);
        voices[9] = MediaPlayer.create(this, R.raw.voiceover_j);
        voices[10] = MediaPlayer.create(this, R.raw.voiceover_k);
        voices[11] = MediaPlayer.create(this, R.raw.voiceover_l);
        voices[12] = MediaPlayer.create(this, R.raw.voiceover_m);
        voices[13] = MediaPlayer.create(this, R.raw.voiceover_n);
        voices[14] = MediaPlayer.create(this, R.raw.voiceover_o);
        voices[15] = MediaPlayer.create(this, R.raw.voiceover_p);
        voices[16] = MediaPlayer.create(this, R.raw.voiceover_q);
        voices[17] = MediaPlayer.create(this, R.raw.voiceover_r);
        voices[18] = MediaPlayer.create(this, R.raw.voiceover_s);
        voices[19] = MediaPlayer.create(this, R.raw.voiceover_t);
        voices[20] = MediaPlayer.create(this, R.raw.voiceover_u);
        voices[21] = MediaPlayer.create(this, R.raw.voiceover_v);
        voices[22] = MediaPlayer.create(this, R.raw.voiceover_w);
        voices[23] = MediaPlayer.create(this, R.raw.voiceover_x);
        voices[24] = MediaPlayer.create(this, R.raw.voiceover_y);
        voices[25] = MediaPlayer.create(this, R.raw.voiceover_z);
        voices_second[0] = MediaPlayer.create(this, R.raw.second_a);
        voices_second[1] = MediaPlayer.create(this, R.raw.second_b);
        voices_second[2] = MediaPlayer.create(this, R.raw.second_c);
        voices_second[3] = MediaPlayer.create(this, R.raw.second_d);
        voices_second[4] = MediaPlayer.create(this, R.raw.second_e);
        voices_second[5] = MediaPlayer.create(this, R.raw.second_f);
        voices_second[6] = MediaPlayer.create(this, R.raw.second_g);
        voices_second[7] = MediaPlayer.create(this, R.raw.second_h);
        voices_second[8] = MediaPlayer.create(this, R.raw.second_i);
        voices_second[9] = MediaPlayer.create(this, R.raw.second_j);
        voices_second[10] = MediaPlayer.create(this, R.raw.second_k);
        voices_second[11] = MediaPlayer.create(this, R.raw.second_l);
        voices_second[12] = MediaPlayer.create(this, R.raw.second_m);
        voices_second[13] = MediaPlayer.create(this, R.raw.second_n);
        voices_second[14] = MediaPlayer.create(this, R.raw.second_o);
        voices_second[15] = MediaPlayer.create(this, R.raw.second_p);
        voices_second[16] = MediaPlayer.create(this, R.raw.second_q);
        voices_second[17] = MediaPlayer.create(this, R.raw.second_r);
        voices_second[18] = MediaPlayer.create(this, R.raw.second_s);
        voices_second[19] = MediaPlayer.create(this, R.raw.second_t);
        voices_second[20] = MediaPlayer.create(this, R.raw.second_u);
        voices_second[21] = MediaPlayer.create(this, R.raw.second_v);
        voices_second[22] = MediaPlayer.create(this, R.raw.second_w);
        voices_second[23] = MediaPlayer.create(this, R.raw.second_x);
        voices_second[24] = MediaPlayer.create(this, R.raw.second_y);
        voices_second[25] = MediaPlayer.create(this, R.raw.second_z);
        voiceEmpty = MediaPlayer.create(this, R.raw.blank);
        for (int i = 0; i < 26; ++i) {
            voices[i].setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mediaPlayers.size() > 0)
                        mediaPlayers.remove(0);
                    if (mediaPlayers.size() > 0)
                        mediaPlayers.get(0).start();
                }
            });
        }

        for (int i = 0; i < 26; ++i) {
            voices_second[i].setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mediaPlayers.size() > 0)
                        mediaPlayers.remove(0);
                    if (mediaPlayers.size() > 0)
                        mediaPlayers.get(0).start();
                }
            });
        }

        voiceEmpty.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mediaPlayers.size() > 0)
                    mediaPlayers.remove(0);
                if (mediaPlayers.size() > 0)
                    mediaPlayers.get(0).start();
            }
        });
    }

    public void initButtons(){
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopInput();
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

    //todo reconstruct
    public void playMedia(MediaPlayer mp){
        mediaPlayers.add(mp);
        if (mediaPlayers.size() == 1)
            mediaPlayers.get(0).start();
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
                stopVoice();
                readList = "";
                nowCh = '*';
                nowCh2 = '*';

                if (seq.size() == 1){
                    //prob top 2
                    if (letters.get(0).freq > 0) {
                        nowCh = letters.get(0).text.charAt(0);
                        playMedia(voices[nowCh - 'a']);
                        readList += nowCh;
                        if (letters.get(1).freq * 10 > letters.get(0).freq){
                            nowCh2 = letters.get(1).text.charAt(0);
                            playMedia(voices_second[nowCh2 - 'a']);
                            readList += nowCh2;
                        }
                    }
                    else {
                        //current key
                        nowCh = ch;
                        playMedia(voices[nowCh - 'a']);
                        readList += nowCh;
                    }
                }
                else{
                    //current key
                    nowCh = ch;
                    playMedia(voices[nowCh - 'a']);
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
        setContentView(R.layout.activity_main_relative);

        keyboard = (ImageView)findViewById(R.id.keyboard);
        text = (TextView)findViewById(R.id.text);
        candidatesView = (TextView)findViewById(R.id.candidates);
        confirmButton = (Button)findViewById(R.id.confirm_button);
        readListView = (TextView)findViewById(R.id.readList);
        initModeButton = (Button)findViewById(R.id.init_mode_button);

        initKeyPosition();
        initDict();
        initVoice();
        initButtons();
        initKeyboard();
        //left 0 right 1440 top 1554 bottom 2320
    }

    public void deleteLastChar() {
        if (currentWord.length() > 0) {
            currentWord = currentWord.substring(0, currentWord.length() - 1);
            currentWord2 = currentWord2.substring(0, currentWord2.length() - 1);
            currentBaseline = currentBaseline.substring(0, currentBaseline.length() - 1);
        }
        predict(currentWord, currentWord2);
        refresh();
    }

    public void deleteAllChar(){
        currentWord = "";
        currentWord2 = "";
        currentBaseline = "";
        predict(currentWord, currentWord2);
        refresh();
    }

    int downX, downY;
    long downTime;
    final long STAY_TIME = 200;
    final int SLIP_DIST = 90;

    public boolean onTouchEvent(MotionEvent event){
        if (event.getPointerCount() == 1 && event.getY() >= 1500) {//in the keyboard area
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
                    }
                    else if (x > downX + SLIP_DIST && System.currentTimeMillis() < downTime + STAY_TIME) {
                        deleteAllChar();
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
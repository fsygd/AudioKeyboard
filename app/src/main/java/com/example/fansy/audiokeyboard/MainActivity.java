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
    TextView text, candidatesView, readList;
    Button confirmButton, initModeButton;
    String currentWord = "";
    String currentWord2 = "";
    char nowCh = 0;
    char nowCh2 = 0;
    ArrayList<Word> dict = new ArrayList();
    MediaPlayer voices[] = new MediaPlayer[26];
    MediaPlayer voices_second[] = new MediaPlayer[26];
    final int emptyTimes = 4;
    MediaPlayer voiceEmpty = new MediaPlayer();
    ArrayList<MediaPlayer> mediaPlayers = new ArrayList<>();
    ArrayList<Character> seq = new ArrayList<Character>();
    String keys[] = new String[] {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    String keysNearby[] = new String[26];
    double keysNearbyProb[][] = new double[26][10];
    int key_left[] = new int[26];
    int key_right[] = new int[26];
    int key_top[] = new int[26];
    int key_bottom[] = new int[26];
    int deltaX = 0, deltaY = 0;
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

    public void refresh(){
        text.setText(currentWord + "\n" + currentWord2);
        String str = "";
        for (int i = 0; i < candidates.size(); ++i)
            str += candidates.get(i).text + "\n";
        candidatesView.setText(str);
        if (initMode == INIT_MODE_ABSOLUTE)
            initModeButton.setText("absolute");
        else
            initModeButton.setText("relative");
    }

    final int MAX_CANDIDATE = 5;
    public ArrayList<Word> candidates = new ArrayList<Word>();
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

    public void stopVoice(){
        if (mediaPlayers.size() > 0)
            mediaPlayers.get(0).pause();
        mediaPlayers.clear();
    }

    public void stopInput(){
        seq.clear();
        stopVoice();
    }

    public void newPoint(int x, int y){
        if (seq.size() == 0){
            if (initMode == INIT_MODE_ABSOLUTE) {
                deltaX = 0;
                deltaY = 0;
                char ch = getKeyByPosition(x, y);
                if (ch == KEY_NOT_FOUND)
                    return;
                deltaY = (key_top[ch - 'a'] + key_bottom[ch - 'a']) / 2 - y;
                addToSeq(getKeyByPosition(x, y), true);
            }
            else {
                deltaX = 0;
                deltaY = 0;
                char ch = getKeyByPosition(x, y);
                if (ch == KEY_NOT_FOUND)
                    return;
                char best = addToSeq(ch, false);
                Log.i("best", best + "");
                deltaX = key_left[best - 'a'] - key_left[ch - 'a'];
                deltaY = (key_top[best - 'a'] + key_bottom[best - 'a']) / 2 - y;
                addToSeq(best, true);
            }
        }
        else{
            addToSeq(getKeyByPosition(x, y), true);
        }
    }

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
        for (int i = 0; i < 26; ++i)
            Log.i("keyboard", (char)('a' + i) + " " + keysNearby[i]);
        /*keyboard.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if (event.getPointerCount() >= 2)
                    return false;
                int x = (int)event.getX();
                int y = (int)event.getY();
                switch (event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        newPoint(x, y);
                        //action down
                        break;
                    case MotionEvent.ACTION_MOVE:
                        newPoint(x, y);
                        //action move
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        stopInput();
                        currentWord += nowCh;
                        currentWord2 += nowCh2;
                        predict(currentWord, currentWord2);
                        refresh();
                        break;
                }
                return true;
            }
        });*/
    }

    final int DICT_SIZE = 10000;
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
            Log.i("error", "read dictionary failed");
        }
    }

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
                currentWord = "";
                currentWord2 = "";
                readList.setText("");
                predict("", "");
                refresh();
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

    public void playMedia(MediaPlayer mp){
        mediaPlayers.add(mp);
        if (mediaPlayers.size() == 1)
            mediaPlayers.get(0).start();
    }

    public char addToSeq(char ch, boolean write){
        if (ch != KEY_NOT_FOUND){
            if (seq.size() == 0 || seq.get(seq.size() - 1) != ch) {
                if (seq.size() != 0){
                    Vibrator vibrator =  (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = {0, 30};
                    vibrator.vibrate(pattern, -1);
                }

                if (write) {
                    seq.add(ch);
                    Log.i("voice", ch + "");
                }
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
                if (!write)
                    return letters.get(0).text.charAt(0);
                stopVoice();
                String rlist = "";
                nowCh = '*';
                nowCh2 = '*';

                if (seq.size() == 1){
                    //prob top 2
                    if (letters.get(0).freq > 0) {
                        nowCh = letters.get(0).text.charAt(0);
                        playMedia(voices[nowCh - 'a']);
                        //for (int i = 0; i < emptyTimes; ++i)
                        //    playMedia(voiceEmpty);
                        rlist += nowCh;
                        if (letters.get(1).freq * 10 > letters.get(0).freq){
                            nowCh2 = letters.get(1).text.charAt(0);
                            playMedia(voices_second[nowCh2 - 'a']);
                            //for (int i = 0; i < emptyTimes; ++i)
                            //    playMedia(voiceEmpty);
                            rlist += nowCh2;
                        }
                    }
                    else {
                        //current key
                        nowCh = ch;
                        playMedia(voices[nowCh - 'a']);
                        //for (int i = 0; i < emptyTimes; ++i)
                        //    playMedia(voiceEmpty);
                        rlist += nowCh;
                    }
                }
                else{
                    //current key
                    nowCh = ch;
                    playMedia(voices[nowCh - 'a']);
                    //for (int i = 0; i < emptyTimes; ++i)
                    //    playMedia(voiceEmpty);
                    rlist += nowCh;
                }

                //todo ...

                /*
                if (letters.get(0).freq > 0 && letters.get(0).text.charAt(0) != ch){
                    nowCh = letters.get(0).text.charAt(0);
                    nowCh2 = ch;
                    playMedia(voices[nowCh - 'a']);
                    for (int i = 0; i < emptyTimes; ++i)
                        playMedia(voiceEmpty);
                    playMedia(voices[nowCh2 - 'a']);
                    for (int i = 0; i < emptyTimes; ++i)
                        playMedia(voiceEmpty);
                    rlist += nowCh;
                    rlist += nowCh2;
                }
                else if (letters.get(0).freq > 0){
                    nowCh = ch;
                    playMedia(voices[nowCh - 'a']);
                    for (int i = 0; i < emptyTimes; ++i)
                        playMedia(voiceEmpty);
                    rlist += nowCh;
                    if (letters.size() >= 2 && letters.get(1).freq > 0){
                        nowCh2 = letters.get(1).text.charAt(0);
                        playMedia(voices[nowCh2 - 'a']);
                        for (int i = 0; i < emptyTimes; ++i)
                            playMedia(voiceEmpty);
                        rlist += nowCh2;
                    }
                }
                else{
                    nowCh = ch;
                    playMedia(voices[nowCh - 'a']);
                    for (int i = 0; i < emptyTimes; ++i)
                        playMedia(voiceEmpty);
                    rlist += nowCh;
                }*/
                readList.setText(rlist);
            }
        }
        return 'a';
    }

    final char KEY_NOT_FOUND = 0;
    char getKeyByPosition(int x, int y){
        x += deltaX;
        y += deltaY;
        Log.i("test", "getKey x:" + x + " y:" + y);
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
        readList = (TextView)findViewById(R.id.readList);
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
        }
        predict(currentWord, currentWord2);
        refresh();
    }

    public void deleteAllChar(){
        currentWord = "";
        currentWord2 = "";
        predict(currentWord, currentWord2);
        refresh();
    }

    int downX, downY;
    long downTime;
    final long STAY_TIME = 200;
    final int SLIP_DIST = 90;

    public boolean onTouchEvent(MotionEvent event){
        if (event.getPointerCount() == 1 && event.getY() >= 1500) {
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
                    Log.i("gesture", downX + "," + downTime + " " + x + "," + System.currentTimeMillis());
                    if (x < downX - SLIP_DIST && System.currentTimeMillis() < downTime + STAY_TIME) {
                        Log.i("gesture", "left wipe");
                        deleteLastChar();
                    }
                    else if (x > downX + SLIP_DIST && System.currentTimeMillis() < downTime + STAY_TIME) {
                        Log.i("gesture", "right wipe");
                        deleteAllChar();
                    }
                    else {
                        currentWord += nowCh;
                        currentWord2 += nowCh2;
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
package com.example.fansy.audiokeyboard;

import android.media.MediaPlayer;
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
    ArrayList<Word> dict = new ArrayList();
    MediaPlayer voices[] = new MediaPlayer[26];
    final int emptyTimes = 4;
    MediaPlayer voiceEmpty = new MediaPlayer();
    ArrayList<MediaPlayer> mediaPlayers = new ArrayList<>();
    ArrayList<Character> seq = new ArrayList<Character>();
    String keys[] = new String[] {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    String keysNearby[] = new String[26];
    int key_left[] = new int[26];
    int key_right[] = new int[26];
    int key_top[] = new int[26];
    int key_bottom[] = new int[26];
    int deltaX, deltaY;
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
        text.setText(currentWord);
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
    public void predict(String currentWord){
        candidates.clear();
        for (int i = 0; i < dict.size(); ++i){
            if (candidates.size() >= MAX_CANDIDATE)
                break;
            Word candidate = dict.get(i);
            if (candidate.text.length() != currentWord.length())
                continue;

            boolean flag = true;
            for (int j = 0; j < currentWord.length(); ++j)
                if (keysNearby[currentWord.charAt(j) - 'a'].indexOf(candidate.text.charAt(j)) == -1){
                    flag = false;
                    break;
                }
            if (flag)
                candidates.add(candidate);
        }
        refresh();
    }

    public void stopInput(){
        seq.clear();
        if (mediaPlayers.size() > 0)
            mediaPlayers.get(0).pause();
        mediaPlayers.clear();
    }

    public void newPoint(int x, int y){
        if (seq.size() == 0){
            if (initMode == INIT_MODE_ABSOLUTE)
                addToSeq(getKeyByPosition(x, y), true);
            else {
                deltaX = 0;
                deltaY = 0;
                char ch = getKeyByPosition(x, y);
                if (ch == KEY_NOT_FOUND)
                    return;
                char best = addToSeq(ch, false);
                Log.i("best", best + "");
                deltaX = key_left[best - 'a'] - key_left[ch - 'a'];
                deltaY = key_top[best - 'a'] - key_top[ch - 'a'];
                addToSeq(best, true);
            }
        }
        else{
            addToSeq(getKeyByPosition(x, y), true);
        }
    }

    public void initKeyboard(){
        int delta[][] = new int [][]{{-1, -1, 0, 0, 0, 1, 1},{0, 1, -1, 0, 1, -1, 0}};
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < keys[i].length(); ++j){
                char ch = keys[i].charAt(j);
                keysNearby[ch - 'a'] = "";
                for (int k = 0; k < 7; ++k){
                    int _i = i + delta[0][k], _j = j + delta[1][k];
                    if (_i >= 0 && _i < 3 && _j >= 0 && _j < keys[_i].length())
                        keysNearby[ch - 'a'] += keys[_i].charAt(_j);
                }
            }
        for (int i = 0; i < 26; ++i)
            Log.i("keyboard", (char)('a' + i) + " " + keysNearby[i]);
        keyboard.setOnTouchListener(new View.OnTouchListener(){
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
                        if (getKeyByPosition(x, y) != KEY_NOT_FOUND) {
                            currentWord += getKeyByPosition(x, y);
                            predict(currentWord);
                            refresh();
                        }
                        break;
                }
                return true;
            }
        });
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
                dict.add(new Word(ss[0], Integer.valueOf(ss[1])));
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
                predict("");
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
                if (write) {
                    seq.add(ch);
                    Log.i("voice", ch + "");
                }
                ArrayList<Word> letters = new ArrayList<Word>();
                for (int i = 0; i < keysNearby[ch - 'a'].length(); ++i)
                    letters.add(new Word(keysNearby[ch - 'a'].charAt(i) + "", 0));
                String str = currentWord + ch;
                for (int i = 0; i < dict.size(); ++i)
                    if (dict.get(i).text.length() >= str.length()){
                        boolean flag = true;
                        Word word = dict.get(i);
                        for (int j = 0; j < str.length(); ++j)
                            if (keysNearby[str.charAt(j) - 'a'].indexOf(word.text.charAt(j)) == -1){
                                flag = false;
                                break;
                            }
                        if (flag){
                            for (int j = 0; j < letters.size(); ++j)
                                if (letters.get(j).text.charAt(0) == word.text.charAt(str.length() - 1))
                                    letters.get(j).freq += word.freq;
                        }
                    }
                Collections.sort(letters);
                if (!write)
                    return letters.get(0).text.charAt(0);
                String rlist = "";
                rlist += ch;
                playMedia(voices[ch - 'a']);
                for (int i = 0; i < emptyTimes; ++i)
                    playMedia(voiceEmpty);
                for (int i = 0; i < letters.size(); ++i)
                if (letters.get(i).text.charAt(0) != ch){
                    Log.i("letters", letters.get(i).text + " " + letters.get(i).freq);
                    rlist += letters.get(i).text.charAt(0);
                    playMedia(voices[letters.get(i).text.charAt(0) - 'a']);
                    for (int j = 0; j < emptyTimes; ++j)
                        playMedia(voiceEmpty);
                }
                readList.setText(rlist);
            }
        }
        return 'a';
    }

    final char KEY_NOT_FOUND = 0;
    char getKeyByPosition(int x, int y){
        if (initMode == INIT_MODE_RELATIVE){
            x += deltaX;
            y += deltaY;
        }
        for (int i = 0; i < 26; ++i)
            if (x >= key_left[i] && x <= key_right[i] && y >= key_top[i] && y <= key_bottom[i])
                return (char)('a' + i);
        return KEY_NOT_FOUND;
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
        initKeyboard();
        initDict();
        initVoice();
        initButtons();
        //left 0 right 1440 top 1554 bottom 2320
    }

    class Word implements Comparable<Word>{
        String text;
        int freq;
        Word(String text, int freq){
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

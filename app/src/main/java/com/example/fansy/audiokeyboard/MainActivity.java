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

public class MainActivity extends AppCompatActivity {
    ImageView keyboard;
    TextView text, candidatesView;
    Button confirmButton;
    String currentWord = "";
    ArrayList<Word> dict = new ArrayList();
    MediaPlayer voices[] = new MediaPlayer[26];
    ArrayList<MediaPlayer> mediaPlayers = new ArrayList<>();
    ArrayList<Character> seq = new ArrayList<Character>();
    String keys[] = new String[] {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    String keysNearby[] = new String[26];
    int key_left[] = new int[26];
    int key_right[] = new int[26];
    int key_top[] = new int[26];
    int key_bottom[] = new int[26];
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
                        addToSeq(getKeyByPosition(x, y));
                        //action down
                        break;

                    case MotionEvent.ACTION_MOVE:
                        addToSeq(getKeyByPosition(x, y));
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

    public void initVoice(){
        voices[ 0] = MediaPlayer.create(this, R.raw.voiceover_a);
        voices[ 1] = MediaPlayer.create(this, R.raw.voiceover_b);
        voices[ 2] = MediaPlayer.create(this, R.raw.voiceover_c);
        voices[ 3] = MediaPlayer.create(this, R.raw.voiceover_d);
        voices[ 4] = MediaPlayer.create(this, R.raw.voiceover_e);
        voices[ 5] = MediaPlayer.create(this, R.raw.voiceover_f);
        voices[ 6] = MediaPlayer.create(this, R.raw.voiceover_g);
        voices[ 7] = MediaPlayer.create(this, R.raw.voiceover_h);
        voices[ 8] = MediaPlayer.create(this, R.raw.voiceover_i);
        voices[ 9] = MediaPlayer.create(this, R.raw.voiceover_j);
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
        for (int i = 0; i < 26; ++i){
            voices[i].setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mediaPlayers.size() > 0)
                        mediaPlayers.remove(0);
                    if (mediaPlayers.size() > 0)
                        mediaPlayers.get(0).start();
                }
            });
        }
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
    }

    public void playMedia(int idx){
        mediaPlayers.add(voices[idx]);
        if (mediaPlayers.size() == 1)
            mediaPlayers.get(0).start();
    }

    public void addToSeq(char ch){
        if (ch != KEY_NOT_FOUND){
            if (seq.size() == 0 || seq.get(seq.size() - 1) != ch) {
                seq.add(ch);
                Log.i("voice", ch + "");
                for (int i = 0; i < keysNearby[ch - 'a'].length(); ++i)
                    playMedia(keysNearby[ch - 'a'].charAt(i) - 'a');
            }
        }
    }

    final char KEY_NOT_FOUND = 0;
    char getKeyByPosition(int x, int y){
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

        initKeyPosition();
        initKeyboard();
        initDict();
        initVoice();
        initButtons();
        //left 0 right 1440 top 1554 bottom 2320
    }

    class Word{
        String text;
        int freq;
        Word(String text, int freq){
            this.text = text;
            this.freq = freq;
        }
    }
}

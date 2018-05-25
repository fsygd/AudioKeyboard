package com.example.fansy.audiokeyboard;

import java.util.ArrayList;

public class TestCase {
    public String whole;
    public ArrayList<String> split;
    public TestCase(String line) {
        String[] splits = line.split("\\s");
        this.split = new ArrayList<>();
        this.whole = line;
        for (String str:splits) {
            this.split.add(str);
        }
    }
    public String getSplit(int no) {
        if (no < 0 || no >= this.split.size()) {
            return "";
        }
        return this.split.get(no);
    }
}

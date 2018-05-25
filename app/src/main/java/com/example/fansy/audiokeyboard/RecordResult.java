package com.example.fansy.audiokeyboard;

public class RecordResult {
    public int diffRelative = 0;
    public int diffNothing = 0;
    public int totalInput = 0;
    public RecordResult() {
        reset();
    }
    public void reset() {
        diffRelative = 0;
        diffNothing = 0;
        totalInput = 0;
    }
    private int tempdiffR = 0;
    private int tempdiffN = 0;
    private int tempLen = 0;
    public void setTempDiff(int diffr, int diffn, int len) {
        tempdiffN = diffn;
        tempdiffR = diffr;
        tempLen = len;
    }
    public void confirmTemp() {
        diffRelative += tempdiffR;
        diffNothing += tempdiffN;
        totalInput += tempLen;
    }
    public String getResult() {
        return diffRelative + "--" + diffNothing + "/" + totalInput;
    }
}

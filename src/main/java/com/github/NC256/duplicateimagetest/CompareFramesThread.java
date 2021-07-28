package com.github.NC256.duplicateimagetest;

import java.io.File;

public class CompareFramesThread extends Thread{

    File[] frames;
    Boolean[] duplicateResultStrings;
    int start, end;

    CompareFramesThread(File[] toCompare, Boolean[] results, int startInclusive, int endExclusive){
        this.frames = toCompare;
        this.duplicateResultStrings = results;
        this.start = startInclusive;
        this.end = endExclusive;
    }

    @Override
    public void run() {
        int count = 0;
        for (int i = start; i < end; i++) {
            //duplicateResultStrings[i] = Main.areTheseFramesEqual(frames[i], frames[i+1]);
        }
    }
}

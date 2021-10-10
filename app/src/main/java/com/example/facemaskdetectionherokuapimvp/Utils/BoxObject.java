package com.example.facemaskdetectionherokuapimvp.Utils;

import android.graphics.Rect;

public class BoxObject {
    private int classIndex;
    private Double score;
    private Rect rect;

    public BoxObject(int cls, Double output, Rect rect) {
        this.classIndex = cls;
        this.score = output;
        this.rect = rect;
    }


    public int getClassIndex() {
        return classIndex;
    }

    public Double getScore() {
        return score;
    }

    public Rect getRect() {
        return rect;
    }

    @Override
    public String toString() {
        return "Result{" +
                "classIndex=" + classIndex +
                ", score=" + score +
                ", rect=" + rect +
                '}';
    }
}

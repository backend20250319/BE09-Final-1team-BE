package com.newnormallist.newsservice.recommendation.model;

import lombok.Getter;

// 최종 점수 합성에 쓰는 (wDemo, wPref, wRead, wScrap) 가중치 묶음
// normalize wprhd (합 = 1 보장)
@Getter
public class Weights {
    private double wDemo, wPref, wRead, wScrap;
    public Weights(double wDemo, double wPref, double wRead, double wScrap) {
        this.wDemo = wDemo; this.wPref = wPref; this.wRead = wRead; this.wScrap = wScrap;
    }
    public void normalize() {
        double s = wDemo + wPref + wRead + wScrap;
        if (s > 0) { wDemo/=s; wPref/=s; wRead/=s; wScrap/=s; }
    }
}
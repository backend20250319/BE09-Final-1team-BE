package com.newnormallist.newsservice.recommendation.service;

import com.newnormallist.newsservice.recommendation.model.Weights;

/*
    케이스 1~4 규칙과 신뢰도 보정(fRead, fScrap)을 반영해
    (wDemo,wPref,wRead,wScrap)을 반환.

    예)
    Case1(데이터 적음): (0.70, 0.30, 0, 0)
    Case2(조회 충분): (0.40, 0.20, 0.40×fRead, 0) → normalize
    Case3(스크랩 위주): (0.35, 0.25, 0, 0.40×fScrap) → normalize
    Case4(둘 다 충분): (0.20, 0.10, 0.35×fRead, 0.35×fScrap)(demo 하한 0.15) → normalize
*/
public interface WeightSelector {
    Weights choose(int readCount, int scrapCount);
}

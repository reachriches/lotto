package com.lotto.backend.model.dto;

import com.lotto.backend.model.enums.RecommendStrategy;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LottoRecommendRequest {
    private int count = 1; // 추천 번호 세트 개수 (기본값 1)
    private RecommendStrategy strategy = RecommendStrategy.MIXED; // 추천 전략 (기본값 MIXED)
    private List<Integer> excludeNumbers; // 제외할 번호들
    private Integer minSum; // 번호 합계 최소값
    private Integer maxSum; // 번호 합계 최대값
    private Integer oddCount; // 홀수 개수 지정
    private Integer evenCount; // 짝수 개수 지정
}
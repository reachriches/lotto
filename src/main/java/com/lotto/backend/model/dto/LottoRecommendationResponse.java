package com.lotto.backend.model.dto;

import com.lotto.backend.filter.FilterResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LottoRecommendationResponse {
    private List<Integer> numbers;
    private double score;
    private List<FilterResult> filterResults;
    private String strategy;
    private int attempts;
}
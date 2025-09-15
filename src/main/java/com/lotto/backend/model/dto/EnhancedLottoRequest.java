package com.lotto.backend.model.dto;

import com.lotto.backend.model.enums.RecommendStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedLottoRequest {
    private RecommendStrategy strategy;
    private int count;
    private Set<String> filters;
    private double minScore;
    private Map<String, Object> userPreferences;
    private List<Integer> excludeNumbers;
    private List<Integer> includeNumbers;
    private Map<String, Double> filterWeights;
    
    public int getCount() {
        return count > 0 ? count : 1;
    }
    
    public double getMinScore() {
        return minScore > 0 ? minScore : 0.7;
    }
    
    public RecommendStrategy getStrategy() {
        return strategy != null ? strategy : RecommendStrategy.BALANCED;
    }
}
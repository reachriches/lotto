package com.lotto.backend.filter.impl;

import com.lotto.backend.filter.FilterResult;
import com.lotto.backend.filter.LottoContext;
import com.lotto.backend.filter.LottoFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SumDistributionFilter implements LottoFilter {
    
    @Value("${lotto.filter.sum.enabled:true}")
    private boolean enabled;
    
    @Value("${lotto.filter.sum.mean:138}")
    private double mean;
    
    @Value("${lotto.filter.sum.stddev:30}")
    private double stdDev;
    
    @Value("${lotto.filter.sum.priority:4}")
    private int priority;
    
    @Override
    public String getName() {
        return "sum-distribution";
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public FilterResult apply(List<Integer> numbers, LottoContext context) {
        int sum = numbers.stream().mapToInt(Integer::intValue).sum();
        double score = calculateNormalDistributionScore(sum);
        
        // 극단값 체크 추가 (60 미만, 260 초과)
        if (sum < 60 || sum > 260) {
            score = 0.0;
        }
        
        boolean passed = score >= 0.5;
        
        String range = getOptimalRange();
        String extremeWarning = (sum < 60 || sum > 260) ? " [극단값 제외]" : "";
        
        return FilterResult.builder()
                .passed(passed)
                .score(score)
                .reason(String.format("번호 합계: %d (최적 범위: %s, 점수: %.2f)%s", sum, range, score, extremeWarning))
                .build();
    }
    
    private double calculateNormalDistributionScore(int sum) {
        // 극단값은 apply 메서드에서 처리
        if (sum < 60 || sum > 260) {
            return 0.0;
        }
        
        double zScore = Math.abs(sum - mean) / stdDev;
        
        if (zScore <= 0.5) {
            return 1.0;
        } else if (zScore <= 1.0) {
            return 0.9;
        } else if (zScore <= 1.5) {
            return 0.7;
        } else if (zScore <= 2.0) {
            return 0.5;
        } else if (zScore <= 2.5) {
            return 0.3;
        } else {
            return 0.1;
        }
    }
    
    private String getOptimalRange() {
        int lower = (int)(mean - stdDev);
        int upper = (int)(mean + stdDev);
        return String.format("%d-%d", lower, upper);
    }
}
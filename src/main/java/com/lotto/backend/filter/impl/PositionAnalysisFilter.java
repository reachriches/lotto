package com.lotto.backend.filter.impl;

import com.lotto.backend.filter.FilterResult;
import com.lotto.backend.filter.LottoContext;
import com.lotto.backend.filter.LottoFilter;
import com.lotto.backend.model.entity.LottoResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PositionAnalysisFilter implements LottoFilter {
    
    @Value("${lotto.filter.position.enabled:true}")
    private boolean enabled;
    
    @Value("${lotto.filter.position.priority:5}")
    private int priority;
    
    private static final Map<Integer, int[]> OPTIMAL_RANGES = new HashMap<>() {{
        put(0, new int[]{1, 10});   // 1번째 위치: 1-10
        put(1, new int[]{5, 20});   // 2번째 위치: 5-20
        put(2, new int[]{10, 30});  // 3번째 위치: 10-30
        put(3, new int[]{20, 35});  // 4번째 위치: 20-35
        put(4, new int[]{25, 40});  // 5번째 위치: 25-40
        put(5, new int[]{35, 45});  // 6번째 위치: 35-45
    }};
    
    @Override
    public String getName() {
        return "position-analysis";
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
        List<Integer> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        
        Map<Integer, Map<Integer, Integer>> positionStats = analyzePositions(context.getHistoricalData());
        double score = calculatePositionScore(sorted, positionStats);
        
        boolean passed = score >= 0.6;
        
        return FilterResult.builder()
                .passed(passed)
                .score(score)
                .reason(String.format("위치별 분석 점수: %.2f", score))
                .build();
    }
    
    private Map<Integer, Map<Integer, Integer>> analyzePositions(List<LottoResult> historicalData) {
        Map<Integer, Map<Integer, Integer>> positionStats = new HashMap<>();
        
        if (historicalData == null || historicalData.isEmpty()) {
            return positionStats;
        }
        
        for (LottoResult result : historicalData) {
            List<Integer> sorted = new ArrayList<>(result.getWinningNumbers());
            Collections.sort(sorted);
            
            for (int i = 0; i < sorted.size(); i++) {
                positionStats.computeIfAbsent(i, k -> new HashMap<>());
                int number = sorted.get(i);
                positionStats.get(i).put(number, 
                    positionStats.get(i).getOrDefault(number, 0) + 1);
            }
        }
        
        return positionStats;
    }
    
    private double calculatePositionScore(List<Integer> sorted, Map<Integer, Map<Integer, Integer>> positionStats) {
        double totalScore = 0.0;
        
        for (int i = 0; i < sorted.size(); i++) {
            int number = sorted.get(i);
            int[] optimalRange = OPTIMAL_RANGES.get(i);
            
            double positionScore = 0.0;
            
            if (number >= optimalRange[0] && number <= optimalRange[1]) {
                positionScore = 1.0;
            } else {
                int distance = Math.min(
                    Math.abs(number - optimalRange[0]),
                    Math.abs(number - optimalRange[1])
                );
                positionScore = Math.max(0.3, 1.0 - (distance * 0.05));
            }
            
            if (!positionStats.isEmpty() && positionStats.containsKey(i)) {
                Map<Integer, Integer> positionFreq = positionStats.get(i);
                if (positionFreq.containsKey(number)) {
                    int frequency = positionFreq.get(number);
                    if (frequency > 0) {
                        positionScore += 0.1;
                    }
                }
            }
            
            totalScore += positionScore;
        }
        
        return Math.min(1.0, totalScore / sorted.size());
    }
}
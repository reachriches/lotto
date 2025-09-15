package com.lotto.backend.filter.impl;

import com.lotto.backend.filter.FilterResult;
import com.lotto.backend.filter.LottoContext;
import com.lotto.backend.filter.LottoFilter;
import com.lotto.backend.model.entity.LottoResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DeltaSystemFilter implements LottoFilter {
    
    @Value("${lotto.filter.delta.enabled:true}")
    private boolean enabled;
    
    @Value("${lotto.filter.delta.priority:2}")
    private int priority;
    
    @Override
    public String getName() {
        return "delta-system";
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
        List<Integer> deltas = calculateDeltas(numbers);
        Map<String, Integer> historicalPatterns = analyzeHistoricalDeltas(context.getHistoricalData());
        
        String deltaPattern = deltas.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        
        double score = calculateDeltaScore(deltas, historicalPatterns);
        boolean passed = score >= 0.6;
        
        return FilterResult.builder()
                .passed(passed)
                .score(score)
                .reason(String.format("델타 패턴: [%s] (점수: %.2f)", deltaPattern, score))
                .build();
    }
    
    private List<Integer> calculateDeltas(List<Integer> numbers) {
        List<Integer> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        List<Integer> deltas = new ArrayList<>();
        
        for (int i = 1; i < sorted.size(); i++) {
            deltas.add(sorted.get(i) - sorted.get(i - 1));
        }
        
        return deltas;
    }
    
    private Map<String, Integer> analyzeHistoricalDeltas(List<LottoResult> historicalData) {
        Map<String, Integer> patternCount = new HashMap<>();
        
        if (historicalData == null || historicalData.isEmpty()) {
            return patternCount;
        }
        
        for (LottoResult result : historicalData) {
            List<Integer> deltas = calculateDeltas(result.getWinningNumbers());
            
            for (int delta : deltas) {
                String key = String.valueOf(delta);
                patternCount.put(key, patternCount.getOrDefault(key, 0) + 1);
            }
        }
        
        return patternCount;
    }
    
    private double calculateDeltaScore(List<Integer> deltas, Map<String, Integer> historicalPatterns) {
        if (historicalPatterns.isEmpty()) {
            return 0.7;
        }
        
        double score = 0.0;
        int totalHistoricalDeltas = historicalPatterns.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        
        Map<Integer, Double> deltaDistribution = new HashMap<>();
        deltaDistribution.put(1, 0.05);  // 간격 1은 드물게
        deltaDistribution.put(2, 0.08);
        deltaDistribution.put(3, 0.10);
        deltaDistribution.put(4, 0.12);
        deltaDistribution.put(5, 0.13);
        deltaDistribution.put(6, 0.13);
        deltaDistribution.put(7, 0.12);
        deltaDistribution.put(8, 0.10);
        deltaDistribution.put(9, 0.08);
        deltaDistribution.put(10, 0.06);
        
        for (int delta : deltas) {
            if (delta >= 1 && delta <= 15) {
                double expectedFreq = deltaDistribution.getOrDefault(delta, 0.03);
                score += expectedFreq;
            }
        }
        
        score = score / deltas.size();
        
        boolean hasGoodVariety = new HashSet<>(deltas).size() >= 4;
        if (hasGoodVariety) {
            score += 0.2;
        }
        
        boolean hasExtremeDelta = deltas.stream().anyMatch(d -> d > 20 || d < 1);
        if (hasExtremeDelta) {
            score -= 0.1;
        }
        
        return Math.min(1.0, Math.max(0.0, score));
    }
}
package com.lotto.backend.filter.impl;

import com.lotto.backend.filter.FilterResult;
import com.lotto.backend.filter.LottoContext;
import com.lotto.backend.filter.LottoFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ACValueFilter implements LottoFilter {
    
    @Value("${lotto.filter.ac.enabled:true}")
    private boolean enabled;
    
    @Value("${lotto.filter.ac.min:7}")
    private int minAC;
    
    @Value("${lotto.filter.ac.max:10}")
    private int maxAC;
    
    @Value("${lotto.filter.ac.priority:1}")
    private int priority;
    
    @Override
    public String getName() {
        return "ac-value";
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
        int acValue = calculateAC(numbers);
        boolean passed = acValue >= minAC && acValue <= maxAC;
        
        double score;
        if (passed) {
            score = 1.0;
        } else if (acValue >= minAC - 1 && acValue <= maxAC + 1) {
            score = 0.7;
        } else {
            score = 0.3;
        }
        
        return FilterResult.builder()
                .passed(passed)
                .score(score)
                .reason(String.format("AC값: %d (권장: %d-%d)", acValue, minAC, maxAC))
                .build();
    }
    
    private int calculateAC(List<Integer> numbers) {
        Set<Integer> differences = new HashSet<>();
        List<Integer> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        
        for (int i = 0; i < sorted.size() - 1; i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                differences.add(sorted.get(j) - sorted.get(i));
            }
        }
        
        return differences.size() - (numbers.size() - 1);
    }
}
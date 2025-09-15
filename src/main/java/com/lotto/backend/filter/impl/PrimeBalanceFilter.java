package com.lotto.backend.filter.impl;

import com.lotto.backend.filter.FilterResult;
import com.lotto.backend.filter.LottoContext;
import com.lotto.backend.filter.LottoFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PrimeBalanceFilter implements LottoFilter {
    
    @Value("${lotto.filter.prime.enabled:true}")
    private boolean enabled;
    
    @Value("${lotto.filter.prime.min:2}")
    private int minPrimes;
    
    @Value("${lotto.filter.prime.max:3}")
    private int maxPrimes;
    
    @Value("${lotto.filter.prime.priority:3}")
    private int priority;
    
    @Override
    public String getName() {
        return "prime-balance";
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
        int primeCount = countPrimes(numbers);
        boolean passed = primeCount >= minPrimes && primeCount <= maxPrimes;
        
        double score;
        if (passed) {
            score = 1.0;
        } else if (primeCount == minPrimes - 1 || primeCount == maxPrimes + 1) {
            score = 0.7;
        } else {
            score = 0.4;
        }
        
        return FilterResult.builder()
                .passed(passed)
                .score(score)
                .reason(String.format("소수 개수: %d개 (권장: %d-%d개)", primeCount, minPrimes, maxPrimes))
                .build();
    }
    
    private int countPrimes(List<Integer> numbers) {
        int count = 0;
        for (int num : numbers) {
            if (isPrime(num)) {
                count++;
            }
        }
        return count;
    }
    
    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        
        return true;
    }
}
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
public class RecentHistoryFilter implements LottoFilter {
    
    @Value("${lotto.filter.history.enabled:true}")
    private boolean enabled;
    
    @Value("${lotto.filter.history.priority:7}")
    private int priority;
    
    @Value("${lotto.filter.history.recent-rounds:5}")
    private int recentRounds;
    
    @Override
    public String getName() {
        return "recent-history";
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
        if (context.getHistoricalData() == null || context.getHistoricalData().isEmpty()) {
            return FilterResult.builder()
                    .passed(true)
                    .score(1.0)
                    .reason("과거 데이터 없음")
                    .build();
        }
        
        double score = 1.0;
        List<String> issues = new ArrayList<>();
        
        List<LottoResult> recentResults = context.getHistoricalData().stream()
                .limit(recentRounds)
                .collect(Collectors.toList());
        
        // 1. 완전 일치 체크 (과거 1등 번호와 동일)
        if (hasExactMatch(numbers, context.getHistoricalData())) {
            issues.add("과거 1등 번호와 완전 일치");
            score -= 1.0; // 완전히 탈락
        }
        
        // 2. 최근 5회차와 4개 이상 중복
        int maxOverlap = getMaxOverlap(numbers, recentResults);
        if (maxOverlap >= 4) {
            issues.add(String.format("최근 %d회차와 %d개 중복", recentRounds, maxOverlap));
            score -= 0.3;
        } else if (maxOverlap >= 3) {
            issues.add(String.format("최근 %d회차와 %d개 중복", recentRounds, maxOverlap));
            score -= 0.1;
        }
        
        // 3. 보너스 번호 과다 포함
        int bonusOverlap = getBonusNumberOverlap(numbers, recentResults);
        if (bonusOverlap >= 2) {
            issues.add(String.format("최근 보너스 번호 %d개 포함", bonusOverlap));
            score -= 0.15;
        }
        
        // 4. 연속 회차 패턴 분석
        if (hasConsecutiveRoundPattern(numbers, recentResults)) {
            issues.add("연속 회차 패턴 발견");
            score -= 0.1;
        }
        
        // 5. 최근 인기 번호 과다 집중
        if (hasPopularNumberConcentration(numbers, recentResults)) {
            issues.add("최근 인기 번호 과다 집중");
            score -= 0.05;
        }
        
        score = Math.max(0.0, score);
        boolean passed = score >= 0.6;
        
        String reason = issues.isEmpty() 
            ? "최근 이력 검사 통과" 
            : "이력 이슈: " + String.join(", ", issues);
        
        return FilterResult.builder()
                .passed(passed)
                .score(score)
                .reason(reason)
                .build();
    }
    
    private boolean hasExactMatch(List<Integer> numbers, List<LottoResult> allHistory) {
        Set<Integer> numberSet = new HashSet<>(numbers);
        return allHistory.stream()
                .anyMatch(result -> new HashSet<>(result.getWinningNumbers()).equals(numberSet));
    }
    
    private int getMaxOverlap(List<Integer> numbers, List<LottoResult> recentResults) {
        return recentResults.stream()
                .mapToInt(result -> (int) numbers.stream()
                        .filter(result.getWinningNumbers()::contains)
                        .count())
                .max()
                .orElse(0);
    }
    
    private int getBonusNumberOverlap(List<Integer> numbers, List<LottoResult> recentResults) {
        Set<Integer> recentBonusNumbers = recentResults.stream()
                .map(LottoResult::getBonusNum)
                .collect(Collectors.toSet());
        
        return (int) numbers.stream()
                .filter(recentBonusNumbers::contains)
                .count();
    }
    
    private boolean hasConsecutiveRoundPattern(List<Integer> numbers, List<LottoResult> recentResults) {
        // 연속 2회차에서 각각 2개씩 동일한 번호가 나오는 패턴
        for (int i = 0; i < recentResults.size() - 1; i++) {
            List<Integer> round1 = recentResults.get(i).getWinningNumbers();
            List<Integer> round2 = recentResults.get(i + 1).getWinningNumbers();
            
            long overlap1 = numbers.stream().filter(round1::contains).count();
            long overlap2 = numbers.stream().filter(round2::contains).count();
            
            if (overlap1 >= 2 && overlap2 >= 2) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasPopularNumberConcentration(List<Integer> numbers, List<LottoResult> recentResults) {
        // 최근 5회차에서 가장 자주 나온 상위 10개 번호
        Map<Integer, Integer> frequency = new HashMap<>();
        
        for (LottoResult result : recentResults) {
            for (Integer num : result.getWinningNumbers()) {
                frequency.put(num, frequency.getOrDefault(num, 0) + 1);
            }
        }
        
        List<Integer> popularNumbers = frequency.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        long popularCount = numbers.stream()
                .filter(popularNumbers::contains)
                .count();
        
        return popularCount >= 5; // 6개 중 5개가 인기 번호
    }
}
package com.lotto.backend.filter;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LottoFilterChainManager {
    private final List<LottoFilter> filters;
    private final Map<String, LottoFilter> filterMap;
    
    public LottoFilterChainManager(List<LottoFilter> filters) {
        this.filters = filters.stream()
                .sorted(Comparator.comparing(LottoFilter::getPriority))
                .collect(Collectors.toList());
        
        this.filterMap = filters.stream()
                .collect(Collectors.toMap(LottoFilter::getName, Function.identity()));
    }
    
    public FilterChainResult applyFilters(List<Integer> numbers, 
                                         Set<String> activeFilters,
                                         LottoContext context) {
        List<FilterResult> results = new ArrayList<>();
        List<Integer> currentNumbers = new ArrayList<>(numbers);
        
        for (LottoFilter filter : filters) {
            if (shouldApplyFilter(filter, activeFilters)) {
                FilterResult result = filter.apply(currentNumbers, context);
                results.add(result);
                
                if (result.getAdjustedNumbers() != null && !result.getAdjustedNumbers().isEmpty()) {
                    currentNumbers = result.getAdjustedNumbers();
                }
            }
        }
        
        double totalScore = calculateTotalScore(results, context);
        return new FilterChainResult(currentNumbers, results, totalScore);
    }
    
    public FilterChainResult applyAllEnabledFilters(List<Integer> numbers, LottoContext context) {
        Set<String> allEnabledFilters = filters.stream()
                .filter(LottoFilter::isEnabled)
                .map(LottoFilter::getName)
                .collect(Collectors.toSet());
        
        return applyFilters(numbers, allEnabledFilters, context);
    }
    
    public Set<String> getDefaultFilters() {
        return Set.of("ac-value", "delta-system", "sum-distribution", "prime-balance");
    }
    
    public Set<String> getAllFilterNames() {
        return filterMap.keySet();
    }
    
    public Map<String, Boolean> getFilterStatus() {
        return filters.stream()
                .collect(Collectors.toMap(
                    LottoFilter::getName,
                    LottoFilter::isEnabled
                ));
    }
    
    private boolean shouldApplyFilter(LottoFilter filter, Set<String> activeFilters) {
        if (activeFilters == null || activeFilters.isEmpty()) {
            return filter.isEnabled();
        }
        return activeFilters.contains(filter.getName()) && filter.isEnabled();
    }
    
    private double calculateTotalScore(List<FilterResult> results, LottoContext context) {
        if (results.isEmpty()) {
            return 0.0;
        }
        
        // 1. 치명적 필터 체크 (즉시 0점 반환)
        for (FilterResult result : results) {
            if (isCriticalFailure(result)) {
                return 0.0;
            }
        }
        
        // 2. 나머지는 곱연산으로 계산
        double totalScore = 1.0;
        for (FilterResult result : results) {
            totalScore *= result.getScore();
        }
        
        return totalScore;
    }
    
    private String getFilterNameFromResult(FilterResult result) {
        if (result.getReason() != null) {
            if (result.getReason().contains("AC값")) return "ac-value";
            if (result.getReason().contains("델타")) return "delta-system";
            if (result.getReason().contains("소수")) return "prime-balance";
            if (result.getReason().contains("합계") || result.getReason().contains("번호 합계")) return "sum-distribution";
            if (result.getReason().contains("위치")) return "position-analysis";
            if (result.getReason().contains("패턴") || result.getReason().contains("수열") || result.getReason().contains("홀수") || result.getReason().contains("짝수")) return "pattern-exclusion";
            if (result.getReason().contains("이력") || result.getReason().contains("당첨") || result.getReason().contains("중복")) return "recent-history";
        }
        return "unknown";
    }
    
    private Map<String, Double> getDefaultWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("ac-value", 0.15);
        weights.put("delta-system", 0.10);
        weights.put("prime-balance", 0.05);
        weights.put("sum-distribution", 0.20);
        weights.put("position-analysis", 0.10);
        weights.put("unknown", 0.05);
        return weights;
    }

    public EvaluationResult evaluateFilters(List<FilterResult> results, double minScore) {
        // 1. 치명적 필터만 체크 (즉시 탈락)
        boolean hasCriticalFailure = results.stream()
                .anyMatch(r -> isCriticalFailure(r));

        if (hasCriticalFailure) {
            return EvaluationResult.CRITICAL_FAILURE;
        }

        // 2. 나머지는 전체 점수로 유연하게 결정
        double totalScore = calculateTotalScore(results, null);
        return totalScore >= minScore ? EvaluationResult.PASS : EvaluationResult.FAIL;
    }
    
    private boolean isCriticalFailure(FilterResult result) {
        String filterName = getFilterNameFromResult(result);
        
        // 치명적 패턴들 정의
        switch (filterName) {
            case "pattern-exclusion":
                // 극단적 패턴들 (모든 홀수/짝수, 연속번호 4개+, 등차수열 등)
                return result.getScore() <= 0.0;
                
            case "recent-history":
                // 과거 당첨번호와 완전 일치, 최근 번호와 과도한 중복
                return result.getScore() <= 0.0;
                
            case "sum-distribution":
                // 극단값 (60미만, 260초과)
                return result.getScore() <= 0.0;
                
            default:
                return false;
        }
    }
    
    public enum EvaluationResult {
        PASS,
        FAIL, 
        CRITICAL_FAILURE
    }

}
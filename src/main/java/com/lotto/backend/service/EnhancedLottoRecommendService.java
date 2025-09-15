package com.lotto.backend.service;

import com.lotto.backend.filter.FilterChainResult;
import com.lotto.backend.filter.LottoContext;
import com.lotto.backend.filter.LottoFilterChainManager;
import com.lotto.backend.filter.LottoFilterChainManager.EvaluationResult;
import com.lotto.backend.model.dto.EnhancedLottoRequest;
import com.lotto.backend.model.dto.LottoRecommendationResponse;
import com.lotto.backend.model.entity.LottoResult;
import com.lotto.backend.model.enums.RecommendStrategy;
import com.lotto.backend.repository.LottoResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedLottoRecommendService {
    
    private final LottoFilterChainManager filterChainManager;
    private final LottoResultRepository lottoResultRepository;
    private final LottoAnalysisService analysisService;
    private final Random random = new Random();
    
    private static final int MAX_ATTEMPTS = 100;
    
    public List<LottoRecommendationResponse> recommendMultiple(EnhancedLottoRequest request) {
        return IntStream.range(0, request.getCount())
                .mapToObj(i -> recommendEnhanced(request))
                .collect(Collectors.toList());
    }
    
    public LottoRecommendationResponse recommendEnhanced(EnhancedLottoRequest request) {
        return recommendWithRetry(request, 0);
    }
    
    private LottoRecommendationResponse recommendWithRetry(EnhancedLottoRequest request, int attempt) {
        if (attempt >= MAX_ATTEMPTS) {
            log.warn("Max attempts reached for recommendation generation");
            List<Integer> fallbackNumbers = generateByStrategy(request.getStrategy(), request);
            return LottoRecommendationResponse.builder()
                    .numbers(fallbackNumbers)
                    .score(0.0) // 점수가 높을수록 고품질 + 낮은 효율
                    .strategy(request.getStrategy().name())
                    .attempts(attempt)
                    .build();
        }
        
        // 1. 기본 번호 생성
        List<Integer> baseNumbers = generateByStrategy(request.getStrategy(), request);
        
        // 2. 컨텍스트 생성
        LottoContext context = createContext(request);
        
        // 3. 필터 체인 적용
        Set<String> activeFilters = request.getFilters() != null 
                ? request.getFilters() 
                : filterChainManager.getDefaultFilters();
        
        FilterChainResult filterResult = filterChainManager.applyFilters(
                baseNumbers, 
                activeFilters,
                context
        );
        
        // 4. 하이브리드 평가 (치명적/중요 필터 + 전체 점수)
        EvaluationResult evaluation = filterChainManager.evaluateFilters(
                filterResult.getDetails(), 
                request.getMinScore()
        );
        
        // 5. 평가 결과에 따른 재생성 결정
        switch (evaluation) {
            case CRITICAL_FAILURE:
                log.debug("Critical failure detected (완전일치/극단패턴), retrying... (attempt: {})", attempt);
                return recommendWithRetry(request, attempt + 1);
                
            case FAIL:
                log.debug("Score below threshold ({}), retrying... (attempt: {}, score: {})", 
                         request.getMinScore(), attempt, filterResult.getTotalScore());
                return recommendWithRetry(request, attempt + 1);
                
            case PASS:
                log.debug("Recommendation passed all filters (attempt: {}, score: {})", 
                         attempt, filterResult.getTotalScore());
                break;
        }
        
        // 6. 결과 반환
        return LottoRecommendationResponse.builder()
                .numbers(filterResult.getNumbers())
                .score(filterResult.getTotalScore())
                .filterResults(filterResult.getDetails())
                .strategy(request.getStrategy().name())
                .attempts(attempt + 1)
                .build();
    }
    
    private LottoContext createContext(EnhancedLottoRequest request) {
        List<LottoResult> historicalData = lottoResultRepository.findTop52ByOrderByRoundDesc();
        
        return LottoContext.builder()
                .historicalData(historicalData)
                .userPreferences(request.getUserPreferences())
                .filterWeights(request.getFilterWeights())
                .build();
    }
    
    private List<Integer> generateByStrategy(RecommendStrategy strategy, EnhancedLottoRequest request) {
        List<Integer> numbers = switch (strategy) {
            case RANDOM -> generatePureRandom(request);
            case STATISTICAL -> generateStatistical(request);
            case HOT_NUMBERS -> generateHotNumbers(request);
            case COLD_NUMBERS -> generateColdNumbers(request);
            case BALANCED -> generateBalanced(request);
            case MIXED -> generateMixed(request);
        };
        
        // 포함 번호 처리
        if (request.getIncludeNumbers() != null && !request.getIncludeNumbers().isEmpty()) {
            numbers = mergeWithIncludeNumbers(numbers, request.getIncludeNumbers());
        }
        
        Collections.sort(numbers);
        return numbers;
    }
    
    private List<Integer> generatePureRandom(EnhancedLottoRequest request) {
        Set<Integer> numbers = new HashSet<>();
        List<Integer> excludeList = request.getExcludeNumbers() != null 
                ? request.getExcludeNumbers() 
                : new ArrayList<>();
        
        while (numbers.size() < 6) {
            int num = random.nextInt(45) + 1;
            if (!excludeList.contains(num)) {
                numbers.add(num);
            }
        }
        
        return new ArrayList<>(numbers);
    }
    
    private List<Integer> generateStatistical(EnhancedLottoRequest request) {
        List<Integer> frequentNumbers = analysisService.totalWinsOverThePastYears(1, 20);
        List<Integer> excludeList = request.getExcludeNumbers() != null 
                ? request.getExcludeNumbers() 
                : new ArrayList<>();
        
        Set<Integer> result = new HashSet<>();
        
        // 상위 빈도 번호 3개 선택
        for (Integer num : frequentNumbers) {
            if (!excludeList.contains(num) && result.size() < 3) {
                result.add(num);
            }
        }
        
        // 나머지는 랜덤
        while (result.size() < 6) {
            int num = random.nextInt(45) + 1;
            if (!excludeList.contains(num)) {
                result.add(num);
            }
        }
        
        return new ArrayList<>(result);
    }
    
    private List<Integer> generateHotNumbers(EnhancedLottoRequest request) {
        List<LottoResult> recentResults = lottoResultRepository.findTopNByOrderByRoundDesc(10);
        Map<Integer, Integer> frequency = new HashMap<>();
        
        for (LottoResult result : recentResults) {
            for (Integer num : result.getWinningNumbers()) {
                frequency.put(num, frequency.getOrDefault(num, 0) + 1);
            }
        }
        
        List<Integer> hotNumbers = frequency.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(15)
                .collect(Collectors.toList());
        
        List<Integer> excludeList = request.getExcludeNumbers() != null 
                ? request.getExcludeNumbers() 
                : new ArrayList<>();
        
        Set<Integer> result = new HashSet<>();
        
        for (Integer num : hotNumbers) {
            if (!excludeList.contains(num) && result.size() < 6) {
                result.add(num);
            }
        }
        
        while (result.size() < 6) {
            int num = random.nextInt(45) + 1;
            if (!excludeList.contains(num)) {
                result.add(num);
            }
        }
        
        return new ArrayList<>(result);
    }
    
    private List<Integer> generateColdNumbers(EnhancedLottoRequest request) {
        List<Integer> coldNumbers = analysisService.calculateLowFrequency(15);
        List<Integer> excludeList = request.getExcludeNumbers() != null 
                ? request.getExcludeNumbers() 
                : new ArrayList<>();
        
        Set<Integer> result = new HashSet<>();
        
        for (Integer num : coldNumbers) {
            if (!excludeList.contains(num) && result.size() < 6) {
                result.add(num);
            }
        }
        
        while (result.size() < 6) {
            int num = random.nextInt(45) + 1;
            if (!excludeList.contains(num)) {
                result.add(num);
            }
        }
        
        return new ArrayList<>(result);
    }
    
    private List<Integer> generateBalanced(EnhancedLottoRequest request) {
        List<Integer> excludeList = request.getExcludeNumbers() != null 
                ? request.getExcludeNumbers() 
                : new ArrayList<>();
        Set<Integer> result = new HashSet<>();
        
        // 각 구간에서 균형있게 선택
        int[] ranges = {1, 10, 20, 30, 40, 46};
        
        for (int i = 0; i < ranges.length - 1 && result.size() < 6; i++) {
            int start = ranges[i];
            int end = ranges[i + 1];
            int count = 0;
            int maxFromRange = (i == ranges.length - 2) ? 1 : 1;
            
            int attempts = 0;
            while (count < maxFromRange && result.size() < 6 && attempts < 50) {
                int num = start + random.nextInt(end - start);
                if (!excludeList.contains(num) && !result.contains(num)) {
                    result.add(num);
                    count++;
                }
                attempts++;
            }
        }
        
        while (result.size() < 6) {
            int num = random.nextInt(45) + 1;
            if (!excludeList.contains(num)) {
                result.add(num);
            }
        }
        
        return new ArrayList<>(result);
    }
    
    private List<Integer> generateMixed(EnhancedLottoRequest request) {
        Set<Integer> result = new HashSet<>();
        List<Integer> excludeList = request.getExcludeNumbers() != null 
                ? request.getExcludeNumbers() 
                : new ArrayList<>();
        
        // 저빈도 번호 2개
        List<Integer> lowFreq = analysisService.calculateLowFrequency(10);
        for (Integer num : lowFreq) {
            if (!excludeList.contains(num) && result.size() < 2) {
                result.add(num);
            }
        }
        
        // 고빈도 번호 2개
        List<Integer> highFreq = analysisService.totalWinsOverThePastYears(1, 10);
        for (Integer num : highFreq) {
            if (!excludeList.contains(num) && !result.contains(num) && result.size() < 4) {
                result.add(num);
            }
        }
        
        // 나머지 랜덤
        while (result.size() < 6) {
            int num = random.nextInt(45) + 1;
            if (!excludeList.contains(num)) {
                result.add(num);
            }
        }
        
        return new ArrayList<>(result);
    }
    
    private List<Integer> mergeWithIncludeNumbers(List<Integer> numbers, List<Integer> includeNumbers) {
        Set<Integer> result = new HashSet<>(includeNumbers);
        
        for (Integer num : numbers) {
            if (result.size() >= 6) break;
            result.add(num);
        }
        
        while (result.size() < 6) {
            int num = random.nextInt(45) + 1;
            result.add(num);
        }
        
        return new ArrayList<>(result).subList(0, 6);
    }
}
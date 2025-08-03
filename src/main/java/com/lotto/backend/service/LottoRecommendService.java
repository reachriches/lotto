package com.lotto.backend.service;

import com.lotto.backend.model.dto.LottoRecommendRequest;
import com.lotto.backend.model.enums.RecommendStrategy;
import com.lotto.backend.repository.LottoRecommendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class LottoRecommendService {
    private final LottoAnalysisService analysisService;
    private final LottoResultService resultService;
    private final LottoRecommendRepository recommendRepository;

    private final Random random = new Random();

    public List<List<Integer>> generateRecommendList(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> generateSingleRecommend())
                .collect(Collectors.toList());
    }

    // 추천 번호
    public List<Integer> generateSingleRecommend() {
        // 출현 빈도갯수 선택을 위한 변수
        int lowFrequencyCount = 0;
        // 출현 빈도 낮은 숫자
        List<Integer> lowFrequencyNumbers = analysisService.calculateLowFrequency(lowFrequencyCount);

        return generateUntilValid(() -> {
                    List<Integer> randomNumbers = generateRandomNumbers(lowFrequencyNumbers); // 추가 랜덤 숫자 6-n개 생성
                    List<Integer> finalNumbers = analysisService.preventSequentialNumbers(randomNumbers); // 연속번호 3개 이상 방지
                    Collections.sort(finalNumbers);
                    return finalNumbers;
                    // 같은 자리 수 4개이상 방지 && 과거 1등 당첨 번호 확인
                }, result -> analysisService.isValidLottoCombination(result) && !analysisService.isLastWinningNumber(result)
        );
    }

    // 추가 랜덤 숫자 n개 생성
    private List<Integer> generateRandomNumbers(List<Integer> excludeNumbers) {
        Set<Integer> result = new HashSet<>(excludeNumbers);
        while (result.size() < 6) {
            int num = random.nextInt(45) + 1;
            result.add(num);
        }
        return new ArrayList<>(result);
    }

    private List<Integer> generateUntilValid(Supplier<List<Integer>> generator, Predicate<List<Integer>> validator) {
        int maxTries = 100;
        for (int i = 0; i < maxTries; i++) {
            List<Integer> candidate = generator.get();
            if (validator.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("유효한 추천 번호를 생성할 수 없습니다.");
    }
    
    // 향상된 추천 기능
    public List<List<Integer>> generateAdvancedRecommend(LottoRecommendRequest request) {
        return IntStream.range(0, request.getCount())
                .mapToObj(i -> generateSingleAdvancedRecommend(request))
                .collect(Collectors.toList());
    }
    
    private List<Integer> generateSingleAdvancedRecommend(LottoRecommendRequest request) {
        List<Integer> numbers = switch (request.getStrategy()) {
            case RANDOM -> generatePureRandom(request);
            case STATISTICAL -> generateStatistical(request);
            case HOT_NUMBERS -> generateHotNumbers(request);
            case COLD_NUMBERS -> generateColdNumbers(request);
            case BALANCED -> generateBalanced(request);
            case MIXED -> generateMixed(request);
        };
        
        return applyFilters(numbers, request);
    }
    
    private List<Integer> generatePureRandom(LottoRecommendRequest request) {
        Set<Integer> numbers = new HashSet<>();
        List<Integer> excludeList = request.getExcludeNumbers() != null ? request.getExcludeNumbers() : new ArrayList<>();
        
        while (numbers.size() < 6) {
            int num = random.nextInt(45) + 1;
            if (!excludeList.contains(num)) {
                numbers.add(num);
            }
        }
        
        return new ArrayList<>(numbers);
    }
    
    private List<Integer> generateStatistical(LottoRecommendRequest request) {
        // 최근 52주 통계 기반
        List<Integer> frequentNumbers = analysisService.totalWinsOverThePastYears(1, 20);
        List<Integer> excludeList = request.getExcludeNumbers() != null ? request.getExcludeNumbers() : new ArrayList<>();
        
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
    
    private List<Integer> generateHotNumbers(LottoRecommendRequest request) {
        // 자주 나오는 번호들
        List<Integer> hotNumbers = resultService.getHotNumbers(15);
        List<Integer> excludeList = request.getExcludeNumbers() != null ? request.getExcludeNumbers() : new ArrayList<>();
        
        Set<Integer> result = new HashSet<>();
        
        for (Integer num : hotNumbers) {
            if (!excludeList.contains(num) && result.size() < 6) {
                result.add(num);
            }
        }
        
        // 부족한 경우 랜덤 추가
        while (result.size() < 6) {
            int num = random.nextInt(45) + 1;
            if (!excludeList.contains(num)) {
                result.add(num);
            }
        }
        
        return new ArrayList<>(result);
    }
    
    private List<Integer> generateColdNumbers(LottoRecommendRequest request) {
        // 적게 나오는 번호들
        List<Integer> coldNumbers = analysisService.calculateLowFrequency(15);
        List<Integer> excludeList = request.getExcludeNumbers() != null ? request.getExcludeNumbers() : new ArrayList<>();
        
        Set<Integer> result = new HashSet<>();
        
        for (Integer num : coldNumbers) {
            if (!excludeList.contains(num) && result.size() < 6) {
                result.add(num);
            }
        }
        
        // 부족한 경우 랜덤 추가
        while (result.size() < 6) {
            int num = random.nextInt(45) + 1;
            if (!excludeList.contains(num)) {
                result.add(num);
            }
        }
        
        return new ArrayList<>(result);
    }
    
    private List<Integer> generateBalanced(LottoRecommendRequest request) {
        List<Integer> excludeList = request.getExcludeNumbers() != null ? request.getExcludeNumbers() : new ArrayList<>();
        Set<Integer> result = new HashSet<>();
        
        // 각 구간(1-9, 10-19, 20-29, 30-39, 40-45)에서 균형있게 선택
        int[] ranges = {1, 10, 20, 30, 40, 46};
        
        for (int i = 0; i < ranges.length - 1 && result.size() < 6; i++) {
            int start = ranges[i];
            int end = ranges[i + 1];
            int count = 0;
            int maxFromRange = (i == ranges.length - 2) ? 1 : 1; // 각 구간에서 최대 1-2개
            
            while (count < maxFromRange && result.size() < 6) {
                int num = start + random.nextInt(end - start);
                if (!excludeList.contains(num) && !result.contains(num)) {
                    result.add(num);
                    count++;
                }
            }
        }
        
        // 부족한 경우 랜덤 추가
        while (result.size() < 6) {
            int num = random.nextInt(45) + 1;
            if (!excludeList.contains(num)) {
                result.add(num);
            }
        }
        
        return new ArrayList<>(result);
    }
    
    private List<Integer> generateMixed(LottoRecommendRequest request) {
        // 기존 로직 사용
        return generateSingleRecommend();
    }
    
    private List<Integer> applyFilters(List<Integer> numbers, LottoRecommendRequest request) {
        // 합계 필터
        if (request.getMinSum() != null || request.getMaxSum() != null) {
            int sum = numbers.stream().mapToInt(Integer::intValue).sum();
            int minSum = request.getMinSum() != null ? request.getMinSum() : 0;
            int maxSum = request.getMaxSum() != null ? request.getMaxSum() : Integer.MAX_VALUE;
            
            if (sum < minSum || sum > maxSum) {
                // 다시 생성
                return generateSingleAdvancedRecommend(request);
            }
        }
        
        // 홀짝 필터
        if (request.getOddCount() != null || request.getEvenCount() != null) {
            long oddCount = numbers.stream().filter(n -> n % 2 == 1).count();
            long evenCount = 6 - oddCount;
            
            boolean oddValid = request.getOddCount() == null || oddCount == request.getOddCount();
            boolean evenValid = request.getEvenCount() == null || evenCount == request.getEvenCount();
            
            if (!oddValid || !evenValid) {
                // 다시 생성
                return generateSingleAdvancedRecommend(request);
            }
        }
        
        Collections.sort(numbers);
        return numbers;
    }
    
    // 휠링 시스템 생성
    public List<List<Integer>> generateWheelingSystem(List<Integer> baseNumbers, int systemSize) {
        if (baseNumbers.size() < 6 || baseNumbers.size() > 12) {
            throw new IllegalArgumentException("기본 번호는 6개에서 12개 사이여야 합니다.");
        }
        
        if (systemSize < 1 || systemSize > 10) {
            throw new IllegalArgumentException("시스템 크기는 1에서 10 사이여야 합니다.");
        }
        
        List<List<Integer>> combinations = new ArrayList<>();
        generateCombinations(baseNumbers, 6, 0, new ArrayList<>(), combinations);
        
        // 요청된 크기만큼 반환
        Collections.shuffle(combinations);
        return combinations.stream()
                .limit(systemSize)
                .collect(Collectors.toList());
    }
    
    private void generateCombinations(List<Integer> numbers, int k, int start, List<Integer> current, List<List<Integer>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < numbers.size(); i++) {
            current.add(numbers.get(i));
            generateCombinations(numbers, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}

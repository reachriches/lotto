package com.lotto.backend.service;

import com.lotto.backend.model.entity.LottoResult;
import com.lotto.backend.repository.LottoResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LottoAnalysisService {
    private final LottoResultRepository repository;
    private final Random random = new Random();

    // 출현빈도 계산
    public List<Integer> calculateLowFrequency(int limit) {
        List<LottoResult> results = repository.findTop52ByOrderByRoundDesc(); // 최근 1년 회차 조회
        Map<Integer, Integer> frequencyMap = new HashMap<>();
        for (LottoResult result : results) {
            for (int number : result.getWinningNumbers()) {
                frequencyMap.put(number, frequencyMap.getOrDefault(number, 0) + 1);
            }
        }

        // 출현 빈도가 낮은 번호 n개 선택
        return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()) // 출현 빈도가 낮은 순으로 정렬
                .limit(limit) // 상위 n개 선택
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<Integer> preventSequentialNumbers(List<Integer> randomNumbers) {
        List<Integer> numbers = new ArrayList<>(randomNumbers);
        Collections.sort(numbers);

        boolean hasSequence;
        do {
            hasSequence = false;
            Collections.sort(numbers);
            for (int i = 0; i < numbers.size() - 2; i++) {
                if (numbers.get(i) + 1 == numbers.get(i + 1) && numbers.get(i + 1) + 1 == numbers.get(i + 2)) {
                    hasSequence = true;
                    numbers.set(i + 2, generateNonSequentialNumber(numbers));
                    break; // 연속번호 바꾼 후 재확인
                }
            }
        } while (hasSequence);

        return numbers;
    }

    // 연속 번호를 피하는 랜덤 숫자 생성
    public int generateNonSequentialNumber(List<Integer> exclude) {
        int num;
        boolean threeSequence;

        do {
            num = random.nextInt(45) + 1;

            threeSequence = (exclude.contains(num - 2) && exclude.contains(num - 1))
                    || (exclude.contains(num - 1) && exclude.contains(num + 1))
                    || (exclude.contains(num + 1) && exclude.contains(num + 2));
        } while (exclude.contains(num) || threeSequence);

        return num;
    }

    // 같은 자리 수 4개이상 방지
    public boolean isValidLottoCombination(List<Integer> numbers) {
        Map<Integer, Integer> groupCount = new HashMap<>();

        for (int number : numbers) {
            int group = (number - 1) / 10 + 1; // 1~10 : 1, 11~20 : 2, ...
            groupCount.put(group, groupCount.getOrDefault(group, 0) + 1);

            if (groupCount.get(group) >= 4) {
                return false;
            }
        }

        return true;
    }

    // 과거 1등 당첨 번호와 비교하는 메서드
    public boolean isLastWinningNumber(List<Integer> numbers) {
        return repository.findAll().stream()
                .anyMatch(result -> new HashSet<>(result.getWinningNumbers()).equals(new HashSet<>(numbers)));
    }

    //
    public Map<Integer, Integer> getOccurNumberGroup() {
        List<LottoResult> results = repository.findAll(); // 기존 당첨 데이터 조회
        Map<Integer, Map<Integer, List<Integer>>> occurrences = new HashMap<>();
        Map<Integer, Integer> occurrenceCounts = new HashMap<>();

        for (LottoResult result : results) {
            List<Integer> winningNumbers = result.getWinningNumbers();
            Map<Integer, Integer> groupCount = new HashMap<>();

            for (int number : winningNumbers) {
                int group = (number - 1) / 10 + 1; // 1~10 : 1, 11~20 : 2, ...
                groupCount.put(group, groupCount.getOrDefault(group, 0) + 1);
            }

            // 그룹별 숫자 개수 파악
//            for (int count = 6; count >= 4; count--) { // 4개이상
//                int finalCount = count;
//                if (groupCount.values().stream().anyMatch(g -> g == finalCount)) {
//                    occurrences.computeIfAbsent(count, k -> new HashMap<>())
//                            .put(result.getRound(), winningNumbers);
//                }
//            }

            // 4개 이상 몰린 그룹이 있는지 확인
            for (int count = 6; count >= 4; count--) {
                int finalCount = count;
                if (groupCount.values().stream().anyMatch(g -> g == finalCount)) {
                    occurrenceCounts.put(count, occurrenceCounts.getOrDefault(count, 0) + 1);
                    break; // 한 회차에 여러 조건이 있어도 가장 높은 개수만 반영
                }
            }
        }

        return occurrenceCounts; // key: 같은 그룹 개수 (6~3), value: {회차 번호 → 당첨 번호 리스트}
    }

    // 연속번호 3개 이상 경우의 수 확인 - 3연속 - 64개, 4연속 - 6개
    public Map<Integer, List<Integer>> getSequentialNumberCheck() {
        List<LottoResult> results = repository.findAll(); // 전체 로또 당첨 결과
        Map<Integer, List<Integer>> occurrences = new HashMap<>();

        for (LottoResult result : results) {
            List<Integer> numbers = new ArrayList<>(result.getWinningNumbers());
            Collections.sort(numbers);

            int consecutiveCount = 1;
            int maxConsecutive = 1;

            for (int i = 1; i < numbers.size(); i++) {
                if (numbers.get(i) == numbers.get(i - 1) + 1) {
                    consecutiveCount++;
                    maxConsecutive = Math.max(maxConsecutive, consecutiveCount);
                } else {
                    consecutiveCount = 1;
                }
            }

            if (maxConsecutive >= 4) {
                occurrences.put(result.getRound(), numbers); // 3개 이상 연속번호가 포함된 회차
            }
        }

        return occurrences; // key: 회차, value: 당첨번호 리스트
    }

    public List<Integer> totalWinsOverThePastYears(int years, int limit) {
        List<LottoResult> results;

        if(years == 1){
            results = repository.findTop52ByOrderByRoundDesc(); // 최근 1년 회차 조회
        } else {
            results = repository.findTop104ByOrderByRoundDesc(); // 최근 1년 회차 조회
        }

        Map<Integer, Integer> frequencyMap = new HashMap<>();
        for (LottoResult result : results) {
            for (int number : result.getWinningNumbers()) {
                frequencyMap.put(number, frequencyMap.getOrDefault(number, 0) + 1);
            }
        }

        // 출현 빈도가 낮은 번호 n개 선택
        return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()) // 출현 빈도가 낮은 순으로 정렬
                .limit(limit) // 상위 n개 선택
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    // 끝자리 통계 분석 - 같은 끝자리가 몇 개씩 나왔는지 통계
    public Map<String, Object> analyzeLastDigitStatistics() {
        List<LottoResult> allResults = repository.findAll();
        Map<Integer, Integer> lastDigitCountDistribution = new HashMap<>(); // key: 같은 끝자리 개수, value: 발생 횟수
        List<Map<String, Object>> detailList = new ArrayList<>();
        
        for (LottoResult result : allResults) {
            Map<Integer, Integer> lastDigitCount = new HashMap<>();
            
            // 각 번호의 끝자리 카운트
            for (int number : result.getWinningNumbers()) {
                int lastDigit = number % 10;
                lastDigitCount.put(lastDigit, lastDigitCount.getOrDefault(lastDigit, 0) + 1);
            }
            
            // 가장 많이 나온 끝자리 개수 찾기
            int maxCount = lastDigitCount.values().stream()
                    .max(Integer::compare)
                    .orElse(0);
            
            // 통계에 추가
            lastDigitCountDistribution.put(maxCount, 
                lastDigitCountDistribution.getOrDefault(maxCount, 0) + 1);
            
            // 3개 이상인 경우 상세 정보 저장
            if (maxCount >= 3) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("round", result.getRound());
                detail.put("numbers", result.getWinningNumbers());
                detail.put("maxCount", maxCount);
                detail.put("lastDigitDetail", lastDigitCount);
                detailList.add(detail);
            }
        }
        
        // 결과 정리
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRounds", allResults.size());
        statistics.put("distribution", lastDigitCountDistribution);
        statistics.put("percentage", calculatePercentage(lastDigitCountDistribution, allResults.size()));
        statistics.put("samplesWithThreeOrMore", detailList.size());
        statistics.put("detailSamples", detailList.stream().limit(10).collect(Collectors.toList())); // 최근 10개만
        
        return statistics;
    }
    
    private Map<Integer, String> calculatePercentage(Map<Integer, Integer> distribution, int total) {
        Map<Integer, String> percentageMap = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : distribution.entrySet()) {
            double percentage = (entry.getValue() * 100.0) / total;
            percentageMap.put(entry.getKey(), String.format("%.2f%%", percentage));
        }
        return percentageMap;
    }
    
    // 피보나치 수열 패턴 통계 분석
    public Map<String, Object> analyzeFibonacciPattern() {
        List<LottoResult> allResults = repository.findAll();
        Set<Integer> fibonacciNumbers = Set.of(1, 2, 3, 5, 8, 13, 21, 34);
        Map<Integer, Integer> fibCountDistribution = new HashMap<>();
        List<Map<String, Object>> detailList = new ArrayList<>();
        
        for (LottoResult result : allResults) {
            long fibCount = result.getWinningNumbers().stream()
                    .filter(fibonacciNumbers::contains)
                    .count();
            
            fibCountDistribution.put((int)fibCount, 
                fibCountDistribution.getOrDefault((int)fibCount, 0) + 1);
            
            // 4개 이상인 경우 상세 정보
            if (fibCount >= 4) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("round", result.getRound());
                detail.put("numbers", result.getWinningNumbers());
                detail.put("fibCount", fibCount);
                detail.put("fibNumbers", result.getWinningNumbers().stream()
                    .filter(fibonacciNumbers::contains)
                    .collect(Collectors.toList()));
                detailList.add(detail);
            }
        }
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRounds", allResults.size());
        statistics.put("distribution", fibCountDistribution);
        statistics.put("fourOrMore", detailList.size());
        statistics.put("fiveOrMore", detailList.stream().filter(m -> (long)m.get("fibCount") >= 5).count());
        statistics.put("samples", detailList.stream().limit(10).collect(Collectors.toList()));
        
        return statistics;
    }
    
    // 완전제곱수 패턴 통계 분석
    public Map<String, Object> analyzePerfectSquarePattern() {
        List<LottoResult> allResults = repository.findAll();
        Set<Integer> perfectSquares = Set.of(1, 4, 9, 16, 25, 36);
        Map<Integer, Integer> squareCountDistribution = new HashMap<>();
        List<Map<String, Object>> detailList = new ArrayList<>();
        
        for (LottoResult result : allResults) {
            long squareCount = result.getWinningNumbers().stream()
                    .filter(perfectSquares::contains)
                    .count();
            
            squareCountDistribution.put((int)squareCount, 
                squareCountDistribution.getOrDefault((int)squareCount, 0) + 1);
            
            // 3개 이상인 경우 상세 정보
            if (squareCount >= 3) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("round", result.getRound());
                detail.put("numbers", result.getWinningNumbers());
                detail.put("squareCount", squareCount);
                detail.put("squareNumbers", result.getWinningNumbers().stream()
                    .filter(perfectSquares::contains)
                    .collect(Collectors.toList()));
                detailList.add(detail);
            }
        }
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRounds", allResults.size());
        statistics.put("distribution", squareCountDistribution);
        statistics.put("threeOrMore", detailList.size());
        statistics.put("fourOrMore", detailList.stream().filter(m -> (long)m.get("squareCount") >= 4).count());
        statistics.put("samples", detailList.stream().limit(10).collect(Collectors.toList()));
        
        return statistics;
    }
    
    // 배수 패턴 통계 분석
    public Map<String, Object> analyzeMultiplePatterns() {
        List<LottoResult> allResults = repository.findAll();
        Map<String, Object> allStats = new HashMap<>();
        
        int[] divisors = {4, 5, 6, 7}; // 3개 - 7의배수 19 6배수 52
        
        for (int divisor : divisors) {
            Map<String, Object> divisorStats = analyzeSpecificMultiple(allResults, divisor);
            allStats.put(divisor + "의배수", divisorStats);
        }
        
        allStats.put("totalRounds", allResults.size());
        return allStats;
    }
    
    private Map<String, Object> analyzeSpecificMultiple(List<LottoResult> results, int divisor) {
        List<Integer> multiplesInRange = new ArrayList<>();
        for (int i = divisor; i <= 45; i += divisor) {
            multiplesInRange.add(i);
        }
        
        int allMultiplesCount = 0;
        int fiveOrMoreCount = 0;
        int fourOrMoreCount = 0;
        int threeOrMoreCount = 0;
        List<Map<String, Object>> allSamples = new ArrayList<>();
        List<Map<String, Object>> fiveSamples = new ArrayList<>();
        List<Map<String, Object>> fourSamples = new ArrayList<>();
        List<Map<String, Object>> threeSamples = new ArrayList<>();

        for (LottoResult result : results) {
            long multipleCount = result.getWinningNumbers().stream()
                    .filter(multiplesInRange::contains)
                    .count();
            
            if (multipleCount == 6) { // 모두 해당 배수
                allMultiplesCount++;
                Map<String, Object> sample = new HashMap<>();
                sample.put("round", result.getRound());
                sample.put("numbers", result.getWinningNumbers());
                sample.put("count", multipleCount);
                allSamples.add(sample);
            }
            
            if (multipleCount >= 5) { // 5개 이상
                fiveOrMoreCount++;
                Map<String, Object> sample = new HashMap<>();
                sample.put("round", result.getRound());
                sample.put("numbers", result.getWinningNumbers());
                sample.put("count", multipleCount);
                sample.put("multipleNumbers", result.getWinningNumbers().stream()
                    .filter(multiplesInRange::contains)
                    .collect(Collectors.toList()));
                fiveSamples.add(sample);
            }
            
            if (multipleCount >= 4) { // 4개 이상
                fourOrMoreCount++;
                Map<String, Object> sample = new HashMap<>();
                sample.put("round", result.getRound());
                sample.put("numbers", result.getWinningNumbers());
                sample.put("count", multipleCount);
                sample.put("multipleNumbers", result.getWinningNumbers().stream()
                    .filter(multiplesInRange::contains)
                    .collect(Collectors.toList()));
                fourSamples.add(sample);
            }

            if (multipleCount >= 3) { // 3개 이상
                threeOrMoreCount++;
                Map<String, Object> sample = new HashMap<>();
                sample.put("round", result.getRound());
                sample.put("numbers", result.getWinningNumbers());
                sample.put("count", multipleCount);
                sample.put("multipleNumbers", result.getWinningNumbers().stream()
                        .filter(multiplesInRange::contains)
                        .collect(Collectors.toList()));
                threeSamples.add(sample);
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("availableMultiples", multiplesInRange);
        stats.put("availableCount", multiplesInRange.size());
        stats.put("allMultiplesWins", allMultiplesCount);
        stats.put("fiveOrMoreWins", fiveOrMoreCount);
        stats.put("fourOrMoreWins", fourOrMoreCount);
        stats.put("threeOrMoreWins", threeOrMoreCount);
        stats.put("allSamples", allSamples);
        stats.put("fiveOrMoreSamples", fiveSamples.stream().limit(10).collect(Collectors.toList()));
        stats.put("fourOrMoreSamples", fourSamples.stream().limit(10).collect(Collectors.toList()));
        stats.put("threeOrMoreSamples", threeSamples.stream().limit(10).collect(Collectors.toList()));

        return stats;
    }
    
    // 소수 패턴 통계 분석
    public Map<String, Object> analyzePrimePattern() {
        List<LottoResult> allResults = repository.findAll();
        Set<Integer> primes = Set.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43);
        
        int fourOrMoreCount = 0;
        int fiveOrMoreCount = 0;
        int sixCount = 0;
        List<Map<String, Object>> fourOrMoreSamples = new ArrayList<>();
        List<Map<String, Object>> fiveOrMoreSamples = new ArrayList<>();
        List<Map<String, Object>> sixSamples = new ArrayList<>();
        
        for (LottoResult result : allResults) {
            long primeCount = result.getWinningNumbers().stream()
                    .filter(primes::contains)
                    .count();
            
            if (primeCount == 6) { // 모두 소수
                sixCount++;
                Map<String, Object> sample = new HashMap<>();
                sample.put("round", result.getRound());
                sample.put("numbers", result.getWinningNumbers());
                sample.put("count", primeCount);
                sixSamples.add(sample);
            }
            
            if (primeCount >= 5) { // 5개 이상
                fiveOrMoreCount++;
                Map<String, Object> sample = new HashMap<>();
                sample.put("round", result.getRound());
                sample.put("numbers", result.getWinningNumbers());
                sample.put("count", primeCount);
                sample.put("primeNumbers", result.getWinningNumbers().stream()
                    .filter(primes::contains)
                    .collect(Collectors.toList()));
                fiveOrMoreSamples.add(sample);
            }
            
            if (primeCount >= 4) { // 4개 이상
                fourOrMoreCount++;
                Map<String, Object> sample = new HashMap<>();
                sample.put("round", result.getRound());
                sample.put("numbers", result.getWinningNumbers());
                sample.put("count", primeCount);
                sample.put("primeNumbers", result.getWinningNumbers().stream()
                    .filter(primes::contains)
                    .collect(Collectors.toList()));
                fourOrMoreSamples.add(sample);
            }
        }
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRounds", allResults.size());
        statistics.put("availablePrimes", primes.stream().sorted().collect(Collectors.toList()));
        statistics.put("availableCount", primes.size());
        statistics.put("sixPrimeWins", sixCount);
        statistics.put("fiveOrMoreWins", fiveOrMoreCount);
        statistics.put("fourOrMoreWins", fourOrMoreCount);
        statistics.put("sixSamples", sixSamples);
        statistics.put("fiveOrMoreSamples", fiveOrMoreSamples.stream().limit(10).collect(Collectors.toList()));
        statistics.put("fourOrMoreSamples", fourOrMoreSamples.stream().limit(10).collect(Collectors.toList()));
        
        return statistics;
    }

    //추천번호 분석용으로 만들었는데 언제 어떻게 쓸지 아직 정확히 모르겠음.
    //iterations: 추천번호를 몇 번 생성할지 결정 (ex: testGeneratedNumbers(1000, 3) → 1000번 생성해서 3~4개 이상 같은 그룹에 나온 경우 찾기)
//    public Map<Integer, List<Integer>> testGeneratedNumbers(int iterations, int minCount) {
//        Map<Integer, List<Integer>> occurrences = new HashMap<>();
//
//        for (int i = 1; i <= iterations; i++) {
//            List<Integer> numbers = lottoRecommendService.generateSingleRecommend();
//            Map<Integer, Integer> groupCount = new HashMap<>();
//
//            for (int number : numbers) {
//                int group = (number - 1) / 10 + 1;
//                groupCount.put(group, groupCount.getOrDefault(group, 0) + 1);
//            }
//
//            // 특정 그룹에서 `minCount` 이상 나온 경우 저장
//            if (groupCount.values().stream().anyMatch(count -> count >= minCount)) {
//                occurrences.put(i, numbers); // key: 테스트 번호(1~iterations), value: 생성된 로또 번호
//            }
//        }
//
//        return occurrences; // 같은 그룹에서 3개 이상 발생한 경우만 반환
//    }
}

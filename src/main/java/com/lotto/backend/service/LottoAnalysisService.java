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

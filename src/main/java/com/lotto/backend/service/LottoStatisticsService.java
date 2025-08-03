package com.lotto.backend.service;

import com.lotto.backend.model.dto.NumberFrequencyDto;
import com.lotto.backend.model.dto.TrendAnalysisDto;
import com.lotto.backend.model.entity.LottoResult;
import com.lotto.backend.repository.LottoResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LottoStatisticsService {
    private final LottoResultRepository repository;

    // 번호별 출현 빈도
    public List<NumberFrequencyDto> getNumberFrequency(int weeks) {
        List<LottoResult> results = getRecentResults(weeks);
        Map<Integer, Integer> frequencyMap = new HashMap<>();

        int totalNumbers = results.size() * 6;

        for (LottoResult result : results) {
            for (int number : result.getWinningNumbers()) {
                frequencyMap.put(number, frequencyMap.getOrDefault(number, 0) + 1);
            }
        }

        return frequencyMap.entrySet().stream()
                .map(entry -> new NumberFrequencyDto(
                        entry.getKey(),
                        entry.getValue(),
                        (entry.getValue() * 100.0) / totalNumbers
                ))
                .sorted((a, b) -> Integer.compare(a.getNumber(), b.getNumber()))
                .collect(Collectors.toList());
    }

    // 홀짝 비율 분석
    public Map<String, Object> getOddEvenRatio(int weeks) {
        List<LottoResult> results = getRecentResults(weeks);
        Map<String, Integer> ratioCount = new HashMap<>();

        for (LottoResult result : results) {
            long oddCount = result.getWinningNumbers().stream()
                    .filter(n -> n % 2 == 1)
                    .count();
            String ratio = oddCount + ":" + (6 - oddCount);
            ratioCount.put(ratio, ratioCount.getOrDefault(ratio, 0) + 1);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("distribution", ratioCount);
        response.put("totalRounds", results.size());

        return response;
    }

    // 번호 합계 분포
    public Map<String, Object> getSumDistribution(int weeks) {
        List<LottoResult> results = getRecentResults(weeks);
        Map<String, Integer> sumRanges = new TreeMap<>();

        for (LottoResult result : results) {
            int sum = result.getWinningNumbers().stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            String range = ((sum / 20) * 20) + "-" + ((sum / 20) * 20 + 19);
            sumRanges.put(range, sumRanges.getOrDefault(range, 0) + 1);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("distribution", sumRanges);
        response.put("average", calculateAverageSum(results));

        return response;
    }

    // 구간별 분포
    public Map<String, Integer> getRangeDistribution(int weeks) {
        List<LottoResult> results = getRecentResults(weeks);
        Map<String, Integer> rangeCount = new LinkedHashMap<>();
        rangeCount.put("1-9", 0);
        rangeCount.put("10-19", 0);
        rangeCount.put("20-29", 0);
        rangeCount.put("30-39", 0);
        rangeCount.put("40-45", 0);

        for (LottoResult result : results) {
            for (int number : result.getWinningNumbers()) {
                String range = getRange(number);
                rangeCount.put(range, rangeCount.get(range) + 1);
            }
        }

        return rangeCount;
    }

    // 연속번호 출현 통계
    public Map<String, Object> getConsecutiveStats(int weeks) {
        List<LottoResult> results = getRecentResults(weeks);
        Map<Integer, Integer> consecutiveCount = new HashMap<>();

        for (LottoResult result : results) {
            List<Integer> numbers = new ArrayList<>(result.getWinningNumbers());
            Collections.sort(numbers);

            int maxConsecutive = 1;
            int currentConsecutive = 1;

            for (int i = 1; i < numbers.size(); i++) {
                if (numbers.get(i) == numbers.get(i - 1) + 1) {
                    currentConsecutive++;
                    maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
                } else {
                    currentConsecutive = 1;
                }
            }

            consecutiveCount.put(maxConsecutive,
                consecutiveCount.getOrDefault(maxConsecutive, 0) + 1);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("consecutiveDistribution", consecutiveCount);
        response.put("totalRounds", results.size());

        return response;
    }

    // 트렌드 분석
    public TrendAnalysisDto getTrendAnalysis() {
        // 최근 4주와 전체 52주 비교
        List<LottoResult> recent4Weeks = repository.findTopNByOrderByRoundDesc(4);
        List<LottoResult> recent52Weeks = repository.findTop52ByOrderByRoundDesc();

        Map<Integer, Integer> recent4Freq = calculateFrequency(recent4Weeks);
        Map<Integer, Integer> recent52Freq = calculateFrequency(recent52Weeks);

        // 핫 넘버: 최근 4주에 2번 이상 출현
        List<Integer> hotNumbers = recent4Freq.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        // 콜드 넘버: 최근 8주에 한 번도 안 나온 번호
        List<LottoResult> recent8Weeks = repository.findTopNByOrderByRoundDesc(8);
        Set<Integer> recent8Numbers = new HashSet<>();
        for (LottoResult result : recent8Weeks) {
            recent8Numbers.addAll(result.getWinningNumbers());
        }

        List<Integer> coldNumbers = new ArrayList<>();
        for (int i = 1; i <= 45; i++) {
            if (!recent8Numbers.contains(i)) {
                coldNumbers.add(i);
            }
        }

        // 상승/하락 번호는 간단히 구현
        List<Integer> risingNumbers = new ArrayList<>();
        List<Integer> fallingNumbers = new ArrayList<>();

        return new TrendAnalysisDto(hotNumbers, coldNumbers, risingNumbers, fallingNumbers);
    }

    // 번호 쌍 출현 빈도
    public List<Map<String, Object>> getPairFrequency(int weeks, int limit) {
        List<LottoResult> results = getRecentResults(weeks);
        Map<String, Integer> pairCount = new HashMap<>();

        for (LottoResult result : results) {
            List<Integer> numbers = result.getWinningNumbers();

            for (int i = 0; i < numbers.size() - 1; i++) {
                for (int j = i + 1; j < numbers.size(); j++) {
                    String pair = Math.min(numbers.get(i), numbers.get(j)) + "," +
                                 Math.max(numbers.get(i), numbers.get(j));
                    pairCount.put(pair, pairCount.getOrDefault(pair, 0) + 1);
                }
            }
        }

        return pairCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("pair", entry.getKey());
                    map.put("frequency", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    // 가장 오래 안 나온 번호들
    public List<Map<String, Object>> getOverdueNumbers() {
        List<LottoResult> allResults = repository.findAllByOrderByRoundDesc();
        Map<Integer, Integer> lastSeenRound = new HashMap<>();

        // 각 번호가 마지막으로 나온 회차 찾기
        for (LottoResult result : allResults) {
            for (int number : result.getWinningNumbers()) {
                if (!lastSeenRound.containsKey(number)) {
                    lastSeenRound.put(number, result.getRound());
                }
            }
        }

        int latestRound = allResults.isEmpty() ? 0 : allResults.get(0).getRound();

        // 모든 번호에 대해 확인
        List<Map<String, Object>> overdueList = new ArrayList<>();
        for (int i = 1; i <= 45; i++) {
            Map<String, Object> info = new HashMap<>();
            info.put("number", i);
            info.put("lastRound", lastSeenRound.getOrDefault(i, 0));
            info.put("missedRounds", latestRound - lastSeenRound.getOrDefault(i, 0));
            overdueList.add(info);
        }

        // 놓친 회차가 많은 순으로 정렬
        overdueList.sort((a, b) ->
            Integer.compare((Integer) b.get("missedRounds"), (Integer) a.get("missedRounds")));

        return overdueList.stream().limit(10).collect(Collectors.toList());
    }

    // Helper methods
    private List<LottoResult> getRecentResults(int weeks) {
        if (weeks == 52) {
            return repository.findTop52ByOrderByRoundDesc();
        } else if (weeks == 104) {
            return repository.findTop104ByOrderByRoundDesc();
        } else {
            return repository.findTopNByOrderByRoundDesc(weeks);
        }
    }

    private String getRange(int number) {
        if (number <= 9) return "1-9";
        else if (number <= 19) return "10-19";
        else if (number <= 29) return "20-29";
        else if (number <= 39) return "30-39";
        else return "40-45";
    }

    private double calculateAverageSum(List<LottoResult> results) {
        return results.stream()
                .mapToInt(result -> result.getWinningNumbers().stream()
                        .mapToInt(Integer::intValue)
                        .sum())
                .average()
                .orElse(0.0);
    }

    private Map<Integer, Integer> calculateFrequency(List<LottoResult> results) {
        Map<Integer, Integer> frequency = new HashMap<>();
        for (LottoResult result : results) {
            for (int number : result.getWinningNumbers()) {
                frequency.put(number, frequency.getOrDefault(number, 0) + 1);
            }
        }
        return frequency;
    }
}

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
public class PatternExclusionFilter implements LottoFilter {
    
    @Value("${lotto.filter.pattern.enabled:true}")
    private boolean enabled;
    
    @Value("${lotto.filter.pattern.priority:6}")
    private int priority;
    
    @Override
    public String getName() {
        return "pattern-exclusion";
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
        List<Integer> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        
        List<String> violations = new ArrayList<>();
        double totalScore = 1.0;
        
        // 1. 대칭 패턴 체크
        if (hasSymmetricPattern(sorted)) {
            violations.add("대칭 패턴 발견");
            totalScore -= 0.3;
        }
        
        //TODO - 결과 확인 테스트 필요. 2. 등차수열 체크
        if (hasArithmeticSequence(sorted)) {
            violations.add("등차수열 패턴");
            totalScore -= 0.4;
        }
        
        //TODO - 결과 확인 테스트 필요. 3. 등비수열 체크
        if (hasGeometricSequence(sorted)) {
            violations.add("등비수열 패턴");
            totalScore -= 0.4;
        }
        
        // 4. 같은 끝자리 3개 이상 ( 총 90회 가량 출현으로 애매하지만 제외하는 방향으로 감. 싫으면 빼거나 4개이상으로 했을 시 낮은확률이라 제외가능)
        if (hasSameLastDigit(sorted)) {
            violations.add("같은 끝자리 3개 이상");
            totalScore -= 0.2;
        }
        
        //5. 모두 홀수 또는 모두 짝수 - 테스트결과 : 각 경우 극히 드뭄.
        if (hasAllOddOrEven(sorted)) {
            violations.add("모두 홀수 또는 짝수");
            totalScore -= 0.5;
        }
        
        // 6. 최근 3회차 당첨번호와 4개 이상 중복
        if (hasRecentWinningOverlap(sorted, context)) {
            violations.add("최근 당첨번호와 과도한 중복");
            totalScore -= 0.3;
        }
        
        // 7. 생일/기념일 패턴 - 테스트 결과 : 당첨번호 61%가 31이하의 번호임 -> 제외
//        if (hasBirthdayPattern(sorted)) {
//            violations.add("생일 패턴 (일반적)");
//            totalScore -= 0.1;
//        }
        
        // 8. 키보드 패턴 - 확인결과 제외할 경우의 수가 애매해 대량의 정상조합 제외(3), 그렇다고 4개를 할 경우 크게 의미가 없어지므로 제외.
//        if (hasKeyboardPattern(sorted)) {
//            violations.add("키보드 배열 패턴");
//            totalScore -= 0.2;
//        }
        
        // 9. 피보나치 수열 - 테스트결과 : 흔한번호지만 오히려 당첨번호로는 드뭄
        if (hasFibonacciPattern(sorted)) {
            violations.add("피보나치 수열");
            totalScore -= 0.3;
        }
        
        // 10. 완전제곱수 3개 이상 - 테스트결과 : 흔한번호지만 오히려 당첨번호로는 드뭄
        if (hasPerfectSquarePattern(sorted)) {
            violations.add("완전제곱수 3개 이상");
            totalScore -= 0.2;
        }
        
        // 11. 특정 배수만으로 구성 (각기 다른 기준)
        if (hasMultiplesOfFour(sorted)) {
            violations.add("4의 배수 5개 이상");
            totalScore -= 0.4;
        }
        
        if (hasMultiplesOfFive(sorted)) {
            violations.add("5의 배수 4개 이상");
            totalScore -= 0.4;
        }
        
        if (hasMultiplesOfSix(sorted)) {
            violations.add("6의 배수 3개 이상");
            totalScore -= 0.3;
        }
        
        if (hasMultiplesOfSeven(sorted)) {
            violations.add("7의 배수 3개 이상");
            totalScore -= 0.3;
        }
        
        // 12. 모두 소수
        if (hasAllPrimeNumbers(sorted)) {
            violations.add("모두 소수");
            totalScore -= 0.4;
        }
        
        totalScore = Math.max(0.0, totalScore);
        boolean passed = totalScore >= 0.6;
        
        String reason = violations.isEmpty() 
            ? "패턴 검사 통과" 
            : "패턴 위반: " + String.join(", ", violations);
        
        return FilterResult.builder()
                .passed(passed)
                .score(totalScore)
                .reason(reason)
                .build();
    }
    
    // 1. 대칭 패턴 체크 (등간격)
    private boolean hasSymmetricPattern(List<Integer> numbers) {
        if (numbers.size() < 4) return false;
        
        // [1, 11, 21, 31, 41] 같은 10씩 증가 패턴
        for (int step = 5; step <= 15; step++) {
            int count = 0;
            for (int i = 0; i < numbers.size() - 1; i++) {
                if (numbers.get(i + 1) - numbers.get(i) == step) {
                    count++;
                }
            }
            if (count >= 3) return true; // 3개 이상 등간격
        }
        
        // 대칭축 기준 패턴 (23을 중심으로 대칭)
        int center = 23;
        int symmetricCount = 0;
        for (int num : numbers) {
            int symmetric = 2 * center - num;
            if (symmetric >= 1 && symmetric <= 45 && numbers.contains(symmetric)) {
                symmetricCount++;
            }
        }
        
        return symmetricCount >= 3;
    }
    
    // 2. 등차수열 체크
    private boolean hasArithmeticSequence(List<Integer> numbers) {
        if (numbers.size() < 4) return false;
        
        for (int i = 0; i < numbers.size() - 3; i++) {
            int diff = numbers.get(i + 1) - numbers.get(i);
            if (diff > 0 && diff <= 10) {
                int count = 2;
                for (int j = i + 2; j < numbers.size(); j++) {
                    if (numbers.get(j) - numbers.get(j - 1) == diff) {
                        count++;
                    } else {
                        break;
                    }
                }
                if (count >= 4) return true;
            }
        }
        return false;
    }
    
    // 3. 등비수열 체크
    private boolean hasGeometricSequence(List<Integer> numbers) {
        if (numbers.size() < 4) return false;
        
        for (int i = 0; i < numbers.size() - 3; i++) {
            if (numbers.get(i) == 0) continue;
            double ratio = (double) numbers.get(i + 1) / numbers.get(i);
            if (ratio >= 1.5 && ratio <= 3.0) {
                int count = 2;
                for (int j = i + 2; j < numbers.size(); j++) {
                    if (numbers.get(j - 1) == 0) break;
                    double currentRatio = (double) numbers.get(j) / numbers.get(j - 1);
                    if (Math.abs(currentRatio - ratio) < 0.1) {
                        count++;
                    } else {
                        break;
                    }
                }
                if (count >= 4) return true;
            }
        }
        return false;
    }
    
    // 4. 같은 끝자리 3개 이상
    private boolean hasSameLastDigit(List<Integer> numbers) {
        Map<Integer, Integer> lastDigitCount = new HashMap<>();
        for (int num : numbers) {
            int lastDigit = num % 10;
            lastDigitCount.put(lastDigit, lastDigitCount.getOrDefault(lastDigit, 0) + 1);
        }
        return lastDigitCount.values().stream().anyMatch(count -> count >= 3);
    }
    
    // 5. 모두 홀수 또는 모두 짝수
    private boolean hasAllOddOrEven(List<Integer> numbers) {
        long oddCount = numbers.stream().filter(n -> n % 2 == 1).count();
        return oddCount == 0 || oddCount == numbers.size();
    }
    
    // 6. 최근 당첨번호와 과도한 중복
    private boolean hasRecentWinningOverlap(List<Integer> numbers, LottoContext context) {
        if (context.getHistoricalData() == null || context.getHistoricalData().isEmpty()) {
            return false;
        }
        
        List<LottoResult> recent3 = context.getHistoricalData().stream()
                .limit(3)
                .collect(Collectors.toList());
        
        for (LottoResult result : recent3) {
            long overlap = numbers.stream()
                    .filter(result.getWinningNumbers()::contains)
                    .count();
            if (overlap >= 4) return true;
        }
        return false;
    }
    
    // 7. 생일 패턴 (많은 사람이 선택하는 1-31) -> 생각보다 흔함.
//    private boolean hasBirthdayPattern(List<Integer> numbers) {
//        long birthdayCount = numbers.stream()
//                .filter(n -> n <= 31)
//                .count();
//        return birthdayCount >= 5; // 6개 중 5개가 31 이하
//    }
    
    // 8. 키보드 패턴 (숫자키패드 인접)
//    private boolean hasKeyboardPattern(List<Integer> numbers) {
//        // 간단한 키보드 패턴 (연속된 키)
//        String numberStr = numbers.stream()
//                .sorted()
//                .map(String::valueOf)
//                .collect(Collectors.joining());
//
//        // 123456, 789, 147, 258, 369 등
//        String[] keyboardPatterns = {"123", "456", "789", "147", "258", "369", "159", "357"};
//
//        for (String pattern : keyboardPatterns) {
//            if (numberStr.contains(pattern)) {
//                return true;
//            }
//        }
//        return false;
//    }
    
    // 9. 피보나치 수열
    private boolean hasFibonacciPattern(List<Integer> numbers) {
        Set<Integer> fibonacciNumbers = Set.of(1, 2, 3, 5, 8, 13, 21, 34);
        long fibCount = numbers.stream()
                .filter(fibonacciNumbers::contains)
                .count();
        return fibCount >= 4;
    }
    
    // 10. 완전제곱수 패턴
    private boolean hasPerfectSquarePattern(List<Integer> numbers) {
        Set<Integer> perfectSquares = Set.of(1, 4, 9, 16, 25, 36);
        long squareCount = numbers.stream()
                .filter(perfectSquares::contains)
                .count();
        return squareCount >= 3;
    }
    
    // 11-1. 4의 배수 5개 이상 체크
    private boolean hasMultiplesOfFour(List<Integer> numbers) {
        Set<Integer> fourMultiples = Set.of(4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44);
        long count = numbers.stream()
                .filter(fourMultiples::contains)
                .count();
        return count >= 4;
    }
    
    // 11-2. 5의 배수 4개 이상 체크
    private boolean hasMultiplesOfFive(List<Integer> numbers) {
        Set<Integer> fiveMultiples = Set.of(5, 10, 15, 20, 25, 30, 35, 40, 45);
        long count = numbers.stream()
                .filter(fiveMultiples::contains)
                .count();
        return count >= 4;
    }
    
    // 11-3. 6의 배수 3개 이상 체크
    private boolean hasMultiplesOfSix(List<Integer> numbers) {
        Set<Integer> sixMultiples = Set.of(6, 12, 18, 24, 30, 36, 42);
        long count = numbers.stream()
                .filter(sixMultiples::contains)
                .count();
        return count >= 3;
    }
    
    // 11-4. 7의 배수 3개 이상 체크
    private boolean hasMultiplesOfSeven(List<Integer> numbers) {
        Set<Integer> sevenMultiples = Set.of(7, 14, 21, 28, 35, 42);
        long count = numbers.stream()
                .filter(sevenMultiples::contains)
                .count();
        return count >= 3;
    }
    
    // 12. 모두 소수 체크
    private boolean hasAllPrimeNumbers(List<Integer> numbers) {
        Set<Integer> primes = Set.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43);
        return numbers.stream()
                .allMatch(primes::contains);
    }
}
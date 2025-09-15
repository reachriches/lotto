package com.lotto.backend.filter;

import com.lotto.backend.filter.impl.PatternExclusionFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PatternExclusionFilterTest {
    
    private PatternExclusionFilter filter;
    private LottoContext context;
    
    @BeforeEach
    void setUp() {
        filter = new PatternExclusionFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "priority", 6);
        
        context = LottoContext.builder().build();
    }
    
    @Test
    @DisplayName("대칭 패턴 감지 테스트")
    void testSymmetricPattern() {
        // given - 10씩 증가하는 대칭 패턴
        List<Integer> symmetricNumbers = Arrays.asList(1, 11, 21, 31, 41, 45);
        
        // when
        FilterResult result = filter.apply(symmetricNumbers, context);
        
        // then
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getReason()).contains("대칭 패턴");
        assertThat(result.getScore()).isLessThan(0.8);
    }
    
    @Test
    @DisplayName("등차수열 패턴 감지 테스트")
    void testArithmeticSequencePattern() {
        // given - 등차수열 (공차 5)
        List<Integer> arithmeticNumbers = Arrays.asList(5, 10, 15, 20, 25, 30);
        
        // when
        FilterResult result = filter.apply(arithmeticNumbers, context);
        
        // then
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getReason()).contains("등차수열");
        assertThat(result.getScore()).isLessThan(0.7);
    }
    
    @Test
    @DisplayName("같은 끝자리 패턴 감지 테스트")
    void testSameLastDigitPattern() {
        // given - 끝자리가 1인 번호 3개
        List<Integer> sameLastDigitNumbers = Arrays.asList(1, 11, 21, 32, 43, 45);
        
        // when
        FilterResult result = filter.apply(sameLastDigitNumbers, context);
        
        // then
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getReason()).contains("같은 끝자리");
        assertThat(result.getScore()).isLessThan(1.0);
    }
    
    @Test
    @DisplayName("모두 홀수 패턴 감지 테스트")
    void testAllOddPattern() {
        // given - 모두 홀수
        List<Integer> allOddNumbers = Arrays.asList(1, 3, 5, 7, 9, 11);
        
        // when
        FilterResult result = filter.apply(allOddNumbers, context);
        
        // then
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getReason()).contains("모두 홀수");
        assertThat(result.getScore()).isLessThan(0.6);
    }
    
    @Test
    @DisplayName("생일 패턴 감지 테스트")
    void testBirthdayPattern() {
        // given - 31 이하 숫자 5개 (생일 패턴)
        List<Integer> birthdayNumbers = Arrays.asList(7, 12, 25, 28, 31, 45);
        
        // when
        FilterResult result = filter.apply(birthdayNumbers, context);
        
        // then
        assertThat(result.isPassed()).isTrue(); // 점수 감점되지만 통과
        assertThat(result.getReason()).contains("생일 패턴");
        assertThat(result.getScore()).isLessThan(1.0);
    }
    
    @Test
    @DisplayName("피보나치 패턴 감지 테스트")
    void testFibonacciPattern() {
        // given - 피보나치 수열 4개 포함
        List<Integer> fibonacciNumbers = Arrays.asList(1, 3, 5, 8, 13, 21);
        
        // when
        FilterResult result = filter.apply(fibonacciNumbers, context);
        
        // then
        assertThat(result.isPassed()).isTrue(); // 점수 감점되지만 통과 가능
        assertThat(result.getReason()).contains("피보나치");
        assertThat(result.getScore()).isLessThan(1.0);
    }
    
    @Test
    @DisplayName("정상 패턴 통과 테스트")
    void testValidPattern() {
        // given - 일반적인 번호 조합
        List<Integer> validNumbers = Arrays.asList(7, 14, 23, 29, 35, 42);
        
        // when
        FilterResult result = filter.apply(validNumbers, context);
        
        // then
        assertThat(result.isPassed()).isTrue();
        assertThat(result.getReason()).contains("패턴 검사 통과");
        assertThat(result.getScore()).isEqualTo(1.0);
    }
    
    @Test
    @DisplayName("완전제곱수 패턴 감지 테스트")
    void testPerfectSquarePattern() {
        // given - 완전제곱수 3개 이상
        List<Integer> squareNumbers = Arrays.asList(1, 4, 9, 16, 32, 45);
        
        // when
        FilterResult result = filter.apply(squareNumbers, context);
        
        // then
        assertThat(result.isPassed()).isTrue(); // 점수 감점되지만 통과
        assertThat(result.getReason()).contains("완전제곱수");
        assertThat(result.getScore()).isLessThan(1.0);
    }
    
    @Test
    @DisplayName("복합 패턴 감지 테스트")
    void testMultiplePatterns() {
        // given - 대칭 패턴 + 모두 홀수
        List<Integer> multiplePatterns = Arrays.asList(1, 11, 21, 31, 41, 43);
        
        // when
        FilterResult result = filter.apply(multiplePatterns, context);
        
        // then
        assertThat(result.isPassed()).isFalse();
        assertThat(result.getScore()).isLessThan(0.3); // 여러 패턴으로 인한 큰 감점
        assertThat(result.getReason()).contains("대칭 패턴", "모두 홀수");
    }
}
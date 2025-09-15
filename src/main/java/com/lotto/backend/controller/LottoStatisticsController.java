package com.lotto.backend.controller;

import com.lotto.backend.model.dto.NumberFrequencyDto;
import com.lotto.backend.model.dto.TrendAnalysisDto;
import com.lotto.backend.service.LottoStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lotto/statistics")
@RequiredArgsConstructor
public class LottoStatisticsController {
    private final LottoStatisticsService statisticsService;
    
    // 번호별 출현 빈도
    @GetMapping("/frequency")
    public List<NumberFrequencyDto> getNumberFrequency(
            @RequestParam(defaultValue = "52") int weeks) {
        return statisticsService.getNumberFrequency(weeks);
    }
    
    // 홀짝 비율 분석
    @GetMapping("/odd-even-ratio")
    public Map<String, Object> getOddEvenRatio(
            @RequestParam(defaultValue = "52") int weeks) {
        return statisticsService.getOddEvenRatio(weeks);
    }
    
    // 번호 합계 분포
    @GetMapping("/sum-distribution")
    public Map<String, Object> getSumDistribution(
            @RequestParam(defaultValue = "52") int weeks) {
        return statisticsService.getSumDistribution(weeks);
    }
    
    // 구간별 분포 (1-9, 10-19, 20-29, 30-39, 40-45)
    @GetMapping("/range-distribution")
    public Map<String, Integer> getRangeDistribution(
            @RequestParam(defaultValue = "52") int weeks) {
        return statisticsService.getRangeDistribution(weeks);
    }
    
    // 연속번호 출현 통계
    @GetMapping("/consecutive-stats")
    public Map<String, Object> getConsecutiveStats(
            @RequestParam(defaultValue = "52") int weeks) {
        return statisticsService.getConsecutiveStats(weeks);
    }
    
    // 최근 트렌드 분석 (핫/콜드 넘버)
    @GetMapping("/trend-analysis")
    public TrendAnalysisDto getTrendAnalysis() {
        return statisticsService.getTrendAnalysis();
    }
    
    // 번호 쌍 출현 빈도 (자주 함께 나오는 번호들)
    @GetMapping("/pair-frequency")
    public List<Map<String, Object>> getPairFrequency(
            @RequestParam(defaultValue = "52") int weeks,
            @RequestParam(defaultValue = "10") int limit) {
        return statisticsService.getPairFrequency(weeks, limit);
    }
    
    // 가장 오래 안 나온 번호들
    @GetMapping("/overdue-numbers")
    public List<Map<String, Object>> getOverdueNumbers() {
        return statisticsService.getOverdueNumbers();
    }
}
package com.lotto.backend.controller;

import com.lotto.backend.model.dto.LottoRecommendRequest;
import com.lotto.backend.model.entity.LottoResult;
import com.lotto.backend.service.LottoAnalysisService;
import com.lotto.backend.service.LottoRecommendService;
import com.lotto.backend.service.LottoResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lotto")
@RequiredArgsConstructor
public class LottoController {
    private final LottoRecommendService lottoRecommendService;
    private final LottoResultService lottoResultService;
    private final LottoAnalysisService lottoAnalysisService;

    @GetMapping("/all/round")
    public void insertAllRound(){
        lottoResultService.insertAllRound();
    }

    @GetMapping("/search/result")
    public LottoResult getSearchResult(@RequestParam int round){
        return lottoResultService.getResult(round);
    }

    @GetMapping(value = "/check/numbers")
    public Map<Integer, String> checkNumbers(@RequestParam List<Integer> numbers){
        if (numbers.size() != 6) {
            throw new IllegalArgumentException("로또 번호는 정확히 6개여야 합니다.");
        }

        return lottoResultService.checkNumbersInAllRounds(numbers);
    }

    @GetMapping("/recommend")
    public List<List<Integer>> getRecommend(@RequestParam(defaultValue = "1") int count){
        if (count < 1 || count > 5) {
            throw new IllegalArgumentException("추천번호 요청 갯수는 1~5개까지 가능합니다.");
        }
        return lottoRecommendService.generateRecommendList(count);
    }

    // 연속번호, 같은 자리수 경우의 수 확인을 위한 것으로 실제 사용 api는 아님.
    // 같은 자리수(그룹) 나오는 경우의 수 확인
    @GetMapping(value = "/getOccurNumberGroup")
    public Map<Integer, Integer> getOccurNumberGroup(){
        return lottoAnalysisService.getOccurNumberGroup();
    }

    // 연속번호 3개 이상 나오는 경우의 수 확인
    @GetMapping(value = "/getSequentialNumberCheck")
    public Map<Integer, List<Integer>> getSequentialNumberCheck(){
        return lottoAnalysisService.getSequentialNumberCheck();
    }

    // 최근 x년간 낮은 출현빈도 수
    @GetMapping(value = "/totalWinsOverThePastYears")
    public List<Integer> totalWinsOverThePastYears(@RequestParam(defaultValue = "1") int years, @RequestParam int limit){
        return lottoAnalysisService.totalWinsOverThePastYears(years, limit);
    }
    
    // 향상된 추천 기능
    @PostMapping("/recommend/advanced")
    public List<List<Integer>> getAdvancedRecommend(@RequestBody LottoRecommendRequest request) {
        if (request.getCount() < 1 || request.getCount() > 10) {
            throw new IllegalArgumentException("추천번호 요청 갯수는 1~10개까지 가능합니다.");
        }
        return lottoRecommendService.generateAdvancedRecommend(request);
    }
    
    // 휠링 시스템 생성
    @PostMapping("/wheeling")
    public List<List<Integer>> generateWheeling(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Integer> baseNumbers = (List<Integer>) request.get("baseNumbers");
        Integer systemSize = (Integer) request.get("systemSize");
        
        if (baseNumbers == null || systemSize == null) {
            throw new IllegalArgumentException("기본 번호와 시스템 크기는 필수입니다.");
        }
        
        return lottoRecommendService.generateWheelingSystem(baseNumbers, systemSize);
    }
}

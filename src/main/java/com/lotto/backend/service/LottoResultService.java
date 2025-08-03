package com.lotto.backend.service;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.lotto.backend.model.entity.LottoResult;
import com.lotto.backend.repository.LottoResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LottoResultService {
    private final LottoResultRepository repository;

    public LottoResult getResult(int round) {
        return repository.findById(round).orElse(null);
    }

    public void insertAllRound() {
        AWSLambda lambdaClient = AWSLambdaClientBuilder.defaultClient();

        // Lambda 요청 생성
        InvokeRequest request = new InvokeRequest()
                .withFunctionName("lottoAllRound")
                .withPayload("{\"number\": 42}"); // JSON 형식의 입력값

        // Lambda 실행
        InvokeResult result = lambdaClient.invoke(request); // 동기 - 결과를 받아서 기다리고 후처리 or 결과 처리를 위함.

        // 결과 출력
        String responseJson = new String(result.getPayload().array());
        System.out.println("Lambda Response: " + responseJson);
    }

    public Map<Integer, String> checkNumbersInAllRounds(List<Integer> userNumbers) {
        List<LottoResult> allResults = repository.findAll(); // 모든 회차 가져오기
        Map<Integer, String> results = new HashMap<>();

        for (LottoResult result : allResults) {
            String rank = result.checkWinningRank(userNumbers);
            if (!"낙첨".equals(rank)) {
                results.put(result.getRound(), rank);
            }
        }

        return results;
    }

    // 자주 나오는 번호 조회
    public List<Integer> getHotNumbers(int limit) {
        List<LottoResult> recentResults = repository.findTop52ByOrderByRoundDesc(); // 최근 1년
        Map<Integer, Integer> frequencyMap = new HashMap<>();
        
        for (LottoResult result : recentResults) {
            for (int number : result.getWinningNumbers()) {
                frequencyMap.put(number, frequencyMap.getOrDefault(number, 0) + 1);
            }
        }
        
        // 빈도가 높은 순으로 정렬
        return frequencyMap.entrySet().stream()
            .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
}

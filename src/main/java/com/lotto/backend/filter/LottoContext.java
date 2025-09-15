package com.lotto.backend.filter;

import com.lotto.backend.model.entity.LottoResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LottoContext {
    private List<LottoResult> historicalData;
    private Map<String, Object> userPreferences;
    private Map<String, Double> filterWeights;
    private int targetRound;
}
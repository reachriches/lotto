package com.lotto.backend.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterResult {
    private boolean passed;
    private double score;  // 0.0 ~ 1.0
    private String reason;
    private List<Integer> adjustedNumbers;
}
package com.lotto.backend.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterChainResult {
    private List<Integer> numbers;
    private List<FilterResult> details;
    private double totalScore;
}
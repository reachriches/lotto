package com.lotto.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalysisDto {
    private List<Integer> hotNumbers;  // 최근 자주 나오는 번호
    private List<Integer> coldNumbers; // 최근 적게 나오는 번호
    private List<Integer> risingNumbers; // 출현 빈도가 증가하는 번호
    private List<Integer> fallingNumbers; // 출현 빈도가 감소하는 번호
}
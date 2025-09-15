package com.lotto.backend.filter;

import java.util.List;

public interface LottoFilter {
    String getName();
    int getPriority();
    boolean isEnabled();
    FilterResult apply(List<Integer> numbers, LottoContext context);
}
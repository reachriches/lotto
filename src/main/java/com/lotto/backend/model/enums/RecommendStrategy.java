package com.lotto.backend.model.enums;

public enum RecommendStrategy {
    RANDOM("완전 무작위"),
    STATISTICAL("통계 기반"),
    MIXED("혼합 전략"),
    HOT_NUMBERS("자주 나오는 번호"),
    COLD_NUMBERS("적게 나오는 번호"),
    BALANCED("균형잡힌 번호");
    
    private final String description;
    
    RecommendStrategy(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
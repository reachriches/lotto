package com.lotto.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Entity
@Getter
@Table(name = "tb_lotto_result")
public class LottoResult {
    @Id
    private int round;

    @Column(name = "draw_date")
    private LocalDate drawDate;

    @Column(name = "num1")
    private int num1;

    @Column(name = "num2")
    private int num2;

    @Column(name = "num3")
    private int num3;

    @Column(name = "num4")
    private int num4;

    @Column(name = "num5")
    private int num5;

    @Column(name = "num6")
    private int num6;

    @Column(name = "bonus_num")
    private int bonusNum;

    public List<Integer> getWinningNumbers() {
        return Arrays.asList(num1, num2, num3, num4, num5, num6);
    }

    public String checkWinningRank(List<Integer> userNumbers) {
        List<Integer> winningNumbers = getWinningNumbers();
        long matchCount = userNumbers.stream().filter(winningNumbers::contains).count();

        boolean bonusMatch = userNumbers.contains(bonusNum);

        return switch ((int) matchCount) {
            case 6 -> "1등을 축하드립니다";
            case 5 -> bonusMatch ? "2등" : "3등";
            case 4 -> "4등";
            case 3 -> "5등";
            default -> "낙첨";
        };
    }
}

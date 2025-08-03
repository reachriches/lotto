package com.lotto.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "tb_lotto_recommendation")
public class LottoRecommend {
    @Id
    private Long id;

    @Column(name = "round")
    private int round;

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "method")
    private String method;

    @Column(name = "group_id")
    private Long groupId;
}

package com.lotto.backend.repository;

import com.lotto.backend.model.entity.LottoRecommend;
import com.lotto.backend.model.entity.LottoResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LottoRecommendRepository extends JpaRepository<LottoRecommend, Integer> {

}

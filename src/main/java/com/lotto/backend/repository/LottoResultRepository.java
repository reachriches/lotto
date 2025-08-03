package com.lotto.backend.repository;

import com.lotto.backend.model.entity.LottoResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LottoResultRepository extends JpaRepository<LottoResult, Integer> {
    List<LottoResult> findTop52ByOrderByRoundDesc(); // 최근 1년 회차 조회
    List<LottoResult> findTop104ByOrderByRoundDesc(); // 최근 2년 회차 조회
    
    // 통계 서비스를 위한 추가 메서드
    @Query(value = "SELECT * FROM tb_lotto_result ORDER BY round DESC LIMIT :limit", nativeQuery = true)
    List<LottoResult> findTopNByOrderByRoundDesc(@Param("limit") int limit); // 최근 N개 회차 조회
    
    List<LottoResult> findAllByOrderByRoundDesc(); // 모든 결과를 최신순으로 조회
}

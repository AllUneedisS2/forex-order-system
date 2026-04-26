package com.switchwon.forexordersystem.exchangerate.repository;

import com.switchwon.forexordersystem.common.enums.Currency;
import com.switchwon.forexordersystem.exchangerate.domain.ExchangeRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 환율 수신 이력 Repository
 */
@Repository
public interface ExchangeRateHistoryRepository extends JpaRepository<ExchangeRateHistory, Long> {

    // 통화별 가장 최근 수집된 환율 1건 조회
    Optional<ExchangeRateHistory> findFirstByCurrencyOrderByCollectedAtDesc(Currency currency);
}
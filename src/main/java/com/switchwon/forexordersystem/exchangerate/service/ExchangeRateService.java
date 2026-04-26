package com.switchwon.forexordersystem.exchangerate.service;

import com.switchwon.forexordersystem.common.enums.Currency;
import com.switchwon.forexordersystem.common.exception.BusinessException;
import com.switchwon.forexordersystem.common.response.ResponseCode;
import com.switchwon.forexordersystem.exchangerate.domain.ExchangeRateHistory;
import com.switchwon.forexordersystem.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.forexordersystem.exchangerate.repository.ExchangeRateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.switchwon.forexordersystem.common.util.ForexCalculator.applyDisplayUnit;
import static com.switchwon.forexordersystem.common.util.ForexCalculator.scale;
import static com.switchwon.forexordersystem.common.util.ForexCalculator.truncatedNow;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 환율 도메인 서비스
 * 외부 API 호출은 {@link ExternalExchangeRateClient}에 위임
 * 비즈니스 기능: 스프레드, JPY 100엔 환산, 정밀도, 저장/조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExternalExchangeRateClient externalClient;

    private final ExchangeRateHistoryRepository repository;

    // 매입 스프레드
    @Value("${app.forex.buy-spread:1.05}")
    private BigDecimal buySpread;
    
    // 매도 스프레드
    @Value("${app.forex.sell-spread:0.95}")
    private BigDecimal sellSpread;

    // ------------------------------------------------------------------
    // 1. 수집/저장 - Batch로 호출
    // ------------------------------------------------------------------

    // 환율 API에서 최신 KRW 환율 기준으로 이력 저장
    @Transactional
    public void collectAndSave() {
        // 단순 이력 저장이니 block()
        Map<Currency, BigDecimal> rates = externalClient.fetchKrwRates().block();
        if (rates == null || rates.isEmpty()) {
            log.warn("환율 수집 결과가 비어있어 저장을 건너뜁니다.");
            return;
        }

        LocalDateTime now = truncatedNow();
        List<ExchangeRateHistory> entities = new ArrayList<>();
        rates.forEach(
            (currency, baseRate) -> entities.add(toEntity(currency, baseRate, now))
        );

        repository.saveAll(entities);
        log.info("환율 수집 완료 - {}건 저장 ({})", entities.size(), now);
    }

    // 매앱/매매 기준율로부터 전신환 매입/매도 환율을 계산하여 엔티티
    private ExchangeRateHistory toEntity(Currency currency, BigDecimal tradeStanRate, LocalDateTime collectedAt) {
        BigDecimal buy = scale(tradeStanRate.multiply(buySpread)); // ×1.05
        BigDecimal sell = scale(tradeStanRate.multiply(sellSpread)); // ×0.95

        return ExchangeRateHistory.builder()
                                  .currency(currency)
                                  .tradeStanRate(tradeStanRate)
                                  .buyRate(buy)
                                  .sellRate(sell)
                                  .collectedAt(collectedAt)
                                  .build();
    }

    // ------------------------------------------------------------------
    // 2. 조회 - Controller 호출
    // ------------------------------------------------------------------

    // 각 통화별 최신 환율 조회
    @Transactional(readOnly = true)
    public ExchangeRateResponse getLatest(Currency currency) {
        ExchangeRateHistory entity = findLatestEntity(currency);
        return toResponseWithDisplayUnit(entity);
    }

    // 전체 통화 최신 환율 조회
    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> getLatestAll() {
        List<ExchangeRateResponse> result = new ArrayList<>();
        for (Currency currency : Currency.values()) {
            // KRW 스킵 (외화만)
            if (!currency.isForeign()) continue;
            // 일부 통화 데이터가 아직 없을 수 있으므로 일단 NOT FOUND 처리
            repository.findFirstByCurrencyOrderByCollectedAtDesc(currency)
                      .ifPresent(e -> result.add(toResponseWithDisplayUnit(e)));
        }
        return result;
    }

    // OrderService에 노출하는 환율 스냅샷 - 엔티티 대신 필요한 값만 전달
    public record RateSnapshot(BigDecimal buyRate, BigDecimal sellRate) {}

    // Order 도메인에서 호출 - 매입/매도율만 반환
    @Transactional(readOnly = true)
    public RateSnapshot getLatestRates(Currency currency) {
        ExchangeRateHistory entity = findLatestEntity(currency);
        return new RateSnapshot(entity.getBuyRate(), entity.getSellRate());
    }

    // 내부 조회 공통 - 데이터 없으면 NOT_FOUND
    private ExchangeRateHistory findLatestEntity(Currency currency) {
        return repository.findFirstByCurrencyOrderByCollectedAtDesc(currency)
                         .orElseThrow(() -> new BusinessException(
                            ResponseCode.NOT_FOUND,
                            "해당 통화의 환율이 아직 수집되지 않았습니다: " + currency)
                         );
    }

    // ------------------------------------------------------------------
    // 3. 헬퍼
    // ------------------------------------------------------------------

    // 응답 DTO 변환
    private ExchangeRateResponse toResponseWithDisplayUnit(ExchangeRateHistory entity) {
        Currency currency = entity.getCurrency();
        BigDecimal stan = applyDisplayUnit(entity.getTradeStanRate(), currency);
        BigDecimal buy = applyDisplayUnit(entity.getBuyRate(), currency);
        BigDecimal sell = applyDisplayUnit(entity.getSellRate(), currency);

        return ExchangeRateResponse.builder()
                                   .currency(currency)
                                   .tradeStanRate(stan)
                                   .buyRate(buy)
                                   .sellRate(sell)
                                   .dateTime(entity.getCollectedAt())
                                   .build();
    }


}
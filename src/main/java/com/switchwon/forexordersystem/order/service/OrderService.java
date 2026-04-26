package com.switchwon.forexordersystem.order.service;

import com.switchwon.forexordersystem.common.enums.Currency;
import com.switchwon.forexordersystem.common.exception.BusinessException;
import com.switchwon.forexordersystem.common.response.ResponseCode;
import com.switchwon.forexordersystem.exchangerate.service.ExchangeRateService;
import com.switchwon.forexordersystem.order.domain.Order;
import com.switchwon.forexordersystem.order.domain.OrderType;
import com.switchwon.forexordersystem.order.dto.OrderRequest;
import com.switchwon.forexordersystem.order.dto.OrderResponse;
import com.switchwon.forexordersystem.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.switchwon.forexordersystem.common.util.ForexCalculator.applyDisplayUnit;
import static com.switchwon.forexordersystem.common.util.ForexCalculator.truncatedNow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 외환 주문 서비스
 * 매수/매도 주문 처리 및 이력 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    // 환율 도메인 서비스
    private final ExchangeRateService exchangeRateService;

    // 주문 이력 저장소
    private final OrderRepository orderRepository;

    // ------------------------------------------------------------------
    // 1. 주문 처리
    // ------------------------------------------------------------------

    // 외환 주문 처리 (매수/매도)
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        Currency from = Currency.from(request.getFromCurrency()); // 매도 통화
        Currency to = Currency.from(request.getToCurrency()); // 매수 통화
        BigDecimal forexAmount = request.getForexAmount();

        validateCurrencyPair(from, to); // from/to 검증

        OrderType orderType = (from == Currency.KRW) ? OrderType.BUY : OrderType.SELL; // 매수/매도 판별
        Currency foreignCurrency = (from == Currency.KRW) ? to : from; // 외화 통화 식별

        ExchangeRateService.RateSnapshot latestRate = exchangeRateService.getLatestRates(foreignCurrency); // 없으면 NOT_FOUND

        return (orderType == OrderType.BUY)
                ? buyForeignCurrency(latestRate, forexAmount, from, to)
                : sellForeignCurrency(latestRate, forexAmount, from, to);
    }

    // 매수: KRW → 외화, buyRate 적용
    private OrderResponse buyForeignCurrency(ExchangeRateService.RateSnapshot rate,
                                             BigDecimal forexAmount,
                                             Currency from,
                                             Currency to) {
        BigDecimal rawBuyRate = rate.buyRate(); // 기준 매입율 (×1.05 적용된 값)

        // KRW 환산 + 절사
        BigDecimal krwAmount = forexAmount.multiply(rawBuyRate);
        BigDecimal floorKrw = krwAmount.setScale(0, RoundingMode.FLOOR);

        // 응답 환율은 표시 단위 기준 (JPY 면 ×100)
        BigDecimal displayRate = applyDisplayUnit(rawBuyRate, to);

        Order order = Order.builder()
                           .orderType(OrderType.BUY)
                           .fromCurrency(from)        // KRW
                           .fromAmount(floorKrw)      // 절사된 KRW (출금)
                           .toCurrency(to)            // 외화
                           .toAmount(forexAmount)     // 요청 외화량 그대로 (입금)
                           .tradeRate(displayRate)
                           .createdAt(truncatedNow())
                           .build();

        Order saved = orderRepository.save(order);
        log.info("[Order] BUY 처리 완료 - id={}, {} {} → {} {}",
                saved.getId(), floorKrw, from, forexAmount, to);
        return OrderResponse.fromWithoutId(saved);
    }

    // 매도: 외화 → KRW, sellRate 적용
    private OrderResponse sellForeignCurrency(ExchangeRateService.RateSnapshot rate,
                                              BigDecimal forexAmount,
                                              Currency from,
                                              Currency to) {
        BigDecimal rawSellRate = rate.sellRate(); // 기준 매도율 (×0.95 적용된 값)

        // KRW 환산 + 절사
        BigDecimal krwAmount = forexAmount.multiply(rawSellRate);
        BigDecimal floorKrw = krwAmount.setScale(0, RoundingMode.FLOOR);

        // 응답 환율은 표시 단위 기준
        BigDecimal displayRate = applyDisplayUnit(rawSellRate, from);

        Order order = Order.builder()
                           .orderType(OrderType.SELL)
                           .fromCurrency(from)        // 외화
                           .fromAmount(forexAmount)   // 요청 외화량 그대로 (출금)
                           .toCurrency(to)            // KRW
                           .toAmount(floorKrw)        // 절사된 KRW (입금)
                           .tradeRate(displayRate)
                           .createdAt(truncatedNow())
                           .build();

        Order saved = orderRepository.save(order);
        log.info("[Order] SELL 처리 완료 - id={}, {} {} → {} {}",
                saved.getId(), forexAmount, from, floorKrw, to);
        return OrderResponse.fromWithoutId(saved);
    }

    // ------------------------------------------------------------------
    // 2. 주문 목록 조회
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderList() {
        return orderRepository.findAllByOrderByCreatedAtAsc()
                              .stream()
                              .map(OrderResponse::fromWithId)
                              .toList();
    }

    // ------------------------------------------------------------------
    // 3. 검증 / 헬퍼
    // ------------------------------------------------------------------

    // 통화 쌍 검증 - 한쪽이 반드시 KRW 여야 하고, 양쪽이 달라야함
    private void validateCurrencyPair(Currency from, Currency to) {
        if (from == to) {
            throw new BusinessException(
                    ResponseCode.BAD_REQUEST,
                    "동일 통화 간 주문은 지원하지 않습니다.");
        }
        if (from != Currency.KRW && to != Currency.KRW) {
            throw new BusinessException(
                    ResponseCode.BAD_REQUEST,
                    "이중 외화 주문은 지원하지 않습니다. (한 쪽은 반드시 KRW)");
        }
    }


}
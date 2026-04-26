package com.switchwon.forexordersystem.order.domain;

/**
 * 주문 유형
 *  - BUY  : KRW → 외화 (외화 매수)
 *  - SELL : 외화 → KRW (외화 매도)
 */
public enum OrderType {
    BUY,
    SELL
}
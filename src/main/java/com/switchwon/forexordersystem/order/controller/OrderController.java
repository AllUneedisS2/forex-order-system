package com.switchwon.forexordersystem.order.controller;

import com.switchwon.forexordersystem.common.response.CommonResponse;
import com.switchwon.forexordersystem.order.dto.OrderListResponse;
import com.switchwon.forexordersystem.order.dto.OrderRequest;
import com.switchwon.forexordersystem.order.dto.OrderResponse;
import com.switchwon.forexordersystem.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 외환 주문 REST API
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    // 주문 도메인 서비스
    private final OrderService orderService;

    // 외환 주문 생성 (매수 / 매도 자동 판별)
    // POST /order
    @PostMapping
    public CommonResponse<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        return CommonResponse.success(orderService.placeOrder(request));
    }

    // 주문 목록 조회 (최신순)
    // GET /order/list
    @GetMapping("/list")
    public CommonResponse<OrderListResponse> getOrderList() {
        OrderListResponse body = OrderListResponse.builder()
                                                  .orderList(orderService.getOrderList())
                                                  .build();
        return CommonResponse.success(body);
    }
}
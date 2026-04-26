package com.switchwon.forexordersystem.order.repository;

import com.switchwon.forexordersystem.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 주문 내역 Repository
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 모든 주문을 오름차순 조회 (요구사항에 따라)
    List<Order> findAllByOrderByCreatedAtAsc();
}
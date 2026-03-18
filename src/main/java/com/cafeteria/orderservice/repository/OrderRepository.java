package com.cafeteria.orderservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cafeteria.orderservice.entity.Order;
import com.cafeteria.orderservice.enums.PaymentStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByPaymentStatusOrderByCreatedAtAsc(PaymentStatus paymentStatus);
}

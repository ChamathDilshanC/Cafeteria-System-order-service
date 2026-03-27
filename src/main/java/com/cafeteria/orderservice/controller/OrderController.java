package com.cafeteria.orderservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cafeteria.orderservice.dto.OrderResponse;
import com.cafeteria.orderservice.dto.PlaceOrderRequest;
import com.cafeteria.orderservice.service.OrderService;
import com.cafeteria.orderservice.util.JwtDecoder;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final JwtDecoder jwtDecoder;

    public OrderController(OrderService orderService, JwtDecoder jwtDecoder) {
        this.orderService = orderService;
        this.jwtDecoder = jwtDecoder;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestHeader("Authorization") String auth,
            @Valid @RequestBody PlaceOrderRequest request) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(userId, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestHeader("Authorization") String auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(orderService.getMyOrders(userId));
    }

    @GetMapping("/pending-payment")
    public ResponseEntity<List<OrderResponse>> getPendingPayment(
            @RequestHeader("Authorization") String auth) {
        if (!jwtDecoder.hasRole(auth, "STAFF") && !jwtDecoder.hasRole(auth, "ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(orderService.getAllPendingPayment());
    }

    @PutMapping("/{id}/verify-payment")
    public ResponseEntity<OrderResponse> verifyPayment(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.verifyPayment(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long id) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(orderService.cancelOrder(id, userId));
    }

    /**
     * Internal endpoint — called by kitchen-service via Feign to sync order status.
     */
    @PutMapping("/{id}/internal-status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        orderService.updateStatusFromKitchen(id, body.get("status"));
        return ResponseEntity.ok().build();
    }

    private Long resolveUserId(String auth) {
        Long userId = jwtDecoder.extractUserId(auth);
        if (userId == null)
            throw new RuntimeException("Unable to resolve user identity from token");
        return userId;
    }
}

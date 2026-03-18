package com.cafeteria.orderservice.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cafeteria.orderservice.client.KitchenClient;
import com.cafeteria.orderservice.client.MenuClient;
import com.cafeteria.orderservice.client.dto.CreateTicketRequest;
import com.cafeteria.orderservice.client.dto.MenuItemDto;
import com.cafeteria.orderservice.dto.OrderItemRequest;
import com.cafeteria.orderservice.dto.OrderResponse;
import com.cafeteria.orderservice.dto.PlaceOrderRequest;
import com.cafeteria.orderservice.entity.Order;
import com.cafeteria.orderservice.entity.OrderItem;
import com.cafeteria.orderservice.enums.OrderStatus;
import com.cafeteria.orderservice.enums.PaymentStatus;
import com.cafeteria.orderservice.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuClient menuClient;
    private final KitchenClient kitchenClient;

    public OrderService(OrderRepository orderRepository, MenuClient menuClient, KitchenClient kitchenClient) {
        this.orderRepository = orderRepository;
        this.menuClient = menuClient;
        this.kitchenClient = kitchenClient;
    }

    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        Order order = new Order();
        order.setUserId(userId);
        order.setNotes(request.getNotes());

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            MenuItemDto menuItem = menuClient.getMenuItem(itemReq.getMenuItemId());
            if (!menuItem.isAvailable()) {
                throw new IllegalArgumentException("Menu item not available: " + menuItem.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItemId(menuItem.getId());
            orderItem.setItemName(menuItem.getName());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(menuItem.getPrice());
            orderItem.setSubtotal(menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));

            order.getItems().add(orderItem);
            total = total.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);
        Order saved = orderRepository.save(order);

        return toResponse(saved);
    }

    @Transactional
    public OrderResponse verifyPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Order is already paid");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot approve payment for a cancelled order");
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);

        // Only now notify kitchen — payment is verified
        List<CreateTicketRequest.TicketItem> ticketItems = saved.getItems().stream()
                .map(i -> new CreateTicketRequest.TicketItem(i.getMenuItemId(), i.getItemName(), i.getQuantity()))
                .toList();
        kitchenClient.createTicket(new CreateTicketRequest(
                saved.getId(), saved.getUserId(), ticketItems, saved.getNotes()));

        return toResponse(saved);
    }

    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getAllPendingPayment() {
        return orderRepository.findByPaymentStatusOrderByCreatedAtAsc(PaymentStatus.UNPAID)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void updateStatusFromKitchen(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        try {
            order.setStatus(OrderStatus.valueOf(status));
            if (order.getStatus() == OrderStatus.CANCELLED && order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
            orderRepository.save(order);
        } catch (IllegalArgumentException ignored) {
            // Unknown status — ignore
        }
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = getOrderForUser(orderId, userId);
        if (order.getStatus() == OrderStatus.PREPARING
                || order.getStatus() == OrderStatus.READY
                || order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        return toResponse(orderRepository.save(order));
    }

    private Order getOrderForUser(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied to order: " + orderId);
        }
        return order;
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse r = new OrderResponse();
        r.setId(order.getId());
        r.setUserId(order.getUserId());
        r.setStatus(order.getStatus().name());
        r.setPaymentStatus(order.getPaymentStatus().name());
        r.setTotalAmount(order.getTotalAmount());
        r.setNotes(order.getNotes());
        r.setCreatedAt(order.getCreatedAt());
        r.setItems(order.getItems().stream().map(i -> {
            OrderResponse.OrderItemDto dto = new OrderResponse.OrderItemDto();
            dto.setMenuItemId(i.getMenuItemId());
            dto.setItemName(i.getItemName());
            dto.setQuantity(i.getQuantity());
            dto.setUnitPrice(i.getUnitPrice());
            dto.setSubtotal(i.getSubtotal());
            return dto;
        }).toList());
        return r;
    }
}

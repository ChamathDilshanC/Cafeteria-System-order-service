package com.cafeteria.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.cafeteria.orderservice.client.dto.CreateTicketRequest;

@FeignClient(name = "kitchen-service", path = "/kitchen")
public interface KitchenClient {

    @PostMapping("/tickets")
    void createTicket(@RequestBody CreateTicketRequest request);
}

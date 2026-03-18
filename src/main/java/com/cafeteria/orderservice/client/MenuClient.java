package com.cafeteria.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cafeteria.orderservice.client.dto.MenuItemDto;

@FeignClient(name = "menu-service", path = "/menu")
public interface MenuClient {

    @GetMapping("/{id}")
    MenuItemDto getMenuItem(@PathVariable Long id);
}

package com.cafeteria.orderservice.client.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors MenuItemResponse from menu-service — only the fields we need. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemDto {
  private Long id;
  private String name;
  private BigDecimal price;
  private boolean available;
}

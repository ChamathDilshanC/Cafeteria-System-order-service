package com.cafeteria.orderservice.client.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketRequest {
  private Long orderId;
  private Long userId;
  private List<TicketItem> items;
  private String notes;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TicketItem {
    private Long menuItemId;
    private String itemName;
    private int quantity;
  }
}

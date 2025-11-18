package com.brokerx.matching_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/* Domain model representing an entry in the order book */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookEntry {
    private Long orderId;
    private String stockSymbol;
    private OrderSide side;
    private BigDecimal limitPrice;
    private Integer quantity;
    private Integer remainingQuantity;
    private Instant timestamp;
}

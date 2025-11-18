package com.brokerx.matching_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/* Domain model representing a matched trade */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    private Long buyOrderId;
    private Long sellOrderId;
    private String stockSymbol;
    private Integer quantity;
    private BigDecimal executionPrice;
}

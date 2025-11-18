package com.brokerx.matching_service.infrastructure.kafka.dto;

import java.math.BigDecimal;

/* DTO for OrderMatched event published to Kafka */
public record OrderMatchedEvent(
        Long buyOrderId,
        Long sellOrderId,
        String stockSymbol,
        Integer quantity,
        BigDecimal executionPrice
) {}

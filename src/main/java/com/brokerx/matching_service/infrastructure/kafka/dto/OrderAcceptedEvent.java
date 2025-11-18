package com.brokerx.matching_service.infrastructure.kafka.dto;

import java.math.BigDecimal;

/* DTO for OrderAccepted event consumed from Kafka */
public record OrderAcceptedEvent(
        Long orderId,
        String stockSymbol,
        String side,
        BigDecimal limitPrice,
        Integer quantity
) {}

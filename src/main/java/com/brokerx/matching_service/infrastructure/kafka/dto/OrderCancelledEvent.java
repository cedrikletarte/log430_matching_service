package com.brokerx.matching_service.infrastructure.kafka.dto;

/* Event received when an order is cancelled in order_service */
public record OrderCancelledEvent(
    Long orderId,
    String stockSymbol,
    String side
) {}

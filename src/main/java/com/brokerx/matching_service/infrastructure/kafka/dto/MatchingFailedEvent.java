package com.brokerx.matching_service.infrastructure.kafka.dto;

import java.math.BigDecimal;

/**
 * This event is published when matching fails and compensation is needed
 * Other services (order_service, wallet_service) must rollback their actions
 */
public record MatchingFailedEvent(
        Long orderId,
        String stockSymbol,
        String side,
        BigDecimal limitPrice,
        Integer quantity,
        String reason,
        String compensatedAt
) {
}

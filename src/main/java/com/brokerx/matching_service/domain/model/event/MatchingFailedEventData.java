package com.brokerx.matching_service.domain.model.event;

/* Data transfer object for compensation event */
public record MatchingFailedEventData(
        Long orderId,
        String stockSymbol,
        String side,
        java.math.BigDecimal limitPrice,
        Integer quantity,
        String reason,
        String compensatedAt
) {
    public static MatchingFailedEventData create(
            Long orderId,
            String stockSymbol,
            String side,
            java.math.BigDecimal limitPrice,
            Integer quantity,
            String reason
    ) {
        return new MatchingFailedEventData(
                orderId,
                stockSymbol,
                side,
                limitPrice,
                quantity,
                reason,
                java.time.Instant.now().toString()
        );
    }
}

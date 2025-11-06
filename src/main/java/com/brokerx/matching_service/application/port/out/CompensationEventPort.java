package com.brokerx.matching_service.application.port.out;

/**
 * Port for publishing compensation events
 * Used when a saga fails and needs to be rolled back
 */
public interface CompensationEventPort {
    
    /**
     * Publish a matching failed event for compensation
     */
    void publishMatchingFailed(MatchingFailedEventData eventData);
    
    /**
     * Data transfer object for compensation event
     */
    record MatchingFailedEventData(
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
}

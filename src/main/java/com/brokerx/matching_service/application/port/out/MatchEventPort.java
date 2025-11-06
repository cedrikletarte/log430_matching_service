package com.brokerx.matching_service.application.port.out;

import java.math.BigDecimal;

import com.brokerx.matching_service.domain.model.Match;

/**
 * Output port for publishing match events (hexagonal architecture)
 */
public interface MatchEventPort {
    
    /**
     * Publish a match event when orders are matched
     */
    void publishMatchEvent(MatchEventData eventData);
    
    /**
     * Data for match event
     */
    record MatchEventData(
            Long buyOrderId,
            Long sellOrderId,
            String stockSymbol,
            Integer quantity,
            BigDecimal executionPrice
    ) {
        public static MatchEventData from(Match match) {
            return new MatchEventData(
                    match.getBuyOrderId(),
                    match.getSellOrderId(),
                    match.getStockSymbol(),
                    match.getQuantity(),
                    match.getExecutionPrice()
            );
        }
    }
}

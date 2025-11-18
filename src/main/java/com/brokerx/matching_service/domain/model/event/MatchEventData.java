package com.brokerx.matching_service.domain.model.event;

import java.math.BigDecimal;

import com.brokerx.matching_service.domain.model.Match;

/* Data for match event */
public record MatchEventData(
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

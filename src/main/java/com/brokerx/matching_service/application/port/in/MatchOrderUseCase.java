package com.brokerx.matching_service.application.port.in;

import com.brokerx.matching_service.domain.model.Match;
import java.math.BigDecimal;
import java.util.List;

/**
 * Input port for matching use case (hexagonal architecture)
 */
public interface MatchOrderUseCase {
    
    /**
     * Process a new order and attempt matching
     */
    List<Match> processOrder(ProcessOrderCommand command);
    
    /**
     * Command for processing an order
     */
    record ProcessOrderCommand(
            Long orderId,
            String stockSymbol,
            String side,
            BigDecimal limitPrice,
            Integer quantity
    ) {}
}

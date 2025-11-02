package com.brokerx.matching_service.application.service;

import com.brokerx.matching_service.application.port.in.MatchOrderUseCase;
import com.brokerx.matching_service.application.port.out.MatchEventPort;
import com.brokerx.matching_service.domain.model.Match;
import com.brokerx.matching_service.domain.model.OrderBookEntry;
import com.brokerx.matching_service.domain.model.OrderSide;
import com.brokerx.matching_service.domain.service.MatchingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Application service implementing the matching use case
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService implements MatchOrderUseCase {

    private final MatchingEngine matchingEngine;
    private final MatchEventPort matchEventPort;

    @Override
    public List<Match> processOrder(ProcessOrderCommand command) {
        log.info("Processing order for matching: {}", command);

        // Convert command to domain model
        OrderBookEntry order = OrderBookEntry.builder()
                .orderId(command.orderId())
                .stockSymbol(command.stockSymbol())
                .side(OrderSide.valueOf(command.side()))
                .limitPrice(command.limitPrice())
                .quantity(command.quantity())
                .remainingQuantity(command.quantity())
                .timestamp(Instant.now())
                .build();

        // Execute matching
        List<Match> matches = matchingEngine.addOrder(order);

        // Publish match events
        matches.forEach(match -> {
            MatchEventPort.MatchEventData eventData = MatchEventPort.MatchEventData.from(match);
            matchEventPort.publishMatchEvent(eventData);
            log.info("📤 Published match event: {}", eventData);
        });

        return matches;
    }
}

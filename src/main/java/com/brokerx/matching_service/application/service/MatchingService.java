package com.brokerx.matching_service.application.service;

import com.brokerx.matching_service.application.port.in.command.ProcessOrderCommand;
import com.brokerx.matching_service.application.port.in.useCase.MatchOrderUseCase;
import com.brokerx.matching_service.application.port.out.OutboxPort;
import com.brokerx.matching_service.domain.model.*;
import com.brokerx.matching_service.domain.model.event.MatchEventData;
import com.brokerx.matching_service.domain.service.MatchingEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

/* Simplified matching service using Outbox Pattern for choreography-based saga */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService implements MatchOrderUseCase {

    private final MatchingEngine matchingEngine;
    private final OutboxPort outboxPort;
    private final ObjectMapper objectMapper;
    private final OutboxPublisherService outboxPublisherService;

    /* Process a new order and attempt matching */
    @Override
    @Transactional
    public List<Match> processOrder(ProcessOrderCommand command) {
        log.info("Processing order for matching: orderId={}, symbol={}, side={}, quantity={}", 
                command.orderId(), command.stockSymbol(), command.side(), command.quantity());

        // 1. Convert command to domain model
        OrderBookEntry order = OrderBookEntry.builder()
                .orderId(command.orderId())
                .stockSymbol(command.stockSymbol())
                .side(OrderSide.valueOf(command.side()))
                .limitPrice(command.limitPrice())
                .quantity(command.quantity())
                .remainingQuantity(command.quantity())
                .timestamp(Instant.now())
                .build();

        // 2. Execute matching (in-memory order book)
        List<Match> matches = matchingEngine.addOrder(order);

        // 3. If matches found, save to outbox for reliable Kafka publishing
        if (!matches.isEmpty()) {
            matches.forEach(match -> {
                OutboxEvent outboxEvent = createOutboxEvent(match);
                outboxPort.save(outboxEvent);
                log.info("Match saved to outbox: buyOrder={}, sellOrder={}, symbol={}, quantity={}, price={}", 
                        match.getBuyOrderId(), match.getSellOrderId(), match.getStockSymbol(),
                        match.getQuantity(), match.getExecutionPrice());
            });
            
            log.info("Successfully processed {} match(es) for order {}", matches.size(), command.orderId());
            
            // 4. Trigger immediate publishing after transaction commit
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        outboxPublisherService.publishImmediately();
                    }
                }
            );
        } else {
            log.info("Order {} added to book (no immediate match)", command.orderId());
        }

        return matches;
    }
    
    /* Create an outbox event from a match */
    private OutboxEvent createOutboxEvent(Match match) {
        try {
            MatchEventData eventData = MatchEventData.from(match);
            String payload = objectMapper.writeValueAsString(eventData);
            
            return OutboxEvent.builder()
                    .aggregateType("MATCH")
                    .aggregateId(String.format("%d-%d", match.getBuyOrderId(), match.getSellOrderId()))
                    .eventType("ORDER_MATCHED")
                    .payload(payload)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .createdAt(Instant.now())
                    .retryCount(0)
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize match event", e);
        }
    }
}

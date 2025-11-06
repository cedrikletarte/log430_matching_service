package com.brokerx.matching_service.application.service;

import com.brokerx.matching_service.application.port.in.MatchOrderUseCase;
import com.brokerx.matching_service.application.port.out.MatchEventPort;
import com.brokerx.matching_service.application.port.out.OutboxPort;
import com.brokerx.matching_service.application.port.out.SagaPort;
import com.brokerx.matching_service.domain.model.*;
import com.brokerx.matching_service.domain.service.MatchingEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Application service implementing the matching use case with Outbox Pattern and Saga
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService implements MatchOrderUseCase {

    private final MatchingEngine matchingEngine;
    private final OutboxPort outboxPort;
    private final SagaPort sagaPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional // Unique transaction to ensure consistency between matching and outbox
    public List<Match> processOrder(ProcessOrderCommand command) {
        log.info("Processing order for matching: {}", command);

        // 1. Create or retrieve the saga
        MatchingSaga saga = sagaPort.findByOrderId(command.orderId())
                .orElseGet(() -> createNewSaga(command));
        
        try {
            // 2. Advance the saga to MATCHING status
            saga.advanceToStep(MatchingSaga.SagaStatus.MATCHING, "Processing order in matching engine");
            sagaPort.updateSaga(saga);

            // 3. Convert command to domain model
            OrderBookEntry order = OrderBookEntry.builder()
                    .orderId(command.orderId())
                    .stockSymbol(command.stockSymbol())
                    .side(OrderSide.valueOf(command.side()))
                    .limitPrice(command.limitPrice())
                    .quantity(command.quantity())
                    .remainingQuantity(command.quantity())
                    .timestamp(Instant.now())
                    .build();

            // 4. Execute matching (en mémoire)
            List<Match> matches = matchingEngine.addOrder(order);

            // 5. If matches found, persist in outbox BEFORE publishing
            if (!matches.isEmpty()) {
                saga.advanceToStep(MatchingSaga.SagaStatus.MATCHED, "Matches found, preparing events");
                sagaPort.updateSaga(saga);
                
                matches.forEach(match -> {
                    try {
                        // Persist in outbox (guarantees at-least-once delivery)
                        OutboxEvent outboxEvent = createOutboxEvent(match);
                        outboxPort.save(outboxEvent);
                        log.info("✅ Match saved to outbox: buyOrder={}, sellOrder={}", 
                                match.getBuyOrderId(), match.getSellOrderId());
                    } catch (Exception e) {
                        log.error("❌ Failed to save match to outbox", e);
                        throw new RuntimeException("Failed to save match to outbox", e);
                    }
                });
                
                log.info("📊 {} matches saved to outbox for order {}", matches.size(), command.orderId());
            } else {
                // No matches found, order added to book
                saga.advanceToStep(MatchingSaga.SagaStatus.STARTED, "Order added to book, waiting for match");
                sagaPort.updateSaga(saga);
                log.info("📖 Order {} added to book (no immediate match)", command.orderId());
            }

            return matches;
            
        } catch (Exception e) {
            log.error("❌ Error processing order {}: {}", command.orderId(), e.getMessage(), e);

            // Start compensation
            saga.startCompensation("Matching failed: " + e.getMessage());
            sagaPort.updateSaga(saga);
            
            throw new RuntimeException("Failed to process order: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a new saga for an order
     */
    private MatchingSaga createNewSaga(ProcessOrderCommand command) {
        MatchingSaga saga = MatchingSaga.builder()
                .orderId(command.orderId())
                .stockSymbol(command.stockSymbol())
                .orderSide(command.side())
                .limitPrice(command.limitPrice())
                .quantity(command.quantity())
                .status(MatchingSaga.SagaStatus.STARTED)
                .currentStep("Order received")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        return sagaPort.createSaga(saga);
    }
    
    /**
     * Create an outbox event from a match
     */
    private OutboxEvent createOutboxEvent(Match match) {
        try {
            MatchEventPort.MatchEventData eventData = MatchEventPort.MatchEventData.from(match);
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

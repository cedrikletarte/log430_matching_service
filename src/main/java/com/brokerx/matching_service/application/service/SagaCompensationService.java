package com.brokerx.matching_service.application.service;

import com.brokerx.matching_service.application.port.out.CompensationEventPort;
import com.brokerx.matching_service.application.port.out.OutboxPort;
import com.brokerx.matching_service.application.port.out.SagaPort;
import com.brokerx.matching_service.domain.model.MatchingSaga;
import com.brokerx.matching_service.domain.model.OutboxEvent;
import com.brokerx.matching_service.domain.service.MatchingEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service for handling compensation of failed sagas (Saga Choreography)
 * Implements distributed rollback by publishing compensation events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaCompensationService {
    
    private final SagaPort sagaPort;
    private final OutboxPort outboxPort;
    private final CompensationEventPort compensationEventPort;
    private final MatchingEngine matchingEngine;
    private final ObjectMapper objectMapper;
    
    /**
     * Check and compensate failed sagas every 10 seconds
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 15000)
    @Transactional
    public void compensateFailedSagas() {
        List<MatchingSaga> sagasToCompensate = sagaPort.findSagasNeedingCompensation();
        
        if (sagasToCompensate.isEmpty()) {
            return;
        }
        
        log.warn("⚠️ Found {} sagas needing compensation", sagasToCompensate.size());
        
        sagasToCompensate.forEach(this::compensateSaga);
    }
    
    /**
     * Compensate an individual saga
     * Publishes a MATCHING_FAILED event for order_service and wallet_service to compensate
     */
    private void compensateSaga(MatchingSaga saga) {
        log.warn("🔄 Compensating saga: id={}, orderId={}, reason={}", 
                saga.getId(), saga.getOrderId(), saga.getErrorReason());
        
        try {
            // 1. Remove the order from the order book (if present)
            boolean removed = matchingEngine.cancelOrder(
                    saga.getOrderId(),
                    saga.getStockSymbol(),
                    com.brokerx.matching_service.domain.model.OrderSide.BUY // Try both sides
            );
            
            if (!removed) {
                matchingEngine.cancelOrder(
                        saga.getOrderId(), 
                        saga.getStockSymbol(), 
                        com.brokerx.matching_service.domain.model.OrderSide.SELL
                );
            }

            // 2. Publish MATCHING_FAILED compensation event
            //    → order_service rollback: ACCEPTED → REJECTED
            //    → wallet_service rollback: restore balance, cancel reserves
            CompensationEventPort.MatchingFailedEventData compensationEvent = 
                    CompensationEventPort.MatchingFailedEventData.create(
                            saga.getOrderId(),
                            saga.getStockSymbol(),
                            saga.getOrderSide(),
                            saga.getLimitPrice(),
                            saga.getQuantity(),
                            saga.getErrorReason() != null ? saga.getErrorReason() : "Matching failed"
                    );
            
            compensationEventPort.publishMatchingFailed(compensationEvent);
            log.warn("⚠️ Published MATCHING_FAILED event for compensation: orderId={}", saga.getOrderId());

            // 3. Save also in the outbox for guarantee (double assurance)
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("SAGA_COMPENSATION")
                    .aggregateId(String.valueOf(saga.getOrderId()))
                    .eventType("MATCHING_FAILED")
                    .payload(buildCompensationPayload(saga))
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .createdAt(Instant.now())
                    .retryCount(0)
                    .build();
            
            outboxPort.save(outboxEvent);

            // 4. Mark the saga as compensated
            saga.markAsCompensated();
            sagaPort.updateSaga(saga);
            
            log.info("✅ Saga compensated successfully: id={}, orderId={}", 
                    saga.getId(), saga.getOrderId());
            
        } catch (Exception e) {
            log.error("❌ Failed to compensate saga id={}: {}", saga.getId(), e.getMessage(), e);

            // Mark as definitively failed after several attempts
            saga.markAsFailed("Compensation failed: " + e.getMessage());
            sagaPort.updateSaga(saga);
        }
    }
    
    /**
     * Builds the compensation event payload
     */
    private String buildCompensationPayload(MatchingSaga saga) {
        try {
            var payload = java.util.Map.of(
                    "orderId", saga.getOrderId(),
                    "stockSymbol", saga.getStockSymbol(),
                    "reason", saga.getErrorReason() != null ? saga.getErrorReason() : "Unknown error",
                    "compensatedAt", Instant.now().toString()
            );
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.format("{\"orderId\":%d,\"error\":\"Failed to serialize\"}", saga.getOrderId());
        }
    }
    
    /**
     * Cleans up completed sagas older than 30 days
     */
    @Scheduled(cron = "0 0 2 * * *") // Every day at 2 AM
    @Transactional
    public void cleanupOldSagas() {
        log.info("🧹 Starting cleanup of old sagas");
        // TODO: Implement cleanup if necessary
    }
}

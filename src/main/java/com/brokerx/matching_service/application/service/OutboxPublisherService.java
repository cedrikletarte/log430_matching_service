package com.brokerx.matching_service.application.service;

import com.brokerx.matching_service.application.port.out.MatchEventPort;
import com.brokerx.matching_service.application.port.out.OutboxPort;
import com.brokerx.matching_service.domain.model.OutboxEvent;
import com.brokerx.matching_service.domain.model.event.MatchEventData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for publishing events from Outbox to Kafka
 * Implements the polling pattern to guarantee at-least-once delivery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisherService {
    
    private final OutboxPort outboxPort;
    private final MatchEventPort matchEventPort;
    private final ObjectMapper objectMapper;
    
    /* Publish pending events every 5 seconds */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxPort.findPendingEvents();
        
        if (pendingEvents.isEmpty()) {
            return;
        }
        
        log.info("Publishing {} pending outbox events", pendingEvents.size());
        
        pendingEvents.forEach(event -> {
            try {
                // Deserialize the payload
                MatchEventData eventData = objectMapper.readValue(
                        event.getPayload(), 
                        MatchEventData.class
                );

                // Publish to Kafka
                matchEventPort.publishMatchEvent(eventData);

                // Mark as published
                outboxPort.markAsPublished(event.getId());
                
                log.info("Published outbox event: id={}, type={}", event.getId(), event.getEventType());
                
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}: {}", event.getId(), e.getMessage());

                // Increment retry count
                outboxPort.incrementRetry(event.getId(), e.getMessage());
            }
        });
    }
    
    /* Clean up published events older than 7 days (every day at midnight) */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        outboxPort.deletePublishedEventsBefore(cutoff);
        log.info("Cleaned up published outbox events older than {}", cutoff);
    }
}

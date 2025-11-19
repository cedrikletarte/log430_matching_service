package com.brokerx.matching_service.application.service;

import com.brokerx.matching_service.application.port.out.MatchEventPort;
import com.brokerx.matching_service.application.port.out.OutboxPort;
import com.brokerx.matching_service.domain.model.OutboxEvent;
import com.brokerx.matching_service.domain.model.event.MatchEventData;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/* Service for publishing events from Outbox to Kafka */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisherService {
    
    private final OutboxPort outboxPort;
    private final MatchEventPort matchEventPort;
    private final ObjectMapper objectMapper;
    
    /* Publish pending events immediately (called on startup and after each transaction commit) */
    @PostConstruct
    public void publishImmediately() {
        List<OutboxEvent> pendingEvents = outboxPort.findPendingEvents();
        
        if (pendingEvents.isEmpty()) {
            return;
        }
        
        log.info("Publishing {} pending outbox events", pendingEvents.size());
        pendingEvents.forEach(this::publishEvent);
    }
    
    /* Publish a single outbox event */
    @Transactional
    protected void publishEvent(OutboxEvent event) {
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
    }
}

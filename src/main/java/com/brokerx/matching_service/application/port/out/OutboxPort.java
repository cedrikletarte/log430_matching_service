package com.brokerx.matching_service.application.port.out;

import com.brokerx.matching_service.domain.model.OutboxEvent;

import java.util.List;
import java.util.Optional;

public interface OutboxPort {
    
    /* Save an event in the outbox */
    OutboxEvent save(OutboxEvent event);
    
    /* Retrieve pending events for publication */
    List<OutboxEvent> findPendingEvents();
    
    /* Retrieve an event by ID */
    Optional<OutboxEvent> findById(Long id);
    
    /* Mark an event as published */
    void markAsPublished(Long eventId);
    
    /* Increment the retry counter */
    void incrementRetry(Long eventId, String error);
    
    /* Delete published events older than X days (cleanup) */
    void deletePublishedEventsBefore(java.time.Instant cutoff);
}

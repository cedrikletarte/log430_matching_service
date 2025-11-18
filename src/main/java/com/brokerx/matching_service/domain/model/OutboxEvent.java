package com.brokerx.matching_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain model for Outbox Pattern - event to publish
 * Granted, this is a straightforward translation task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    
    private Long id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String payload;
    private OutboxStatus status;
    private Instant createdAt;
    private Instant publishedAt;
    private Integer retryCount;
    private String errorMessage;
    
    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }
    
    /* Mark the event as published */
    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }
    
    /* Increment the retry counter */
    public void incrementRetry(String error) {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        this.errorMessage = error;

        // After 5 attempts, mark as definitive failure
        if (this.retryCount >= 5) {
            this.status = OutboxStatus.FAILED;
        }
    }
    
    /* Check if the event can be retried */
    public boolean canRetry() {
        return this.status == OutboxStatus.PENDING && 
               (this.retryCount == null || this.retryCount < 5);
    }
}

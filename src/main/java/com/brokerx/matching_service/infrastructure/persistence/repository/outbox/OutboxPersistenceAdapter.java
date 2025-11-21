package com.brokerx.matching_service.infrastructure.persistence.repository.outbox;

import com.brokerx.matching_service.application.port.out.OutboxPort;
import com.brokerx.matching_service.domain.model.OutboxEvent;
import com.brokerx.matching_service.infrastructure.persistence.entity.OutboxEventEntity;
import com.brokerx.matching_service.infrastructure.persistence.mapper.OutboxEventMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/* Adapter for Outbox Pattern persistence */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements OutboxPort {
    
    private final OutboxEventJpaRepository repository;
    private final OutboxEventMapper mapper;
    
    /* Save an outbox event */
    @Override
    @Transactional
    public OutboxEvent save(OutboxEvent event) {
        log.debug("Saving outbox event: aggregateType={}, eventType={}", 
                event.getAggregateType(), event.getEventType());
        
        OutboxEventEntity entity = OutboxEventMapper.fromDomain(event);
        OutboxEventEntity saved = repository.save(entity);
        
        log.info("Outbox event saved: id={}, type={}", saved.getId(), saved.getEventType());
        return mapper.toDomain(saved);
    }
    
    /* Find pending outbox events */
    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPendingEvents() {
        return repository.findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
    
    /* Find outbox event by ID */
    @Override
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }
    
    /* Mark an outbox event as published */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsPublished(Long eventId) {
        log.info("Attempting to mark outbox event as PUBLISHED: id={}", eventId);
        
        Optional<OutboxEventEntity> optionalEntity = repository.findById(eventId);
        
        if (optionalEntity.isEmpty()) {
            log.error("Failed to mark as published - Outbox event not found: id={}", eventId);
            return;
        }
        
        OutboxEventEntity entity = optionalEntity.get();
        log.info("Found outbox event: id={}, currentStatus={}, eventType={}", 
                eventId, entity.getStatus(), entity.getEventType());
        
        entity.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
        entity.setPublishedAt(Instant.now());
        OutboxEventEntity saved = repository.save(entity);
        
        log.info("✓ Outbox event successfully updated: id={}, oldStatus={}, newStatus={}, publishedAt={}", 
                saved.getId(), "PENDING", saved.getStatus(), saved.getPublishedAt());
    }
    
    /* Increment retry count for an outbox event */
    @Override
    @Transactional
    public void incrementRetry(Long eventId, String error) {
        repository.findById(eventId).ifPresent(entity -> {
            entity.setRetryCount((entity.getRetryCount() == null ? 0 : entity.getRetryCount()) + 1);
            entity.setErrorMessage(error);
            
            // If retries exceed 5, mark as FAILED
            if (entity.getRetryCount() >= 5) {
                entity.setStatus(OutboxEvent.OutboxStatus.FAILED);
                log.warn("Outbox event marked as FAILED after {} retries: id={}", 
                        entity.getRetryCount(), eventId);
            }
            
            repository.save(entity);
        });
    }
}

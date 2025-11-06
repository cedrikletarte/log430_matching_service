package com.brokerx.matching_service.infrastructure.persistence.adapter;

import com.brokerx.matching_service.application.port.out.OutboxPort;
import com.brokerx.matching_service.domain.model.OutboxEvent;
import com.brokerx.matching_service.infrastructure.persistence.entity.OutboxEventEntity;
import com.brokerx.matching_service.infrastructure.persistence.mapper.OutboxEventMapper;
import com.brokerx.matching_service.infrastructure.persistence.repository.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapter for Outbox Pattern persistence
 * Implements the OutboxPort (hexagonal architecture)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements OutboxPort {
    
    private final OutboxEventJpaRepository repository;
    private final OutboxEventMapper mapper;
    
    @Override
    @Transactional
    public OutboxEvent save(OutboxEvent event) {
        log.debug("Saving outbox event: aggregateType={}, eventType={}", 
                event.getAggregateType(), event.getEventType());
        
        OutboxEventEntity entity = OutboxEventMapper.fromDomain(event);
        OutboxEventEntity saved = repository.save(entity);
        
        log.info("✅ Outbox event saved: id={}, type={}", saved.getId(), saved.getEventType());
        return mapper.toDomain(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPendingEvents() {
        return repository.findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<OutboxEvent> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    @Transactional
    public void markAsPublished(Long eventId) {
        repository.findById(eventId).ifPresent(entity -> {
            entity.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
            entity.setPublishedAt(Instant.now());
            repository.save(entity);
            log.info("✅ Outbox event marked as published: id={}", eventId);
        });
    }
    
    @Override
    @Transactional
    public void incrementRetry(Long eventId, String error) {
        repository.findById(eventId).ifPresent(entity -> {
            entity.setRetryCount((entity.getRetryCount() == null ? 0 : entity.getRetryCount()) + 1);
            entity.setErrorMessage(error);
            
            // If retries exceed 5, mark as FAILED
            if (entity.getRetryCount() >= 5) {
                entity.setStatus(OutboxEvent.OutboxStatus.FAILED);
                log.warn("⚠️ Outbox event marked as FAILED after {} retries: id={}", 
                        entity.getRetryCount(), eventId);
            }
            
            repository.save(entity);
        });
    }
    
    @Override
    @Transactional
    public void deletePublishedEventsBefore(Instant cutoff) {
        repository.deletePublishedEventsBefore(cutoff);
        log.info("🧹 Cleaned up published outbox events before {}", cutoff);
    }
}

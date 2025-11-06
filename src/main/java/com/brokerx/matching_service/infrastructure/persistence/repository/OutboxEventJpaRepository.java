package com.brokerx.matching_service.infrastructure.persistence.repository;

import com.brokerx.matching_service.domain.model.OutboxEvent;
import com.brokerx.matching_service.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * JPA Repository for Outbox Events
 * Infrastructure layer
 */
@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {
    
    /**
     * Fetch events by status ordered by creation time
     */
    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus status);
    
    /**
     * Delete published events older than a certain date (cleanup)
     */
    @Modifying
    @Query("DELETE FROM OutboxEventEntity o WHERE o.status = 'PUBLISHED' AND o.publishedAt < :cutoff")
    void deletePublishedEventsBefore(@Param("cutoff") Instant cutoff);
    
    /**
     * Count events by status
     */
    long countByStatus(OutboxEvent.OutboxStatus status);
}

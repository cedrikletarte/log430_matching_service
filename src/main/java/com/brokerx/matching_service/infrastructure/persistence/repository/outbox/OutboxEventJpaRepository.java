package com.brokerx.matching_service.infrastructure.persistence.repository.outbox;

import com.brokerx.matching_service.domain.model.OutboxEvent;
import com.brokerx.matching_service.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/* JPA Repository for Outbox Events */
@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {
    
    /* Fetch events by status ordered by creation time */
    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus status);
    
    /* Count events by status */
    long countByStatus(OutboxEvent.OutboxStatus status);
}

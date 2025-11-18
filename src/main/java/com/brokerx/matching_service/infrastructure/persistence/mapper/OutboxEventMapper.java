package com.brokerx.matching_service.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.brokerx.matching_service.domain.model.OutboxEvent;
import com.brokerx.matching_service.infrastructure.persistence.entity.OutboxEventEntity;

@Component
public class OutboxEventMapper {
    
    /* Convert to domain model */
    public OutboxEvent toDomain(OutboxEventEntity entity) {
        return OutboxEvent.builder()
                .id(entity.getId())
                .aggregateType(entity.getAggregateType())
                .aggregateId(entity.getAggregateId())
                .eventType(entity.getEventType())
                .payload(entity.getPayload())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .publishedAt(entity.getPublishedAt())
                .retryCount(entity.getRetryCount())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
    
    /* Create from domain model */
    public static OutboxEventEntity fromDomain(OutboxEvent domain) {
        return OutboxEventEntity.builder()
                .id(domain.getId())
                .aggregateType(domain.getAggregateType())
                .aggregateId(domain.getAggregateId())
                .eventType(domain.getEventType())
                .payload(domain.getPayload())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .publishedAt(domain.getPublishedAt())
                .retryCount(domain.getRetryCount())
                .errorMessage(domain.getErrorMessage())
                .build();
    }
}

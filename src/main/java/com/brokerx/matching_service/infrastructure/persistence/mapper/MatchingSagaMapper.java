package com.brokerx.matching_service.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.brokerx.matching_service.domain.model.MatchingSaga;
import com.brokerx.matching_service.infrastructure.persistence.entity.MatchingSagaEntity;

@Component
public class MatchingSagaMapper {

    /**
     * Convert to domain model
     */
    public MatchingSaga toDomain(MatchingSagaEntity entity) {
        return MatchingSaga.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .stockSymbol(entity.getStockSymbol())
                .orderSide(entity.getOrderSide())
                .limitPrice(entity.getLimitPrice())
                .quantity(entity.getQuantity())
                .status(entity.getStatus())
                .currentStep(entity.getCurrentStep())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .errorReason(entity.getErrorReason())
                .build();
    }
    
    /**
     * Create from domain model
     */
    public static MatchingSagaEntity fromDomain(MatchingSaga domain) {
        return MatchingSagaEntity.builder()
                .id(domain.getId())
                .orderId(domain.getOrderId())
                .stockSymbol(domain.getStockSymbol())
                .orderSide(domain.getOrderSide())
                .limitPrice(domain.getLimitPrice())
                .quantity(domain.getQuantity())
                .status(domain.getStatus())
                .currentStep(domain.getCurrentStep())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .completedAt(domain.getCompletedAt())
                .errorReason(domain.getErrorReason())
                .build();
    }
}

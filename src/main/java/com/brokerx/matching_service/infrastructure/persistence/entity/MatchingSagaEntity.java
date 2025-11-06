package com.brokerx.matching_service.infrastructure.persistence.entity;

import com.brokerx.matching_service.domain.model.MatchingSaga;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA Entity for Saga Choreography
 * Infrastructure layer - persistence concern
 */
@Entity
@Table(name = "matching_sagas", indexes = {
    @Index(name = "idx_saga_order_id", columnList = "order_id", unique = true),
    @Index(name = "idx_saga_status", columnList = "status"),
    @Index(name = "idx_saga_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingSagaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;
    
    @Column(name = "stock_symbol", nullable = false, length = 20)
    private String stockSymbol;
    
    @Column(name = "order_side", length = 10)
    private String orderSide;
    
    @Column(name = "limit_price", precision = 19, scale = 4)
    private BigDecimal limitPrice;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MatchingSaga.SagaStatus status;
    
    @Column(name = "current_step", length = 100)
    private String currentStep;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "error_reason", columnDefinition = "TEXT")
    private String errorReason;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (status == null) {
            status = MatchingSaga.SagaStatus.STARTED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

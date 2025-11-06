package com.brokerx.matching_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain model for Saga Choreography
 * Represents the state and data of a matching saga
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingSaga {
    
    private Long id;
    private Long orderId;
    private String stockSymbol;
    private String orderSide;        // Nouveau: BUY ou SELL
    private BigDecimal limitPrice;   // Nouveau: for compensation event
    private Integer quantity;        // Nouveau: for compensation event
    private SagaStatus status;
    private String currentStep;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private String errorReason;
    
    public enum SagaStatus {
        STARTED,           // Saga started
        MATCHING,          // In matching process
        MATCHED,           // Match found, waiting for confirmation
        WALLET_UPDATED,    // Wallet updated
        ORDER_COMPLETED,   // Order service updated
        COMPLETED,         // Saga completed successfully
        COMPENSATING,      // In compensation (rollback)
        COMPENSATED,       // Compensation completed
        FAILED             // Definitive failure
    }
    
    /**
     * Advance the saga to the next step
     */
    public void advanceToStep(SagaStatus newStatus, String step) {
        this.status = newStatus;
        this.currentStep = step;
        this.updatedAt = Instant.now();
        
        if (newStatus == SagaStatus.COMPLETED || newStatus == SagaStatus.FAILED) {
            this.completedAt = Instant.now();
        }
    }
    
    /**
     * Start the compensation (rollback)
     */
    public void startCompensation(String reason) {
        this.status = SagaStatus.COMPENSATING;
        this.errorReason = reason;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Mark the saga as compensated
     */
    public void markAsCompensated() {
        this.status = SagaStatus.COMPENSATED;
        this.updatedAt = Instant.now();
        this.completedAt = Instant.now();
    }
    
    /**
     * Mark the saga as failed
     */
    public void markAsFailed(String reason) {
        this.status = SagaStatus.FAILED;
        this.errorReason = reason;
        this.updatedAt = Instant.now();
        this.completedAt = Instant.now();
    }
    
    /**
     * Check if the saga needs compensation
     */
    public boolean needsCompensation() {
        return this.status == SagaStatus.COMPENSATING;
    }
    
    /**
     * Check if the saga is terminal (success or failure)
     */
    public boolean isTerminal() {
        return this.status == SagaStatus.COMPLETED || 
               this.status == SagaStatus.FAILED || 
               this.status == SagaStatus.COMPENSATED;
    }
}

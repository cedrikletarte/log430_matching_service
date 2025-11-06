package com.brokerx.matching_service.application.port.out;

import com.brokerx.matching_service.domain.model.MatchingSaga;

import java.util.List;
import java.util.Optional;

/**
 * Port for Saga persistence
 * Domain interface (hexagonal architecture)
 */
public interface SagaPort {
    
    /**
     * Create a new saga
     */
    MatchingSaga createSaga(MatchingSaga saga);
    
    /**
     * Update an existing saga
     */
    MatchingSaga updateSaga(MatchingSaga saga);
    
    /**
     * Retrieve a saga by ID
     */
    Optional<MatchingSaga> findById(Long id);
    
    /**
     * Retrieve a saga by orderId
     */
    Optional<MatchingSaga> findByOrderId(Long orderId);
    
    /**
     * Retrieve all sagas needing compensation
     */
    List<MatchingSaga> findSagasNeedingCompensation();
    
    /**
     * Retrieve all active sagas (non-terminal)
     */
    List<MatchingSaga> findActiveSagas();
}

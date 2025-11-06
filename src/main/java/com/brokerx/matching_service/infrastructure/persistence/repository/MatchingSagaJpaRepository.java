package com.brokerx.matching_service.infrastructure.persistence.repository;

import com.brokerx.matching_service.domain.model.MatchingSaga;
import com.brokerx.matching_service.infrastructure.persistence.entity.MatchingSagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for Matching Sagas
 * Infrastructure layer
 */
@Repository
public interface MatchingSagaJpaRepository extends JpaRepository<MatchingSagaEntity, Long> {
    
    /**
     * Fetch saga by orderId
     */
    Optional<MatchingSagaEntity> findByOrderId(Long orderId);
    
    /**
     * Fetch sagas in compensation
     */
    List<MatchingSagaEntity> findByStatus(MatchingSaga.SagaStatus status);
    
    /**
     * Fetch active sagas (non-terminal)
     */
    @Query("SELECT s FROM MatchingSagaEntity s WHERE s.status NOT IN ('COMPLETED', 'FAILED', 'COMPENSATED')")
    List<MatchingSagaEntity> findActiveSagas();
    
    /**
     * Count sagas by status
     */
    long countByStatus(MatchingSaga.SagaStatus status);
}

package com.brokerx.matching_service.infrastructure.persistence.adapter;

import com.brokerx.matching_service.application.port.out.SagaPort;
import com.brokerx.matching_service.domain.model.MatchingSaga;
import com.brokerx.matching_service.infrastructure.persistence.entity.MatchingSagaEntity;
import com.brokerx.matching_service.infrastructure.persistence.mapper.MatchingSagaMapper;
import com.brokerx.matching_service.infrastructure.persistence.repository.MatchingSagaJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adapter for Saga persistence
 * Implements the SagaPort (hexagonal architecture)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaPersistenceAdapter implements SagaPort {
    
    private final MatchingSagaJpaRepository repository;
    private final MatchingSagaMapper mapper;
    
    @Override
    @Transactional
    public MatchingSaga createSaga(MatchingSaga saga) {
        log.info("Creating saga for orderId={}", saga.getOrderId());
        
        MatchingSagaEntity entity = MatchingSagaMapper.fromDomain(saga);
        MatchingSagaEntity saved = repository.save(entity);
        
        log.info("✅ Saga created: id={}, orderId={}, status={}", 
                saved.getId(), saved.getOrderId(), saved.getStatus());
        return mapper.toDomain(saved);
    }
    
    @Override
    @Transactional
    public MatchingSaga updateSaga(MatchingSaga saga) {
        log.debug("Updating saga: id={}, status={}", saga.getId(), saga.getStatus());
        
        MatchingSagaEntity entity = MatchingSagaMapper.fromDomain(saga);
        MatchingSagaEntity updated = repository.save(entity);
        
        return mapper.toDomain(updated);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<MatchingSaga> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<MatchingSaga> findByOrderId(Long orderId) {
        return repository.findByOrderId(orderId)
                .map(mapper::toDomain);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MatchingSaga> findSagasNeedingCompensation() {
        return repository.findByStatus(MatchingSaga.SagaStatus.COMPENSATING)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MatchingSaga> findActiveSagas() {
        return repository.findActiveSagas()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}

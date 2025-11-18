package com.brokerx.matching_service.application.port.in.useCase;

import com.brokerx.matching_service.application.port.in.command.ProcessOrderCommand;
import com.brokerx.matching_service.domain.model.Match;

import java.util.List;

public interface MatchOrderUseCase {
    
    /* Process a new order and attempt matching */
    List<Match> processOrder(ProcessOrderCommand command);
}

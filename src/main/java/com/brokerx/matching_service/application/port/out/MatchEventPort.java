package com.brokerx.matching_service.application.port.out;

import com.brokerx.matching_service.domain.model.event.MatchEventData;

public interface MatchEventPort {
    
    /* Publish a match event when orders are matched */
    void publishMatchEvent(MatchEventData eventData);
}

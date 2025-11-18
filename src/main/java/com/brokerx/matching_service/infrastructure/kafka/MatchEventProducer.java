package com.brokerx.matching_service.infrastructure.kafka;

import com.brokerx.matching_service.application.port.out.MatchEventPort;
import com.brokerx.matching_service.domain.model.event.MatchEventData;
import com.brokerx.matching_service.infrastructure.kafka.dto.OrderMatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer adapter - publishes OrderMatched events
 * This is an outbound adapter in hexagonal architecture
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchEventProducer implements MatchEventPort {

    @Value("${kafka.topic.order-matched}")
    private String orderMatchedTopic;
    
    private final KafkaTemplate<String, OrderMatchedEvent> kafkaTemplate;

    /* Publish an OrderMatched event */
    @Override
    public void publishMatchEvent(MatchEventData eventData) {
        OrderMatchedEvent event = new OrderMatchedEvent(
                eventData.buyOrderId(),
                eventData.sellOrderId(),
                eventData.stockSymbol(),
                eventData.quantity(),
                eventData.executionPrice()
        );

        try {
            kafkaTemplate.send(orderMatchedTopic, eventData.stockSymbol(), event);
            log.info("Published OrderMatched event to topic {}: buyOrder={}, sellOrder={}, qty={} @ {}",
                    orderMatchedTopic, event.buyOrderId(), event.sellOrderId(), event.quantity(), event.executionPrice());
        } catch (Exception e) {
            log.error("Failed to publish OrderMatched event: {}", e.getMessage(), e);
        }
    }
}

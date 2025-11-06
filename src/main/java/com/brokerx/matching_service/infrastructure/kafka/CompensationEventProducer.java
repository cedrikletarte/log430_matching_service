package com.brokerx.matching_service.infrastructure.kafka;

import com.brokerx.matching_service.application.port.out.CompensationEventPort;
import com.brokerx.matching_service.infrastructure.kafka.dto.MatchingFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer adapter for compensation events
 * Publish MATCHING_FAILED events to trigger rollback in order_service and wallet_service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationEventProducer implements CompensationEventPort {

    @Value("${kafka.topic.matching-failed:matching.failed}")
    private String matchingFailedTopic;
    
    private final KafkaTemplate<String, MatchingFailedEvent> kafkaTemplate;

    @Override
    public void publishMatchingFailed(MatchingFailedEventData eventData) {
        MatchingFailedEvent event = new MatchingFailedEvent(
                eventData.orderId(),
                eventData.stockSymbol(),
                eventData.side(),
                eventData.limitPrice(),
                eventData.quantity(),
                eventData.reason(),
                eventData.compensatedAt()
        );

        try {
            kafkaTemplate.send(matchingFailedTopic, eventData.stockSymbol(), event);
            log.warn("⚠️ Published MATCHING_FAILED event to topic {}: orderId={}, reason={}",
                    matchingFailedTopic, event.orderId(), event.reason());
        } catch (Exception e) {
            log.error("❌ Failed to publish MATCHING_FAILED event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish compensation event", e);
        }
    }
}

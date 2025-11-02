package com.brokerx.matching_service.infrastructure.kafka;

import com.brokerx.matching_service.application.port.in.MatchOrderUseCase;
import com.brokerx.matching_service.infrastructure.kafka.dto.OrderAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer adapter - listens to OrderAccepted events
 * This is an inbound adapter in hexagonal architecture
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    private final MatchOrderUseCase matchOrderUseCase;

    @KafkaListener(topics = "${kafka.topic.order-accepted}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderAccepted(OrderAcceptedEvent event) {
        log.info("📥 Received OrderAccepted event: orderId={}, symbol={}, side={}, price={}, qty={}",
                event.orderId(), event.stockSymbol(), event.side(), event.limitPrice(), event.quantity());

        try {
            // Convert event to command and process
            MatchOrderUseCase.ProcessOrderCommand command = new MatchOrderUseCase.ProcessOrderCommand(
                    event.orderId(),
                    event.stockSymbol(),
                    event.side(),
                    event.limitPrice(),
                    event.quantity()
            );

            matchOrderUseCase.processOrder(command);
            
            log.info("✅ Order {} processed successfully", event.orderId());
        } catch (Exception e) {
            log.error("❌ Failed to process OrderAccepted event for order {}: {}", 
                    event.orderId(), e.getMessage(), e);
        }
    }
}

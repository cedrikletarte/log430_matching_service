package com.brokerx.matching_service.infrastructure.kafka.consumer;

import com.brokerx.matching_service.domain.model.OrderSide;
import com.brokerx.matching_service.domain.service.MatchingEngine;
import com.brokerx.matching_service.infrastructure.kafka.dto.OrderCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for handling OrderCancelled events from order_service
 * Removes cancelled orders from the in-memory order book
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledEventConsumer {

    private final MatchingEngine matchingEngine;

    /* Listens for OrderCancelled events and removes them from the order book */
    @KafkaListener(
        topics = "${kafka.topic.order-cancelled:order.cancelled}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelled event: orderId={}, symbol={}, side={}",
                event.orderId(), event.stockSymbol(), event.side());

        try {
            OrderSide side = OrderSide.valueOf(event.side());
            
            boolean removed = matchingEngine.cancelOrder(event.orderId(), event.stockSymbol(), side);
            
            if (removed) {
                log.info("Successfully removed order {} from order book", event.orderId());
            } else {
                log.warn("Order {} not found in order book (may have been already matched)", event.orderId());
            }
        } catch (Exception e) {
            log.error("Failed to process OrderCancelled event for orderId {}: {}", 
                    event.orderId(), e.getMessage(), e);
            // Don't throw - order is already cancelled in order_service DB
            // Missing removal from order book is not critical (order will never match)
        }
    }
}

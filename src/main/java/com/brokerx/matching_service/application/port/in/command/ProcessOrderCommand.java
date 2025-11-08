package com.brokerx.matching_service.application.port.in.command;

import java.math.BigDecimal;

/**
 * Command for processing an order
 */
public record ProcessOrderCommand(
        Long orderId,
        String stockSymbol,
        String side,
        BigDecimal limitPrice,
        Integer quantity
) {}

package com.brokerx.matching_service.domain.service;

import com.brokerx.matching_service.domain.model.Match;
import com.brokerx.matching_service.domain.model.OrderBookEntry;
import com.brokerx.matching_service.domain.model.OrderSide;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Domain service implementing the matching engine logic
 * Uses price-time priority matching algorithm
 */
@Slf4j
@Service
public class MatchingEngine {

    // Order books per symbol: symbol -> side -> price-ordered list
    private final Map<String, Map<OrderSide, TreeMap<BigDecimal, Queue<OrderBookEntry>>>> orderBooks = 
        new ConcurrentHashMap<>();

    /* Add an order to the book and attempt to match it */
    public List<Match> addOrder(OrderBookEntry order) {
        log.info("Adding order to book: {} {} {} @ {} qty={}",
                order.getOrderId(), order.getSide(), order.getStockSymbol(), 
                order.getLimitPrice(), order.getQuantity());

        List<Match> matches = new ArrayList<>();
        
        // Try to match against opposite side
        OrderSide oppositeSide = order.getSide() == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY;
        matches = tryMatch(order, oppositeSide);

        // If order has remaining quantity, add to book
        if (order.getRemainingQuantity() > 0) {
            addToBook(order);
            log.info("Order {} added to book with remaining qty={}", 
                    order.getOrderId(), order.getRemainingQuantity());
        } else {
            log.info("Order {} fully matched", order.getOrderId());
        }
        return matches;
    }

    /* Try to match an order against the opposite side of the book */
    private List<Match> tryMatch(OrderBookEntry incomingOrder, OrderSide oppositeSide) {
        List<Match> matches = new ArrayList<>();
        
        TreeMap<BigDecimal, Queue<OrderBookEntry>> oppositePriceMap = getOrderBook(
                incomingOrder.getStockSymbol(), oppositeSide);

        while (incomingOrder.getRemainingQuantity() > 0 && !oppositePriceMap.isEmpty()) {
            // Get best price for opposite side
            BigDecimal bestPrice = oppositeSide == OrderSide.BUY 
                    ? oppositePriceMap.lastKey()  // Highest buy price
                    : oppositePriceMap.firstKey(); // Lowest sell price

            // Check if prices cross
            boolean pricesCross = oppositeSide == OrderSide.BUY
                    ? incomingOrder.getLimitPrice().compareTo(bestPrice) <= 0  // Incoming sell <= best buy
                    : incomingOrder.getLimitPrice().compareTo(bestPrice) >= 0; // Incoming buy >= best sell

            if (!pricesCross) {
                break; // No match possible
            }

            Queue<OrderBookEntry> ordersAtPrice = oppositePriceMap.get(bestPrice);
            
            while (incomingOrder.getRemainingQuantity() > 0 && !ordersAtPrice.isEmpty()) {
                OrderBookEntry restingOrder = ordersAtPrice.peek();
                
                int matchQty = Math.min(incomingOrder.getRemainingQuantity(), 
                                       restingOrder.getRemainingQuantity());

                // Determine execution price: always use the SELL price (lower price benefits both)
                // If incoming is SELL, use its price; if incoming is BUY, use resting SELL price
                BigDecimal executionPrice = incomingOrder.getSide() == OrderSide.SELL
                        ? incomingOrder.getLimitPrice()  // SELL is incoming, use its price
                        : restingOrder.getLimitPrice();   // BUY is incoming, use resting SELL price

                // Create match
                Match match = Match.builder()
                        .buyOrderId(incomingOrder.getSide() == OrderSide.BUY 
                                ? incomingOrder.getOrderId() 
                                : restingOrder.getOrderId())
                        .sellOrderId(incomingOrder.getSide() == OrderSide.SELL 
                                ? incomingOrder.getOrderId() 
                                : restingOrder.getOrderId())
                        .stockSymbol(incomingOrder.getStockSymbol())
                        .quantity(matchQty)
                        .executionPrice(executionPrice)
                        .build();

                matches.add(match);

                log.info("MATCH: Buy #{} Sell #{} {} shares @ {} of {}",
                        match.getBuyOrderId(), match.getSellOrderId(),
                        match.getQuantity(), match.getExecutionPrice(), match.getStockSymbol());

                // Update quantities
                incomingOrder.setRemainingQuantity(incomingOrder.getRemainingQuantity() - matchQty);
                restingOrder.setRemainingQuantity(restingOrder.getRemainingQuantity() - matchQty);

                // Remove resting order if fully filled
                if (restingOrder.getRemainingQuantity() == 0) {
                    ordersAtPrice.poll();
                    log.info("Resting order {} fully filled, removed from book", 
                            restingOrder.getOrderId());
                }
            }

            // Remove price level if empty
            if (ordersAtPrice.isEmpty()) {
                oppositePriceMap.remove(bestPrice);
            }
        }

        return matches;
    }

    /* Add order to the book */
    private void addToBook(OrderBookEntry order) {
        TreeMap<BigDecimal, Queue<OrderBookEntry>> priceMap = getOrderBook(
                order.getStockSymbol(), order.getSide());

        priceMap.computeIfAbsent(order.getLimitPrice(), k -> new LinkedList<>())
                .add(order);
    }

    /* Get or create order book for a symbol and side */
    private TreeMap<BigDecimal, Queue<OrderBookEntry>> getOrderBook(String symbol, OrderSide side) {
        orderBooks.putIfAbsent(symbol, new ConcurrentHashMap<>());
        
        Map<OrderSide, TreeMap<BigDecimal, Queue<OrderBookEntry>>> symbolBook = orderBooks.get(symbol);
        
        // BUY side: highest price first (descending)
        // SELL side: lowest price first (ascending)
        Comparator<BigDecimal> comparator = side == OrderSide.BUY 
                ? Comparator.reverseOrder() 
                : Comparator.naturalOrder();
        
        symbolBook.putIfAbsent(side, new TreeMap<>(comparator));
        
        return symbolBook.get(side);
    }

    /* Cancel an order from the book */
    public boolean cancelOrder(Long orderId, String symbol, OrderSide side) {
        TreeMap<BigDecimal, Queue<OrderBookEntry>> priceMap = getOrderBook(symbol, side);
        
        for (Queue<OrderBookEntry> queue : priceMap.values()) {
            if (queue.removeIf(entry -> entry.getOrderId().equals(orderId))) {
                log.info("Order {} cancelled from book", orderId);
                return true;
            }
        }
        
        return false;
    }
}

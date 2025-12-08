package com.positronic.agents;

import com.positronic.model.domain.*;
import java.time.LocalDateTime;
import java.util.*;

public class TradingAgent {
    
    private enum TradeState { DRAFT, PENDING, BOOKED, CONFIRMED, TERMINATED }
    
    private Map<String, TradeState> tradeStates = new HashMap<>();
    private List<LifecycleEvent> eventLog = new ArrayList<>();
    
    /**
     * Pre-trade validation with multi-layered checks
     */
    public ValidationResult validateTrade(Product product, Party buyer, Party seller) {
        System.out.println("[TradingAgent] Running pre-trade validation...");
        
        List<String> errors = new ArrayList<>();
        
        // Credit limit check
        if (!checkCreditLimit(buyer, seller)) {
            errors.add("Credit limit exceeded for counterparty");
        }
        
        // Market risk check  
        if (!checkMarketRisk(product)) {
            errors.add("Market risk limits breached");
        }
        
        //Operational capacity check
        if (!checkOperationalCapacity()) {
            errors.add("Operational capacity at maximum");
        }
        
        boolean isValid = errors.isEmpty();
        System.out.println("[TradingAgent] Validation result: " + (isValid ? "PASSED" : "FAILED - " + errors));
        
        return new ValidationResult(isValid, errors);
    }
    
    private boolean checkCreditLimit(Party buyer, Party seller) {
        // Simulate credit limit check against risk system
        return true; // Simplified
    }
    
    private boolean checkMarketRisk(Product product) {
        // Simulate market risk analysis
        return true; // Simplified
    }
    
    private boolean checkOperationalCapacity() {
        // Check if system can handle additional trade
        return true; // Simplified
    }
    
    /**
     * Trade enrichment with static data and pricing
     */
    public Product enrichTrade(Product product) {
        System.out.println("[TradingAgent] Enriching trade with static data and pricing...");
        
        // Add market data
        // Add pricing/valuation
        // Add booking instructions
        
        return product;
    }
    
    /**
     * Book trade with state machine management
     */
    public void bookTrade(Product product, Party buyer, Party seller) {
        ValidationResult validation = validateTrade(product, buyer, seller);
        
        if (!validation.isValid()) {
            throw new IllegalStateException("Trade validation failed: " + validation.getErrors());
        }
        
        // Enrich trade
        Product enrichedProduct = enrichTrade(product);
        
        // Transition state: DRAFT -> BOOKED
        tradeStates.put(product.getId(), TradeState.BOOKED);
        System.out.println("[TradingAgent] Trade " + product.getId() + " state: " + TradeState.BOOKED);
        
        // Record execution event
        ExecutionEvent execEvent = new ExecutionEvent(
            "EVT-" + UUID.randomUUID().toString().substring(0, 8),
            product.getId(),
            LocalDateTime.now(),
            "ELECTRONIC"
        );
        eventLog.add(execEvent);
        
        // Publish event to downstream consumers (async message queue)
        publishTradeBookedEvent(enrichedProduct);
        
        System.out.println("[TradingAgent] Successfully booked trade: " + product.getId());
    }
    
    private void publishTradeBookedEvent(Product product) {
        // Simulate publishing to Kafka/RabbitMQ
        System.out.println("[TradingAgent] Published TRADE_BOOKED event to message bus");
    }
    
    /**
     * Apply lifecycle event with state transitions
     */
    public void applyLifecycleEvent(String tradeId, LifecycleEvent event) {
        TradeState currentState = tradeStates.getOrDefault(tradeId, TradeState.DRAFT);
        
        System.out.println("[TradingAgent] Applying " + event.getEventType() + " to trade " + tradeId);
        
        // State machine logic
        if (event instanceof ConfirmationEvent) {
            if (currentState == TradeState.BOOKED) {
                tradeStates.put(tradeId, TradeState.CONFIRMED);
                System.out.println("[TradingAgent] State transition: BOOKED -> CONFIRMED");
            }
        } else if (event instanceof TerminationEvent) {
            tradeStates.put(tradeId, TradeState.TERMINATED);
            System.out.println("[TradingAgent] State transition: " + currentState + " -> TERMINATED");
        }
        
        eventLog.add(event);
    }
    
    public List<LifecycleEvent> getEventLog() {
        return new ArrayList<>(eventLog);
    }
}

class ValidationResult {
    private boolean valid;
    private List<String> errors;
    
    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }
    
    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
}

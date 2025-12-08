package com.positronic.model.domain;

import java.time.LocalDateTime;

public abstract class LifecycleEvent {
    protected String eventId;
    protected String tradeId;
    protected LocalDateTime eventTime;
    protected String eventType;
    
    public LifecycleEvent(String eventId, String tradeId, LocalDateTime eventTime, String eventType) {
        this.eventId = eventId;
        this.tradeId = tradeId;
        this.eventTime = eventTime;
        this.eventType = eventType;
    }
    
    public String getEventId() { return eventId; }
    public String getTradeId() { return tradeId; }
    public LocalDateTime getEventTime() { return eventTime; }
    public String getEventType() { return eventType; }
}

class ExecutionEvent extends LifecycleEvent {
    private String venue;
    
    public ExecutionEvent(String eventId, String tradeId, LocalDateTime eventTime, String venue) {
        super(eventId, tradeId, eventTime, "EXECUTION");
        this.venue = venue;
    }
}

class ConfirmationEvent extends LifecycleEvent {
    private String status; // MATCHED, MISMATCHED, DISPUTED
    
    public ConfirmationEvent(String eventId, String tradeId, LocalDateTime eventTime, String status) {
        super(eventId, tradeId, eventTime, "CONFIRMATION");
        this.status = status;
    }
    
    public String getStatus() { return status; }
}

class AmendmentEvent extends LifecycleEvent {
    private String field;
    private String oldValue;
    private String newValue;
    
    public AmendmentEvent(String eventId, String tradeId, LocalDateTime eventTime, String field, String oldValue, String newValue) {
        super(eventId, tradeId, eventTime, "AMENDMENT");
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}

class TerminationEvent extends LifecycleEvent {
    private LocalDateTime terminationDate;
    private double terminationPayment;
    
    public TerminationEvent(String eventId, String tradeId, LocalDateTime eventTime, LocalDateTime terminationDate, double terminationPayment) {
        super(eventId, tradeId, eventTime, "TERMINATION");
        this.terminationDate = terminationDate;
        this.terminationPayment = terminationPayment;
    }
}

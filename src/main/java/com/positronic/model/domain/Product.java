package com.positronic.model.domain;

import java.time.LocalDate;

public abstract class Product {
    protected String id;
    protected String assetClass;
    protected EconomicTerms economics;
    protected Party buyer;
    protected Party seller;
    
    public Product(String id, String assetClass) {
        this.id = id;
        this.assetClass = assetClass;
    }
    
    public String getId() { return id; }
    public String getAssetClass() { return assetClass; }
    public abstract String getProductType();
}

// Interest Rate Products
class InterestRateSwap extends Product {
    private SwapLeg payLeg;
    private SwapLeg receiveLeg;
    
    public InterestRateSwap(String id, SwapLeg payLeg, SwapLeg receiveLeg) {
        super(id, "InterestRate");
        this.payLeg = payLeg;
        this.receiveLeg = receiveLeg;
    }
    
    @Override
    public String getProductType() { return "InterestRateSwap"; }
    
    public SwapLeg getPayLeg() { return payLeg; }
    public SwapLeg getReceiveLeg() { return receiveLeg; }
}

class SwapLeg {
    private Rate rate;
    private Schedule schedule;
    private Notional notional;
    
    public SwapLeg(Rate rate, Schedule schedule, Notional notional) {
        this.rate = rate;
        this.schedule = schedule;
        this.notional = notional;
    }
    
    public Rate getRate() { return rate; }
    public Schedule getSchedule() { return schedule; }
    public Notional getNotional() { return notional; }
}

// FX Products
class FxOption extends Product {
    private Currency putCurrency;
    private Currency callCurrency;
    private double strikeRate;
    private LocalDate expiryDate;
    private String optionType; // CALL, PUT
    
    public FxOption(String id, Currency putCurrency, Currency callCurrency, double strikeRate, LocalDate expiryDate, String optionType) {
        super(id, "ForeignExchange");
        this.putCurrency = putCurrency;
        this.callCurrency = callCurrency;
        this.strikeRate = strikeRate;
        this.expiryDate = expiryDate;
        this.optionType = optionType;
    }
    
    @Override
    public String getProductType() { return "FxOption"; }
}

// Credit Products
class CreditDefaultSwap extends Product {
    private String referenceEntity;
    private double spread;
    private Notional notional;
    private Schedule schedule;
    
    public CreditDefaultSwap(String id, String referenceEntity, double spread, Notional notional, Schedule schedule) {
        super(id, "Credit");
        this.referenceEntity = referenceEntity;
        this.spread = spread;
        this.notional = notional;
        this.schedule = schedule;
    }
    
    @Override
    public String getProductType() { return "CreditDefaultSwap"; }
}

// Equity Products
class EquityOption extends Product {
    private String underlying;
    private double strike;
    private LocalDate expiryDate;
    private String optionType;
    private int quantity;
    
    public EquityOption(String id, String underlying, double strike, LocalDate expiryDate, String optionType, int quantity) {
        super(id, "Equity");
        this.underlying = underlying;
        this.strike = strike;
        this.expiryDate = expiryDate;
        this.optionType = optionType;
        this.quantity = quantity;
    }
    
    @Override
    public String getProductType() { return "EquityOption"; }
}

// Commodity Products
class CommoditySwap extends Product {
    private String commodityType;
    private SwapLeg fixedLeg;
    private SwapLeg floatLeg;
    
    public CommoditySwap(String id, String commodityType, SwapLeg fixedLeg, SwapLeg floatLeg) {
        super(id, "Commodity");
        this.commodityType = commodityType;
        this.fixedLeg = fixedLeg;
        this.floatLeg = floatLeg;
    }
    
    @Override
    public String getProductType() { return "CommoditySwap"; }
}

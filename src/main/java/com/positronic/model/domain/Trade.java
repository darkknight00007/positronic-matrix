package com.positronic.model.domain;

import java.time.LocalDate;
import java.util.List;

// Simple POJOs mimicking Legend generated classes
public class Trade {
    private String id;
    private String productType;
    private String assetClass;
    private double notional;
    private String currency;
    private List<String> parties;
    private LocalDate tradeDate;

    public Trade(String id, String productType, String assetClass, double notional, String currency, List<String> parties) {
        this.id = id;
        this.productType = productType;
        this.assetClass = assetClass;
        this.notional = notional;
        this.currency = currency;
        this.parties = parties;
        this.tradeDate = LocalDate.now();
    }

    public String getId() { return id; }
    public String getProductType() { return productType; }
    public String getAssetClass() { return assetClass; }
    public double getNotional() { return notional; }
    public String getCurrency() { return currency; }
    public List<String> getParties() { return parties; }
    public LocalDate getTradeDate() { return tradeDate; }
    
    @Override
    public String toString() {
        return "Trade{id='" + id + "', type='" + productType + "'}";
    }
}

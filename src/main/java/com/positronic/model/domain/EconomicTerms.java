package com.positronic.model.domain;

import java.time.LocalDate;
import java.util.List;

public class EconomicTerms {
    private Notional notional;
    private Schedule schedule;
    private Rate rate;
    
    public EconomicTerms(Notional notional, Schedule schedule, Rate rate) {
        this.notional = notional;
        this.schedule = schedule;
        this.rate = rate;
    }
    
    public Notional getNotional() { return notional; }
    public Schedule getSchedule() { return schedule; }
    public Rate getRate() { return rate; }
}

class Notional {
    private double amount;
    private Currency currency;
    
    public Notional(double amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }
    
    public double getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
}

class Schedule {
    private LocalDate effectiveDate;
    private LocalDate maturityDate;
    private String frequency; // ANNUAL, SEMI_ANNUAL, QUARTERLY, MONTHLY
    private DayCountFraction dayCount;
    
    public Schedule(LocalDate effectiveDate, LocalDate maturityDate, String frequency, DayCountFraction dayCount) {
        this.effectiveDate = effectiveDate;
        this.maturityDate = maturityDate;
        this.frequency = frequency;
        this.dayCount = dayCount;
    }
    
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public String getFrequency() { return frequency; }
    public DayCountFraction getDayCount() { return dayCount; }
}

class Rate {
    private RateType type;
    private Double fixedRate;
    private String floatingIndex; // SOFR, EURIBOR, etc.
    private Double spread;
    
    public Rate(RateType type, Double fixedRate, String floatingIndex, Double spread) {
        this.type = type;
        this.fixedRate = fixedRate;
        this.floatingIndex = floatingIndex;
        this.spread = spread;
    }
    
    public RateType getType() { return type; }
    public Double getFixedRate() { return fixedRate; }
    public String getFloatingIndex() { return floatingIndex; }
    public Double getSpread() { return spread; }
}

enum RateType { FIXED, FLOATING }
enum Currency { USD, EUR, GBP, JPY, CHF }
enum DayCountFraction { ACT_360, ACT_365, THIRTY_360 }

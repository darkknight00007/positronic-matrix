package com.positronic.model.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

// Regulatory Reports
public class RegulatoryReport {
    private String reportId;
    private String tradeId;
    private String regime; // CFTC_PART_43, CFTC_PART_45, EMIR
    private Map<String, Object> fields;
    private LocalDateTime generatedTime;
    
    public RegulatoryReport(String reportId, String tradeId, String regime, Map<String, Object> fields) {
        this.reportId = reportId;
        this.tradeId = tradeId;
        this.regime = regime;
        this.fields = fields;
        this.generatedTime = LocalDateTime.now();
    }
    
    public String getReportId() { return reportId; }
    public String getRegime() { return regime; }
    public Map<String, Object> getFields() { return fields; }
}

// Settlement Instructions
class SettlementInstruction {
    private String instructionId;
    private String tradeId;
    private LocalDate settlementDate;
    private double amount;
    private Currency currency;
    private Account payerAccount;
    private Account receiverAccount;
    private String status; // PENDING, SETTLED, FAILED
    
    public SettlementInstruction(String instructionId, String tradeId, LocalDate settlementDate, 
                                 double amount, Currency currency, Account payerAccount, Account receiverAccount) {
        this.instructionId = instructionId;
        this.tradeId = tradeId;
        this.settlementDate = settlementDate;
        this.amount = amount;
        this.currency = currency;
        this.payerAccount = payerAccount;
        this.receiverAccount = receiverAccount;
        this.status = "PENDING";
    }
    
    public String getInstructionId() { return instructionId; }
    public double getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

// Confirmation Document
class ConfirmationDocument {
    private String confirmId;
    private String tradeId;
    private String format; // FPML, ISDA
    private String content;
    private String status; // SENT, MATCHED, DISPUTED
    
    public ConfirmationDocument(String confirmId, String tradeId, String format, String content) {
        this.confirmId = confirmId;
        this.tradeId = tradeId;
        this.format = format;
        this.content = content;
        this.status = "SENT";
    }
    
    public String getConfirmId() { return confirmId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

// Ledger Entry
class LedgerEntry {
    private String entryId;
    private String ledgerType; // TRADE, POSITION, CASH, COLLATERAL
    private LocalDateTime timestamp;
    private String tradeId;
    private double debit;
    private double credit;
    private Currency currency;
    
    public LedgerEntry(String entryId, String ledgerType, String tradeId, double debit, double credit, Currency currency) {
        this.entryId = entryId;
        this.ledgerType = ledgerType;
        this.timestamp = LocalDateTime.now();
        this.tradeId = tradeId;
        this.debit = debit;
        this.credit = credit;
        this.currency = currency;
    }
    
    public String getLedgerType() { return ledgerType; }
    public double getDebit() { return debit; }
    public double getCredit() { return credit; }
}

// Margin Sensitivity
class Sensitivity {
    private String sensitivityType; // DELTA, VEGA, CURVATURE
    private String riskBucket;
    private double value;
    
    public Sensitivity(String sensitivityType, String riskBucket, double value) {
        this.sensitivityType = sensitivityType;
        this.riskBucket = riskBucket;
        this.value = value;
    }
    
    public String getSensitivityType() { return sensitivityType; }
    public String getRiskBucket() { return riskBucket; }
    public double getValue() { return value; }
}

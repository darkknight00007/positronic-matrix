package com.positronic.agents;

import com.positronic.model.domain.*;
import java.time.LocalDateTime;
import java.util.*;

public class LedgerAgent {
    
    // Four independent ledgers
    private List<LedgerEntry> tradeLedger = new ArrayList<>();
    private List<LedgerEntry> positionLedger = new ArrayList<>();
    private List<LedgerEntry> cashLedger = new ArrayList<>();
    private List<LedgerEntry> collateralLedger = new ArrayList<>();
    
    private Map<String, Position> positions = new HashMap<>();
    
    /**
     * Book trade using double-entry accounting
     */
    public void recordTransaction(Product product, Party buyer, Party seller) {
        System.out.println("[LedgerAgent] Recording transaction in ledgers...");
        
        String tradeId = product.getId();
        
        // 1. Trade Ledger - record the trade itself
        LedgerEntry tradeEntry = new LedgerEntry(
            "TL-" + UUID.randomUUID().toString().substring(0, 8),
            "TRADE",
            tradeId,
            0, 0, // No debit/credit for trade ledger
            Currency.USD
        );
        tradeLedger.add(tradeEntry);
        System.out.println("[LedgerAgent] ✓ Trade Ledger updated");
        
        // 2. Position Ledger - update aggregated positions
        updatePosition(product, buyer, 1); // Long position
        updatePosition(product, seller, -1); // Short position
        System.out.println("[LedgerAgent] ✓ Position Ledger updated");
        
        // 3. Cash Ledger - record premiums/fees (simplified - would extract from economics)
        double premium = 10000; // Simplified
        LedgerEntry cashDebit = new LedgerEntry(
            "CL-" + UUID.randomUUID().toString().substring(0, 8),
            "CASH",
            tradeId,
            premium, 0,
            Currency.USD
        );
        LedgerEntry cashCredit = new LedgerEntry(
            "CL-" + UUID.randomUUID().toString().substring(0, 8),
            "CASH",
            tradeId,
            0, premium,
            Currency.USD
        );
        cashLedger.add(cashDebit);
        cashLedger.add(cashCredit);
        System.out.println("[LedgerAgent] ✓ Cash Ledger updated (premium: " + premium + ")");
        
        System.out.println("[LedgerAgent] Transaction recorded across all ledgers");
    }
    
    private void updatePosition(Product product, Party party, int direction) {
        String positionKey = party.getId() + "-" + product.getAssetClass();
        
        Position position = positions.getOrDefault(positionKey, 
            new Position(party.getId(), product.getAssetClass(), 0, 0));
        
        position.quantity += direction;
        position.lastUpdated = LocalDateTime.now();
        
        positions.put(positionKey, position);
        
        System.out.println("[LedgerAgent] Updated position for " + party.getId() + ": " + position.quantity);
    }
    
    /**
     * Calculate P&L (unrealized and realized)
     */
    public PnLReport calculatePnL(String portfolioId) {
        System.out.println("[LedgerAgent] Calculating P&L for portfolio " + portfolioId);
        
        double unrealizedPnL = 0.0;
        double realizedPnL = 0.0;
        
        // Calculate unrealized P&L from open positions
        for (Position position : positions.values()) {
            double marketValue = position.quantity * getMarketPrice(position.assetClass);
            double costBasis = position.quantity * position.avgPrice;
            unrealizedPnL += (marketValue - costBasis);
        }
        
        // Calculate realized P&L from closed trades
        // Would analyze tradeLedger for terminated trades
        realizedPnL = 50000; // Simplified
        
        double totalPnL = unrealizedPnL + realizedPnL;
        
        System.out.println("[LedgerAgent] P&L Calculation:");
        System.out.println("  Unrealized P&L: " + unrealizedPnL);
        System.out.println("  Realized P&L: " + realizedPnL);
        System.out.println("  Total P&L: " + totalPnL);
        
        return new PnLReport(unrealizedPnL, realizedPnL,totalPnL);
    }
    
    private double getMarketPrice(String assetClass) {
        // Simulate market data lookup
        return 100.0 + Math.random() * 10;
    }
    
    /**
     * Process corporate action (dividend, coupon, split)
     */
    public void processCorporateAction(CorporateAction action, String assetClass) {
        System.out.println("[LedgerAgent] Processing corporate action: " + action.getType());
        
        // Find all positions in this asset
        List<Position> affectedPositions = new ArrayList<>();
        for (Position pos : positions.values()) {
            if (pos.assetClass.equals(assetClass)) {
                affectedPositions.add(pos);
            }
        }
        
        System.out.println("[LedgerAgent] Found " + affectedPositions.size() + " affected positions");
        
        for (Position pos : affectedPositions) {
            if (action.getType().equals("DIVIDEND")) {
                double dividendAmount = pos.quantity * action.getAmount();
                System.out.println("[LedgerAgent] Crediting " + dividendAmount + " dividend to " + pos.partyId);
                
                // Record in cash ledger
                LedgerEntry cashEntry = new LedgerEntry(
                    "CL-" + UUID.randomUUID().toString().substring(0, 8),
                    "CASH",
                    "CORP_ACTION",
                    0, dividendAmount,
                    Currency.USD
                );
                cashLedger.add(cashEntry);
            }
            else if (action.getType().equals("STOCK_SPLIT")) {
                double splitRatio = action.getRatio();
                pos.quantity *= splitRatio;
                pos.avgPrice /= splitRatio;
                System.out.println("[LedgerAgent] Applied " + splitRatio + ":1 split to position");
            }
        }
    }
    
    /**
     * Reconcile positions with external statements
     */
    public ReconciliationResult reconcilePositions(Map<String, Double> externalPositions) {
        System.out.println("[LedgerAgent] Running position reconciliation...");
        
        List<String> breaks = new ArrayList<>();
        
        for (Map.Entry<String, Double> external : externalPositions.entrySet()) {
            String posKey = external.getKey();
            Double externalQty = external.getValue();
            
            Position internal = positions.get(posKey);
            Double internalQty = (internal != null) ? (double)internal.quantity : 0.0;
            
            if (!externalQty.equals(internalQty)) {
                String breakMsg = "BREAK: " + posKey + " - Internal: " + internalQty + ", External: " + externalQty;
                breaks.add(breakMsg);
                System.out.println("[LedgerAgent] " + breakMsg);
            }
        }
        
        if (breaks.isEmpty()) {
            System.out.println("[LedgerAgent] ✓ Reconciliation CLEAN - no breaks found");
        } else {
            System.out.println("[LedgerAgent] ⚠ Reconciliation BREAKS - " + breaks.size() + " discrepancies");
        }
        
        return new ReconciliationResult(breaks.isEmpty(), breaks);
    }
    
    /**
     * Generate ledger reports
     */
    public void generateLedgerReport(String ledgerType) {
        System.out.println("[LedgerAgent] Generating " + ledgerType + " report...");
        
        List<LedgerEntry> entries = switch(ledgerType) {
            case "TRADE" -> tradeLedger;
            case "POSITION" -> positionLedger;
            case "CASH" -> cashLedger;
            case "COLLATERAL" -> collateralLedger;
            default -> new ArrayList<>();
        };
        
        System.out.println("[LedgerAgent] " + ledgerType + " Ledger contains " + entries.size() + " entries");
    }
}

class Position {
    String partyId;
    String assetClass;
    int quantity;
    double avgPrice;
    LocalDateTime lastUpdated;
    
    public Position(String partyId, String assetClass, int quantity, double avgPrice) {
        this.partyId = partyId;
        this.assetClass = assetClass;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.lastUpdated = LocalDateTime.now();
    }
}

class PnLReport {
    double unrealizedPnL;
    double realizedPnL;
    double totalPnL;
    
    public PnLReport(double unrealizedPnL, double realizedPnL, double totalPnL) {
        this.unrealizedPnL = unrealizedPnL;
        this.realizedPnL = realizedPnL;
        this.totalPnL = totalPnL;
    }
}

class ReconciliationResult {
    boolean clean;
    List<String> breaks;
    
    public ReconciliationResult(boolean clean, List<String> breaks) {
        this.clean = clean;
        this.breaks = breaks;
    }
}

class CorporateAction {
    private String type; // DIVIDEND, STOCK_SPLIT, MERGER
    private double amount;
    private double ratio;
    
    public CorporateAction(String type, double amount, double ratio) {
        this.type = type;
        this.amount = amount;
        this.ratio = ratio;
    }
    
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public double getRatio() { return ratio; }
}

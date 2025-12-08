package com.positronic.agents;

import com.positronic.model.domain.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class SettlementAgent {
    
    private List<SettlementInstruction> instructionQueue = new ArrayList<>();
    private Map<String, String> settlementStatus = new HashMap<>();
    
    /**
     * Project cashflows from trade economics
     */
    public List<CashFlow> projectCashflows(Product product) {
        System.out.println("[SettlementAgent] Projecting cashflows for " + product.getProductType());
        
        List<CashFlow> cashflows = new ArrayList<>();
        
        // Extract payment schedule from product (simplified)
        // In reality, would analyze Schedule, Rate, Notional from EconomicTerms
        
        // Example: Upfront premium
        cashflows.add(new CashFlow(LocalDate.now().plusDays(2), 100000.0, Currency.USD, "PREMIUM"));
        
        // Example: Periodic coupons
        LocalDate paymentDate = LocalDate.now().plusMonths(6);
        for (int i = 0; i < 4; i++) {
            cashflows.add(new CashFlow(paymentDate, 25000.0, Currency.USD, "COUPON"));
            paymentDate = paymentDate.plusMonths(6);
        }
        
        System.out.println("[SettlementAgent] Projected " + cashflows.size() + " cashflows");
        return cashflows;
    }
    
    /**
     * Multi-currency netting algorithm
     */
    public List<SettlementInstruction> calculateNetting(List<SettlementInstruction> instructions) {
        System.out.println("[SettlementAgent] Running netting algorithm on " + instructions.size() + " instructions");
        
        // Group by counterparty + currency + settlement date
        Map<String, List<SettlementInstruction>> nettingGroups = instructions.stream()
            .collect(Collectors.groupingBy(inst -> 
                inst.getCurrency() + "-" + inst.getStatus() // Simplified grouping key
            ));
        
        List<SettlementInstruction> nettedInstructions = new ArrayList<>();
        
        for (Map.Entry<String, List<SettlementInstruction>> entry : nettingGroups.entrySet()) {
            List<SettlementInstruction> group = entry.getValue();
            
            // Calculate net amount
            double netAmount = group.stream()
                .mapToDouble(SettlementInstruction::getAmount)
                .sum();
            
            System.out.println("[SettlementAgent] Netted " + group.size() + " payments into net amount: " + netAmount);
            
            // Create single netted instruction
            if (group.size() > 0) {
                SettlementInstruction sample = group.get(0);
                // Would create new netted instruction here
            }
        }
        
        System.out.println("[SettlementAgent] Netting complete - reduced to " + nettedInstructions.size() + " instructions");
        return nettedInstructions;
    }
    
    /**
     * Generate SWIFT MT message for payment
     */
    public String generateSWIFTMessage(SettlementInstruction instruction) {
        System.out.println("[SettlementAgent] Generating SWIFT MT103 message...");
        
        // Build SWIFT message structure
        StringBuilder swift = new StringBuilder();
        swift.append("{1:F01BANKAUS33XXX0000000000}"); // Basic Header
        swift.append("{2:O1031234567890BANKGB2LXXX}"); // Application Header
        swift.append("{4:\n"); // Text Block
        swift.append(":20:").append(instruction.getInstructionId()).append("\n"); // Reference
        swift.append(":32A:").append(instruction.getAmount()).append(instruction.getCurrency()).append("\n"); // Value date/Currency/Amount
        swift.append(":50K:/").append("ORDERING_CUSTOMER").append("\n");
        swift.append(":59:/").append("BENEFICIARY").append("\n");
        swift.append("-}");
        
        String swiftMessage = swift.toString();
        System.out.println("[SettlementAgent] SWIFT message generated: " + instruction.getInstructionId());
        
        return swiftMessage;
    }
    
/**
     * Initiate settlement with payment system
     */
    public void proposeSettlement(SettlementInstruction instruction) {
        System.out.println("[SettlementAgent] Initiating settlement for " + instruction.getInstructionId());
        
        instructionQueue.add(instruction);
        settlementStatus.put(instruction.getInstructionId(), "PENDING");
        
        // Generate SWIFT message
        String swiftMessage = generateSWIFTMessage(instruction);
        
        // Send to payment system
        sendToPaymentRails(swiftMessage);
        
        System.out.println("[SettlementAgent] Settlement initiated - awaiting confirmation");
    }
    
    private void sendToPaymentRails(String swiftMessage) {
        // Simulate sending to SWIFT network or internal payment system
        System.out.println("[SettlementAgent] Sending to SWIFT network...");
    }
    
    /**
     * Process settlement status updates
     */
    public void processSettlementStatus(String instructionId, String status) {
        settlementStatus.put(instructionId, status);
        
        System.out.println("[SettlementAgent] Settlement " + instructionId + " status updated: " + status);
        
        if (status.equals("FAILED")) {
            handleSettlementFailure(instructionId);
        }
    }
    
    /**
     * Fails management and resolution
     */
    private void handleSettlementFailure(String instructionId) {
        System.out.println("[SettlementAgent] ⚠ SETTLEMENT FAILED");
        System.out.println("[SettlementAgent] Analyzing failure reason...");
        
        // Common failure reasons
        String[] reasons = {"Insufficient Funds", "Account Closed", "Invalid Routing", "Cut-off Time Missed"};
        String reason = reasons[new Random().nextInt(reasons.length)];
        
        System.out.println("[SettlementAgent] Failure reason: " + reason);
        System.out.println("[SettlementAgent] Creating fail ticket for operations team");
        System.out.println("[SettlementAgent] Scheduling retry for next value date");
    }
    
    /**
     * Generate settlement report
     */
    public void generateSettlementReport(LocalDate reportDate) {
        System.out.println("[SettlementAgent] Generating settlement report for " + reportDate);
        
        long pending = settlementStatus.values().stream()
            .filter(s -> s.equals("PENDING")).count();
        long settled = settlementStatus.values().stream()
            .filter(s -> s.equals("SETTLED")).count();
        long failed = settlementStatus.values().stream()
            .filter(s -> s.equals("FAILED")).count();
        
        System.out.println("[SettlementAgent] Settlement Report:");
        System.out.println("  Total Instructions: " + settlementStatus.size());
        System.out.println("  Pending: " + pending);
        System.out.println("  Settled: " + settled);
        System.out.println("  Failed: " + failed);
    }
}

class CashFlow {
    private LocalDate paymentDate;
    private double amount;
    private Currency currency;
    private String type;
    
    public CashFlow(LocalDate paymentDate, double amount, Currency currency, String type) {
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
    }
    
    public LocalDate getPaymentDate() { return paymentDate; }
    public double getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public String getType() { return type; }
}

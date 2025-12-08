package com.positronic.agents;

import com.positronic.model.domain.*;
import java.util.*;

public class ConfirmationAgent {
    
    private Map<String, ConfirmationDocument> outboundConfirms = new HashMap<>();
    private Map<String, ConfirmationDocument> inboundConfirms = new HashMap<>();
    private List<String> disputes = new ArrayList<>();
    
    /**
     * Determine if trade requires confirmation
     */
    public boolean isConfirmable(Product product, Party buyer, Party seller) {
        System.out.println("[ConfirmationAgent] Checking confirmability for " + product.getProductType());
        
        // Certain products exempt from confirmation
        if (product.getProductType().contains("Cash")) {
            System.out.println("[ConfirmationAgent] Cash product - confirmation not required");
            return false;
        }
        
        // Interdealer trades always require confirmation 
        if (buyer.getId().contains("DEALER") && seller.getId().contains("DEALER")) {
            return true;
        }
        
        return true; // Default: confirmable
    }
    
    /**
     * Generate ISDA/FpML confirmation document
     */
 public ConfirmationDocument generateConfirmation(Product product, String uti, Party buyer, Party seller) {
        if (!isConfirmable(product, buyer, seller)) {
            return null;
        }
        
        System.out.println("[ConfirmationAgent] Generating FpML confirmation document...");
        
        // Build FpML XML structure (simplified - would use actual FpML library)
        StringBuilder fpmlContent = new StringBuilder();
        fpmlContent.append("<Fpm lMessage>");
        fpmlContent.append("  <trade>");
        fpmlContent.append("    <tradeHeader>");
        fpmlContent.append("      <uniqueTransactionIdentifier>").append(uti).append("</uniqueTransactionIdentifier>");
        fpmlContent.append("      <tradeDate>").append(java.time.LocalDate.now()).append("</tradeDate>");
        fpmlContent.append("    </tradeHeader>");
        fpmlContent.append("    <product>").append(product.getProductType()).append("</product>");
        fpmlContent.append("  </trade>");
        fpmlContent.append("</FpmlMessage>");
        
        ConfirmationDocument confirm = new ConfirmationDocument(
            "CONF-" + UUID.randomUUID().toString().substring(0, 8),
            product.getId(),
            "FPML",
            fpmlContent.toString()
        );
        
        outboundConfirms.put(product.getId(), confirm);
        System.out.println("[ConfirmationAgent] Generated confirmation " + confirm.getConfirmId());
        
        // Simulate sending to electronic confirmation platform (MarkitServ/AcadiaSoft)
        sendToElectronicPlatform(confirm);
        
        return confirm;
    }
    
    private void sendToElectronicPlatform(ConfirmationDocument confirm) {
        System.out.println("[ConfirmationAgent] Sending to electronic confirmation platform");
        // Simulate integration with MarkitServ DTCC Deriv/SERV
    }
    
    /**
     * Matching engine - compare outbound vs inbound confirms
     */
    public void processInboundConfirmation(String tradeId, ConfirmationDocument inbound) {
        System.out.println("[ConfirmationAgent] Received inbound confirmation for trade " + tradeId);
        
        inboundConfirms.put(tradeId, inbound);
        
        // Check if we have outbound for this trade
        if (outboundConfirms.containsKey(tradeId)) {
            ConfirmationDocument outbound = outboundConfirms.get(tradeId);
            
            boolean matched = matchConfirmations(outbound, inbound);
            
            if (matched) {
                outbound.setStatus("MATCHED");
                inbound.setStatus("MATCHED");
                System.out.println("[ConfirmationAgent] ✓ MATCHED - Trade " + tradeId + " confirmed");
            } else {
                outbound.setStatus("DISPUTED");
                inbound.setStatus("DISPUTED");
                initiateDisputeWorkflow(tradeId, outbound, inbound);
            }
        }
    }
    
    private boolean matchConfirmations(ConfirmationDocument outbound, ConfirmationDocument inbound) {
        // Tolerance-based matching on economic terms
        // In reality, would parse FpML and compare critical fields
        return outbound.getConfirmId().hashCode() % 10 != 0; // 90% match rate simulation
    }
    
    /**
     * Dispute management workflow
     */
    private void initiateDisputeWorkflow(String tradeId, ConfirmationDocument outbound, ConfirmationDocument inbound) {
        System.out.println("[ConfirmationAgent] ⚠ MISMATCH DETECTED - Initiating dispute workflow");
        
        disputes.add(tradeId);
        
        // Create dispute record
        System.out.println("[ConfirmationAgent] Creating dispute case in workflow system");
        
        // Identify discrepancies
        System.out.println("[ConfirmationAgent] Discrepancy Analysis:");
        System.out.println("  - Outbound confirm hash: " + outbound.getConfirmId().hashCode());
        System.out.println("  - Inbound confirm hash: " + inbound.getConfirmId().hashCode());
        
        // Assign to operations team
        System.out.println("[ConfirmationAgent] Assigning to Operations team for manual review");
        
        // Set escalation timeline
        System.out.println("[ConfirmationAgent] Escalation timeline: T+1 if not resolved");
    }
    
    /**
     * Affirmation tracking for fund trades
     */
    public void trackAffirmation(String tradeId, String fundManager) {
        System.out.println("[ConfirmationAgent] Tracking affirmation from fund manager: " + fundManager);
        System.out.println("[ConfirmationAgent] Awaiting affirmation via Omgeo CTM/Central Trade Manager");
    }
    
    public List<String> getDisputedTrades() {
        return new ArrayList<>(disputes);
    }
}

package com.positronic.agents;

import com.positronic.model.domain.*;
import java.util.*;

public class TradeProcessingAgent {
    
    private Map<String, String> nettingSets = new HashMap<>();
    private Map<String, List<Product>> intercompanyMirrors = new HashMap<>();
    
    /**
     * Generate Unique Transaction Identifier (UTI)
     */
    public String generateUTI(Product product, Party buyer, Party seller) {
        // Format: [LEI]:[YYYYMMDD]-[RANDOM]
        String buyerLEI = buyer.getLei();
        String timestamp = java.time.LocalDate.now().toString().replace("-", "");
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        String uti = buyerLEI + ":" + timestamp + "-" + random;
        
        System.out.println("[ProcessingAgent] Generated UTI: " + uti);
        return uti;
    }
    
    /**
     * Detect and process intercompany trades
     */
    public List<Product> processIntercompany(Product product, Party buyer, Party seller) {
        System.out.println("[ProcessingAgent] Analyzing parties for intercompany relationship...");
        
        List<Product> trades = new ArrayList<>();
        trades.add(product);
        
        // Check if both parties belong to same entity group
        if (isIntercompany(buyer, seller)) {
            System.out.println("[ProcessingAgent] Detected INTERCOMPANY trade - generating mirror");
            
            // Generate mirror trade for internal accounting
            Product mirrorTrade = createMirrorTrade(product, buyer, seller);
            trades.add(mirrorTrade);
            
            // Store for reconciliation
            intercompanyMirrors.put(product.getId(), trades);
        }
        
        return trades;
    }
    
    private boolean isIntercompany(Party buyer, Party seller) {
        // Check if partyIDs belong to same legal entity group
        return buyer.getId().startsWith("ENTITY_") && seller.getId().startsWith("ENTITY_");
    }
    
    private Product createMirrorTrade(Product original, Party buyer, Party seller) {
        // Create reciprocal trade with swapped parties
        System.out.println("[ProcessingAgent] Creating mirror trade with reversed party roles");
        // In reality, this would deep clone and reverse the product
        return original; // Simplified
    }
    
    /**
     * Assign trade to netting set for collateral/margin purposes
     */
    public String assignNettingSet(Product product, Party buyer, Party seller) {
        String nettingSetId = calculateNettingSet(buyer, seller, product.getAssetClass());
        
        nettingSets.put(product.getId(), nettingSetId);
        System.out.println("[ProcessingAgent] Assigned trade " + product.getId() + " to netting set: " + nettingSetId);
        
        return nettingSetId;
    }
    
    private String calculateNettingSet(Party buyer, Party seller, String assetClass) {
        // Netting sets are typically defined by: counterparty + CSA agreement + asset class
        return "NS-" + buyer.getId() + "-" + seller.getId() + "-" + assetClass;
    }
    
    /**
     * Process block trade allocation
     */
    public List<Product> allocateBlockTrade(Product blockTrade, Map<String, Double> allocations) {
        System.out.println("[ProcessingAgent] Allocating block trade into " + allocations.size() + " child trades");
        
        List<Product> allocatedTrades = new ArrayList<>();
        
        for (Map.Entry<String, Double> entry : allocations.entrySet()) {
            String accountId = entry.getKey();
            Double allocationPct = entry.getValue();
            
            // Create allocated trade (simplified - would actually clone and scale notional)
            System.out.println("[ProcessingAgent] Allocated " + (allocationPct * 100) + "% to account " + accountId);
        }
        
        return allocatedTrades;
    }
    
    /**
     * Trigger portfolio reconciliation
     */
    public void triggerReconciliation(String portfolioId) {
        System.out.println("[ProcessingAgent] Triggering portfolio reconciliation for " + portfolioId);
        System.out.println("[ProcessingAgent] Reconciliation job queued for overnight batch");
    }
}

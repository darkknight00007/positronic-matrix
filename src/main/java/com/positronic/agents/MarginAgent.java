package com.positronic.agents;

import com.positronic.model.domain.*;
import java.util.*;

public class MarginAgent {
    
    private Map<String, List<Sensitivity>> portfolioSensitivities = new HashMap<>();
    private Map<String, MarginCall> activeMarginCalls = new HashMap<>();
    
    /**
     * Calculate ISDA SIMM Initial Margin
     */
    public SIMMResult calculatePortfolioMargin(String portfolioId, List<Product> trades) {
        System.out.println("[MarginAgent] Calculating ISDA SIMM for portfolio " + portfolioId);
        System.out.println("[MarginAgent] Analyzing " + trades.size() + " trades");
        
        // Step 1: Compute sensitivities (Delta, Vega, Curvature)
        List<Sensitivity> sensitivities = computeSensitivities(trades);
        portfolioSensitivities.put(portfolioId, sensitivities);
        
        // Step 2: Apply risk weights per ISDA SIMM methodology
        double weightedDelta = applySIMMWeights(sensitivities, "DELTA");
        double weightedVega = applySIMMWeights(sensitivities, "VEGA");
        double weightedCurvature = applySIMMWeights(sensitivities, "CURVATURE");
        
        // Step 3: Apply correlation matrices
        double deltaMargin = applyCorrelations(weightedDelta, "DELTA");
        double vegaMargin = applyCorrelations(weightedVega, "VEGA");
        double curvatureMargin = applyCorrelations(weightedCurvature, "CURVATURE");
        
        // Step 4: Aggregate across risk types
        double totalIM = Math.sqrt(
            deltaMargin * deltaMargin +
            vegaMargin * vegaMargin +
            curvatureMargin * curvatureMargin
        );
        
        System.out.println("[MarginAgent] ISDA SIMM Calculation:");
        System.out.println("  Delta Margin: $" + String.format("%,.2f", deltaMargin));
        System.out.println("  Vega Margin: $" + String.format("%,.2f", vegaMargin));
        System.out.println("  Curvature Margin: $" + String.format("%,.2f", curvatureMargin));
        System.out.println("  Total Initial Margin: $" + String.format("%,.2f", totalIM));
        
        return new SIMMResult(deltaMargin, vegaMargin, curvatureMargin, totalIM);
    }
    
    private List<Sensitivity> computeSensitivities(List<Product> trades) {
        System.out.println("[MarginAgent] Computing risk sensitivities...");
        
        List<Sensitivity> sensitivities = new ArrayList<>();
        
        for (Product trade : trades) {
            // Compute Delta (sensitivity to underlying price/rate)
            double delta = calculateDelta(trade);
            sensitivities.add(new Sensitivity("DELTA", getBucket(trade), delta));
            
            // Compute Vega (sensitivity to volatility)
            if (isOptionProduct(trade)) {
                double vega = calculateVega(trade);
                sensitivities.add(new Sensitivity("VEGA", getBucket(trade), vega));
            }
            
            // Compute Curvature (second-order sensitivity)
            double curvature = calculateCurvature(trade);
            sensitivities.add(new Sensitivity("CURVATURE", getBucket(trade), curvature));
        }
        
        System.out.println("[MarginAgent] Computed " + sensitivities.size() + " sensitivities");
        return sensitivities;
    }
    
    private double calculateDelta(Product trade) {
        // Simplified - in reality would use pricing models
        return 50000 + Math.random() * 100000;
    }
    
    private double calculateVega(Product trade) {
        return 10000 + Math.random() * 20000;
    }
    
    private double calculateCurvature(Product trade) {
        return 5000 + Math.random() * 10000;
    }
    
    private boolean isOptionProduct(Product trade) {
        return trade.getProductType().contains("Option");
    }
    
    private String getBucket(Product trade) {
        // SIMM risk buckets (simplified)
        return trade.getAssetClass() + "-Bucket1";
    }
    
    private double applySIMMWeights(List<Sensitivity> sensitivities, String riskType) {
        // Apply ISDA SIMM risk weights (simplified - actual weights are detailed in SIMM methodology)
        double weightedSum = sensitivities.stream()
            .filter(s -> s.getSensitivityType().equals(riskType))
            .mapToDouble(s -> s.getValue() * getSIMMWeight(s.getRiskBucket()))
            .sum();
        
        return weightedSum;
    }
    
    private double getSIMMWeight(String bucket) {
        // Simplified - actual SIMM weights vary by product class and tenor
        Map<String, Double> weights = Map.of(
            "InterestRate-Bucket1", 2.0,
            "ForeignExchange-Bucket1", 1.5,
            "Credit-Bucket1", 3.0,
            "Equity-Bucket1", 2.5,
            "Commodity-Bucket1", 3.5
        );
        return weights.getOrDefault(bucket, 2.0);
    }
    
    private double applyCorrelations(double weightedSensitivity, String riskType) {
        // Apply SIMM correlation matrices (simplified)
        double correlationFactor = 0.85; // Simplified correlation
        return weightedSensitivity * Math.sqrt(correlationFactor);
    }
    
    /**
     * Calculate Variation Margin (mark-to-market)
     */
    public double calculateVariationMargin(String portfolioId, double mtmValue, double collateralBalance) {
        System.out.println("[MarginAgent] Calculating Variation Margin...");
        
        double vm = mtmValue - collateralBalance;
        
        System.out.println("[MarginAgent] MTM Value: $" + String.format("%,.2f", mtmValue));
        System.out.println("[MarginAgent] Collateral Balance: $" + String.format("%,.2f", collateralBalance));
        System.out.println("[MarginAgent] Variation Margin: $" + String.format("%,.2f", vm));
        
        return vm;
    }
    
    /**
     * Generate margin call
     */
    public MarginCall generateMarginCall(String portfolioId, String counterparty, double imRequired, double vmRequired, double currentCollateral) {
        System.out.println("[MarginAgent] Evaluating margin requirement vs posted collateral...");
        
        double totalRequired = imRequired + vmRequired;
        double shortfall = totalRequired - currentCollateral;
        
        if (shortfall > 0) {
            System.out.println("[MarginAgent] ⚠ MARGIN CALL TRIGGERED");
            System.out.println("[MarginAgent] Shortfall: $" + String.format("%,.2f", shortfall));
            
            MarginCall call = new MarginCall(
                "MC-" + UUID.randomUUID().toString().substring(0, 8),
                portfolioId,
                counterparty,
                shortfall,
                java.time.LocalDateTime.now()
            );
            
            activeMarginCalls.put(call.getId(), call);
            
            System.out.println("[MarginAgent] Margin call " + call.getId() + " issued to " + counterparty);
            return call;
        } else {
            System.out.println("[MarginAgent] ✓ Collateral sufficient - no margin call required");
            return null;
        }
    }
    
    /**
     * Collateral optimization
     */
    public void optimizeCollateral(String portfolioId, List<CollateralAsset> availableAssets) {
        System.out.println("[MarginAgent] Running collateral optimization algorithm...");
        
        // Optimize for cheapest-to-deliver considering haircuts
        availableAssets.sort(Comparator.comparingDouble(CollateralAsset::getCost));
        
        System.out.println("[MarginAgent] Optimal collateral basket:");
        for (int i = 0; i < Math.min(3, availableAssets.size()); i++) {
            CollateralAsset asset = availableAssets.get(i);
            System.out.println("  " + (i+1) + ". " + asset.getType() + " (haircut: " + asset.getHaircut() + "%)");
        }
    }
    
    /**
     * Generate UMR regulatory margin report
     */
    public void produceRegulatoryMarginReport(String portfolioId) {
        System.out.println("[MarginAgent] Generating UMR/EMIR regulatory margin report...");
        
        SIMMResult simm = calculatePortfolioMargin(portfolioId, new ArrayList<>()); // Simplified
        
        System.out.println("[MarginAgent] Regulatory Margin Report:");
        System.out.println("  Regime: UMR (Uncleared Margin Rules)");
        System.out.println("  Portfolio: " + portfolioId);
        System.out.println("  Initial Margin Posted: $" + String.format("%,.2f", simm.totalIM));
        System.out.println("  Calculation Method: ISDA SIMM v2.6");
        System.out.println("  Report Date: " + java.time.LocalDate.now());
    }
}

class SIMMResult {
    double deltaMargin;
    double vegaMargin;
    double curvatureMargin;
    double totalIM;
    
    public SIMMResult(double deltaMargin, double vegaMargin, double curvatureMargin, double totalIM) {
        this.deltaMargin = deltaMargin;
        this.vegaMargin = vegaMargin;
        this.curvatureMargin = curvatureMargin;
        this.totalIM = totalIM;
    }
}

class MarginCall {
    private String id;
    private String portfolioId;
    private String counterparty;
    private double amount;
    private java.time.LocalDateTime issuedTime;
    
    public MarginCall(String id, String portfolioId, String counterparty, double amount, java.time.LocalDateTime issuedTime) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.counterparty = counterparty;
        this.amount = amount;
        this.issuedTime = issuedTime;
    }
    
    public String getId() { return id; }
}

class CollateralAsset {
    private String type; // CASH, GOVT_BOND, CORP_BOND
    private double value;
    private double haircut; // Percentage
    private double cost; // Opportunity cost
    
    public CollateralAsset(String type, double value, double haircut, double cost) {
        this.type = type;
        this.value = value;
        this.haircut = haircut;
        this.cost = cost;
    }
    
    public String getType() { return type; }
    public double getCost() { return cost; }
    public double getHaircut() { return haircut; }
}

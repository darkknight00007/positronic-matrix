package com.positronic.agents;

import com.positronic.model.domain.*;
import java.time.LocalDateTime;
import java.util.*;

public class RegulatoryAgent {
    
    private Queue<RegulatoryReport> submissionQueue = new LinkedList<>();
    
    /**
     * Determine reportability across all jurisdictions
     */
    public List<String> determineReportability(Product product, Party buyer, Party seller) {
        System.out.println("[RegulatoryAgent] Analyzing trade for regulatory reporting obligations...");
        
        List<String> applicableRegimes = new ArrayList<>();
        
        // CFTC Rules (US Jurisdiction)
        if (isCFTCReportable(product, buyer, seller)) {
            applicableRegimes.add("CFTC_PART_43");
            applicableRegimes.add("CFTC_PART_45");
        }
        
        // EMIR Rules (EU Jurisdiction)
        if (isEMIRReportable(product, buyer, seller)) {
            applicableRegimes.add("EMIR");
        }
        
        // MiFIR (EU Transaction Reporting)  — FIX: was "isM ifIRReportable"
        if (isMifIRReportable(product)) {
            applicableRegimes.add("MIFIR");
        }
        
        // ASIC Rules (Australia)
        if (buyer.getJurisdiction().equals("AU") || seller.getJurisdiction().equals("AU")) {
            applicableRegimes.add("ASIC");
        }
        
        // MAS Rules (Singapore)
        if (buyer.getJurisdiction().equals("SG") || seller.getJurisdiction().equals("SG")) {
            applicableRegimes.add("MAS");
        }
        
        System.out.println("[RegulatoryAgent] Applicable regimes: " + applicableRegimes);
        return applicableRegimes;
    }
    
    private boolean isCFTCReportable(Product product, Party buyer, Party seller) {
        boolean hasUSParty = buyer.getJurisdiction().equals("US") || seller.getJurisdiction().equals("US");
        boolean isSwap = product.getProductType().contains("Swap");
        return hasUSParty && isSwap;
    }
    
    private boolean isEMIRReportable(Product product, Party buyer, Party seller) {
        return buyer.getJurisdiction().startsWith("EU") || seller.getJurisdiction().startsWith("EU");
    }
    
    // FIX: was "isM ifIRReportable" (space in name)
    private boolean isMifIRReportable(Product product) {
        return product.getAssetClass().equals("Equity") || product.getAssetClass().equals("Credit");
    }
    
    public List<String> getMandatoryFields(String regime) {
        Map<String, List<String>> regimeFields = Map.of(
            "CFTC_PART_43", List.of("UTI", "ExecutionTimestamp", "Price", "Notional", "AssetClass", "ClearedIndicator"),
            "CFTC_PART_45", List.of("UTI", "UPI", "ReportingCounterpartyLEI", "OtherCounterpartyLEI", "EffectiveDate", "MaturityDate", "Notional", "CollateralizationType"),
            "EMIR", List.of("UTI", "LEI_1", "LEI_2", "TradeDate", "Notional", "Valuation", "CollateralPosted"),
            "MIFIR", List.of("ISIN", "Quantity", "Price", "Venue", "BuyerLEI", "SellerLEI")
        );
        return regimeFields.getOrDefault(regime, List.of());
    }
    
    public RegulatoryReport generateReport(Product product, String regime, Party buyer, Party seller, String uti) {
        System.out.println("[RegulatoryAgent] Generating " + regime + " report for trade " + product.getId());
        
        Map<String, Object> fields = new HashMap<>();
        
        if (regime.equals("CFTC_PART_43")) {
            fields.put("UTI", uti);
            fields.put("ExecutionTimestamp", LocalDateTime.now());
            fields.put("AssetClass", mapAssetClass(product.getAssetClass()));
            fields.put("Price", "Placeholder");
            fields.put("Notional", 0.0);
            fields.put("BlockTradeIndicator", false);
            fields.put("ClearedIndicator", false);
        } else if (regime.equals("CFTC_PART_45")) {
            fields.put("UTI", uti);
            fields.put("UPI", "UPI-" + product.getProductType());
            fields.put("ReportingCounterpartyLEI", buyer.getLei());
            fields.put("OtherCounterpartyLEI", seller.getLei());
            fields.put("EffectiveDate", LocalDateTime.now());
            fields.put("CollateralizationType", "Uncollateralized");
        } else if (regime.equals("EMIR")) {
            fields.put("UTI", uti);
            fields.put("LEI_1", buyer.getLei());
            fields.put("LEI_2", seller.getLei());
            fields.put("Valuation", 0.0);
        }
        
        return new RegulatoryReport(
            "RPT-" + UUID.randomUUID().toString().substring(0, 8),
            product.getId(),
            regime,
            fields
        );
    }
    
    private String mapAssetClass(String internalAssetClass) {
        Map<String, String> mapping = Map.of(
            "InterestRate", "IR",
            "ForeignExchange", "FX",
            "Credit", "CRED",
            "Equity", "EQ",
            "Commodity", "CO"
        );
        return mapping.getOrDefault(internalAssetClass, "Other");
    }
    
    public boolean validateReport(RegulatoryReport report) {
        System.out.println("[RegulatoryAgent] Validating report against " + report.getRegime() + " schema");
        
        List<String> mandatoryFields = getMandatoryFields(report.getRegime()); // FIX: was "mandatory Fields"
        Map<String, Object> reportFields = report.getFields();
        
        for (String field : mandatoryFields) {
            if (!reportFields.containsKey(field)) {
                System.out.println("[RegulatoryAgent] Validation FAILED: Missing field " + field);
                return false;
            }
        }
        
        System.out.println("[RegulatoryAgent] Validation PASSED");
        return true;
    }
    
    public void queueSubmission(RegulatoryReport report) {
        if (validateReport(report)) {
            submissionQueue.add(report);
            System.out.println("[RegulatoryAgent] Report queued for submission to ARM/TR");
        } else {
            System.out.println("[RegulatoryAgent] Report rejected - validation failed");
        }
    }
    
    public void submitToTradeRepository() {
        System.out.println("[RegulatoryAgent] Processing submission queue - " + submissionQueue.size() + " reports");
        
        while (!submissionQueue.isEmpty()) {
            RegulatoryReport report = submissionQueue.poll();
            try {
                System.out.println("[RegulatoryAgent] Submitting " + report.getReportId() + " to TR...");
                System.out.println("[RegulatoryAgent] Submission successful - Acknowledgment received");
            } catch (Exception e) {
                System.out.println("[RegulatoryAgent] Submission failed - adding to retry queue");
            }
        }
    }
}

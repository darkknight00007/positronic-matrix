package com.positronic.orchestration;

import com.positronic.agents.*;
import com.positronic.model.domain.Trade;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ControlPlane {

    private final TradingAgent tradingAgent;
    private final TradeProcessingAgent processingAgent;
    private final ConfirmationAgent confirmationAgent;
    private final SettlementAgent settlementAgent;
    private final RegulatoryAgent regulatoryAgent;
    private final LedgerAgent ledgerAgent;
    private final MarginAgent marginAgent;

    public ControlPlane() {
        this.tradingAgent = new TradingAgent();
        this.processingAgent = new TradeProcessingAgent();
        this.confirmationAgent = new ConfirmationAgent();
        this.settlementAgent = new SettlementAgent();
        this.regulatoryAgent = new RegulatoryAgent();
        this.ledgerAgent = new LedgerAgent();
        this.marginAgent = new MarginAgent();
    }

    public void handleTradeRequest(String productType, String assetClass, double notional, String currency, List<String> parties) {
        System.out.println("\n========== STARTING TRADE LIFECYCLE ==========");
        
        // 1. Trading Agent: Book the trade
        Trade trade = tradingAgent.bookTrade(productType, assetClass, notional, currency, parties);
        
        // 2. Processing Agent: Add IDs and check intercompany
        String contractId = processingAgent.generateContractId(trade);
        processingAgent.processIntercompany(trade);
        
        // 3. Parallel Execution of Functional Agents
        System.out.println("--- dispatching functional agents ---");
        
        CompletableFuture<Void> confirmTask = CompletableFuture.runAsync(() -> {
            confirmationAgent.generateConfirmation(trade);
        });

        CompletableFuture<Void> regTask = CompletableFuture.runAsync(() -> {
            List<String> regimes = regulatoryAgent.determineReportability(trade);
            for (String regime : regimes) {
                regulatoryAgent.generateSubmission(trade, regime);
            }
        });

        CompletableFuture<Void> settleTask = CompletableFuture.runAsync(() -> {
            settlementAgent.generatePaymentInstructions(trade);
            settlementAgent.proposeSettlement(trade);
        });
        
        CompletableFuture<Void> ledgerTask = CompletableFuture.runAsync(() -> {
            ledgerAgent.recordTransaction(trade);
            ledgerAgent.processAssetServicing(trade);
        });

        CompletableFuture<Void> marginTask = CompletableFuture.runAsync(() -> {
            marginAgent.calculatePortfolioMargin(trade);
            marginAgent.produceRegulatoryMarginReport();
        });

        // Wait for all to complete
        CompletableFuture.allOf(confirmTask, regTask, settleTask, ledgerTask, marginTask).join();
        
        System.out.println("========== TRADE LIFECYCLE COMPLETE ==========\n");
    }
}

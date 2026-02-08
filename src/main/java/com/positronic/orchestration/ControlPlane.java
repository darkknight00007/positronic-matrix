package com.positronic.orchestration;

import com.positronic.agents.*;
import com.positronic.model.domain.*;

import java.time.LocalDate;
import java.util.Arrays;
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

    public void handleTradeRequest(String productType, String assetClass, double notional,
                                   String currency, List<String> partyNames) {
        System.out.println("\n========== STARTING TRADE LIFECYCLE ==========");

        // --- Build domain objects from request parameters ---
        Party buyer = new Party(
            partyNames.get(0), partyNames.get(0),
            "LEI-" + partyNames.get(0), inferJurisdiction(partyNames.get(0))
        );
        Party seller = new Party(
            partyNames.get(1), partyNames.get(1),
            "LEI-" + partyNames.get(1), inferJurisdiction(partyNames.get(1))
        );

        Product product = ProductFactory.create(productType, assetClass, notional, currency, buyer, seller);

        // 1. Trading Agent: Validate & book
        tradingAgent.bookTrade(product, buyer, seller);

        // 2. Processing Agent: UTI, intercompany, netting set
        String uti = processingAgent.generateUTI(product, buyer, seller);
        processingAgent.processIntercompany(product, buyer, seller);
        String nettingSet = processingAgent.assignNettingSet(product, buyer, seller);

        // 3. Parallel execution of functional agents
        System.out.println("--- dispatching functional agents ---");

        CompletableFuture<Void> confirmTask = CompletableFuture.runAsync(() ->
            confirmationAgent.generateConfirmation(product, uti, buyer, seller)
        );

        CompletableFuture<Void> regTask = CompletableFuture.runAsync(() -> {
            List<String> regimes = regulatoryAgent.determineReportability(product, buyer, seller);
            for (String regime : regimes) {
                RegulatoryReport report = regulatoryAgent.generateReport(product, regime, buyer, seller, uti);
                regulatoryAgent.queueSubmission(report);
            }
            regulatoryAgent.submitToTradeRepository();
        });

        CompletableFuture<Void> settleTask = CompletableFuture.runAsync(() ->
            settlementAgent.projectCashflows(product)
        );

        CompletableFuture<Void> ledgerTask = CompletableFuture.runAsync(() ->
            ledgerAgent.recordTransaction(product, buyer, seller)
        );

        CompletableFuture<Void> marginTask = CompletableFuture.runAsync(() -> {
            marginAgent.calculatePortfolioMargin(nettingSet, Arrays.asList(product));
            marginAgent.produceRegulatoryMarginReport(nettingSet);
        });

        // Wait for all to complete
        CompletableFuture.allOf(confirmTask, regTask, settleTask, ledgerTask, marginTask).join();

        System.out.println("========== TRADE LIFECYCLE COMPLETE ==========\n");
    }

    private String inferJurisdiction(String partyName) {
        if (partyName.contains("LONDON") || partyName.contains("_GB")) return "EU_GB";
        if (partyName.contains("PARIS") || partyName.contains("_EU"))  return "EU";
        if (partyName.contains("_SG")) return "SG";
        if (partyName.contains("_AU")) return "AU";
        return "US";
    }
}

/**
 * Bridges raw request params to the Product domain model.
 */
class ProductFactory {

    public static Product create(String productType, String assetClass,
                                 double notional, String currency,
                                 Party buyer, Party seller) {

        String id = "TRD-" + System.currentTimeMillis();
        Currency curr = Currency.valueOf(currency);
        Notional notionalObj = new Notional(notional, curr);
        Schedule schedule = new Schedule(
            LocalDate.now(), LocalDate.now().plusYears(5),
            "SEMI_ANNUAL", DayCountFraction.ACT_360
        );

        return switch (productType) {
            case "InterestRateSwap" -> {
                SwapLeg payLeg  = new SwapLeg(new Rate(RateType.FIXED,    0.025, null,   null),  schedule, notionalObj);
                SwapLeg recvLeg = new SwapLeg(new Rate(RateType.FLOATING, null,  "SOFR", 0.001), schedule, notionalObj);
                yield new InterestRateSwap(id, payLeg, recvLeg);
            }
            case "FxForward", "FxOption" ->
                new FxOption(id, Currency.EUR, Currency.USD, 1.10, LocalDate.now().plusMonths(3), "CALL");
            case "CreditDefaultSwap" ->
                new CreditDefaultSwap(id, "ACME Corp", 0.005, notionalObj, schedule);
            default ->
                new GenericProduct(id, assetClass, productType);
        };
    }
}

/** Catch-all for product types without a dedicated class. */
class GenericProduct extends Product {
    private final String type;

    public GenericProduct(String id, String assetClass, String type) {
        super(id, assetClass);
        this.type = type;
    }

    @Override
    public String getProductType() { return type; }
}

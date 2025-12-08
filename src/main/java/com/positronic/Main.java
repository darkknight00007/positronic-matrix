package com.positronic;

import com.positronic.orchestration.ControlPlane;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        ControlPlane controlPlane = new ControlPlane();
        
        // Scenario 1: Standard USD Interest Rate Swap
        controlPlane.handleTradeRequest(
            "InterestRateSwap", 
            "InterestRate", 
            10000000.0, 
            "USD", 
            Arrays.asList("BANK_A", "CLIENT_B")
        );

        // Scenario 2: EU Cross-Border Trade
        controlPlane.handleTradeRequest(
            "FxForward", 
            "ForeignExchange", 
            5000000.0, 
            "EUR", 
            Arrays.asList("BANK_A_LONDON", "BANK_B_PARIS_EU")
        );
    }
}

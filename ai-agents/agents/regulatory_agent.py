"""
Regulatory Agent with Vertex AI and ADK
AI-powered agent for multi-jurisdiction regulatory reporting
"""

from google.cloud import aiplatform
from vertexai.generative_models import GenerativeModel, Part, Tool, FunctionDeclaration
import vertexai
import json
from typing import List, Dict, Any
from datetime import datetime


class RegulatoryAgent:
    """
    AI-powered Regulatory Agent using Vertex AI Gemini

    Capabilities:
    - Intelligent jurisdiction determination
    - Schema mapping with LLM reasoning
    - Adaptive rule interpretation
    - Natural language query of regulations
    """

    def __init__(self, project_id: str, location: str = "us-central1"):
        self.project_id = project_id
        self.location = location

        # FIX: use vertexai.init() (not aiplatform.init — different SDK entry point)
        vertexai.init(project=project_id, location=location)

        # Initialize Gemini model
        self.model = GenerativeModel("gemini-1.5-pro")

        # Define tools (functions the AI can call)
        self.tools = self._define_tools()

        # Grounding corpus
        self.legend_corpus = self._load_legend_models()

    # ------------------------------------------------------------------ tools
    def _define_tools(self) -> List[Tool]:
        jurisdiction_check = FunctionDeclaration(
            name="check_jurisdiction",
            description="Determine which regulatory regimes apply to a trade",
            parameters={
                "type": "object",
                "properties": {
                    "buyer_jurisdiction": {"type": "string"},
                    "seller_jurisdiction": {"type": "string"},
                    "product_type": {"type": "string"},
                    "trade_currency": {"type": "string"},
                },
                "required": ["buyer_jurisdiction", "seller_jurisdiction", "product_type"],
            },
        )

        generate_report = FunctionDeclaration(
            name="generate_regulatory_report",
            description="Generate a regulatory report for a specific regime",
            parameters={
                "type": "object",
                "properties": {
                    "regime": {"type": "string"},
                    "trade_data": {"type": "object"},
                    "uti": {"type": "string"},
                },
                "required": ["regime", "trade_data", "uti"],
            },
        )

        validate_report = FunctionDeclaration(
            name="validate_against_schema",
            description="Validate a regulatory report against schema requirements",
            parameters={
                "type": "object",
                "properties": {
                    "regime": {"type": "string"},
                    "report_data": {"type": "object"},
                },
                "required": ["regime", "report_data"],
            },
        )

        return [Tool(function_declarations=[jurisdiction_check, generate_report, validate_report])]

    def _load_legend_models(self) -> str:
        return """
        Legend OTC Derivatives Model:
        - CFTC Part 43 requires: UTI, ExecutionTimestamp, Price, Notional, AssetClass, ClearedIndicator
        - CFTC Part 45 requires: UTI, UPI, ReportingCounterpartyLEI, OtherCounterpartyLEI, EffectiveDate, MaturityDate
        - EMIR requires: UTI, LEI_1, LEI_2, TradeDate, Notional, Valuation, CollateralPosted
        - MiFIR requires: ISIN, Quantity, Price, Venue, BuyerLEI, SellerLEI
        """

    # ---------------------------------------------------------- main entry point
    async def process_trade_for_reporting(self, trade_data: Dict[str, Any]) -> Dict[str, Any]:
        """Use AI reasoning to determine and execute regulatory reporting."""

        prompt = f"""
        You are a Regulatory Reporting Agent with deep knowledge of global derivatives regulations.

        Analyze this trade and determine the complete regulatory reporting strategy:

        Trade Details:
        - Product: {trade_data.get('product_type')}
        - Asset Class: {trade_data.get('asset_class')}
        - Buyer: {trade_data.get('buyer')} (Jurisdiction: {trade_data.get('buyer_jurisdiction')})
        - Seller: {trade_data.get('seller')} (Jurisdiction: {trade_data.get('seller_jurisdiction')})
        - Notional: {trade_data.get('notional')} {trade_data.get('currency')}
        - UTI: {trade_data.get('uti')}

        Tasks:
        1. Determine ALL applicable regulatory regimes using check_jurisdiction tool
        2. For each regime, identify mandatory fields
        3. Generate reports using generate_regulatory_report tool
        4. Validate each report using validate_against_schema tool
        5. Provide submission priority and timeline

        Think step-by-step and use tools to execute the reporting workflow.
        """

        try:
            # FIX: use synchronous generate_content (works reliably across SDK versions)
            response = self.model.generate_content(
                contents=[prompt],
                tools=self.tools,
                generation_config={"temperature": 0.1},
            )

            results = self._execute_tool_calls(response)

            return {
                "reasoning": response.text if hasattr(response, "text") else str(response),
                "reports_generated": results,
                "timestamp": datetime.now().isoformat(),
            }
        except Exception as e:
            # FIX: graceful fallback so dev/testing works without Gemini quota
            print(f"[RegulatoryAgent] AI reasoning failed ({e}), using rule-based fallback")
            return self._fallback_reporting(trade_data)

    # ---------------------------------------------------------- tool execution
    def _execute_tool_calls(self, response) -> List[Dict]:
        results = []
        try:
            for part in response.candidates[0].content.parts:
                if hasattr(part, "function_call"):
                    fc = part.function_call
                    if fc.name == "check_jurisdiction":
                        results.append(self._check_jurisdiction(**dict(fc.args)))
                    elif fc.name == "generate_regulatory_report":
                        results.append(self._generate_report(**dict(fc.args)))
                    elif fc.name == "validate_against_schema":
                        results.append(self._validate_schema(**dict(fc.args)))
        except (AttributeError, IndexError) as e:
            print(f"[RegulatoryAgent] Error processing tool calls: {e}")
        return results

    # ---------------------------------------------------------- fallback
    def _fallback_reporting(self, trade_data: Dict[str, Any]) -> Dict[str, Any]:
        """Rule-based fallback when Gemini API is unavailable."""
        jur = self._check_jurisdiction(
            buyer_jurisdiction=trade_data.get("buyer_jurisdiction", ""),
            seller_jurisdiction=trade_data.get("seller_jurisdiction", ""),
            product_type=trade_data.get("product_type", ""),
            trade_currency=trade_data.get("currency", ""),
        )

        reports = []
        for regime in jur["applicable_regimes"]:
            reports.append(
                self._generate_report(
                    regime=regime,
                    trade_data=trade_data,
                    uti=trade_data.get("uti", "UTI-UNKNOWN"),
                )
            )

        return {
            "reasoning": "Rule-based fallback (AI unavailable)",
            "reports_generated": reports,
            "timestamp": datetime.now().isoformat(),
        }

    # -------------------------------------------------------- tool implementations
    def _check_jurisdiction(
        self,
        buyer_jurisdiction: str,
        seller_jurisdiction: str,
        product_type: str,
        trade_currency: str = "",
    ) -> Dict:
        regimes: List[str] = []

        if buyer_jurisdiction == "US" or seller_jurisdiction == "US":
            regimes.extend(["CFTC_PART_43", "CFTC_PART_45"])

        if "EU" in buyer_jurisdiction or "EU" in seller_jurisdiction:
            regimes.append("EMIR")
            if product_type in ["EquityOption", "CreditDefaultSwap"]:
                regimes.append("MIFIR")

        if buyer_jurisdiction == "AU" or seller_jurisdiction == "AU":
            regimes.append("ASIC")

        if buyer_jurisdiction == "SG" or seller_jurisdiction == "SG":
            regimes.append("MAS")

        return {
            "applicable_regimes": regimes,
            "confidence": "high",
            "reasoning": f"Based on jurisdictions {buyer_jurisdiction}/{seller_jurisdiction}",
        }

    def _generate_report(self, regime: str, trade_data: Dict, uti: str) -> Dict:
        fields: Dict[str, Any] = {}

        if regime == "CFTC_PART_43":
            fields = {
                "UTI": uti,
                "ExecutionTimestamp": datetime.now().isoformat(),
                "AssetClass": trade_data.get("asset_class"),
                "Price": trade_data.get("price", "N/A"),
                "Notional": trade_data.get("notional"),
                "ClearedIndicator": False,
                "BlockTradeIndicator": False,
            }
        elif regime == "CFTC_PART_45":
            fields = {
                "UTI": uti,
                "UPI": f"UPI-{trade_data.get('product_type')}",
                "ReportingCounterpartyLEI": trade_data.get("buyer_lei", ""),
                "OtherCounterpartyLEI": trade_data.get("seller_lei", ""),
                "EffectiveDate": datetime.now().date().isoformat(),
                "CollateralizationType": "Uncollateralized",
            }
        elif regime == "EMIR":
            fields = {
                "UTI": uti,
                "LEI_1": trade_data.get("buyer_lei", ""),
                "LEI_2": trade_data.get("seller_lei", ""),
                "TradeDate": datetime.now().date().isoformat(),
                "Notional": trade_data.get("notional"),
                "Valuation": 0.0,
                "CollateralPosted": 0.0,
            }

        return {
            "regime": regime,
            "report_id": f"RPT-{regime}-{uti[:8]}",
            "fields": fields,
            "status": "generated",
        }

    def _validate_schema(self, regime: str, report_data: Dict) -> Dict:
        required_fields = {
            "CFTC_PART_43": ["UTI", "ExecutionTimestamp", "AssetClass", "Notional"],
            "CFTC_PART_45": ["UTI", "UPI", "ReportingCounterpartyLEI"],
            "EMIR": ["UTI", "LEI_1", "LEI_2"],
        }

        missing = [
            f for f in required_fields.get(regime, []) if f not in report_data.get("fields", {})
        ]

        return {"valid": len(missing) == 0, "missing_fields": missing, "regime": regime}


# ------------------------------------------------------------------- main
if __name__ == "__main__":
    import asyncio

    agent = RegulatoryAgent(project_id="nextgen3")

    sample_trade = {
        "product_type": "InterestRateSwap",
        "asset_class": "InterestRate",
        "buyer": "BANK_A",
        "buyer_jurisdiction": "US",
        "buyer_lei": "1234567890ABCDEF1234",
        "seller": "BANK_B_EU",
        "seller_jurisdiction": "EU_GB",
        "seller_lei": "FEDCBA0987654321FEDC",
        "notional": 10000000,
        "currency": "USD",
        "uti": "UTI-123456789",
    }

    result = asyncio.run(agent.process_trade_for_reporting(sample_trade))
    print(json.dumps(result, indent=2))

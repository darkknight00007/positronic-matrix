"""
Regulatory Agent with Vertex AI and ADK
AI-powered agent for multi-jurisdiction regulatory reporting
"""

from google.cloud import aiplatform
from vertexai.preview import reasoning_engines
from vertexai.generative_models import GenerativeModel, Part, Tool, FunctionDeclaration
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
        aiplatform.init(project=project_id, location=location)
        
        # Initialize Gemini model
        self.model = GenerativeModel("gemini-1.5-pro")
        
        # Define tools (functions the AI can call)
        self.tools = self._define_tools()
        
        # Grounding corpus (Legend models as knowledge base)
        self.legend_corpus = self._load_legend_models()
    
    def _define_tools(self) -> List[Tool]:
        """Define function tools the AI agent can invoke"""
        
        jurisdiction_check = FunctionDeclaration(
            name="check_jurisdiction",
            description="Determine which regulatory regimes apply to a trade based on counterparty jurisdictions and product type",
            parameters={
                "type": "object",
                "properties": {
                    "buyer_jurisdiction": {"type": "string", "description": "Buyer's jurisdiction code (US, EU, UK, SG, AU, JP)"},
                    "seller_jurisdiction": {"type": "string", "description": "Seller's jurisdiction code"},
                    "product_type": {"type": "string", "description": "Product type (InterestRateSwap, FxOption, CreditDefaultSwap, etc.)"},
                    "trade_currency": {"type": "string", "description": "Trade currency"}
                },
                "required": ["buyer_jurisdiction", "seller_jurisdiction", "product_type"]
            }
        )
        
        generate_report = FunctionDeclaration(
            name="generate_regulatory_report",
            description="Generate a regulatory report for a specific regime with required fields",
            parameters={
                "type": "object",
                "properties": {
                    "regime": {"type": "string", "description": "Regulatory regime (CFTC_PART_43, CFTC_PART_45, EMIR, MIFIR, ASIC, MAS)"},
                    "trade_data": {"type": "object", "description": "Trade data object"},
                    "uti": {"type": "string", "description": "Unique Transaction Identifier"}
                },
                "required": ["regime", "trade_data", "uti"]
            }
        )
        
        validate_report = FunctionDeclaration(
            name="validate_against_schema",
            description="Validate a regulatory report against the regime's schema requirements",
            parameters={
                "type": "object",
                "properties": {
                    "regime": {"type": "string"},
                    "report_data": {"type": "object"}
                },
                "required": ["regime", "report_data"]
            }
        )
        
        return [Tool(function_declarations=[jurisdiction_check, generate_report, validate_report])]
    
    def _load_legend_models(self) -> str:
        """Load Legend Pure model definitions as grounding context"""
        # In production, this would load from Vertex AI Search corpus
        return """
        Legend OTC Derivatives Model:
        - CFTC Part 43 requires: UTI, ExecutionTimestamp, Price, Notional, AssetClass, ClearedIndicator
        - CFTC Part 45 requires: UTI, UPI, ReportingCounterpartyLEI, OtherCounterpartyLEI, EffectiveDate, MaturityDate
        - EMIR requires: UTI, LEI_1, LEI_2, TradeDate, Notional, Valuation, CollateralPosted
        - MiFIR requires: ISIN, Quantity, Price, Venue, BuyerLEI, SellerLEI
        """
    
    async def process_trade_for_reporting(self, trade_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Main agent entry point - uses AI reasoning to determine and execute regulatory reporting
        """
        
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
        
        # Agent reasoning with tool use
        response = await self.model.generate_content_async(
            contents=[prompt],
            tools=self.tools,
            generation_config={"temperature": 0.1}  # Low temperature for factual accuracy
        )
        
        # Process tool calls
        results = await self._execute_tool_calls(response)
        
        return {
            "reasoning": response.text,
            "reports_generated": results,
            "timestamp": datetime.now().isoformat()
        }
    
    async def _execute_tool_calls(self, response) -> List[Dict]:
        """Execute function calls made by the AI"""
        results = []
        
        for part in response.candidates[0].content.parts:
            if hasattr(part, 'function_call'):
                func_call = part.function_call
                
                if func_call.name == "check_jurisdiction":
                    result = self._check_jurisdiction(**dict(func_call.args))
                    results.append(result)
                    
                elif func_call.name == "generate_regulatory_report":
                    result = self._generate_report(**dict(func_call.args))
                    results.append(result)
                    
                elif func_call.name == "validate_against_schema":
                    result = self._validate_schema(**dict(func_call.args))
                    results.append(result)
        
        return results
    
    def _check_jurisdiction(self, buyer_jurisdiction: str, seller_jurisdiction: str, 
                           product_type: str, trade_currency: str = "") -> Dict:
        """Tool implementation: Check jurisdiction"""
        regimes = []
        
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
            "reasoning": f"Based on jurisdictions {buyer_jurisdiction}/{seller_jurisdiction}"
        }
    
    def _generate_report(self, regime: str, trade_data: Dict, uti: str) -> Dict:
        """Tool implementation: Generate report"""
        report_fields = {}
        
        if regime == "CFTC_PART_43":
            report_fields = {
                "UTI": uti,
                "ExecutionTimestamp": datetime.now().isoformat(),
                "AssetClass": trade_data.get("asset_class"),
                "Price": trade_data.get("price", "N/A"),
                "Notional": trade_data.get("notional"),
                "ClearedIndicator": False,
                "BlockTradeIndicator": False
            }
        elif regime == "CFTC_PART_45":
            report_fields = {
                "UTI": uti,
                "UPI": f"UPI-{trade_data.get('product_type')}",
                "ReportingCounterpartyLEI": trade_data.get("buyer_lei", ""),
                "OtherCounterpartyLEI": trade_data.get("seller_lei", ""),
                "EffectiveDate": datetime.now().date().isoformat(),
                "CollateralizationType": "Uncollateralized"
            }
        
        return {
            "regime": regime,
            "report_id": f"RPT-{regime}-{uti[:8]}",
            "fields": report_fields,
            "status": "generated"
        }
    
    def _validate_schema(self, regime: str, report_data: Dict) -> Dict:
        """Tool implementation: Validate schema"""
        required_fields = {
            "CFTC_PART_43": ["UTI", "ExecutionTimestamp", "AssetClass", "Notional"],
            "CFTC_PART_45": ["UTI", "UPI", "ReportingCounterpartyLEI"],
            "EMIR": ["UTI", "LEI_1", "LEI_2"]
        }
        
        missing = []
        for field in required_fields.get(regime, []):
            if field not in report_data.get("fields", {}):
                missing.append(field)
        
        return {
            "valid": len(missing) == 0,
            "missing_fields": missing,
            "regime": regime
        }


# Example usage
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
        "uti": "UTI-123456789"
    }
    
    result = asyncio.run(agent.process_trade_for_reporting(sample_trade))
    print(json.dumps(result, indent=2))

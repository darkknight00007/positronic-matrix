"""
FastAPI Server for AI Agent Workflow
Exposes REST API and WebSocket for real-time workflow execution
"""

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import asyncio
import json
from typing import Dict
from workflows.multi_agent_workflow import TradeProcessingWorkflow

app = FastAPI(title="AI Agent Workflow API")

# Enable CORS for React frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Active WebSocket connections
active_connections: list[WebSocket] = []

# Workflow instance
workflow = None

class TradeRequest(BaseModel):
    trade_id: str
    product_type: str
    buyer_jurisdiction: str
    seller_jurisdiction: str
    notional: float = 10000000
    currency: str = "USD"

@app.on_event("startup")
async def startup_event():
    global workflow
    # Initialize workflow with your GCP project
    workflow = TradeProcessingWorkflow(project_id="your-gcp-project-id")

@app.post("/workflow/execute")
async def execute_workflow(trade: TradeRequest):
    """Execute the multi-agent workflow for a trade"""
    
    trade_data = trade.dict()
    
    # Execute workflow
    result = await workflow.execute(trade_data)
    
    # Broadcast to connected WebSocket clients
    await broadcast_update({
        "type": "workflow_complete",
        "workflow_state": result
    })
    
    return {"status": "success", "result": result}

@app.websocket("/workflow/stream")
async def websocket_endpoint(websocket: WebSocket):
    """WebSocket for real-time workflow updates"""
    await websocket.accept()
    active_connections.append(websocket)
    
    try:
        while True:
            # Keep connection alive
            data = await websocket.receive_text()
            
            # Echo back (or handle commands)
            await websocket.send_text(f"Received: {data}")
    
    except WebSocketDisconnect:
        active_connections.remove(websocket)

async def broadcast_update(message: Dict):
    """Broadcast workflow updates to all connected clients"""
    for connection in active_connections:
        await connection.send_text(json.dumps(message))

@app.get("/health")
async def health_check():
    return {"status": "healthy", "agents": 7}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

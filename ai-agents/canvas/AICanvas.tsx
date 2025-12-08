import React, { useState, useEffect } from 'react';
import ReactFlow, {
    Node,
    Edge,
    Controls,
    Background,
    useNodesState,
    useEdgesState,
    MarkerType
} from 'reactflow';
import 'reactflow/dist/style.css';

/**
 * AI Canvas - Visual workflow for multi-agent orchestration
 * Real-time visualization of agent execution state
 */

interface AgentStatus {
    name: string;
    status: 'pending' | 'running' | 'complete' | 'error';
    reasoning?: string;
    output?: any;
    timestamp?: string;
}

const AgentNode = ({ data }: { data: any }) => {
    const statusColors = {
        pending: '#9CA3AF',
        running: '#F59E0B',
        complete: '#10B981',
        error: '#EF4444'
    };

    return (
        <div className="px-6 py-4 shadow-lg rounded-lg border-2 bg-white"
            style={{ borderColor: statusColors[data.status] }}>
            <div className="flex items-center space-x-2">
                {data.status === 'running' && (
                    <div className="animate-spin h-4 w-4 border-2 border-orange-500 border-t-transparent rounded-full" />
                )}
                <div className="font-bold text-gray-800">{data.label}</div>
            </div>
            {data.reasoning && (
                <div className="mt-2 text-xs text-gray-600 max-w-xs">
                    💭 {data.reasoning}
                </div>
            )}
            <div className="mt-1 text-xs font-mono" style={{ color: statusColors[data.status] }}>
                {data.status.toUpperCase()}
            </div>
        </div>
    );
};

const nodeTypes = {
    agentNode: AgentNode
};

export const AICanvas: React.FC = () => {
    const [agents Status, setAgentsStatus] = useState<Record<string, AgentStatus>>({
        trading: { name: 'Trading Agent', status: 'pending' },
        processing: { name: 'Processing Agent', status: 'pending' },
        regulatory: { name: 'Regulatory Agent', status: 'pending' },
        confirmation: { name: 'Confirmation Agent', status: 'pending' },
        settlement: { name: 'Settlement Agent', status: 'pending' },
        ledger: { name: 'Ledger Agent', status: 'pending' },
        margin: { name: 'Margin Agent', status: 'pending' }
    });

    const [workflowData, setWorkflowData] = useState<any>(null);

    // Define workflow graph nodes
    const initialNodes: Node[] = [
        {
            id: 'trading',
            type: 'agentNode',
            position: { x: 250, y: 50 },
            data: { label: '🎯 Trading Agent', status: agentsStatus.trading.status, reasoning: agentsStatus.trading.reasoning }
        },
        {
            id: 'processing',
            type: 'agentNode',
            position: { x: 250, y: 180 },
            data: { label: '⚙️ Processing Agent', status: agentsStatus.processing.status, reasoning: agentsStatus.processing.reasoning }
        },
        {
            id: 'regulatory',
            type: 'agentNode',
            position: { x: 100, y: 350 },
            data: { label: '📋 Regulatory', status: agentsStatus.regulatory.status, reasoning: agentsStatus.regulatory.reasoning }
        },
        {
            id: 'confirmation',
            type: 'agentNode',
            position: { x: 250, y: 350 },
            data: { label: '✅ Confirmation', status: agentsStatus.confirmation.status, reasoning: agentsStatus.confirmation.reasoning }
        },
        {
            id: 'settlement',
            type: 'agentNode',
            position: { x: 400, y: 350 },
            data: { label: '💸 Settlement', status: agentsStatus.settlement.status, reasoning: agentsStatus.settlement.reasoning }
        },
        {
            id: 'ledger',
            type: 'agentNode',
            position: { x: 150, y: 500 },
            data: { label: '📒 Ledger', status: agentsStatus.ledger.status, reasoning: agentsStatus.ledger.reasoning }
        },
        {
            id: 'margin',
            type: 'agentNode',
            position: { x: 350, y: 500 },
            data: { label: '📊 Margin', status: agentsStatus.margin.status, reasoning: agentsStatus.margin.reasoning }
        }
    ];

    const initialEdges: Edge[] = [
        { id: 'e1', source: 'trading', target: 'processing', animated: true, markerEnd: { type: MarkerType.ArrowClosed } },
        { id: 'e2', source: 'processing', target: 'regulatory', animated: true, markerEnd: { type: MarkerType.ArrowClosed } },
        { id: 'e3', source: 'processing', target: 'confirmation', animated: true, markerEnd: { type: MarkerType.ArrowClosed } },
        { id: 'e4', source: 'processing', target: 'settlement', animated: true, markerEnd: { type: MarkerType.ArrowClosed } },
        { id: 'e5', source: 'processing', target: 'ledger', animated: true, markerEnd: { type: MarkerType.ArrowClosed } },
        { id: 'e6', source: 'processing', target: 'margin', animated: true, markerEnd: { type: MarkerType.ArrowClosed } }
    ];

    const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
    const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

    // Connect to workflow backend via WebSocket
    useEffect(() => {
        const ws = new WebSocket('ws://localhost:8000/workflow/stream');

        ws.onmessage = (event) => {
            const update = JSON.parse(event.data);

            // Update agent status
            if (update.agent && update.status) {
                setAgentsStatus(prev => ({
                    ...prev,
                    [update.agent]: {
                        ...prev[update.agent],
                        status: update.status,
                        reasoning: update.reasoning,
                        timestamp: new Date().toISOString()
                    }
                }));
            }

            // Update workflow data
            if (update.workflow_state) {
                setWorkflowData(update.workflow_state);
            }
        };

        return () => ws.close();
    }, []);

    // Update nodes when agent status changes
    useEffect(() => {
        setNodes(nodes =>
            nodes.map(node => ({
                ...node,
                data: {
                    ...node.data,
                    status: agentsStatus[node.id]?.status || 'pending',
                    reasoning: agentsStatus[node.id]?.reasoning
                }
            }))
        );
    }, [agentsStatus, setNodes]);

    const startWorkflow = async () => {
        const response = await fetch('http://localhost:8000/workflow/execute', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                trade_id: 'TRD-12345',
                product_type: 'InterestRateSwap',
                buyer_jurisdiction: 'US',
                seller_jurisdiction: 'EU_GB'
            })
        });

        const result = await response.json();
        console.log('Workflow started:', result);
    };

    return (
        <div className="h-screen w-full flex flex-col">
            {/* Header */}
            <div className="bg-gradient-to-r from-blue-600 to-purple-600 text-white p-4 shadow-lg">
                <h1 className="text-2xl font-bold">AI Multi-Agent Canvas</h1>
                <p className="text-sm opacity-90">Real-time OTC Derivatives Processing Workflow</p>
            </div>

            {/* Control Panel */}
            <div className="bg-gray-100 p-4 flex space-x-4 items-center">
                <button
                    onClick={startWorkflow}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg font-semibold">
                    ▶️ Execute Workflow
                </button>
                <div className="flex space-x-4 text-sm">
                    <div className="flex items-center space-x-2">
                        <div className="w-3 h-3 bg-gray-400 rounded-full"></div>
                        <span>Pending</span>
                    </div>
                    <div className="flex items-center space-x-2">
                        <div className="w-3 h-3 bg-orange-500 rounded-full animate-pulse"></div>
                        <span>Running</span>
                    </div>
                    <div className="flex items-center space-x-2">
                        <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                        <span>Complete</span>
                    </div>
                </div>
            </div>

            {/* Canvas */}
            <div className="flex-1 relative">
                <ReactFlow
                    nodes={nodes}
                    edges={edges}
                    onNodesChange={onNodesChange}
                    onEdgesChange={onEdgesChange}
                    nodeTypes={nodeTypes}
                    fitView
                >
                    <Background />
                    <Controls />
                </ReactFlow>
            </div>

            {/* Status Panel */}
            <div className="bg-gray-50 border-t p-4 max-h-48 overflow-y-auto">
                <h3 className="font-bold mb-2">Workflow State</h3>
                {workflowData && (
                    <pre className="text-xs bg-white p-3 rounded border overflow-x-auto">
                        {JSON.stringify(workflowData, null, 2)}
                    </pre>
                )}
            </div>
        </div>
    );
};

export default AICanvas;

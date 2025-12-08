# Google Vertex AI Workbench Setup Guide

## Quick Start: Deploy AI Agents in Vertex AI Workbench

This guide walks through setting up the entire AI agent framework in a Vertex AI Workbench notebook instance.

---

## Step 1: Create Vertex AI Workbench Instance

### Option A: Via Google Cloud Console (Recommended)

1. Navigate to **Vertex AI** → **Workbench** → **User-Managed Notebooks**
   ```
   https://console.cloud.google.com/vertex-ai/workbench/user-managed
   ```

2. Click **"+ NEW NOTEBOOK"**

3. Configure the instance:
   - **Name**: `otc-ai-agents`
   - **Region**: `us-central1` (or your preferred region)
   - **Machine type**: `n1-standard-4` (4 vCPUs, 15 GB RAM)
   - **GPU**: None (optional: add T4 for faster inference)
   - **Environment**: 
     - Select **"Python 3"**
     - Framework: **TensorFlow Enterprise 2.11** (has good Python env)

4. Click **"CREATE"**

   ⏱ Instance creation takes ~5 minutes

5. Once ready, click **"OPEN JUPYTERLAB"**

### Option B: Via gcloud CLI

```bash
gcloud notebooks instances create otc-ai-agents \
    --location=us-central1-a \
    --machine-type=n1-standard-4 \
    --vm-image-project=deeplearning-platform-release \
    --vm-image-family=tf-ent-2-11-cpu \
    --install-gpu-driver
```

---

## Step 2: Upload Project Files

### Method 1: Git Clone (Recommended)

In the Workbench JupyterLab terminal:

```bash
cd ~/
git clone https://github.com/YOUR_USERNAME/positronic-matrix.git
# OR if you haven't pushed yet, use file upload method below
```

### Method 2: File Upload

1. In JupyterLab, click the **Upload** button (⬆ icon)
2. Upload your local `positronic-matrix` folder
3. Or compress locally and upload:
   ```bash
   # On your local machine
   cd c:\Users\dutta\Developer
   tar -czf positronic-matrix.tar.gz positronic-matrix/
   # Then upload via JupyterLab UI
   ```

4. Extract in Workbench terminal:
   ```bash
   cd ~/
   tar -xzf positronic-matrix.tar.gz
   ```

---

## Step 3: Install Python Dependencies

Open a **Terminal** in JupyterLab:

```bash
cd ~/positronic-matrix/ai-agents

# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Upgrade pip
pip install --upgrade pip

# Install all dependencies
pip install -r requirements.txt

# Verify installation
python -c "from google.cloud import aiplatform; print('✓ Vertex AI SDK installed')"
python -c "import langgraph; print('✓ LangGraph installed')"
```

---

## Step 4: Configure Google Cloud Authentication

**Good news**: Workbench instances have automatic authentication with your GCP project!

### Verify Authentication:

```bash
# In terminal
gcloud auth list
gcloud config get-value project
```

### Set Project ID (if needed):

```bash
export GCP_PROJECT_ID=$(gcloud config get-value project)
export GCP_LOCATION="us-central1"

echo "export GCP_PROJECT_ID=$GCP_PROJECT_ID" >> ~/.bashrc
echo "export GCP_LOCATION=$GCP_LOCATION" >> ~/.bashrc
```

### Update Agent Code with Project ID:

```python
# Edit ai-agents/agents/regulatory_agent.py
# Line ~17: Change "your-gcp-project" to your actual project ID

# Quick find & replace in terminal:
PROJECT_ID=$(gcloud config get-value project)
sed -i "s/your-gcp-project/$PROJECT_ID/g" ~/positronic-matrix/ai-agents/agents/regulatory_agent.py
sed -i "s/your-gcp-project/$PROJECT_ID/g" ~/positronic-matrix/ai-agents/workflows/multi_agent_workflow.py
sed -i "s/your-gcp-project-id/$PROJECT_ID/g" ~/positronic-matrix/ai-agents/api_server.py
```

---

## Step 5: Test Regulatory Agent

Create a new **Jupyter Notebook** to test:

```python
# File: test_regulatory_agent.ipynb

import sys
sys.path.append('/home/jupyter/positronic-matrix/ai-agents')

from agents.regulatory_agent import RegulatoryAgent
import asyncio

# Initialize agent
agent = RegulatoryAgent(
    project_id="YOUR_PROJECT_ID",  # Will auto-detect from environment
    location="us-central1"
)

# Test trade
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
    "uti": "UTI-TEST-001"
}

# Execute
result = await agent.process_trade_for_reporting(sample_trade)
print(result)
```

Run the notebook cell. You should see:
```
AI reasoning about jurisdictions...
Report generation...
✓ Success!
```

---

## Step 6: Run FastAPI Server

### In Workbench Terminal:

```bash
cd ~/positronic-matrix/ai-agents
source venv/bin/activate

# Run server
python api_server.py
```

The server will start on `http://localhost:8000`

### Access the API from Notebook:

```python
# In a Jupyter notebook
import requests

response = requests.post(
    "http://localhost:8000/workflow/execute",
    json={
        "trade_id": "TRD-001",
        "product_type": "InterestRateSwap",
        "buyer_jurisdiction": "US",
        "seller_jurisdiction": "EU_GB",
        "notional": 10000000,
        "currency": "USD"
    }
)

print(response.json())
```

---

## Step 7: Setup React Canvas (Optional)

For the visualization canvas, you need Node.js:

```bash
# Install Node.js in Workbench
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Verify
node --version
npm --version

# Setup canvas
cd ~/positronic-matrix/ai-agents/canvas
npm install

# Start dev server
npm run dev
```

**Access via Port Forwarding:**

Workbench doesn't expose ports by default. Use **VS Code Remote SSH** or **gcloud CLI tunnel**:

```bash
# On your local machine
gcloud compute ssh otc-ai-agents \
    --project=YOUR_PROJECT_ID \
    --zone=us-central1-a \
    -- -L 3000:localhost:3000 -L 8000:localhost:8000
```

Then access:
- Canvas: `http://localhost:3000`
- API: `http://localhost:8000/docs` (FastAPI Swagger UI)

---

## Step 8: Run Complete Workflow

Create a notebook: `run_workflow.ipynb`

```python
import sys
sys.path.append('/home/jupyter/positronic-matrix/ai-agents')

from workflows.multi_agent_workflow import TradeProcessingWorkflow
import asyncio
import json

# Initialize workflow
workflow = TradeProcessingWorkflow(
    project_id="YOUR_PROJECT_ID"
)

# Sample trade
trade_data = {
    "id": "TRD-WORKBENCH-001",
    "product_type": "CreditDefaultSwap",
    "asset_class": "Credit",
    "buyer": "HEDGE_FUND_A",
    "buyer_jurisdiction": "US",
    "buyer_lei": "ABC123XYZ456",
    "seller": "BANK_LONDON",
    "seller_jurisdiction": "EU_GB",
    "seller_lei": "DEF789UVW012",
    "notional": 5000000,
    "currency": "USD"
}

# Execute workflow
result = await workflow.execute(trade_data)

# Pretty print results
print(json.dumps(result, indent=2, default=str))
```

Run the notebook and observe the complete multi-agent execution!

---

## Monitoring & Debugging

### View Logs in Real-Time:

```bash
# Vertex AI Agent logs
gcloud logging read "resource.type=aiplatform.googleapis.com" \
    --limit=50 \
    --format=json

# Or use Cloud Console: Logging > Logs Explorer
```

### Debug in Notebook:

```python
# Enable verbose logging
import logging
logging.basicConfig(level=logging.DEBUG)

# Test individual agent functions
agent = RegulatoryAgent(project_id="YOUR_PROJECT_ID")
result = agent._check_jurisdiction("US", "EU_GB", "InterestRateSwap")
print(result)
```

---

## Production Deployment

### Deploy API Server to Cloud Run:

```bash
# Build container
cd ~/positronic-matrix/ai-agents

cat > Dockerfile <<EOF
FROM python:3.10-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["python", "api_server.py"]
EOF

# Build and deploy
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/ai-agents
gcloud run deploy ai-agents \
    --image gcr.io/YOUR_PROJECT_ID/ai-agents \
    --platform managed \
    --region us-central1 \
    --allow-unauthenticated \
    --memory 2Gi \
    --timeout 300
```

---

## Troubleshooting

### Issue: "Module not found"
```bash
# Ensure virtual environment is activated
source ~/positronic-matrix/ai-agents/venv/bin/activate
pip install -r requirements.txt
```

### Issue: "Vertex AI permission denied"
```bash
# Grant Vertex AI permissions to Workbench service account
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
    --member="serviceAccount:SERVICE_ACCOUNT@developer.gserviceaccount.com" \
    --role="roles/aiplatform.user"
```

### Issue: "Quota exceeded"
- Go to **IAM & Admin** → **Quotas** → Request increase for "AI Platform" quotas

---

## Cost Estimation

**Workbench Instance:**
- n1-standard-4: ~$0.19/hour (~$4.56/day if running 24/7)
- **Tip**: Stop instance when not in use to save costs

**Vertex AI (Gemini):**
- Gemini 1.5 Flash: $0.075 per 1M tokens (development)
- Gemini 1.5 Pro: $3.50 per 1M tokens (production)

**Example**: Processing 100 trades/day with Flash = ~$0.50/day

---

## Next Steps

1. ✅ Test each agent individually in notebooks
2. ✅ Run complete workflow end-to-end
3. ✅ Customize agent prompts for your use case
4. ✅ Deploy to Cloud Run for production
5. ✅ Set up monitoring dashboards

You now have a fully functional AI-powered multi-agent system running in Vertex AI Workbench!

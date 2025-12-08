# AI Agent Infrastructure Setup

## Prerequisites

1. **Google Cloud Platform Account**
   - Project with billing enabled
   - Vertex AI API enabled
   - Service account with required permissions

2. **Node.js & Python**
   - Python 3.10+
   - Node.js 18+
   - npm or yarn

## GCP Setup

### 1. Enable APIs
```bash
gcloud services enable aiplatform.googleapis.com
gcloud services enable compute.googleapis.com
gcloud services enable storage.googleapis.com
```

### 2. Create Service Account
```bash
gcloud iam service-accounts create otc-agents-sa \
    --display-name="OTC Agents Service Account"

gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
    --member="serviceAccount:otc-agents-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/aiplatform.user"
```

### 3. Create credentials
```bash
gcloud iam service-accounts keys create ai-agent-key.json \
    --iam-account=otc-agents-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com
```

### 4. Set Environment Variables
```bash
export GOOGLE_APPLICATION_CREDENTIALS="./ai-agent-key.json"
export GCP_PROJECT_ID="your-project-id"
export GCP_LOCATION="us-central1"
```

## Python Setup

### 1. Create Virtual Environment
```bash
cd ai-agents
python -m venv venv
source venv/bin/activate  # Windows: venv\\Scripts\\activate
```

### 2. Install Dependencies
```bash
pip install -r requirements.txt
```

### 3. Run API Server
```bash
python api_server.py
```

Server will start at `http://localhost:8000`

## React Canvas Setup

### 1. Navigate to Canvas Directory
```bash
cd canvas
```

### 2. Install Dependencies
```bash
npm install reactflow react react-dom
npm install -D @types/react @types/react-dom typescript
npm install -D tailwindcss postcss autoprefixer
```

### 3. Start Development Server
```bash
npm run dev
```

Canvas will be available at `http://localhost:3000`

## Testing the Workflow

### 1. Start Backend
```bash
# Terminal 1
cd ai-agents
python api_server.py
```

### 2 Start Frontend
```bash
# Terminal 2
cd ai-agents/canvas
npm run dev
```

### 3. Execute Workflow
Open browser to `http://localhost:3000` and click "Execute Workflow"

## Deployment to GCP

### Option A: Cloud Run (Recommended)
```bash
# Build container
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/ai-agents

# Deploy to Cloud Run
gcloud run deploy ai-agents \
    --image gcr.io/YOUR_PROJECT_ID/ai-agents \
    --platform managed \
    --region us-central1 \
    --allow-unauthenticated
```

### Option B: GKE
```bash
# Create GKE cluster
gcloud container clusters create ai-agents-cluster \
    --num-nodes=3 \
    --machine-type=n1-standard-4

# Deploy with kubectl
kubectl apply -f kubernetes/deployment.yaml
```

## Monitoring

### View Vertex AI Logs
```bash
gcloud logging read "resource.type=aiplatform.googleapis.com/Endpoint"
```

### View Agent Traces
Visit Cloud Console > Vertex AI > Agent Builder > Your Agent > Traces

## Cost Optimization

- Use Gemini 1.5 Flash for development ($0.075 / 1M tokens)
- Use Gemini 1.5 Pro for production ($3.50 / 1M tokens)
- Enable request batching
- Implement caching for Legend model grounding

## Security

- Store secrets in Google Secret Manager
- Use Workload Identity for GKE
- Enable VPC Service Controls
- Implement rate limiting on API endpoints

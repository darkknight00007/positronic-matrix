FROM python:3.10-slim

WORKDIR /app

COPY ai-agents/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY ai-agents/ .

EXPOSE 8080

CMD ["uvicorn", "api_server:app", "--host", "0.0.0.0", "--port", "8080"]

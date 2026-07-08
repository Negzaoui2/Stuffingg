# syntax=docker/dockerfile:1.7
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000
# ⚠️ Adapte "main:app" au vrai point d'entrée (voir note)
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
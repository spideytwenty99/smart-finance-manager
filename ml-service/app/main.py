"""
FastAPI service that predicts the category of a bank transaction.

Run locally from the ml-service folder:
    uvicorn app.main:app --reload --port 8000

Then open http://localhost:8000/docs to try it.
"""

from contextlib import asynccontextmanager
from typing import Literal

from fastapi import FastAPI
from pydantic import BaseModel, Field

from app.model import load_model


class PredictRequest(BaseModel):
    description: str = Field(min_length=1, max_length=255, examples=["KARTENZAHLUNG REWE FRANKFURT"])
    transactionType: Literal["EXPENSE", "INCOME"]


class PredictResponse(BaseModel):
    category: str
    confidence: float


models = {}


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Load the model once at startup, not on every request
    models["category"] = load_model()
    yield
    models.clear()


app = FastAPI(
    title="Smart Finance ML Service",
    description="Predicts the category of a bank transaction from its description.",
    version="0.1.0",
    lifespan=lifespan,
)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/predict/category", response_model=PredictResponse)
def predict_category(request: PredictRequest):
    prediction = models["category"].predict(request.description, request.transactionType)
    return PredictResponse(category=prediction.category, confidence=prediction.confidence)
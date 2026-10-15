"""
Loads the trained model once and turns transaction descriptions into
category predictions with a confidence value.

The service only relies on two things from the saved model:
predict_proba() and classes_. Any scikit-learn pipeline that offers
them (TF-IDF today, embeddings later) can replace models/model.joblib
without changing this code.
"""

import os
from dataclasses import dataclass
from pathlib import Path

import joblib

from app.preprocessing import build_input

DEFAULT_MODEL_FILE = Path(__file__).resolve().parent.parent / "models" / "model.joblib"


@dataclass
class Prediction:
    category: str
    confidence: float


class CategoryModel:

    def __init__(self, model_file: Path):
        self.pipeline = joblib.load(model_file)

    def predict(self, description: str, transaction_type: str) -> Prediction:
        text = build_input(description, transaction_type)
        probabilities = self.pipeline.predict_proba([text])[0]
        best = probabilities.argmax()
        return Prediction(
            category=str(self.pipeline.classes_[best]),
            confidence=round(float(probabilities[best]), 4),
        )


def load_model() -> CategoryModel:
    model_file = Path(os.getenv("MODEL_PATH", DEFAULT_MODEL_FILE))
    return CategoryModel(model_file)
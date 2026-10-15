"""
Shared test setup.

The tests train a small model on the synthetic data and point the service
at it via MODEL_PATH, so they never depend on models/model.joblib existing.
"""

import joblib
import pytest
from fastapi.testclient import TestClient
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import make_pipeline

from app.preprocessing import build_input
from training.generate_data import generate


@pytest.fixture(scope="session")
def model_file(tmp_path_factory):
    df = generate()
    X = [build_input(d, t) for d, t in zip(df["description"], df["transaction_type"])]
    model = make_pipeline(
        TfidfVectorizer(analyzer="char_wb", ngram_range=(3, 5)),
        LogisticRegression(max_iter=1000)
    )
    model.fit(X, df["category"])

    path = tmp_path_factory.mktemp("models") / "model.joblib"
    joblib.dump(model, path)
    return path


@pytest.fixture
def client(model_file, monkeypatch):
    monkeypatch.setenv("MODEL_PATH", str(model_file))
    from app.main import app
    with TestClient(app) as test_client:
        yield test_client
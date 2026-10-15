"""
Trains the category classifier on the synthetic data and saves it.

Evaluates a TF-IDF + Logistic Regression model with cross-validation
grouped by merchant, so every test fold only contains merchants the
model has never seen. Prints the overall score and a per-category
report, then trains on all data and saves to models/model.joblib.

Run from the ml-service folder:
    python -m training.train
"""

from pathlib import Path

import pandas as pd
import joblib

from sklearn.pipeline import make_pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import StratifiedGroupKFold, cross_val_score, cross_val_predict
from sklearn.metrics import classification_report

from app.preprocessing import build_input


BASE_DIR = Path(__file__).resolve().parent.parent
DATA_FILE = BASE_DIR / "data" / "transactions.csv"
MODEL_FILE = BASE_DIR / "models" / "model.joblib"


def main():
    # Load the dataset
    df = pd.read_csv(DATA_FILE)

    #Prepare input text and labels
    X = [
        build_input(description, transaction_type)
        for description, transaction_type
        in zip(df["description"], df["transaction_type"])
    ]

    y = df["category"]
    groups = df["merchant"]

    #Create the Logistic Regression model
    model = make_pipeline(
        TfidfVectorizer(analyzer="char_wb", ngram_range=(3, 5)),
        LogisticRegression(max_iter=1000)
    )

    #Evaluate the model
    cv = StratifiedGroupKFold(
        n_splits=4, shuffle=True, random_state=42
    )

    scores = cross_val_score(
        model, X, y,
        groups=groups,
        cv=cv,
        scoring="f1_macro"
    )

    print("Macro F1:", round(scores.mean(), 3))
    print("Standard deviation:", round(scores.std(), 3))

    # Show results for each category
    predictions = cross_val_predict(
        model, X, y,
        groups=groups,
        cv=cv
    )

    print("\nClassification Report:")
    print(classification_report(y, predictions, digits=3))

    #Train on all data
    model.fit(X, y)

    #Save the model
    MODEL_FILE.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(model, MODEL_FILE)

    print("\nModel saved to:", MODEL_FILE)


if __name__ == "__main__":
    main()
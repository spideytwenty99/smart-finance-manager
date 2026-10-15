"""
Trains the transaction category classifier and saves it.

Steps:
1. Load the synthetic dataset.
2. Split it into training and test data (80/20, stratified).
3. Find the best hyperparameters with GridSearchCV (5-fold CV on the training data).
4. Evaluate on the test data: accuracy, macro F1, classification report, confusion matrix.
5. Check how well the model handles merchants it has never seen
   (4-fold cross-validation grouped by merchant, every merchant held out once).
6. Train the final model on all data and save it to models/model.joblib.

Run from the ml-service folder:
    python -m training.train
"""

from pathlib import Path

import pandas as pd
import joblib

from sklearn.base import clone
from sklearn.pipeline import make_pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split, GridSearchCV, StratifiedGroupKFold, cross_val_predict
from sklearn.metrics import accuracy_score, f1_score, classification_report, confusion_matrix

from app.preprocessing import build_input


BASE_DIR = Path(__file__).resolve().parent.parent
DATA_FILE = BASE_DIR / "data" / "transactions.csv"
MODEL_FILE = BASE_DIR / "models" / "model.joblib"

# Print the whole confusion matrix instead of hiding columns with "..."
pd.set_option("display.max_columns", None)
pd.set_option("display.width", 250)


def main():
    # 1. Load the data
    df = pd.read_csv(DATA_FILE)

    X = [
        build_input(description, transaction_type)
        for description, transaction_type
        in zip(df["description"], df["transaction_type"])
    ]

    y = df["category"]
    groups = df["merchant"]

    # 2. Split into training and test data
    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=0.2,
        random_state=42,
        stratify=y
    )

    print("Training samples:", len(X_train))
    print("Test samples:", len(X_test))

    # 3. Create the model
    model = make_pipeline(
        TfidfVectorizer(analyzer="char_wb"),
        LogisticRegression(max_iter=1000)
    )

    # 4. Find the best hyperparameters
    parameters = {
        "tfidfvectorizer__ngram_range": [(2, 4), (3, 5)],
        "logisticregression__C": [0.1, 1, 10, 100]
    }

    search = GridSearchCV(
        model,
        parameters,
        cv=5,
        scoring="f1_macro"
    )

    search.fit(X_train, y_train)

    model = search.best_estimator_

    print("\nBest parameters:", search.best_params_)
    print("Cross-validation Macro F1:", round(search.best_score_, 3))

    # 5. Evaluate on the test data
    predictions = model.predict(X_test)

    train_accuracy = accuracy_score(y_train, model.predict(X_train))
    test_accuracy = accuracy_score(y_test, predictions)

    print("\nTraining accuracy:", round(train_accuracy, 3))
    print("Test accuracy:", round(test_accuracy, 3))
    print("Difference:", round(train_accuracy - test_accuracy, 3))
    print("Test Macro F1:", round(f1_score(y_test, predictions, average="macro"), 3))

    print("\nClassification report:")
    print(classification_report(y_test, predictions, digits=3))

    labels = sorted(y.unique())
    matrix = confusion_matrix(y_test, predictions, labels=labels)
    print("Confusion matrix (rows = true category, columns = predicted category):")
    print(pd.DataFrame(matrix, index=labels, columns=labels))

    # 6. Test on unseen merchants (every merchant is held out once)
    group_cv = StratifiedGroupKFold(n_splits=4, shuffle=True, random_state=42)

    unseen_predictions = cross_val_predict(
        clone(search.best_estimator_),
        X, y,
        groups=groups,
        cv=group_cv
    )

    print(
        "\nUnseen merchant Macro F1:",
        round(f1_score(y, unseen_predictions, average="macro"), 3)
    )

    print("\nClassification report for unseen merchants:")
    print(classification_report(y, unseen_predictions, digits=3))

    unseen_matrix = confusion_matrix(y, unseen_predictions, labels=labels)
    print("Confusion matrix for unseen merchants:")
    print(pd.DataFrame(unseen_matrix, index=labels, columns=labels))

    # 7. Train on all data and save
    model.fit(X, y)

    MODEL_FILE.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(model, MODEL_FILE)

    print("\nModel saved to:", MODEL_FILE)


if __name__ == "__main__":
    main()
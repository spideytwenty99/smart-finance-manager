"""
Trains and evaluates a Linear SVM on the transaction data, for comparison
with the Logistic Regression model in train.py.

Same steps as train.py:
1. Load the synthetic dataset.
2. Split it into training and test data (80/20, stratified).
3. Find the best hyperparameters with GridSearchCV. The search uses
   cross-validation grouped by merchant, so it picks the settings that work
   best on merchants the model has not seen (a normal CV scores 1.0 for
   almost every setting and cannot tell them apart).
4. Evaluate on the test data: accuracy, macro F1, classification report, confusion matrix.
5. Check how well the model handles merchants it has never seen
   (4-fold cross-validation grouped by merchant, every merchant held out once).

The model is not saved: LinearSVC has no predict_proba, and the service
needs confidence values, so the app uses the Logistic Regression model.

Run from the ml-service folder:
    python -m training.train_svm
"""

from pathlib import Path

import pandas as pd

from sklearn.base import clone
from sklearn.pipeline import make_pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.svm import LinearSVC
from sklearn.model_selection import train_test_split, GridSearchCV, StratifiedGroupKFold, cross_val_predict
from sklearn.metrics import accuracy_score, f1_score, classification_report, confusion_matrix

from app.preprocessing import build_input


BASE_DIR = Path(__file__).resolve().parent.parent
DATA_FILE = BASE_DIR / "data" / "transactions.csv"

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

    # 2. Split into training and test data (groups are split along)
    X_train, X_test, y_train, y_test, groups_train, groups_test = train_test_split(
        X, y, groups,
        test_size=0.2,
        random_state=42,
        stratify=y
    )

    print("Training samples:", len(X_train))
    print("Test samples:", len(X_test))

    # 3. Create the model
    model = make_pipeline(
        TfidfVectorizer(analyzer="char_wb"),
        LinearSVC()
    )

    # 4. Find the best hyperparameters
    parameters = {
        "tfidfvectorizer__analyzer": ["char_wb", "char"],
        "tfidfvectorizer__ngram_range": [(1, 3), (2, 4), (2, 5), (3, 5), (3, 6)],
        "tfidfvectorizer__sublinear_tf": [False, True],
        "linearsvc__C": [0.01, 0.03, 0.1, 0.3, 1, 3]
    }

    tuning_cv = StratifiedGroupKFold(n_splits=4, shuffle=True, random_state=42)

    search = GridSearchCV(
        model,
        parameters,
        cv=tuning_cv,
        scoring="f1_macro"
    )

    search.fit(X_train, y_train, groups=groups_train)

    model = search.best_estimator_

    print("\nBest parameters:", search.best_params_)
    print("Grouped cross-validation Macro F1:", round(search.best_score_, 3))

    print("\nTop 5 settings:")
    results = pd.DataFrame(search.cv_results_).sort_values("rank_test_score")
    print(results[["params", "mean_test_score", "std_test_score"]].head(5).to_string(index=False))

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


if __name__ == "__main__":
    main()
"""
Experiment 4: evaluates the categorization on real, hand-labeled transactions.

All models are trained on synthetic data only and tested on
data/private/real_transactions.csv (never committed). Rows labeled
"Uncategorized" have no matching category in the app; for them the
correct result is Uncategorized.

Compared:
- Keyword rules: old keywords against new keywords
- TF-IDF + Logistic Regression: trained on the old synthetic data (v1)
  against the new, more realistic synthetic data (v2)
- The app pipeline (keywords first, then the model), before and after

Run from the ml-service folder:
    python -m training.evaluate
"""

from pathlib import Path

import numpy as np
import pandas as pd

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score
from sklearn.pipeline import make_pipeline

from app.preprocessing import build_input


BASE_DIR = Path(__file__).resolve().parent.parent
OLD_SYNTHETIC_FILE = BASE_DIR / "data" / "transactions_v1.csv"
SYNTHETIC_FILE = BASE_DIR / "data" / "transactions.csv"
REAL_FILE = BASE_DIR / "data" / "private" / "real_transactions.csv"

THRESHOLD = 0.3
UNCATEGORIZED = "Uncategorized"

# Keywords before Experiment 4
OLD_KEYWORDS = {
    "EXPENSE": {
        "Groceries": "rewe,lidl,aldi,edeka,netto,penny,kaufland,tegut",
        "Eating Out": "restaurant,mcdonald,burger king,kfc,lieferando,pizza",
        "Transport": "deutsche bahn,rmv,tankstelle,aral,flixbus,uber",
        "Rent": "miete,kaltmiete",
        "Utilities": "strom,stadtwerke,mainova,telekom,vodafone",
        "Entertainment": "kino,disney",
        "Subscriptions": "netflix,spotify,amazon prime",
        "Shopping": "amazon,zalando,ikea,mediamarkt",
        "Health": "apotheke",
    },
    "INCOME": {
        "Salary": "gehalt,salary,lohn",
        "Other Income": "erstattung,refund",
    },
}

# Same keyword rules as DefaultCategories.java in the Spring app (keep both in sync)
KEYWORDS = {
    "EXPENSE": {
        "Groceries": "rewe,lidl,aldi,edeka,netto,penny,kaufland,tegut,"
                     "alnatura,bio company,famila,marktkauf,selgros,nahkauf,carrefour,auchan,migros,denner,"
                     "waitrose,rossmann,drogerie,flaschenpost,bringmeister,gorillas,hellofresh",
        "Eating Out": "restaurant,mcdonald,burger king,kfc,lieferando,pizza,"
                      "uber eats,hans im glueck,hans im glück,peter pane,frittenwerk,five guys,vapiano,"
                      "burgermeister,subway,dean&david,dean and david,le crobag,kamps,backwerk,baeckerei,"
                      "bäckerei,starbucks,nordsee",
        "Transport": "deutsche bahn,rmv,tankstelle,aral,flixbus,uber,"
                     "flixtrain,blablacar,free now,nextbike,db vertrieb,tier mobility,share now,eurowings",
        "Rent": "miete,kaltmiete",
        "Utilities": "strom,stadtwerke,mainova,telekom,vodafone,"
                     "freenet,telefonica,vattenfall,lichtblick,congstar,entega",
        "Entertainment": "kino,disney,"
                         "cinestar,cinemaxx,eventim,ticketmaster,playstation,nintendo",
        "Subscriptions": "netflix,spotify,amazon prime,"
                         "crunchyroll,paramount,hbo max,viaplay,youtube premium,audible,microsoft 365,deezer",
        "Shopping": "amazon,zalando,ikea,mediamarkt,"
                    "aliexpress,tchibo,galeria,bonprix,about you,breuninger,deichmann,snipes,adidas,"
                    "bestsecret,lascana,sephora,douglas,flaconi,notino,saturn,cyberport,notebooksbilliger,"
                    "mindfactory,wayfair,westwing,xxxlutz,hornbach,bauhaus,hagebau,hellweg,manomano,"
                    "thalia,hugendubel,medimops,momox,kleinanzeigen,vinted,booklooker,gamestop,"
                    "decathlon,intersport,sportscheck,bergfreunde,zooplus,fressnapf,zooroyal,"
                    "shein,ebay,temu,primark,tk maxx",
        "Health": "apotheke,docmorris,medpex,aponeo,fielmann,apollo optik",
    },
    "INCOME": {
        "Salary": "gehalt,salary,lohn",
        "Other Income": "erstattung,refund",
    },
}

pd.set_option("display.max_columns", None)
pd.set_option("display.width", 250)


def keyword_category(description, transaction_type, keywords=KEYWORDS):
    """Longest matching keyword wins, exactly like CategorizationService.matchKeywords."""
    text = description.lower()
    best_category = None
    best_keyword = ""
    for category, words in keywords[transaction_type].items():
        for keyword in words.split(","):
            if keyword in text and len(keyword) > len(best_keyword):
                best_category = category
                best_keyword = keyword
    return best_category


def keyword_decisions(real, keywords):
    """The keyword result for every real transaction, or Uncategorized if nothing matches."""
    decisions = []
    for description, transaction_type in zip(real["description"], real["transaction_type"]):
        category = keyword_category(description, transaction_type, keywords)
        if category is None:
            category = UNCATEGORIZED
        decisions.append(category)
    return np.array(decisions)


def to_inputs(df):
    return [build_input(d, t) for d, t in zip(df["description"], df["transaction_type"])]


def create_model():
    # Same settings as the best model in train.py
    return make_pipeline(
        TfidfVectorizer(analyzer="char_wb", ngram_range=(3, 5)),
        LogisticRegression(max_iter=1000),
    )


def model_predictions(model, train, test):
    """Trains the model and returns the predicted category and its confidence for each test row."""
    model.fit(to_inputs(train), train["category"])
    probabilities = model.predict_proba(to_inputs(test))
    predicted = model.classes_[probabilities.argmax(axis=1)]
    confidence = probabilities.max(axis=1)
    return predicted, confidence


def summarize(name, truth, decided):
    """decided = the final category, or Uncategorized when the method did not decide."""
    in_scope = truth != UNCATEGORIZED
    auto = decided != UNCATEGORIZED
    return {
        "method": name,
        "overall accuracy": accuracy_score(truth, decided),
        "in-scope auto-categorized": (in_scope & auto).sum() / in_scope.sum(),
        "accuracy when auto-categorized": accuracy_score(truth[in_scope & auto], decided[in_scope & auto]),
        "Uncategorized wrongly assigned": (~in_scope & auto).sum() / (~in_scope).sum(),
    }


def main():
    real = pd.read_csv(REAL_FILE)
    truth = real["category"].to_numpy()
    in_scope = truth != UNCATEGORIZED
    print(f"Real transactions: {len(real)} ({in_scope.sum()} with an app category, "
          f"{(~in_scope).sum()} Uncategorized)\n")

    # 1. Keyword rules
    old_keywords = keyword_decisions(real, OLD_KEYWORDS)
    new_keywords = keyword_decisions(real, KEYWORDS)

    # 2. The model, trained once on each synthetic dataset
    old_data = pd.read_csv(OLD_SYNTHETIC_FILE)
    new_data = pd.read_csv(SYNTHETIC_FILE)
    old_predicted, old_confidence = model_predictions(create_model(), old_data, real)
    new_predicted, new_confidence = model_predictions(create_model(), new_data, real)

    # Below the threshold, the model leaves the transaction Uncategorized
    old_model = np.where(old_confidence >= THRESHOLD, old_predicted, UNCATEGORIZED)
    new_model = np.where(new_confidence >= THRESHOLD, new_predicted, UNCATEGORIZED)

    # 3. The app pipeline: keywords first, then the model
    before = np.where(old_keywords != UNCATEGORIZED, old_keywords, old_model)
    after = np.where(new_keywords != UNCATEGORIZED, new_keywords, new_model)

    results = [
        summarize("Keyword rules (old)", truth, old_keywords),
        summarize("Keyword rules (new)", truth, new_keywords),
        summarize(f"TF-IDF, old synthetic data (threshold {THRESHOLD})", truth, old_model),
        summarize(f"TF-IDF, new synthetic data (threshold {THRESHOLD})", truth, new_model),
        summarize("App pipeline before (old keywords + old data)", truth, before),
        summarize("App pipeline after (new keywords + new data)", truth, after),
    ]
    print(f"=== All {len(real)} real transactions ===")
    print(pd.DataFrame(results).round(3).to_string(index=False))

    # Which real transactions does the new pipeline still get wrong?
    wrong = truth != after
    mistakes = real[wrong].assign(predicted=after[wrong])
    print("\nMost frequent mistakes of the new pipeline:")
    print(mistakes.groupby(["merchant", "category", "predicted"]).size()
          .sort_values(ascending=False).head(10).to_string())


if __name__ == "__main__":
    main()
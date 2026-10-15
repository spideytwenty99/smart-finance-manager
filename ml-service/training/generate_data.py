"""
Generates a synthetic dataset of German bank transaction descriptions.

Every row is labeled with the category it was generated for, so the data
can be used to train a supervised text classifier. All data is fictional:
no real people, accounts or transactions.

Run from the ml-service folder:
    python training/generate_data.py
"""

import random
import string
from pathlib import Path

import pandas as pd

SEED = 42
ROWS_PER_CATEGORY = 400
OUTPUT_FILE = Path(__file__).resolve().parent.parent / "data" / "transactions.csv"

CITIES = [
    "FRANKFURT", "FRANKFURT AM MAIN", "OFFENBACH", "DARMSTADT", "WIESBADEN",
    "MAINZ", "BAD HOMBURG", "ESCHBORN", "BERLIN", "MUENCHEN", "HAMBURG", "KOELN",
]
MONTHS = ["JAN", "FEB", "MRZ", "APR", "MAI", "JUN", "JUL", "AUG", "SEP", "OKT", "NOV", "DEZ"]

# One entry per category. Names must match the categories in the Spring app.
CATEGORIES = {
    "Groceries": {
        "type": "EXPENSE",
        "merchants": ["REWE", "REWE MARKT GMBH", "LIDL", "LIDL DIENSTLEISTUNG", "ALDI SUED",
                      "EDEKA", "EDEKA CENTER", "PENNY", "NETTO MARKEN-DISCOUNT", "KAUFLAND",
                      "TEGUT", "DM DROGERIE MARKT", "ROSSMANN"],
        "payment_methods": ["card", "online"],
    },
    "Eating Out": {
        "type": "EXPENSE",
        "merchants": ["MCDONALDS", "BURGER KING", "VAPIANO", "L OSTERIA", "PIZZERIA NAPOLI",
                      "SUBWAY", "STARBUCKS", "KAMPS BAECKEREI", "LIEFERANDO", "WOLT",
                      "DEAN AND DAVID", "DOENER PALAST", "CAFE EXTRABLATT"],
        "payment_methods": ["card", "online"],
    },
    "Transport": {
        "type": "EXPENSE",
        "merchants": ["DB VERTRIEB GMBH", "DB FERNVERKEHR", "RMV", "SHELL", "ARAL", "ESSO",
                      "TOTALENERGIES", "UBER", "FREE NOW", "TIER MOBILITY", "LIME", "FLIXBUS",
                      "PARKHAUS HAUPTWACHE"],
        "payment_methods": ["card", "online", "debit"],
    },
    "Rent": {
        "type": "EXPENSE",
        "merchants": ["HAUSVERWALTUNG SCHNEIDER", "VONOVIA", "ABG FRANKFURT HOLDING",
                      "STUDIERENDENWERK FRANKFURT", "WOHNBAU MAINBLICK GMBH"],
        "payment_methods": ["transfer"],
        "purposes": ["MIETE", "MIETE INKL NK", "KALTMIETE UND NEBENKOSTEN", "MIETE WOHNUNG"],
    },
    "Utilities": {
        "type": "EXPENSE",
        "merchants": ["MAINOVA", "STADTWERKE OFFENBACH", "VATTENFALL", "E.ON ENERGIE",
                      "TELEKOM DEUTSCHLAND", "VODAFONE", "O2 TELEFONICA", "1UND1",
                      "RUNDFUNK ARD ZDF DRADIO", "LICHTBLICK"],
        "payment_methods": ["debit", "online"],
    },
    "Entertainment": {
        "type": "EXPENSE",
        "merchants": ["CINESTAR KINO", "UCI KINOWELT", "EVENTIM", "STEAM", "PLAYSTATION STORE",
                      "NINTENDO ESHOP", "BOWLING WORLD", "DISNEY PLUS", "JUMP HOUSE"],
        "payment_methods": ["card", "online"],
    },
    "Subscriptions": {
        "type": "EXPENSE",
        "merchants": ["NETFLIX", "SPOTIFY", "AMAZON PRIME", "APPLE.COM BILL", "YOUTUBE PREMIUM",
                      "AUDIBLE", "MCFIT", "URBAN SPORTS CLUB", "ADOBE", "MICROSOFT 365"],
        "payment_methods": ["debit", "online"],
    },
    "Shopping": {
        "type": "EXPENSE",
        "merchants": ["AMAZON", "AMAZON MARKETPLACE", "ZALANDO", "H UND M", "PRIMARK",
                      "MEDIAMARKT", "SATURN", "IKEA", "DECATHLON", "OTTO", "EBAY", "THALIA",
                      "TK MAXX"],
        "payment_methods": ["card", "online"],
    },
    "Health": {
        "type": "EXPENSE",
        "merchants": ["ADLER APOTHEKE", "LINDEN APOTHEKE", "APOTHEKE AM BAHNHOF", "DOCMORRIS",
                      "SHOP APOTHEKE", "ZAHNARZTPRAXIS DR WEBER", "FIELMANN",
                      "PHYSIOTHERAPIE MAINUFER"],
        "payment_methods": ["card", "online", "transfer"],
        "purposes": ["RECHNUNG", "BEHANDLUNG", "RE-NR"],
    },
    "Salary": {
        "type": "INCOME",
        "merchants": ["MAINBLICK SOFTWARE GMBH", "RHEIN-MAIN LOGISTIK AG",
                      "CAFE AM ROEMER GMBH", "HESSEN DATA SERVICES"],
        "payment_methods": ["salary"],
    },
    "Other Income": {
        "type": "INCOME",
        "merchants": ["FINANZAMT FRANKFURT", "FAMILIENKASSE", "BUNDESAMT BAFOEG",
                      "AMAZON EU RETOURE", "ZALANDO RETOURE", "PAYPAL RUECKZAHLUNG",
                      "TECHNIKER KRANKENKASSE", "ZINSEN TAGESGELD"],
        "payment_methods": ["refund"],
    },
}

# Text patterns for each payment method. {placeholders} are filled in randomly.
TEMPLATES = {
    "card": [
        "KARTENZAHLUNG {merchant} {city}",
        "{merchant}//{city}/DE {date}",
        "GIROCARD {merchant} {number} {city}",
        "VISA {merchant} {city} {date}",
        "APPLE PAY {merchant} {city}",
    ],
    "online": [
        "PAYPAL *{merchant}",
        "PAYPAL EUROPE {merchant} {ref}",
        "VISA {merchant}.DE {ref}",
        "{merchant} SAGT DANKE {ref}",
    ],
    "debit": [
        "SEPA-LASTSCHRIFT {merchant} {ref} KD-NR {number}",
        "LASTSCHRIFT {merchant} MANDAT {ref}",
        "{merchant} ABBUCHUNG {month} 2026",
    ],
    "transfer": [
        "DAUERAUFTRAG {merchant} {purpose} {month} 2026",
        "UEBERWEISUNG {merchant} {purpose} {number}",
    ],
    "salary": [
        "GEHALT {month}/2026 {merchant}",
        "LOHN/GEHALT {merchant} PERS-NR {number}",
        "{merchant} ENTGELT {month} 2026",
    ],
    "refund": [
        "GUTSCHRIFT {merchant} {ref}",
        "RUECKERSTATTUNG {merchant} {ref}",
        "ERSTATTUNG {merchant} {date}",
    ],
}


def random_ref():
    return "".join(random.choices(string.ascii_uppercase + string.digits, k=10))


def make_description(info, merchant, method):
    template = random.choice(TEMPLATES[method])
    return template.format(
        merchant=merchant,
        city=random.choice(CITIES),
        date=f"{random.randint(1, 28):02d}.{random.randint(1, 12):02d}.2026",
        month=random.choice(MONTHS),
        number=random.randint(1000, 99999),
        ref=random_ref(),
        purpose=random.choice(info.get("purposes", ["ZAHLUNG"])),
    )


def generate():
    random.seed(SEED)
    rows = []
    for category, info in CATEGORIES.items():
        for _ in range(ROWS_PER_CATEGORY):
            merchant = random.choice(info["merchants"])
            method = random.choice(info["payment_methods"])
            rows.append({
                "description": make_description(info, merchant, method),
                "transaction_type": info["type"],
                "category": category,
                "merchant": merchant,
            })
    return pd.DataFrame(rows).sample(frac=1, random_state=SEED)


def main():
    df = generate()
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(OUTPUT_FILE, index=False)
    print(f"Wrote {len(df)} rows to {OUTPUT_FILE}")
    print(df["category"].value_counts())


if __name__ == "__main__":
    main()
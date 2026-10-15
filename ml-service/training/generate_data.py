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
HINT_PROBABILITY = 0.5  # share of rows that get a category word like "TANKSTELLE"
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
        "merchants": ["REWE", "LIDL", "ALDI SUED", "EDEKA", "PENNY", "NETTO MARKEN-DISCOUNT",
                      "KAUFLAND", "TEGUT", "DM DROGERIE MARKT", "ROSSMANN", "NORMA", "GLOBUS",
                      "MARKTKAUF", "ALNATURA", "DENNS BIOMARKT", "HIT MARKT", "NAH UND GUT",
                      "ASIA MARKT LOTUS", "ANADOLU FEINKOST", "BIO COMPANY"],
        "payment_methods": ["card", "online"],
        "hints": ["SUPERMARKT", "LEBENSMITTEL", "DISCOUNTER", "MARKT"],
    },
    "Eating Out": {
        "type": "EXPENSE",
        "merchants": ["MCDONALDS", "BURGER KING", "VAPIANO", "L OSTERIA", "PIZZERIA NAPOLI",
                      "SUBWAY", "STARBUCKS", "KAMPS BAECKEREI", "LIEFERANDO", "WOLT",
                      "DEAN AND DAVID", "DOENER PALAST", "CAFE EXTRABLATT", "KFC", "NORDSEE",
                      "BACKWERK", "HANS IM GLUECK", "PETER PANE", "SUSHI CIRCLE",
                      "TRATTORIA ROMA", "CURRY 36", "THAI WOK"],
        "payment_methods": ["card", "online"],
        "hints": ["RESTAURANT", "IMBISS", "CAFE", "LIEFERSERVICE", "GASTRONOMIE"],
    },
    "Transport": {
        "type": "EXPENSE",
        "merchants": ["DB VERTRIEB GMBH", "RMV", "SHELL", "ARAL", "ESSO", "TOTALENERGIES",
                      "UBER", "FREE NOW", "TIER MOBILITY", "LIME", "FLIXBUS",
                      "PARKHAUS HAUPTWACHE", "JET", "AVIA", "BOLT", "SHARE NOW",
                      "MILES MOBILITY", "BVG", "MVV", "APCOA PARKING", "SIXT", "EUROWINGS"],
        "payment_methods": ["card", "online", "debit"],
        "hints": ["TANKSTELLE", "FAHRKARTE", "TICKET", "PARKEN", "MOBILITAET"],
    },
    "Rent": {
        "type": "EXPENSE",
        "merchants": ["HAUSVERWALTUNG SCHNEIDER", "VONOVIA", "ABG FRANKFURT HOLDING",
                      "STUDIERENDENWERK FRANKFURT", "WOHNBAU MAINBLICK GMBH", "LEG IMMOBILIEN",
                      "NASSAUISCHE HEIMSTAETTE", "IMMOBILIEN KRAUSE"],
        "payment_methods": ["transfer"],
        "purposes": ["MIETE", "MIETE INKL NK", "KALTMIETE UND NEBENKOSTEN", "MIETE WOHNUNG"],
    },
    "Utilities": {
        "type": "EXPENSE",
        "merchants": ["MAINOVA", "STADTWERKE OFFENBACH", "VATTENFALL", "E.ON ENERGIE",
                      "TELEKOM DEUTSCHLAND", "VODAFONE", "O2 TELEFONICA", "1UND1",
                      "RUNDFUNK ARD ZDF DRADIO", "LICHTBLICK", "ENBW", "EWE TEL", "CONGSTAR",
                      "FRAENK", "NETCOLOGNE", "OSTROM", "TIBBER", "ENTEGA", "SUWAG ENERGIE",
                      "HESSENWASSER"],
        "payment_methods": ["debit", "online", "transfer"],
        "purposes": ["RECHNUNG", "ABSCHLAG", "VERTRAGSNR"],
        "hints": ["STROM", "GAS", "MOBILFUNK", "INTERNET", "ABSCHLAG"],
    },
    "Entertainment": {
        "type": "EXPENSE",
        "merchants": ["CINESTAR KINO", "UCI KINOWELT", "EVENTIM", "STEAM", "PLAYSTATION STORE",
                      "NINTENDO ESHOP", "BOWLING WORLD", "DISNEY PLUS", "JUMP HOUSE", "CINEMAXX",
                      "TICKETMASTER", "XBOX STORE", "ESCAPE ROOM FRANKFURT", "ZOO FRANKFURT",
                      "SENCKENBERG MUSEUM", "LASERTAG ARENA", "MINIGOLF PARK", "EPIC GAMES"],
        "payment_methods": ["card", "online", "transfer"],
        "purposes": ["TICKETS", "BESTELLUNG", "RECHNUNG"],
        "hints": ["KINO", "TICKETS", "GAMES", "FREIZEIT"],
    },
    "Subscriptions": {
        "type": "EXPENSE",
        "merchants": ["NETFLIX", "SPOTIFY", "AMAZON PRIME", "APPLE.COM BILL", "YOUTUBE PREMIUM",
                      "AUDIBLE", "MCFIT", "URBAN SPORTS CLUB", "ADOBE", "MICROSOFT 365", "DAZN",
                      "WOW TV", "SKY DEUTSCHLAND", "DEEZER", "GOOGLE ONE", "DROPBOX", "FITX",
                      "CLEVER FIT", "KINDLE UNLIMITED", "ZEIT ONLINE"],
        "payment_methods": ["debit", "online"],
        "hints": ["ABO", "MITGLIEDSCHAFT", "MONATSBEITRAG", "PREMIUM"],
    },
    "Shopping": {
        "type": "EXPENSE",
        "merchants": ["AMAZON", "ZALANDO", "H UND M", "PRIMARK", "MEDIAMARKT", "SATURN", "IKEA",
                      "DECATHLON", "OTTO", "EBAY", "THALIA", "TK MAXX", "ABOUT YOU", "C UND A",
                      "ZARA", "DOUGLAS", "HORNBACH", "OBI", "NOTEBOOKSBILLIGER", "CYBERPORT",
                      "SHEIN", "DEICHMANN"],
        "payment_methods": ["card", "online", "transfer"],
        "purposes": ["RECHNUNG", "BESTELLUNG"],
        "hints": ["ONLINESHOP", "BESTELLUNG", "MODE", "ELEKTRONIK"],
    },
    "Health": {
        "type": "EXPENSE",
        "merchants": ["ADLER APOTHEKE", "LINDEN APOTHEKE", "APOTHEKE AM BAHNHOF", "DOCMORRIS",
                      "SHOP APOTHEKE", "ZAHNARZTPRAXIS DR WEBER", "FIELMANN",
                      "PHYSIOTHERAPIE MAINUFER", "APOLLO OPTIK", "HAUSARZTPRAXIS DR KLEIN",
                      "ORTHOPAEDIE ZENTRUM FFM", "MEDPEX", "LABOR SYNLAB", "HOERGERAETE KIND",
                      "DERMATOLOGIE PRAXIS"],
        "payment_methods": ["card", "online", "transfer"],
        "purposes": ["RECHNUNG", "BEHANDLUNG", "RE-NR"],
        "hints": ["APOTHEKE", "PRAXIS", "GESUNDHEIT", "OPTIKER"],
    },
    "Salary": {
        "type": "INCOME",
        "merchants": ["MAINBLICK SOFTWARE GMBH", "RHEIN-MAIN LOGISTIK AG", "CAFE AM ROEMER GMBH",
                      "HESSEN DATA SERVICES", "TAUNUS CONSULTING GMBH", "NORDEND IT SOLUTIONS",
                      "OFFENBACHER BAU GMBH", "DIGITAL HUB FRANKFURT"],
        "payment_methods": ["salary"],
    },
    "Other Income": {
        "type": "INCOME",
        "merchants": ["FINANZAMT FRANKFURT", "FAMILIENKASSE", "BUNDESAMT BAFOEG",
                      "AMAZON EU RETOURE", "ZALANDO RETOURE", "PAYPAL RUECKZAHLUNG",
                      "TECHNIKER KRANKENKASSE", "ZINSEN TAGESGELD", "AOK HESSEN",
                      "DEUTSCHE RENTENVERSICHERUNG", "VINTED AUSZAHLUNG", "KLEINANZEIGEN ZAHLUNG"],
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
    # Real bank texts often name the kind of business, e.g. "SHELL TANKSTELLE".
    if "hints" in info and random.random() < HINT_PROBABILITY:
        merchant = f"{merchant} {random.choice(info['hints'])}"

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
    return pd.DataFrame(rows)


def main():
    df = generate()
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(OUTPUT_FILE, index=False)
    print(f"Wrote {len(df)} rows to {OUTPUT_FILE}")
    print(df["category"].value_counts())


if __name__ == "__main__":
    main()
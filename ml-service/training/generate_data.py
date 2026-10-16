"""
Generates a synthetic dataset of German bank transaction descriptions.

Every row is labeled with the category it was generated for, so the data
can be used to train a supervised text classifier. All data is fictional:
no real people, accounts or transactions.

Version 2 (Experiment 4) makes the data more realistic:
- Merchant names: the original hand-written lists plus 227 real merchants
  from public sources (see merchants.py).
- Bank formats seen on real statements: payment provider prefixes
  ("SUMUP *", "PAYPAL *"), branch numbers, legal forms (GMBH, OHG) and
  descriptions cut off after 22 characters.
- A new "Uncategorized" class for transactions without an app category:
  top-ups, transfers, cash, bank fees and hotels. These are built from
  standard German booking texts, not from brand names.

Run from the ml-service folder:
    python -m training.generate_data
"""

import random
import string
from pathlib import Path

import pandas as pd

from training.merchants import MERCHANTS

SEED = 42
ROWS_PER_CATEGORY = 400
HINT_PROBABILITY = 0.3      # share of rows that get a category word like "TANKSTELLE"
LEGAL_FORM_PROBABILITY = 0.2  # share of rows that get "GMBH", "OHG", ...
CUT_OFF_PROBABILITY = 0.3   # share of card payments cut off after 22 characters
CARD_TEXT_LENGTH = 22       # card terminals often send only 22 characters

OUTPUT_FILE = Path(__file__).resolve().parent.parent / "data" / "transactions.csv"

CITIES = [
    "FRANKFURT", "FRANKFURT AM MAIN", "OFFENBACH", "DARMSTADT", "WIESBADEN",
    "MAINZ", "BAD HOMBURG", "ESCHBORN", "BERLIN", "MUENCHEN", "HAMBURG", "KOELN",
]
MONTHS = ["JAN", "FEB", "MRZ", "APR", "MAI", "JUN", "JUL", "AUG", "SEP", "OKT", "NOV", "DEZ"]
LEGAL_FORMS = ["GMBH", "GMBH & CO KG", "OHG", "E.K.", "AG", "KG"]

# One entry per category. Names must match the categories in the Spring app.
# "merchants" are the original hand-written lists; the real merchants from
# merchants.py are added on top in all_merchants().
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
        "hints": ["RESTAURANT", "IMBISS", "CAFE", "LIEFERSERVICE", "BAECKEREI"],
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
        # Short formats as they appear on real card statements
        "{merchant} {city}",
        "{merchant} FIL {number}",
        "{merchant} {number}",
        "SUMUP *{merchant}",
        "NYX*{merchant}",
        "{merchant} SAGT DANKE",
    ],
    "online": [
        "PAYPAL *{merchant}",
        "PAYPAL EUROPE {merchant} {ref}",
        "VISA {merchant}.DE {ref}",
        "{merchant} SAGT DANKE {ref}",
        # Formats of online shops and apps
        "{merchant}* {ref}",
        "{merchant}.COM",
        "WWW.{merchant}.DE",
        "{merchant} PAYMENTS",
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

# Uncategorized: transactions that belong to no app category.
# Each entry is (merchant, list of description patterns).
UNCATEGORIZED_EXPENSE = [
    ("TOP-UP", ["{bank} TOP-UP", "{bank} TOPUP {ref}", "AUFLADUNG {bank}"]),
    ("PRIVATE TRANSFER", ["UEBERWEISUNG {ref}", "ECHTZEITUEBERWEISUNG {number}",
                          "DAUERAUFTRAG {number}"]),
    ("CASH WITHDRAWAL", ["BARGELDAUSZAHLUNG {city}", "GELDAUTOMAT {city} {number}",
                         "AUSZAHLUNG GAA {number}"]),
    ("BANK FEES", ["SOLLZINSEN", "ENTGELT KONTOFUEHRUNG", "KONTOFUEHRUNGSGEBUEHR {month}",
                   "ZINSEN DISPO {month} 2026"]),
    ("SAVINGS", ["UMBUCHUNG TAGESGELD", "UEBERTRAG AUF SPARKONTO", "SPARPLAN {ref}"]),
]
UNCATEGORIZED_INCOME = [
    ("CASH DEPOSIT", ["EINZAHLUNG", "BAREINZAHLUNG {city}", "EINZAHLUNG GAA {number}"]),
    ("PRIVATE TRANSFER", ["GUTSCHRIFT UEBERWEISUNG {ref}", "ECHTZEITGUTSCHRIFT {number}"]),
    ("OWN ACCOUNT", ["ZAHLUNG AUFS GIROKONTO", "UEBERTRAG VON {bank}", "UMBUCHUNG VON TAGESGELD"]),
]
UNCATEGORIZED_INCOME_SHARE = 0.3  # 30 % of the Uncategorized rows are income
BANKS = ["N26", "REVOLUT", "WISE", "BUNQ", "VIVID", "C24", "TOMORROW", "DKB", "ING", "COMDIRECT"]


def random_ref():
    return "".join(random.choices(string.ascii_uppercase + string.digits, k=10))


def bank_style(name):
    """Writes a merchant name the way banks do: upper case, sometimes without umlauts."""
    name = name.upper()
    if random.random() < 0.5:
        name = name.replace("Ä", "AE").replace("Ö", "OE").replace("Ü", "UE").replace("ß", "SS")
    return name


def all_merchants(category, info):
    """Original merchants plus the real merchants from merchants.py, without duplicates."""
    merchants = list(info["merchants"])
    for name in MERCHANTS.get(category, []):
        if name.upper() not in merchants:
            merchants.append(name.upper())
    return merchants


def fill_template(template, merchant="", purpose="ZAHLUNG"):
    return template.format(
        merchant=merchant,
        city=random.choice(CITIES),
        date=f"{random.randint(1, 28):02d}.{random.randint(1, 12):02d}.2026",
        month=random.choice(MONTHS),
        number=random.randint(1000, 99999),
        ref=random_ref(),
        purpose=purpose,
        bank=random.choice(BANKS),
    )


def make_description(info, merchant, method):
    name = bank_style(merchant)

    # Real bank texts often name the kind of business, e.g. "SHELL TANKSTELLE".
    if "hints" in info and random.random() < HINT_PROBABILITY:
        name = f"{name} {random.choice(info['hints'])}"

    # Some businesses add their legal form, e.g. "KAMPS GMBH".
    if random.random() < LEGAL_FORM_PROBABILITY:
        name = f"{name} {random.choice(LEGAL_FORMS)}"

    template = random.choice(TEMPLATES[method])
    purpose = random.choice(info.get("purposes", ["ZAHLUNG"]))
    description = fill_template(template, name, purpose)

    # Card terminals often cut the text off, e.g. "NEW YORKER DRITTE RETA".
    if method == "card" and random.random() < CUT_OFF_PROBABILITY:
        description = description[:CARD_TEXT_LENGTH].strip()

    return description


def make_uncategorized_row():
    if random.random() < UNCATEGORIZED_INCOME_SHARE:
        transaction_type = "INCOME"
        merchant, patterns = random.choice(UNCATEGORIZED_INCOME)
        description = fill_template(random.choice(patterns))
    else:
        transaction_type = "EXPENSE"
        # Every fourth expense is a hotel or booking site from merchants.py
        if random.random() < 0.25:
            merchant = random.choice(MERCHANTS["Uncategorized"]).upper()
            description = fill_template(random.choice(TEMPLATES["online"]), merchant)
        else:
            merchant, patterns = random.choice(UNCATEGORIZED_EXPENSE)
            description = fill_template(random.choice(patterns))

    return {
        "description": description,
        "transaction_type": transaction_type,
        "category": "Uncategorized",
        "merchant": merchant,
    }


def generate():
    random.seed(SEED)
    rows = []

    for category, info in CATEGORIES.items():
        merchants = all_merchants(category, info)
        for _ in range(ROWS_PER_CATEGORY):
            merchant = random.choice(merchants)
            method = random.choice(info["payment_methods"])
            rows.append({
                "description": make_description(info, merchant, method),
                "transaction_type": info["type"],
                "category": category,
                "merchant": merchant,
            })

    for _ in range(ROWS_PER_CATEGORY):
        rows.append(make_uncategorized_row())

    return pd.DataFrame(rows)


def main():
    df = generate()
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(OUTPUT_FILE, index=False)
    print(f"Wrote {len(df)} rows to {OUTPUT_FILE}")
    print(df["category"].value_counts())


if __name__ == "__main__":
    main()
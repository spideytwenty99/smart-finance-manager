from app.preprocessing import build_input, clean


def test_clean_removes_numbers_codes_and_punctuation():
    assert clean("SEPA-LASTSCHRIFT NETFLIX K7XQ2M9TB4 KD-NR 48291") == "sepa lastschrift netflix kd nr"


def test_clean_splits_glued_text_and_removes_dates():
    assert clean("KARTENZAHLUNG REWE//FRANKFURT/DE 05.10.2026") == "kartenzahlung rewe frankfurt de"


def test_clean_keeps_short_names_with_digits():
    assert clean("LASTSCHRIFT 1UND1 MANDAT AB12CD34EF") == "lastschrift 1und1 mandat"


def test_clean_ignores_upper_and_lower_case():
    assert clean("REWE Markt") == clean("rewe markt")


def test_build_input_puts_type_first():
    assert build_input("KARTENZAHLUNG REWE", "EXPENSE") == "expense kartenzahlung rewe"
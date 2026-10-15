def test_health(client):
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_predicts_known_merchant(client):
    response = client.post("/predict/category", json={
        "description": "KARTENZAHLUNG REWE FRANKFURT 05.10.2026",
        "transactionType": "EXPENSE",
    })

    assert response.status_code == 200
    assert response.json()["category"] == "Groceries"


def test_confidence_is_between_0_and_1(client):
    response = client.post("/predict/category", json={
        "description": "SEPA-LASTSCHRIFT NETFLIX K7XQ2M9TB4",
        "transactionType": "EXPENSE",
    })

    confidence = response.json()["confidence"]
    assert 0 <= confidence <= 1


def test_income_goes_to_income_category(client):
    response = client.post("/predict/category", json={
        "description": "GEHALT OKT/2026 MAINBLICK SOFTWARE GMBH",
        "transactionType": "INCOME",
    })

    assert response.json()["category"] == "Salary"


def test_unknown_merchant_has_low_confidence(client):
    response = client.post("/predict/category", json={
        "description": "PAYPAL *BLORX",
        "transactionType": "EXPENSE",
    })

    assert response.json()["confidence"] < 0.3


def test_rejects_empty_description(client):
    response = client.post("/predict/category", json={
        "description": "",
        "transactionType": "EXPENSE",
    })

    assert response.status_code == 422


def test_rejects_invalid_transaction_type(client):
    response = client.post("/predict/category", json={
        "description": "KARTENZAHLUNG REWE",
        "transactionType": "SOMETHING",
    })

    assert response.status_code == 422
# Smart Finance Manager

![CI](https://github.com/spideytwenty99/smart-finance-manager/actions/workflows/ci.yml/badge.svg)

A REST API for personal finance: track income and expenses, set monthly budgets per category, and get a monthly report showing where your money went. Transactions without a category are categorized automatically, first by the user's keyword rules and then by a machine learning model. Every user has their own account, and all data is private to that user.

Evaluated on 649 real, hand-labeled bank transactions, the automatic categorization assigns the correct category in **82%** of cases (up from 64%), and when it assigns a category, it is correct **98%** of the time.

## Features

- **User accounts**: register and log in with email and password; passwords are hashed with BCrypt
- **JWT authentication**: login returns a signed token (HS256, valid for one hour) that is sent as a Bearer token with every request
- **Private data per user**: every category, transaction and budget belongs to one user; other users' data returns 404, as if it didn't exist
- **Transactions**: create, update, delete and list, filtered by month, date range or category
- **Automatic categorization in two stages**:
  - **Keyword rules**: "REWE Frankfurt Hbf" lands in Groceries; the longest matching keyword wins, and only the user's own categories are considered
  - **Machine learning**: transactions no rule covers are sent to an ML service; its prediction is used only when the model is confident enough, otherwise the transaction stays Uncategorized
- **Default categories**: every new user gets their own starter set with German-relevant keywords, which they can rename, extend or delete
- **Monthly budgets**: one spending limit per expense category and month
- **Monthly report**: income, expenses, balance, and per category how much of the budget is used (OK, WARNING from 80%, OVER_LIMIT above 100%)
- **Consistent errors**: every error, including 401 Unauthorized, has the same JSON shape, with field-level messages for validation errors
- **Logging**: important events are logged by user id only, never passwords, tokens, emails or financial details
- **API documentation**: OpenAPI with Swagger UI for both services
- **Tests**: unit and controller tests for the backend, API tests for the ML service, all run on every push with GitHub Actions

## Architecture

```
┌──────────────┐       ┌───────────────────────┐       ┌──────────────────┐
│    Client    │ ───▶  │   Spring Boot API     │ ───▶  │   PostgreSQL     │
│ (Swagger UI) │       │   :8080               │       │   :5433          │
└──────────────┘       └───────────┬───────────┘       └──────────────────┘
                                   │ POST /predict/category
                                   ▼
                       ┌───────────────────────┐
                       │ ML service (FastAPI)  │
                       │   :8000               │
                       └───────────────────────┘
```

When a transaction is created without a category, the Spring app decides in this order:

1. **Keyword rules**: the user's own rules always win.
2. **ML model**: used only if its confidence is at least 0.3 and the user has a category with the predicted name.
3. **Uncategorized**: if neither applies.

If the ML service is unavailable, the app continues without it. Creating a transaction never fails because of the ML service.

## Machine Learning

The ML service (`ml-service/`) classifies bank transaction descriptions into categories using **TF-IDF character n-grams + Logistic Regression** (scikit-learn).

**Training data.** No public dataset of labeled German bank transactions exists, so the model is trained on a **synthetic dataset** of 4,800 transactions. It uses 369 merchants, including 227 real merchant names collected from 12 public sources (`ml-service/training/merchants.py`), typical German bank text formats (card payments, PayPal, SumUp, SEPA direct debits, standing orders, salary, refunds), and an `Uncategorized` class for transfers, top-ups, cash and bank fees.

**Evaluation on real data.** The system was tested on 649 real bank transactions from four people, anonymized and labeled by hand. They are used for testing only and are never committed. Results for the full app pipeline (keyword rules first, then the model with a confidence threshold of 0.3):

| | Before | After |
|---|---|---|
| Overall accuracy | 0.641 | **0.818** |
| Accuracy when a category is assigned | 0.920 | **0.976** |
| Transfers, top-ups etc. wrongly given a category | 36.5% | **0.6%** |

"Before" is the first version (smaller synthetic dataset, 45 keywords); "after" adds the realistic synthetic data and 150 keywords from the same public sources. No real transaction was used for training, and merchant names were never taken from the real data.

The full experiment log, including a comparison with a Linear SVM, evaluation on unseen merchants and the reasoning behind each decision, is in [docs/ml-experiments.md](docs/ml-experiments.md).

## Tech Stack

**Backend**
- Java 21, Spring Boot 4
- Spring Web, Spring Data JPA (Hibernate), Bean Validation
- Spring Security, OAuth2 Resource Server (JWT)
- PostgreSQL 17
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, MockMvc, Spring Security Test
- Maven, Lombok, SLF4J

**ML service**
- Python 3.12, FastAPI, uvicorn
- scikit-learn, pandas, joblib
- pytest

**Infrastructure**
- Docker, Docker Compose, GitHub Actions

## Quick Start

You only need [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
git clone https://github.com/spideytwenty99/smart-finance-manager.git
cd smart-finance-manager
cp .env.example .env        # on Windows PowerShell: copy .env.example .env
```

Open `.env` and set `JWT_SECRET` to a random Base64 key of at least 32 bytes. You can generate one with:

```bash
openssl rand -base64 32
```

or in PowerShell:

```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Then start everything:

```bash
docker compose up --build
```

This starts the database, the Spring API and the ML service. The ML model is trained automatically while its image is built.

- Spring API: **http://localhost:8080/swagger-ui.html**
- ML service: **http://localhost:8000/docs**

![Swagger UI](docs/images/swagger-ui.png)

## Trying It Out

**1. Register** with `POST /api/auth/register`:

```json
{
  "email": "alice@example.com",
  "password": "supersecret123"
}
```

The response contains a token:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Your account starts with its own set of default categories. Later, use `POST /api/auth/login` with the same body to get a new token.

**2. Authorize.** Click **Authorize** at the top of Swagger UI and paste the token, without the word "Bearer". All following requests send it automatically.

**3. Create a transaction** with `POST /api/transactions`:

```json
{
  "amount": 12.50,
  "description": "REWE Frankfurt Hbf",
  "transactionDate": "2026-10-05",
  "transactionType": "EXPENSE"
}
```

The keyword rule matches, and the response shows the category Groceries with `"autoCategorized": true`.

**4. Let the model decide.** Use a description that no keyword covers:

```json
{
  "amount": 23.40,
  "description": "KARTENZAHLUNG NORMA FRANKFURT",
  "transactionDate": "2026-10-05",
  "transactionType": "EXPENSE"
}
```

This time the ML service predicts Groceries. A description the model doesn't recognize, like `PAYPAL *BLORX`, stays Uncategorized.

You can also ask the ML service directly at http://localhost:8000/docs with `POST /predict/category`, which returns the predicted category and the model's confidence.

Outside Swagger, send the token in the `Authorization` header:

```bash
curl http://localhost:8080/api/transactions \
  -H "Authorization: Bearer <your token>"
```

Requests without a valid token return `401` with the usual error format.

## Running the Tests

**Backend.** One test starts the whole application, so the database container needs to be running:

```bash
docker compose up -d db
./mvnw test
```

Set `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET` to the values from your `.env` first.

**ML service.** From the `ml-service` folder, with Python 3.12:

```bash
pip install -r requirements.txt
python -m pytest
```

The tests train their own small model, so no trained model file is needed. Both test suites run automatically on GitHub for every push.

## Running from an IDE

Start only the database with `docker compose up -d db`, set the environment variables `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET` to the values from `.env`, and run `SmartFinanceManagerApplication`. The database is reachable on port 5433, so it doesn't clash with a local PostgreSQL on 5432.

To use the ML model from the IDE as well, start the ML service in a second terminal:

```bash
cd ml-service
python -m training.train
uvicorn app.main:app --port 8000
```

The Spring app reaches it at `http://localhost:8000` by default. Without it, categorization simply falls back to keyword rules and Uncategorized.

## Documentation

- [OpenAPI specification](docs/api/openapi.json): import it into Postman, or paste it into [editor.swagger.io](https://editor.swagger.io)
- [ML experiments](docs/ml-experiments.md): data, models, evaluation and the reasoning behind each decision
- [Requirements](docs/requirements.md)
- [Architecture](docs/architecture.md)
- [Database Design](docs/database-design.md)
- [API Design](docs/api-design.md)
- [Devlog](docs/devlog.md): what was built when, and the decisions behind it

## Project Structure

```
smart-finance-manager
├── src/main/java/com/saurabh/financemanager
│   ├── authentication  register, login, JWT creation, 401 handling
│   ├── user            user accounts
│   ├── category        categories, default categories, keyword + ML categorization
│   ├── ml              client for the ML service
│   ├── transaction     transactions
│   ├── budget          monthly budgets
│   ├── report          monthly report
│   ├── exception       error handling
│   └── config          security and OpenAPI configuration
└── ml-service
    ├── app             FastAPI service, model loading, text preprocessing
    ├── training        public merchant lists, synthetic data generator, training and evaluation
    ├── data            synthetic datasets (real test data stays local and is never committed)
    └── tests           pytest tests
```

## Next

- Learning from user corrections, so a merchant is categorized correctly after the first manual change
- Database migrations with Flyway instead of Hibernate schema updates
- An AI assistant that answers questions about your spending and budgets
- A small React frontend with a dashboard, transactions and budgets

## Author

Saurabh, Informatik student at Frankfurt University of Applied Sciences
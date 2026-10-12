# Smart Finance Manager

![CI](https://github.com/spideytwenty99/smart-finance-manager/actions/workflows/ci.yml/badge.svg)

A REST API for personal finance: track income and expenses, set monthly budgets per category, and get a monthly report showing where your money went. Transactions without a category are categorized automatically from their description.

## Features

- **Transactions**: create, update, delete and list, filtered by month, date range or category
- **Automatic categorization**: "REWE Frankfurt Hbf" lands in Groceries by itself; the longest matching keyword wins, and a category chosen by the user is never overridden
- **Built-in categories**: a starter set with German-relevant keywords, plus your own categories
- **Monthly budgets**: one spending limit per expense category and month
- **Monthly report**: income, expenses, balance, and per category how much of the budget is used (OK, WARNING from 80%, OVER_LIMIT above 100%)
- **Consistent errors**: every error has the same JSON shape, with field-level messages for validation errors
- **API documentation**: OpenAPI with Swagger UI
- **Tests**: unit and controller tests, run on every push with GitHub Actions

## Tech Stack

- Java 21, Spring Boot 4
- Spring Web, Spring Data JPA (Hibernate), Bean Validation
- PostgreSQL 17
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, MockMvc
- Docker, Docker Compose, GitHub Actions
- Maven, Lombok

## Quick Start

You only need [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
git clone https://github.com/spideytwenty99/smart-finance-manager.git
cd smart-finance-manager
cp .env.example .env        # on Windows PowerShell: copy .env.example .env
docker compose up --build
```

Then open Swagger UI at **http://localhost:8080/swagger-ui.html**.

![Swagger UI](docs/images/swagger-ui.png)

The app starts with the built-in categories already in place. Try `POST /api/transactions` with:

```json
{
  "amount": 12.50,
  "description": "REWE Frankfurt Hbf",
  "transactionDate": "2026-10-05",
  "transactionType": "EXPENSE"
}
```

The response shows the category Groceries with `"autoCategorized": true`.

## Running the Tests

The tests include one that starts the whole application, so the database container needs to be running:

```bash
docker compose up -d db
./mvnw test
```

Set `DB_USERNAME` and `DB_PASSWORD` to the values from your `.env` first. The same tests run automatically on GitHub for every push.

## Running from an IDE

Start only the database with `docker compose up -d db`, set the environment variables `DB_USERNAME` and `DB_PASSWORD` to the values from `.env`, and run `SmartFinanceManagerApplication`. The database is reachable on port 5433, so it doesn't clash with a local PostgreSQL on 5432.

## Documentation

- [OpenAPI specification](docs/api/openapi.json) — import it into Postman, or paste it into [editor.swagger.io](https://editor.swagger.io)
- [Requirements](docs/requirements.md)
- [Architecture](docs/architecture.md)
- [Database Design](docs/database-design.md)
- [API Design](docs/api-design.md)
- [Devlog](docs/devlog.md) — what was built when, and the decisions behind it

## Project Structure

Organized by feature:

```
com.saurabh.financemanager
├── category      categories, built-in seeding, automatic categorization
├── transaction   transactions
├── budget        monthly budgets
├── report        monthly report
├── exception     error handling
└── config        OpenAPI configuration
```

## Next

- User accounts and login with Spring Security and JWT
- A small React frontend with a dashboard, transactions and budgets
- Machine learning for categorization, trained on corrected guesses

## Author

Saurabh, Informatik student at Frankfurt University of Applied Sciences
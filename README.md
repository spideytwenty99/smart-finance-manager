# Smart Finance Manager

A personal finance REST API for tracking income and expenses, organizing transactions into categories, setting monthly budgets, and seeing where your money goes.

Built with Java 21, Spring Boot and PostgreSQL.

> **Status:** In active development. The first version is single-user; authentication and multi-user support are planned for a later version.

## Features

Planned for version 1:

- Add, view, update and delete income and expense transactions
- Filter transactions by date range, category and type
- Default categories (Food, Rent, Transport, Shopping, Entertainment, Salary) plus custom categories
- Monthly budgets per category with usage tracking
- Dashboard with total income, expenses, balance and spending by category
- Input validation and consistent error responses

Planned for later versions:

- User registration and login with Spring Security and JWT
- Database migrations with Flyway
- Docker setup and CI with GitHub Actions
- Recurring transactions, savings goals and CSV import
- AI-based transaction categorization

## Tech Stack

- Java 21
- Spring Boot (Spring Web, Spring Data JPA, Validation)
- PostgreSQL
- Hibernate
- Lombok
- Maven
- JUnit 5 and Mockito

## Getting Started

### Prerequisites

- Java 21
- PostgreSQL running locally

### 1. Clone the repository

```bash
git clone https://github.com/YOUR-USERNAME/smart-finance-manager.git
cd smart-finance-manager
```

### 2. Create the database

```sql
CREATE DATABASE smart_finance;
```

### 3. Set environment variables

The database password is read from an environment variable so it is never stored in the repository.

```
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

### 4. Run the application

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The API runs at `http://localhost:8080/api/v1`.

### Run the tests

```bash
./mvnw test
```

## Documentation

Design documents are in the [`docs`](docs) folder:

- [Requirements](docs/requirements.md)
- [Architecture](docs/architecture.md)
- [Database Design](docs/database-design.md)
- [API Design](docs/api-design.md)

## Project Structure

The code is organized by feature:

```
com.saurabh.financemanager
├── category
├── transaction
├── budget
├── dashboard
├── config
└── common
```

Each feature package contains its own entity, repository, service, controller and DTOs.

## Author

Saurabh, Informatik student at Frankfurt University of Applied Sciences

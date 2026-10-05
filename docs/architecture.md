# Smart Finance Manager - Architecture

## 1. Architecture Overview

Smart Finance Manager is a Spring Boot REST API with a layered architecture.

```
Client
  |
  v
Security Filter (JWT)
  |
  v
Controller
  |
  v
Service
  |
  v
Repository
  |
  v
PostgreSQL
```

Each layer has one responsibility, and each layer only talks to the layer directly below it.

## 2. Controller Layer

The controller layer handles HTTP requests and responses.

Responsibilities:

- Receive API requests
- Validate request DTOs using `@Valid`
- Call the appropriate service
- Return the correct HTTP status and response DTO

Example:

```
POST /api/v1/transactions
        |
        v
TransactionController
```

Controllers do not contain business logic.

## 3. Service Layer

The service layer contains the application's business logic.

Responsibilities:

- Apply business rules (for example: no duplicate budget for the same category and month)
- Perform calculations (balances, budget usage, dashboard totals)
- Check that the current user owns the requested data
- Coordinate repositories
- Convert between entities and DTOs using mappers

Example:

```
TransactionController
        |
        v
TransactionService
        |
        v
TransactionRepository
```

## 4. Repository Layer

The repository layer handles database access using Spring Data JPA.

Repositories provide operations such as:

- `save`
- `findById`
- `findAll` with pagination
- `delete`
- Custom queries scoped to a user, for example `findByIdAndUserId`

Queries always filter by the current user's id so one user can never load another user's data.

## 5. Data Transfer Objects

DTOs prevent database entities from being exposed directly through the API.

Example:

```
TransactionRequest
TransactionResponse
```

Flow:

```
JSON Request
    |
    v
Request DTO  (validated)
    |
    v
Service  ->  Entity  ->  Repository  ->  Database
    |
    v
Response DTO
    |
    v
JSON Response
```

## 6. Authentication Architecture

Authentication uses Spring Security with stateless JWT tokens.

```
Client
   |
   | POST /api/v1/auth/login
   v
AuthController
   |
   v
JWT access token returned
   |
   v
Client sends "Authorization: Bearer <token>" with every request
   |
   v
JwtAuthFilter validates the token and sets the current user
   |
   v
Protected API
```

Passwords are hashed with BCrypt. Logout is handled on the client by deleting the token.

## 7. Error Handling

A single `GlobalExceptionHandler` (`@RestControllerAdvice`) converts exceptions into a consistent JSON error response.

| Exception | Status |
|---|---|
| Validation errors | 400 Bad Request |
| Invalid or missing token | 401 Unauthorized |
| Resource not found (or owned by another user) | 404 Not Found |
| Duplicate data (email, category name, budget) | 409 Conflict |
| Unexpected errors | 500 Internal Server Error |

## 8. Technology Stack

Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA / Hibernate
- Spring Security
- JWT (jjwt)
- Bean Validation
- Lombok

Database

- PostgreSQL
- Flyway (schema migrations)

API Documentation

- OpenAPI / Swagger (springdoc-openapi)

Logging and Monitoring

- SLF4J with Logback
- Spring Boot Actuator

Testing

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers (integration tests against real PostgreSQL)

Build and Deployment

- Maven
- Docker
- GitHub Actions (build and test on every push)

## 9. Package Structure

The code is organized by feature. Each feature package contains its own controller, service, repository, entity and DTOs.

```
com.saurabh.financemanager
|
├── FinanceManagerApplication.java
|
├── auth
│   ├── AuthController
│   ├── AuthService
│   ├── JwtService
│   ├── JwtAuthFilter
│   └── dto
|
├── user
│   ├── User
│   ├── UserController
│   ├── UserService
│   ├── UserRepository
│   └── dto
|
├── transaction
│   ├── Transaction
│   ├── TransactionType
│   ├── TransactionController
│   ├── TransactionService
│   ├── TransactionRepository
│   ├── TransactionMapper
│   └── dto
|
├── category
│   ├── Category
│   ├── CategoryController
│   ├── CategoryService
│   ├── CategoryRepository
│   └── dto
|
├── budget
│   ├── Budget
│   ├── BudgetController
│   ├── BudgetService
│   ├── BudgetRepository
│   └── dto
|
├── dashboard
│   ├── DashboardController
│   ├── DashboardService
│   └── dto
|
├── config
│   ├── SecurityConfig
│   └── OpenApiConfig
|
└── common
    └── exception
        ├── GlobalExceptionHandler
        ├── ResourceNotFoundException
        ├── ConflictException
        └── ErrorResponse
```

Database migrations live in `src/main/resources/db/migration`. The test folder mirrors the main package structure.

## 10. Future Architecture

Future versions may introduce:

- Redis for caching dashboard results
- Kafka for asynchronous events (for example, budget alerts)
- A separate Python ML service for spending predictions
- Spring AI for automatic transaction categorization

These technologies will only be added when the core application needs them.

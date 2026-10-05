# Smart Finance Manager - Requirements

## 1. Project Overview

Smart Finance Manager is a personal finance management application that allows users to track income and expenses, manage budgets, categorize transactions, and analyze their financial activity.

The goal of the application is to help users understand where their money is going and make better financial decisions.

## 2. Target Users

The application is intended for individual users who want to manage their personal finances.

## 3. Functional Requirements

### User Management

The system must allow users to:

- Register an account
- Log in
- Log out
- View their profile
- Update their profile (name and password)

Note on logout: authentication uses stateless JWT tokens. Logging out means the client deletes its stored token. The server does not keep a session, so no logout endpoint is required in the MVP.

### Transaction Management

The system must allow users to:

- Add an income transaction
- Add an expense transaction
- View their transactions (paginated)
- View a single transaction
- Update a transaction
- Delete a transaction
- Filter transactions by date range
- Filter transactions by category
- Filter transactions by transaction type

A transaction can only use a category that is either a system category or one of the user's own categories.

### Category Management

The system provides two kinds of categories:

- **System categories**: predefined and shared by all users. They cannot be edited or deleted by users.
- **Custom categories**: created by a user and visible only to that user.

The system must allow users to:

- View system categories and their own custom categories
- Create custom categories
- Update custom categories
- Delete custom categories

A user cannot have two custom categories with the same name. When a custom category is deleted, its transactions remain but are no longer assigned to a category.

Default system categories:

- Food
- Rent
- Transport
- Shopping
- Entertainment
- Salary

### Budget Management

A budget is a monthly spending limit for one category.

The system must allow users to:

- Create a monthly budget for a category
- Update a budget
- Delete a budget
- View all their budgets
- View how much of each budget has been used
- View the remaining amount of each budget

A user cannot have more than one budget for the same category in the same month.

### Dashboard

For a selected month (default: current month), the system must display:

- Total income
- Total expenses
- Current balance (income minus expenses)
- Spending by category
- Budget usage

The dashboard must also show monthly spending totals for recent months.

## 4. Future Requirements

The following features are planned for later versions:

- Savings goals
- Recurring transactions
- Notifications
- CSV bank statement import
- PDF reports
- AI-based transaction categorization
- Machine-learning spending predictions

## 5. Non-Functional Requirements

### Security

- Passwords must never be stored in plain text. They are hashed with BCrypt.
- Passwords must be at least 8 characters long.
- Users must only be able to access their own financial data.
- Requests for another user's resource return `404 Not Found`, so the API does not reveal that the resource exists.
- All endpoints except register and login require authentication.

### Performance

- Typical API requests should respond in under 500 ms for a single user's data on a local or small cloud deployment.
- Transaction lists must be paginated.

### Maintainability

- The backend follows a layered architecture (controller, service, repository).
- Business logic is kept out of controllers.
- DTOs are used between the API and the application layers. Entities are never returned directly.
- Database schema changes are managed with Flyway migrations.

### Data Integrity

- Monetary amounts use a precise decimal type (`BigDecimal` in Java, `NUMERIC(12,2)` in PostgreSQL).
- Amounts and budget limits must be greater than zero.
- Required fields must not accept null values.
- Relationships between tables use foreign keys.

### Observability

- The application logs important events (registration, login failures, unexpected errors) using SLF4J.
- Sensitive data such as passwords and tokens is never logged.

## 6. MVP Scope

Version 1 will include:

- User registration and login (JWT)
- User profile
- Transaction management with filtering and pagination
- Category management (system and custom)
- Budget management
- Dashboard analytics
- Input validation
- Consistent error handling
- Unit and integration tests
- API documentation with Swagger

Features outside the MVP will be implemented after the core application is stable.

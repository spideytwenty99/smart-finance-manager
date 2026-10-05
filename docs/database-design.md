# Smart Finance Manager - Database Design

## 1. Database

The application uses PostgreSQL. The schema is created and versioned with Flyway migrations in `src/main/resources/db/migration`.

All primary keys are UUIDs. All timestamps use `TIMESTAMPTZ` (timestamp with time zone).

## 2. Main Entities

The MVP contains four entities:

- User
- Category
- Transaction
- Budget

## 3. Users Table

```
users
--------------------------------------------
id              UUID          PK
name            VARCHAR(100)  NOT NULL
email           VARCHAR(255)  NOT NULL UNIQUE
password_hash   VARCHAR(255)  NOT NULL
created_at      TIMESTAMPTZ   NOT NULL
updated_at      TIMESTAMPTZ   NOT NULL
```

Rules:

- `email` must be unique and is stored in lowercase.
- `password_hash` stores a BCrypt hash, never the plain password.

## 4. Categories Table

```
categories
--------------------------------------------
id              UUID          PK
name            VARCHAR(50)   NOT NULL
user_id         UUID          FK -> users.id, NULLABLE
created_at      TIMESTAMPTZ   NOT NULL
```

Rules:

- `user_id IS NULL` means a system category, shared by all users and read-only.
- `user_id IS NOT NULL` means a custom category owned by that user.
- A user cannot have two custom categories with the same name: `UNIQUE (user_id, name)`.
- System category names are also unique among system categories.

System categories (Food, Rent, Transport, Shopping, Entertainment, Salary) are inserted by a Flyway migration.

## 5. Transactions Table

```
transactions
--------------------------------------------
id                UUID           PK
user_id           UUID           NOT NULL  FK -> users.id
category_id       UUID           NULLABLE  FK -> categories.id
amount            NUMERIC(12,2)  NOT NULL
type              VARCHAR(10)    NOT NULL
description       VARCHAR(255)
transaction_date  DATE           NOT NULL
created_at        TIMESTAMPTZ    NOT NULL
updated_at        TIMESTAMPTZ    NOT NULL
```

Constraints:

```
CHECK (amount > 0)
CHECK (type IN ('INCOME', 'EXPENSE'))
```

Rules:

- Every transaction belongs to exactly one user.
- A transaction may have a category. If set, the category must be a system category or one of the user's own categories (checked in the service layer).
- The amount is always positive. Whether it adds or subtracts is decided by `type`.

## 6. Budgets Table

```
budgets
--------------------------------------------
id              UUID           PK
user_id         UUID           NOT NULL  FK -> users.id
category_id     UUID           NOT NULL  FK -> categories.id
amount_limit    NUMERIC(12,2)  NOT NULL
month           SMALLINT       NOT NULL
year            SMALLINT       NOT NULL
created_at      TIMESTAMPTZ    NOT NULL
updated_at      TIMESTAMPTZ    NOT NULL
```

Constraints:

```
CHECK (amount_limit > 0)
CHECK (month BETWEEN 1 AND 12)
UNIQUE (user_id, category_id, month, year)
```

Rules:

- A budget belongs to one user and one category.
- A user cannot have two budgets for the same category in the same month.
- Budget usage is not stored. It is calculated from the user's EXPENSE transactions in that category and month.

## 7. Relationships

```
User
 |
 |------< Transaction
 |------< Category (custom only)
 |------< Budget

Category
 |
 |------< Transaction
 |------< Budget
```

- One user has many transactions, custom categories and budgets.
- One category can be used by many transactions and budgets.

## 8. Foreign Keys and Delete Behaviour

```
transactions.user_id      -> users.id       ON DELETE CASCADE
transactions.category_id  -> categories.id  ON DELETE SET NULL
categories.user_id        -> users.id       ON DELETE CASCADE
budgets.user_id           -> users.id       ON DELETE CASCADE
budgets.category_id       -> categories.id  ON DELETE CASCADE
```

What this means:

```
User deleted
    |
    +-- Transactions deleted
    +-- Budgets deleted
    +-- Custom categories deleted

Custom category deleted
    |
    +-- Transactions kept, category_id set to NULL
    +-- Budgets for that category deleted
```

System categories cannot be deleted by users (enforced in the service layer).

## 9. Indexes

Primary keys and unique constraints create indexes automatically (`users.email`, `categories (user_id, name)`, `budgets (user_id, category_id, month, year)`).

Additional indexes:

```
transactions (user_id, transaction_date)
transactions (category_id)
categories   (user_id)
```

The composite index on `(user_id, transaction_date)` supports the most common query: a user's transactions within a date range.

## 10. Money Representation

Money values use PostgreSQL `NUMERIC(12,2)` and Java `BigDecimal`, never `float` or `double`.

This avoids floating-point rounding errors. For example, `0.1 + 0.2` in floating point is `0.30000000000000004`, which is unacceptable for financial data.

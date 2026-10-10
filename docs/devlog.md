# Devlog

A running log of what I built, what I decided and what I learned while working on Smart Finance Manager.

---

## 2026-10-05 — Project setup

Commit: `Initial project setup with design docs and database config`

- Created the Spring Boot project (Java 21, Maven) with Web, Data JPA, Validation, PostgreSQL and Lombok
- Connected it to a local PostgreSQL database called `smart_finance`, with the username and password read from environment variables so they never end up on GitHub
- Wrote the design docs before writing code: requirements, architecture, database design and API design, all in the `docs` folder
- Wrote the README with the feature plan, tech stack and setup steps

---

## 2026-10-06 — Category CRUD

Commit: `feat(category): add category CRUD with entity, repository, service and controller`

### What I built

The first real feature of the API: categories. A category is something like "Food", "Rent" or "Salary", and later every transaction will point to one, so this had to come before transactions.

I added a `category` package with the full layer stack:

- `Category` entity mapped to a `categories` table, with a UUID id, a name, a type (`INCOME` or `EXPENSE`) and `createdAt` / `updatedAt` timestamps filled in automatically by Hibernate
- `TransactionType` enum in the `transaction` package, so the same type can be reused when I build transactions
- `CategoryRepository` on top of `JpaRepository`, plus one derived query, `findByTransactionType`, so I didn't have to write any SQL
- `CategoryService` with the logic for create, read, update and delete, and small mapper methods that turn entities into DTOs and back
- `CategoryRequest` and `CategoryResponse` DTOs, so the API never exposes the entity directly
- `CategoryController` with five endpoints:

| Method | Path | What it does |
|---|---|---|
| POST | `/api/category` | Create a category, returns `201 Created` |
| GET | `/api/category` | List all categories, optional `?type=INCOME` or `?type=EXPENSE` filter |
| GET | `/api/category/{id}` | Get one category |
| PUT | `/api/category/{id}` | Update name and type |
| DELETE | `/api/category/{id}` | Delete a category, returns `204 No Content` |

Input is validated with Bean Validation on the request DTO: the name can't be blank and is at most 50 characters, and the type is required. Bad input is rejected with `400 Bad Request` before it reaches the service.

### Decisions

- **Feature-based packages.** Everything for categories lives in one `category` package instead of separate `controllers/`, `services/` folders. It keeps related code together and matches the structure in the README.
- **DTOs instead of returning entities.** The request DTO only accepts the fields a client is allowed to set, and the response DTO controls exactly what goes back out.
- **UUIDs as ids.** They're harder to guess than 1, 2, 3 and match the database design doc.
- **Constructor injection** for the repository and service, rather than `@Autowired` on fields.
- **Added a type to categories.** This wasn't in the original database design, but it makes sense to separate income categories (Salary) from expense ones (Food), and it powers the `?type=` filter.
- **Still single-user.** No `user_id` on categories yet. That comes back once login is added.

### What I learned

- How JPA turns an annotated class into a table, and how `@Enumerated(EnumType.STRING)` stores the enum as readable text instead of a number
- That Spring Data can generate a query just from a method name like `findByTransactionType`
- How `@Valid` together with validation annotations on a DTO gives free input checking
- Returning the right status codes with `ResponseEntity` (`201`, `200`, `204`)


### Known gaps (things to fix next)

Being honest about what's not finished yet:

- **Wrong status for "not found".** When a category doesn't exist, the service throws a plain `RuntimeException`, which comes back as `500 Internal Server Error`. It should be `404 Not Found`. Fix: a custom `ResourceNotFoundException` and a `@RestControllerAdvice` global exception handler.
- **Duplicate names are allowed.** You can create "Food" twice. The API design says this should return `409 Conflict`.
- **No default categories yet.** The plan is to seed Food, Rent, Transport, Shopping, Entertainment and Salary.

### Next steps

1. Global exception handling with proper `404` and `400` error responses
2. Duplicate name check with `409 Conflict`
3. Unit tests for `CategoryService` with JUnit and Mockito
4. Start the Transaction feature, linked to categories

---
## 2026-10-08: Global error handling

Done:
- Same JSON error format for every error (ErrorResponse)
- 404 for missing resources, 409 for duplicates, 400 for invalid
  input, generic 500 for unexpected errors
- One GlobalExceptionHandler, so controllers need no try/catch

Fixed:
- Not-found and duplicate errors no longer return 500

## 2026-10-08: Transaction feature

Done:
- Transaction CRUD, linked to categories
- Filter by month and category, newest first
- Transaction type must match the category type
- Categories in use can't be deleted

Decisions:
- BigDecimal for money, amounts always positive, type decides in or out
- Refuse deleting a used category instead of losing its transactions

Known issues:
- N+1 queries when listing transactions; fix later with a join fetch

Next:
- Budget feature

## 2026-10-09: Budget feature

Done:
- Monthly budget per expense category
- List budgets by month, update the limit, delete
- Category deletion now also checks budgets

Decisions:
- Month stored as the first day of the month, sent as "2026-10" in the API
- Unique rule on category + month in the database itself
- Only the limit can be updated; a new category or month means a new budget
- Budgets only for EXPENSE categories

Next:
- Monthly report

## 2026-10-11: Monthly report

Done:
- Monthly report: total income, expenses, balance and spending per category
- Budget status per category: OK, WARNING (80% or more), OVER_LIMIT, NOT_SET
- Categories with a budget but no spending still appear with 0 spent
- Uncategorized expenses shown as their own line
- Defaults to the current month when no month is given

Decisions:
- Sums calculated in the database with GROUP BY instead of looping in Java
- An empty month returns zeros, not a 404
- BigDecimal compared with compareTo and divided with explicit rounding

Fixed along the way:
- SUM over no rows returns null; converted to zero before calculating

Next:
- Automatic categorization
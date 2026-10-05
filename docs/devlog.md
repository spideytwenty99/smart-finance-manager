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
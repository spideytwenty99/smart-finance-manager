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

## 2026-10-11 — Built-in categories and automatic categorization

**What I built**

- The app now ships with built-in categories (Groceries, Transport, Rent, Salary
  and more), each with keywords, plus an "Uncategorized" category per type.
  A seeder creates them at startup and adopts matching categories I already had.
- Transactions without a category are categorized automatically by matching the
  description against keywords of the same type. The longest matching keyword wins;
  if nothing matches, the transaction goes to Uncategorized.
- An `autoCategorized` flag shows which categories were guessed and which were
  chosen by the user.

**Key decisions** (full list in design.md)

- Every transaction always has a category, so the type still comes from the
  category and the report didn't need to change.
- The user's choice always wins; the rules only run when no category is sent.
- Built-ins are protected and found by a fixed code, never by a hardcoded id.

**Known issues**

- My old FOOD category overlaps with Groceries.
- Substring matching can still give false matches for keywords of 3+ letters.
- Still single-user.

**Next**

- User accounts and login.
- Unit tests for the categorization logic.g

## 2026-10-11 — API documentation with Swagger

I planned to start on user accounts next, but did Swagger first. It's a small step,
makes the whole API easy to try in the browser, and the login button for JWT will
go on the same page later.

**What I built**

- Added springdoc-openapi (3.x, for Spring Boot 4). The app now generates an
  OpenAPI description of the API automatically from the controllers and DTOs.
- Swagger UI at `/swagger-ui.html` lists every endpoint and lets you send real
  requests from the browser. The raw OpenAPI JSON is at `/v3/api-docs`.
- An `OpenApiConfig` class with the API title, description, version and a link
  to the GitHub repo.

**Notes**

- Requests sent from Swagger are real and change the database, just like Postman.
- The API has no authentication yet, so it's meant for local use only until
  login is added.

**Known issues**

- Status codes decided inside the method (201 on create, 204 on delete) and the
  error responses (400, 404, 409) aren't documented yet, so Swagger shows them as
  "Undocumented".
- Example request bodies are still generic placeholders.

**Next**

-JUnit, Mockito @MockMvc

## 2026-10-12 — Tests

**What I built**

- Unit tests with JUnit and Mockito for the services that hold the business logic:
  - CategorizationService: matching, case-insensitivity, longest keyword wins,
    no match, expenses never land in income categories, short keywords rejected,
    categories without keywords never match.
  - ReportService: totals and balance, the status boundaries (exactly 80% is
    WARNING, exactly 100% is not OVER, anything above is OVER), budgets with no
    spending, uncategorized expenses, empty months, null sums, sorting.
  - BudgetService: duplicate budgets, income categories, update changes only the limit.
  - TransactionService: chosen category, unknown category, type mismatch, rules
    only run when no category is given, and the month filter uses exactly the first
    and last day of the month.
- A controller test with MockMvc for TransactionController: valid POST gives 201,
  invalid body gives 400 with field errors, missing id gives 404 in the
  ErrorResponse shape, and an invalid month gives 400.

**Bugs found by tests**

- `shouldNotCrashWhenSumsAreNull` failed with a NullPointerException: ReportService
  called `subtract` on a sum that can be null. COALESCE in the query was hiding it.
  Fixed with a small `zeroIfNull` helper, so the service no longer depends on the query.
- `shouldReturn400ForInvalidMonth` showed that `month=October` returned 500.
  GlobalExceptionHandler had no handler for MethodArgumentTypeMismatchException,
  so it fell into the catch-all. Added a 400 handler, which also fixes bad UUIDs
  and enum values everywhere in the API.
- The controller tests first failed with 401/403 on every request, because
  spring-boot-starter-security-test pulled Spring Security into the tests.
  Removed it until login is built.

**Decisions**

- Tested where the logic and boundaries are, not every class. The other
  controllers use the same validation and error handling that
  TransactionControllerTest already covers.

**Known issues**

- CategoryService has no tests yet (keyword cleanup, built-in protection,
  duplicate check on update, delete rules).

**Next**

- Finish Swagger: endpoint descriptions, documented status codes and example values.
- User accounts and login.

## 2026-10-12 — API documentation

**What I built**

- Every controller has a tag with a one-line description, and every endpoint has a
  summary. Endpoints with rules people can't guess also explain them, e.g. how
  automatic categorization works, why a category can't be deleted, and when a
  budget counts as WARNING or OVER_LIMIT.
- Every path and query parameter is described, with examples for dates and months.
- Each endpoint lists exactly the status codes it can return, with the reason, and
  error responses show the ErrorResponse shape with real example bodies.
- Every DTO field has a description and a realistic example; required fields and
  limits come from the validation annotations.
- Exported the spec to `docs/api/openapi.json` and linked it in the README, so the
  API can be browsed without running the app.

**Decisions**

- Turned off springdoc's automatic error responses
  (`springdoc.override-with-generic-response=false`). Otherwise it reads the
  GlobalExceptionHandler and claims every endpoint can return every error, even
  404 and 409 on "list all".
- Documented what the code really does, not what I first planned. Deleting a
  category that's still in use returns 409, not 400.
- Months are documented as strings ("2026-10"), and timestamps as local date-time
  without a time zone, because that's what the API actually sends.

**Found along the way**

- Several bad requests returned 500 instead of the documented 400, because
  GlobalExceptionHandler had no handlers for missing parameters,
  IllegalArgumentException (rule violations like a type mismatch) and unreadable
  bodies (broken JSON, unknown enum values). Added all three.
- editor.swagger.io flagged the timestamp examples, because LocalDateTime has no
  time zone but was labelled as a standard date-time. Fixed the label.

**Known issues**

- Timestamps have no time zone; switching to OffsetDateTime or Instant would fix it.
- The exported spec is a snapshot and has to be re-exported after API changes.
- The editor warns that `example` is the older OpenAPI style; harmless.

**Next**

- I'd planned user accounts and login next, but decided to set up Docker,
  GitHub Actions and an updated README first. Login touches almost every part
  of the app, so I want CI running the tests on every push before I start it.
- Then user accounts and login.

## 2026-10-12 — Docker, GitHub Actions and README

**What I built**

- PostgreSQL 17 runs in a Docker container, with a health check and a volume so the
  data survives restarts. Credentials come from `.env`, which is ignored by Git;
  `.env.example` shows the keys.
- A multi-stage Dockerfile for the app: a Maven + JDK image builds the jar, and a
  slim JRE image runs it as a non-root user. The final image is about 130 MB
  compressed.
- `docker compose up --build` starts the database and the app together. The app
  waits until the database is healthy.
- GitHub Actions runs `./mvnw verify` on every push, with a throwaway PostgreSQL
  service for the test that starts the whole application. The first run passed.
- Rewrote the README: CI badge, features, tech stack, a four-command quick start
  with Docker, a Swagger screenshot and an example request.

**Decisions**

- The container's database is on port 5433, so it doesn't clash with my local
  PostgreSQL on 5432.
- Inside Docker, the app reaches the database by its service name (`db:5432`), not
  `localhost`, because `localhost` inside a container means the container itself.
  An environment variable overrides the URL, so the same code runs from IntelliJ
  and from Docker without changes.
- Dependencies are downloaded in their own layer before the source is copied, so
  rebuilds after a code change take seconds.
- Tests are skipped in the Docker build, because GitHub Actions runs them.
- Started with a fresh database in the container instead of migrating my old test
  data.

**Known issues**

- Running the tests locally needs the database container and the DB environment
  variables set.
- Hibernate still creates the tables (`ddl-auto=update`); there are no database
  migrations yet.
- The image is built locally and not published to a registry.

**Next**

- User accounts and login with Spring Security and JWT.

## 2026-10-14: Authentication, Data Ownership and Logging

### Summary

User accounts with JWT-based authentication were introduced, and all data was made user-owned. Previously, the API was single-user and every client had full access to all data. All endpoints except registration, login and the API documentation now require a valid token, and every query is scoped to the authenticated user.

### Implementation

**Authentication endpoints**
- `POST /api/auth/register` (201) and `POST /api/auth/login` (200) both return an `AuthResponse` containing the token, the token type (`Bearer`) and its lifetime in seconds (3600).
- Registration authenticates the new user right away, so no separate login call is needed.
- Emails are trimmed and lowercased before they are stored or looked up, so they are effectively case-insensitive. Duplicate registrations return 409 Conflict.
- Passwords must be between 8 and 72 characters. The upper limit matches BCrypt's 72-byte input limit.

**Password storage**
- Passwords are hashed with BCrypt. Plaintext passwords are never stored or logged.

**Token handling**
- Tokens are signed with HS256. The signing key is loaded from the `JWT_SECRET` environment variable and is never stored in source code or committed configuration.
- Claims: issuer, issued-at, expiry, subject (user id) and email.
- Incoming tokens are validated for signature, expiry and issuer.

**Data ownership**
- `Category`, `Transaction` and `Budget` each have a required `@ManyToOne` relationship to `User`.
- Uniqueness constraints are now per user: categories on (user, name, type) and (user, code), budgets on (user, category, month).
- Existing development data had no owner, so the database was reset (`docker compose down -v`) instead of migrated.

**Per-user default categories**
- The shared, startup-seeded category set was replaced by a per-user copy created at registration. Users can rename or delete their categories without affecting anyone else.
- The defaults are defined in a single place (`DefaultCategories`), and the old `CategorySeeder` was removed.
- The two `Uncategorized` categories (expense and income) are system categories and can't be deleted, because automatic categorization falls back to them.

**Ownership enforcement**
- Controllers get the user id from the token via `@AuthenticationPrincipal Jwt` and pass it to the service layer.
- Every repository query is scoped by user id (for example `findByIdAndUserId` instead of `findById`).
- The scoping also covers the monthly report aggregations, the automatic categorization lookup, and checking that a category id given on a transaction or budget belongs to the current user.

**Logging**
- SLF4J loggers via Lombok's `@Slf4j`:
  - `INFO`: user registered, user logged in, transaction created, category assigned automatically
  - `WARN`: failed login attempt
  - `ERROR`: unhandled exceptions in `GlobalExceptionHandler`, with the full stack trace
- Logs contain identifiers only. Passwords, tokens, email addresses, amounts and transaction descriptions are never logged.

**API documentation**
- A bearer token security scheme in Swagger UI, with an "Authorize" button.
- A 401 response with the `ErrorResponse` schema documented on every protected endpoint.
- A dedicated "Authentication" tag. `openapi.json` was re-exported.

### Design Decisions

**Built-in resource server instead of a custom JWT filter.** A common approach is a hand-written `JwtAuthFilter`. Spring Security's OAuth2 resource server already provides `BearerTokenAuthenticationFilter`, which handles header parsing, signature and claim validation, and setting the security context. Using it cuts the amount of security-critical custom code down to a single configuration line, and with it the risk of implementation mistakes.

**AuthenticationManager for credential checks.** Login delegates to Spring's `AuthenticationManager`, backed by a `DaoAuthenticationProvider` with a custom `UserDetailsService` and the BCrypt encoder. This follows the framework's standard authentication flow instead of comparing passwords by hand.

**Uniform response for invalid credentials.** An unknown email and a wrong password both return 401 with "Invalid email or password". This prevents account enumeration.

**404 instead of 403 for foreign resources.** A request for another user's resource returns 404 Not Found. A 403 would confirm that the resource exists.

**Stateless sessions and disabled CSRF protection.** The server keeps no session state. CSRF protection is disabled because the token is sent in the `Authorization` header, which browsers never attach on their own, unlike cookies. If token storage ever moves to cookies, CSRF protection must be turned back on.

**Consistent 401 responses.** Authentication failures are rejected in the security filter chain, before any controller runs, so `GlobalExceptionHandler` never sees them. A custom `AuthenticationEntryPoint` returns them in the same `ErrorResponse` format as every other error.

### Issues Found and Resolved

**Cross-user data access in automatic categorization (found by unit tests).**
`CategorizationService.categorize()` took a user id but never used it. It still called an unscoped `findByTransactionType` query, so a transaction could be assigned to a category owned by another user. Manual testing didn't reveal this, because every user starts with identical default categories. `CategorizationServiceTest` caught it when Mockito reported the user-scoped stub as unused. The query was fixed, and the unscoped repository method was removed entirely, so this kind of access is no longer possible.

**Authentication entry point not registered (found by controller tests).**
`JsonAuthenticationEntryPoint` was implemented but never registered in the security configuration. Spring's default entry point returned 401 responses with an empty body. It is now registered for both missing tokens (`exceptionHandling`) and invalid tokens (`oauth2ResourceServer`).

**Missing configuration in CI.**
The application context failed to start in GitHub Actions because `JWT_SECRET` wasn't defined there. A dedicated, non-production test key was added to the workflow.

**Issues found during implementation:**
- Passwords were initially saved without hashing.
- Emails weren't trimmed before they were saved.
- A malformed property placeholder (`{jwt.expiry}` instead of `${jwt.expiry}`) meant the expiry value was never read.
- Tokens were initially created without an expiry claim.
- A logger was initialized with the wrong class (`AuthService.class` inside `TransactionService`).

**Security test setup.**
`spring-boot-starter-security-test` was removed earlier because it made all controller tests fail with 401/403 before authentication existed. It has now been added back. Controller tests use `.with(jwt())` to simulate an authenticated user and `@Import(SecurityConfig.class)` so they run against the application's real security rules.

### Workflow

All of this work was first committed to local `main` by mistake. Before pushing, the commits were moved to a `feature/auth` branch and `main` was reset to `origin/main`. The changes went through pull request #1: CI flagged the issues above, they were fixed on the branch, and the PR was merged after all checks passed. From now on, new work starts on a feature branch.

### Key Takeaways

- Using framework-provided security components and configuring them correctly is preferable to custom implementations.
- A security component only has an effect once it is registered. The missing entry point compiled and passed review without doing anything.
- A method that takes a `userId` but doesn't use it is an easy leak to overlook. Removing unscoped repository methods prevents it structurally.
- Ownership tests with two separate users are the most important tests in a multi-user API.

### Next Steps

- Update the README: registration and login, authorizing in Swagger UI, and configuring `JWT_SECRET`.
- Tag release `v1.1.0`.
- Add ML-based features, starting with transaction categorization to complement the keyword matching.
- Explore an optional AI assistant that answers questions about the user's spending and budgets.

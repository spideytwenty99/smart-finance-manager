# Smart Finance Manager - API Design

## 1. Base URL

```
/api/v1
```

All requests and responses use JSON. All dates use ISO format (`2026-10-05`).

Interactive documentation is available at `/swagger-ui.html` when the application is running.

## 2. Authentication

All endpoints except `register` and `login` require:

```
Authorization: Bearer <JWT>
```

Users can only access their own data. Requests for another user's resource return `404 Not Found`.

Logout is handled by the client deleting its token. There is no logout endpoint.

### Register

```
POST /api/v1/auth/register
```

Request:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securePassword"
}
```

Response `201 Created`:

```json
{
  "id": "uuid",
  "name": "John Doe",
  "email": "john@example.com"
}
```

Errors: `400` invalid input, `409` email already registered.

### Login

```
POST /api/v1/auth/login
```

Request:

```json
{
  "email": "john@example.com",
  "password": "securePassword"
}
```

Response `200 OK`:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Errors: `401` wrong email or password.

## 3. User API

### Get Current User

```
GET /api/v1/users/me
```

Response `200 OK`:

```json
{
  "id": "uuid",
  "name": "John Doe",
  "email": "john@example.com",
  "createdAt": "2026-10-05T10:15:00Z"
}
```

### Update Current User

```
PUT /api/v1/users/me
```

Request:

```json
{
  "name": "John D."
}
```

### Change Password

```
PUT /api/v1/users/me/password
```

Request:

```json
{
  "currentPassword": "securePassword",
  "newPassword": "newSecurePassword"
}
```

Response: `204 No Content`.

## 4. Transaction API

### Create Transaction

```
POST /api/v1/transactions
```

Request:

```json
{
  "amount": 25.50,
  "type": "EXPENSE",
  "categoryId": "uuid",
  "description": "Lunch",
  "transactionDate": "2026-10-05"
}
```

Response `201 Created`:

```json
{
  "id": "uuid",
  "amount": 25.50,
  "type": "EXPENSE",
  "category": {
    "id": "uuid",
    "name": "Food"
  },
  "description": "Lunch",
  "transactionDate": "2026-10-05",
  "createdAt": "2026-10-05T12:30:00Z"
}
```

Validation:

- `amount` required, greater than 0
- `type` required, `INCOME` or `EXPENSE`
- `transactionDate` required
- `categoryId` optional, must be a system category or one of the user's categories
- `description` optional, max 255 characters

### Get Transactions

```
GET /api/v1/transactions
```

Optional query parameters:

| Parameter | Example | Description |
|---|---|---|
| `type` | `EXPENSE` | Filter by type |
| `categoryId` | `uuid` | Filter by category |
| `startDate` | `2026-10-01` | Inclusive |
| `endDate` | `2026-10-31` | Inclusive |
| `page` | `0` | Page number, default 0 |
| `size` | `20` | Page size, default 20, max 100 |

Results are sorted by `transactionDate` descending.

Example:

```
GET /api/v1/transactions?type=EXPENSE&startDate=2026-10-01&endDate=2026-10-31&page=0&size=20
```

Response `200 OK`:

```json
{
  "content": [
    {
      "id": "uuid",
      "amount": 25.50,
      "type": "EXPENSE",
      "category": { "id": "uuid", "name": "Food" },
      "description": "Lunch",
      "transactionDate": "2026-10-05",
      "createdAt": "2026-10-05T12:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Get Transaction

```
GET /api/v1/transactions/{id}
```

### Update Transaction

```
PUT /api/v1/transactions/{id}
```

Same request body and validation as create. Response `200 OK`.

### Delete Transaction

```
DELETE /api/v1/transactions/{id}
```

Response `204 No Content`.

## 5. Category API

### Get Categories

```
GET /api/v1/categories
```

Returns system categories and the user's custom categories.

Response `200 OK`:

```json
[
  { "id": "uuid", "name": "Food", "system": true },
  { "id": "uuid", "name": "Gym", "system": false }
]
```

### Create Category

```
POST /api/v1/categories
```

Request:

```json
{
  "name": "Gym"
}
```

Response `201 Created`. Errors: `409` if the user already has a category with that name.

### Update Category

```
PUT /api/v1/categories/{id}
```

Only custom categories can be updated. Updating a system category returns `403 Forbidden`.

### Delete Category

```
DELETE /api/v1/categories/{id}
```

Only custom categories can be deleted. Transactions in the category are kept with no category. Budgets for the category are deleted.

Response `204 No Content`.

## 6. Budget API

### Create Budget

```
POST /api/v1/budgets
```

Request:

```json
{
  "categoryId": "uuid",
  "amountLimit": 300.00,
  "month": 10,
  "year": 2026
}
```

Response `201 Created`. Errors: `409` if a budget already exists for that category and month.

### Get Budgets

```
GET /api/v1/budgets?month=10&year=2026
```

`month` and `year` default to the current month.

### Get Budget Status

```
GET /api/v1/budgets/status?month=10&year=2026
```

Response `200 OK`:

```json
[
  {
    "budgetId": "uuid",
    "category": "Food",
    "limit": 300.00,
    "spent": 185.50,
    "remaining": 114.50,
    "percentageUsed": 61.83
  }
]
```

`remaining` can be negative if the user has overspent.

### Update Budget

```
PUT /api/v1/budgets/{id}
```

### Delete Budget

```
DELETE /api/v1/budgets/{id}
```

Response `204 No Content`.

## 7. Dashboard API

```
GET /api/v1/dashboard?month=10&year=2026
```

`month` and `year` default to the current month.

Response `200 OK`:

```json
{
  "month": 10,
  "year": 2026,
  "totalIncome": 2500.00,
  "totalExpenses": 1450.00,
  "balance": 1050.00,
  "spendingByCategory": [
    { "category": "Rent", "amount": 800.00 },
    { "category": "Food", "amount": 185.50 }
  ],
  "monthlySpending": [
    { "month": 8, "year": 2026, "amount": 1320.00 },
    { "month": 9, "year": 2026, "amount": 1510.00 },
    { "month": 10, "year": 2026, "amount": 1450.00 }
  ],
  "budgetUsage": [
    { "category": "Food", "limit": 300.00, "spent": 185.50, "percentageUsed": 61.83 }
  ]
}
```

## 8. HTTP Status Codes

| Code | When |
|---|---|
| 200 OK | Successful read or update |
| 201 Created | Resource created |
| 204 No Content | Successful delete or password change |
| 400 Bad Request | Validation failed or malformed JSON |
| 401 Unauthorized | Missing, invalid or expired token, or wrong login |
| 403 Forbidden | Trying to modify a system category |
| 404 Not Found | Resource does not exist or belongs to another user |
| 409 Conflict | Duplicate email, category name or budget |
| 500 Internal Server Error | Unexpected error |

## 9. Error Response Format

All errors use the same structure:

```json
{
  "timestamp": "2026-10-05T20:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Transaction not found",
  "path": "/api/v1/transactions/123"
}
```

Validation errors also include field details:

```json
{
  "timestamp": "2026-10-05T20:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/transactions",
  "fieldErrors": [
    { "field": "amount", "message": "must be greater than 0" }
  ]
}
```

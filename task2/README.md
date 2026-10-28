# Local Coin Banking API

A small Java 8 and Spring Boot project for the banking assessment. It supports individual customers and exposes three REST endpoints for opening an account, viewing account details and recording withdrawals.

## Requirements

- JDK 8 (or a compatible JDK supported by Spring Boot 2.7)
- Maven
- MySQL 8.0.16+ only if you want to run against the MySQL schema in `task1.sql`; H2 is used for local testing by default

Spring Boot 2.7 is used to keep the application compatible with Java 8.

## Start the application

Open a terminal in the directory containing `pom.xml` and run:

```bash
mvn spring-boot:run
```

The default local database is an in-memory H2 database. It starts empty each time the application starts and is reset when the application stops.

## Open Swagger UI

1. Start the app with `mvn spring-boot:run`.
2. Wait until Spring Boot reports that the application has started.
3. Open <http://localhost:8080/swagger-ui.html>.
4. You should see **Local Coin Banking API** at the top, with the three endpoints below.
5. Expand an endpoint, select **Try it out**, enter the request data and select **Execute**.

The API definition is also available at <http://localhost:8080/v3/api-docs>.

## Test the endpoints locally

The account-opening endpoint requires an existing active branch. Because the default H2 database is empty, create a test branch first.

### 1. Open the H2 console

With the application running, open <http://localhost:8080/h2-console> and connect with these values:

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:localcoin;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE` |
| User name | `sa` |
| Password | Leave blank |

Click **Connect**. The console connects to the same in-memory database used by the running application.

### 2. Insert an active branch

Run this SQL in the H2 console:

```sql
INSERT INTO branch (
    branch_code, name, address_line1, city, postal_code, country_code, status
) VALUES (
    'TOR-001', 'Toronto Central', '100 King Street', 'Toronto', 'M5H1J9', 'CA', 'ACTIVE'
);

SELECT branch_id, branch_code, status FROM branch;
```

Note the `branch_id` returned by the query. Use that ID in the account-opening request. If you run this insert again without clearing the database, choose a different `branch_code` because the branch code is unique.

### 3. Open an account

In Swagger UI, expand `POST /api/v1/accounts`, click **Try it out**, and submit this request. Replace `1` with the branch ID returned in step 2 if it is different.

```json
{
  "firstName": "Asha",
  "middleName": null,
  "lastName": "Rao",
  "dateOfBirth": "1990-01-10",
  "email": "asha@example.com",
  "phone": "+14165550123",
  "addressLine1": "100 King Street",
  "addressLine2": null,
  "city": "Toronto",
  "provinceState": "Ontario",
  "postalCode": "M5H1J9",
  "countryCode": "CA",
  "branchId": 1,
  "currency": "CAD"
}
```

A successful call returns `201 Created`, including the new `accountId`. Save that ID for the next steps.

### 4. Get account details

Expand `GET /api/v1/accounts/{accountId}`, click **Try it out**, enter the new account ID and execute the request. The response includes the holder's identity details, account status, currency and current balances. Both balances start at `0.00`.

### 5. Try a withdrawal with no funds

Expand `POST /api/v1/accounts/{accountId}/withdrawals` and submit this body with your account ID in the path:

```json
{
  "amount": 25.00,
  "currency": "CAD",
  "description": "Test withdrawal",
  "idempotencyKey": "withdrawal-test-001"
}
```

The first attempt should be rejected with an insufficient-funds response. This is expected because the account starts at zero and a deposit endpoint is not part of the assessment requirements.

### 6. Add test funds and retry the withdrawal

For a local demo only, use the H2 console to set a test balance. Replace `1` with your account ID:

```sql
UPDATE account_balance
SET available_balance = 500.00,
    ledger_balance = 500.00
WHERE account_id = 1;
```

This SQL is test setup only; it bypasses the normal transaction and ledger workflow. Do not use direct balance updates in a live banking system.

Submit the withdrawal request from step 5 again. It should return `201 Created`. Then call `GET /api/v1/accounts/{accountId}` again; both balances should be `475.00`.

Submit the exact same withdrawal request again with the same `idempotencyKey`. The response should indicate that the previous result was replayed, and the balance should remain `475.00`. Use a new key for a different withdrawal request.

**Note:** H2 is in-memory. The branch, account and test balance disappear when the application stops, so repeat the setup after restarting it.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/accounts` | Open an individual account at an active branch |
| `GET` | `/api/v1/accounts/{accountId}` | Return account details, holder identity and balances |
| `POST` | `/api/v1/accounts/{accountId}/withdrawals` | Record a withdrawal |

## Use MySQL instead of H2

1. Create a development database and user.
2. Run `task1.sql` against MySQL 8.0.16 or later.
3. Set these environment variables before starting the app:

```bash
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL='jdbc:mysql://localhost:3306/localcoin_banking?useSSL=false&serverTimezone=UTC'
export DB_USERNAME='your_user'
export DB_PASSWORD='your_password'
mvn spring-boot:run
```

Seed an active branch in MySQL before opening an account:

```sql
INSERT INTO branch (branch_code, name, address_line1, city, postal_code, country_code, status)
VALUES ('TOR-001', 'Toronto Central', '100 King Street', 'Toronto', 'M5H1J9', 'CA', 'ACTIVE');
```

## Main design choices

- Money uses `DECIMAL(19,2)` in MySQL and `BigDecimal` in Java.
- A withdrawal runs in one database transaction and locks the balance row before checking and changing the balance.
- A withdrawal records a transaction, a ledger entry and an audit event.
- Repeating a withdrawal with the same account and idempotency key does not debit the account twice.
- This version supports individual customers only. Authentication/authorization, a production KYC process and production key management are outside this assessment scope.

## Tests

Run the automated tests with:

```bash
mvn clean test
```

The tests use H2. Run additional checks against MySQL if you change the schema or rely on MySQL-specific behavior.

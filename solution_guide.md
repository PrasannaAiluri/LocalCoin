# Local Coin Banking Take-Home — Solution Walkthrough and Personalization Checklist

## 1. What this submission contains

- `task1.sql`: MySQL 8+ DDL for branches, legal persons, addresses, accounts, balances, banking transactions, ledger entries and audit events.
- `task2.zip`: Maven/Spring Boot project with JPA entities, repositories, service layer, REST controllers, validation, exception handling, sample configuration and a context-load test.
- This guide: design rationale, endpoint flows, trade-offs, known limitations, and a checklist for making the solution genuinely your own.

Treat this as a starting point. Read it, run it, make deliberate changes, and be ready to explain every decision. Do not claim you implemented or tested anything you have not personally verified.

## 2. Architecture at a glance

The request path is:

`HTTP Controller -> DTO validation -> BankingService -> Spring Data JPA repository -> relational database`

- **Controller** owns HTTP routes, request/response shape and status codes.
- **DTOs** define the external API contract. They prevent clients from writing entities directly and allow input validation.
- **Service** contains business rules and transaction boundaries.
- **Repositories** isolate persistence operations.
- **Entities** map Java objects to relational tables.
- **Database constraints** provide a second line of defense against invalid data.

This is a small modular monolith. For a one-hour assessment, it is easier to reason about than splitting a basic account workflow across microservices.

## 3. Schema design rationale

### Branch
`branch` stores a stable branch ID and unique branch code, contact/location fields, and active/inactive status. An account references a branch with a foreign key. The branch status check prevents opening accounts at an inactive branch in the service layer.

### Legal person and address
`legal_person` is the identity/party record; `legal_person_address` stores addresses separately so that address changes can be represented historically instead of overwriting the only address. `valid_from`, `valid_to` and `is_current` support temporal address history. In a production design, enforce only one current address per person/type with a migration-supported constraint or service transaction.

Government ID data is intentionally represented as an encrypted binary field plus last four characters. A real solution needs a managed key service, rotation, access restrictions, retention rules and a policy for which identity attributes may be stored. Never log the full identifier.

### Account
`account` links a legal person to a branch and records currency, account type, lifecycle status and opening time. `account_number` is unique and separate from the internal primary key. `version` supports optimistic concurrency for account-level updates.

### Balance
`account_balance` has one row per account, with available and ledger balances. `DECIMAL(19,4)` avoids binary floating-point rounding. The ledger balance represents posted accounting value; available balance can differ when holds/reservations are later introduced. For this starter, the two values move together during a withdrawal.

### Transaction and ledger
`banking_transaction` is the business event (withdrawal, deposit, payment or transfer). `ledger_entry` is the accounting effect on an account. A transfer should create at least a debit and a credit ledger entry in one database transaction. The sample withdrawal creates a transaction and a debit ledger entry.

For stronger accounting correctness, make the ledger append-only, prohibit direct edits, reconcile balances from entries, and store a `balance_after` snapshot if operationally useful. The current Java implementation does not yet populate every SQL ledger field.

### Audit event
`audit_event` supports investigation and compliance review by recording actor, event, target entity, outcome, correlation ID and limited metadata. The sample schema provides the table, but the sample service does not yet write audit events. Add audit writes for account creation, withdrawals, denied withdrawals and identity changes. Keep metadata allow-listed and free of secrets/PII where possible.

### Keys, indexes and constraints
- Foreign keys protect referential integrity.
- Unique keys protect branch codes, account numbers and transaction references.
- Composite indexes target common account-holder and transaction-history lookups.
- Positive amount and currency format checks catch malformed records.
- `DECIMAL` is used for money; never use `float` or `double`.

MySQL version matters: use MySQL 8+ and verify that your exact version enforces the `CHECK` constraints you rely on.

## 4. API contract and flow

### `POST /api/v1/accounts`
1. Bean Validation checks required names, address fields, country/currency formats and optional date of birth.
2. The service verifies that the branch exists and is active.
3. It creates the legal person, registered address, account and zero-balance row in one `@Transactional` unit.
4. The controller returns `201 Created` and a `Location` header with the new account resource.

The assessment wording says “identity details (name, DOB etc.), registered address, branch ID, and currency.” This implementation captures a practical subset. Personalize the DTO based on the precise contract you want to defend, and avoid collecting extra personal data without a clear business need.

### `GET /api/v1/accounts/{accountId}`
1. Load the account or raise a not-found exception.
2. Load its balance.
3. Compose a response DTO with account details, holder name, DOB, email and balances.
4. Global exception handling maps missing resources to HTTP 404.

In a larger API, prefer a repository projection or fetch-join query to avoid N+1 queries and lazy-loading pitfalls. `spring.jpa.open-in-view=false` is set intentionally.

### `POST /api/v1/accounts/{accountId}/withdrawals`
1. Load the account and check it is active.
2. Verify request currency matches the account currency.
3. Acquire a pessimistic write lock on the balance row.
4. Check funds and debit available/ledger balance.
5. Save a posted transaction and debit ledger entry in the same DB transaction.
6. Return `201 Created`.

The lock is important: two concurrent withdrawals must not both read the same balance and overspend. The database transaction ensures the balance update and transaction/ledger writes commit or roll back together.

**Important limitations:** the account begins with zero funds; no deposit endpoint was requested, so a successful withdrawal needs seeded funds or an additional deposit workflow. The `idempotencyKey` is accepted but not persisted/enforced yet. Implement it before treating retries as safe.

## 5. Running and testing

Requirements: Java 17 and Maven 3.9+. Spring Boot 3 uses `jakarta.*` APIs and does not run on Java 8.

1. Unzip `task2.zip`.
2. Run `mvn test`.
3. Run `mvn spring-boot:run`.
4. The default profile uses an in-memory H2 database in MySQL compatibility mode.
5. For MySQL, create the database and run `task1.sql`, then set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_DRIVER` environment variables.
6. Insert an active branch before opening an account. Example:
   `INSERT INTO branch (branch_code,name,address_line1,city,postal_code,country_code,status) VALUES ('TOR-001','Toronto Central','100 King Street','Toronto','M5H1J9','CA','ACTIVE');`
7. Use the sample JSON in the project README to call the endpoint.

Do not rely on H2 tests alone to validate MySQL-specific constraints or behavior. Add MySQL Testcontainers integration tests if time permits.

## 6. Personalization TODO — do these before submission

### Must-do (high value)
- [ ] Read all source files and rename packages/artifact IDs to a naming scheme you can explain.
- [ ] Run `mvn test`; fix any compile/test failures in your environment.
- [ ] Add tests for: opening an account at an invalid branch, GET missing account (404), currency mismatch, insufficient funds, successful withdrawal, and concurrent withdrawals.
- [ ] Add a small seed-data strategy for local demos; do not silently seed production.
- [ ] Implement persisted idempotency: unique key scoped to client/account, store request fingerprint and result, and return the original result for a safe retry.
- [ ] Add an audit-event repository/service and record account opening and withdrawal success/failure without storing secrets.
- [ ] Decide whether opening an account should default to `PENDING` until KYC/identity verification, rather than immediately `ACTIVE`. Explain the trade-off.
- [ ] Review account-number generation. Use a collision-safe allocator with unique-constraint retry; do not treat random generation as a real banking standard.
- [ ] Update README with commands/results you actually tested.

### Nice-to-have
- [ ] Add Flyway or Liquibase migrations; switch `ddl-auto` to `validate`.
- [ ] Add Spring Security authentication and role-based authorization. Do not expose account data based only on an account ID.
- [ ] Add a proper structured error response with stable application error codes.
- [ ] Add Testcontainers for MySQL integration tests.
- [ ] Add JaCoCo coverage, Checkstyle/Spotless and a Sonar configuration.
- [ ] Add Docker Compose for API + MySQL; add health checks and non-root container execution.
- [ ] Add NGINX reverse-proxy configuration, request size/rate limits and TLS termination assumptions.
- [ ] Add observability: structured logs, correlation IDs, metrics and traces. Do not log full identity data or payment details.
- [ ] Document retention, access, correction, deletion/legal hold and data residency policies with the compliance team.
- [ ] For transfers, define transaction semantics, currency handling, idempotency and two-sided ledger posting before implementing the endpoint.

## 7. Security and compliance discussion points

- **Data minimization:** collect only the personal information required for onboarding.
- **Encryption:** TLS in transit; database/storage encryption at rest; application-level encryption for highly sensitive identifiers; managed keys and rotation.
- **Access control:** authentication, least privilege, account ownership checks, staff/admin roles and audited privileged access.
- **Auditability:** append-only audit and ledger records, correlation IDs and traceable state changes.
- **Financial correctness:** atomic transactions, decimal arithmetic, row locking, idempotency, reconciliation and reversal events rather than deleting posted transactions.
- **Privacy lifecycle:** retention schedules, lawful basis, correction requests, data-subject requests and legal holds.
- **Operational safety:** rate limiting, alerting, backups, disaster recovery, incident playbooks and safe error messages.
- **Threat modeling:** account enumeration, IDOR, replayed requests, concurrent withdrawals, SQL injection, leaked PII in logs and insider misuse.

The schema is compliance-aware but is not itself proof of regulatory compliance. Confirm applicable requirements with Local Coin's engineering/security/compliance stakeholders.

## 8. How to explain the key choices in an interview

**Why separate transaction and ledger entry?** The transaction captures business intent and lifecycle; ledger entries capture the financial effects. This allows one transfer to produce balanced debit/credit entries and gives an audit trail.

**Why use `DECIMAL`?** Money requires exact decimal arithmetic. Floating point can introduce representation errors.

**Why use both a balance table and a ledger?** A balance row gives fast reads; the ledger supports audit and reconciliation. They must be updated atomically and periodically reconciled.

**Why lock the balance?** It serializes competing debits for one account and prevents overspending under concurrency. Alternatives include optimistic versioning with retries or an atomic conditional SQL update.

**Why DTOs instead of accepting entities?** The public API contract should not expose persistence internals, and validation/security rules should be explicit.

**What would you change for production?** Add authentication/authorization, idempotency, audit writes, schema migrations, integration tests against MySQL, KYC lifecycle, encryption/key management, observability, reconciliation and operational controls.

## 9. Honest scope statement

This deliverable implements the requested route shapes and the core happy path. It is a demonstration scaffold, not a fully compliant or production-grade banking platform. Before submission, validate compilation and behavior locally, add the highest-value tests, and personalize the design decisions so you can discuss them confidently.

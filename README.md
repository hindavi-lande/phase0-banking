# phase0-banking

Phase 0 scaffold: two entities, plain CRUD, one foreign-key relation. No auth, no
capabilities, no search — deliberately.

## Stack

| | |
|---|---|
| Java | 21 (toolchain target) |
| Spring Boot | 3.5.6 |
| Persistence | Spring Data JPA + Hibernate |
| Database | H2 in-memory (`MODE=PostgreSQL`) |
| Build | Maven |

## Run

```bash
mvn spring-boot:run          # http://localhost:8080
mvn test                     # 28 tests
mvn clean package            # executable jar in target/
```

H2 console: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:banking`, user `sa`, empty password).

## Domain

```
Customer 1 ──── * Account
```

**Customer** — `id`, `firstName`, `lastName`, `email` (unique), `phone`, `status` (`ACTIVE` | `INACTIVE`)

**Account** — `id`, `customerId` (FK → Customer, required), `accountNumber` (unique),
`type` (`SAVINGS` | `CURRENT`), `balance` (non-negative, 2 dp), `status` (`ACTIVE` | `CLOSED`),
`currency` (ISO 4217 code, e.g. `USD`; defaults to `USD` when omitted on create; mutable via `PUT`)

The FK is mapped as a lazy `@ManyToOne` on `Account`; the wire format exposes it as a
flat `customerId` so responses never leak the entity graph.

## API

Both resources expose the same five operations.

| Method | Path | Success | 
|---|---|---|
| `POST` | `/api/{customers,accounts}` | `201` + `Location` |
| `GET` | `/api/{customers,accounts}/{id}` | `200` |
| `GET` | `/api/{customers,accounts}` | `200` (unpaged list) |
| `PUT` | `/api/{customers,accounts}/{id}` | `200` (full replace) |
| `DELETE` | `/api/{customers,accounts}/{id}` | `204` |

### Example

```bash
CUSTOMER=$(curl -s -X POST localhost:8080/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","phone":"+44 20 7946 0958","status":"ACTIVE"}')

ID=$(echo "$CUSTOMER" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

curl -s -X POST localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d "{\"customerId\":\"$ID\",\"accountNumber\":\"ACC-0001\",\"type\":\"SAVINGS\",\"balance\":250.00,\"status\":\"ACTIVE\",\"currency\":\"USD\"}"
```

`currency` may be omitted, in which case it defaults to `USD`.

## Errors

Every handled failure returns the same body, so clients parse one shape:

```json
{
  "timestamp": "2026-08-21T07:39:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": { "email": "email must be a valid address" }
}
```

| Status | Cause |
|---|---|
| `400` | bean-validation failure (`fieldErrors` populated), or malformed JSON / unknown enum value |
| `404` | unknown id — including an `Account` pointing at a non-existent `Customer` |
| `409` | duplicate `email` / `accountNumber`, or deleting a `Customer` that still has accounts |

That last case is guarded in the service rather than left to the DB constraint, so it
surfaces as a `409` instead of a `500`.

## Layout

```
com.example.banking
├── BankingApplication.java
├── common/          ApiError, GlobalExceptionHandler, 3 exception types
├── customer/        entity, enum, repository, service, controller, dto/
└── account/         entity, 2 enums, repository, service, controller, dto/
```

Each slice is self-contained; `AccountService` resolves the FK through
`CustomerService.findOrThrow`, which is the single place the `Customer` 404 is raised.

## Tests

28 tests, all passing:

- `BankingApplicationTests` — context loads with both slices wired
- `CustomerServiceTest`, `AccountServiceTest` — unit tests over mocked repositories
- `CustomerControllerIntegrationTest`, `AccountControllerIntegrationTest` — `MockMvc`
  against real H2: full CRUD lifecycle per resource, plus the 400/404/409 paths

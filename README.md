# Smart Parking Management System

> A modular monolith built with **Spring Modulith** — managing parking reservations, EV charging, billing, and notifications through clean module boundaries and event-driven communication.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Smart Parking Platform                       │
│                                                                  │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌────────────┐  │
│  │  User    │   │  Zone    │   │ Pricing  │   │Reservation │  │
│  │ Mgmt     │──▶│  Mgmt    │◀──│          │◀──│            │  │
│  └──────────┘   └──────────┘   └──────────┘   └────────────┘  │
│       │               │                              │          │
│       │         ┌─────▼──────┐   ┌──────────┐       │          │
│       │         │  Charging  │   │ Billing  │◀──────┘          │
│       │         └─────┬──────┘   └──────────┘                  │
│       │               │                │                        │
│       └───────────────┴────────────────▼                        │
│                                 ┌────────────┐                  │
│                                 │Notification│                  │
│                                 └────────────┘                  │
└─────────────────────────────────────────────────────────────────┘
```

The application is organized into **7 independent modules**, each exposing only what it needs to share:

| Module | Responsibility | Published Events |
|---|---|---|
| `usermgmt` | Registration, login, JWT issuance | `UserRegisteredEvent` |
| `zonemgmt` | Zones, spaces, availability, state | `SpaceStateChangedEvent` |
| `pricing` | Tariff rules, fee estimation | `PricingRuleChangedEvent` |
| `reservation` | Time-slot bookings, concurrency control | `ReservationCreatedEvent`, `ReservationCancelledEvent` |
| `billing` | Invoices, mock payment gateway | `InvoiceGeneratedEvent` |
| `charging` | EV charging sessions, energy tracking | `ChargingStartedEvent`, `ChargingCompletedEvent` |
| `notification` | Cross-cutting notification dispatch | `NotificationSentEvent` |

---

## Key Features

- **Pessimistic locking** — `SELECT ... FOR UPDATE` on parking spaces prevents double-booking under concurrent load
- **Event-driven modules** — JPA-backed Spring Modulith event publication; modules communicate without direct dependencies
- **EV charging** — Lifecycle management (PENDING → ACTIVE → COMPLETED) with live kWh calculation at 7.4 kW
- **Dynamic pricing** — Per-zone, per-space-type tariff rules with validity periods; separate rates for parking vs. charging
- **JWT authentication** — Stateless token-based auth with role separation (CITIZEN / ADMIN)
- **Module architecture validation** — `ModularityTest` enforces boundary rules at build time

---

## Event Flow

### Reservation

```
POST /reservations
    │
    ▼
ReservationService
    ├── lockSpace() ← pessimistic lock
    ├── check time overlap
    ├── estimate fee via IPricingPolicy
    └── publish ReservationCreatedEvent
            │
            ├──▶ BillingService    → creates RESERVATION invoice → mock payment
            ├──▶ ChargingService   → creates PENDING session (if withCharging=true)
            └──▶ NotificationService → confirmation to citizen
```

### EV Charging

```
POST /charging/sessions/{id}/start
    └── publish ChargingStartedEvent → BillingService creates pending CHARGING invoice

POST /charging/sessions/{id}/stop
    └── publish ChargingCompletedEvent → BillingService calculates kWh × €0.30, settles invoice
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.4.4 |
| Modularity | Spring Modulith 1.3.4 |
| Persistence | Spring Data JPA + Hibernate, H2 (in-memory) |
| Security | Spring Security 6 + JWT (jjwt 0.12.6) |
| Java | 21 |
| Build | Maven |
| Tests | JUnit 5, MockMvc, Awaitility |

---

## Module Boundaries

Each module exposes a set of interfaces that other modules may depend on:

```
zonemgmt     → IZoneAvailability, ISpaceQuery, ISpaceStateManager, IZoneQuery
pricing      → IPricingPolicy
reservation  → IReservationRepo
billing      → IPaymentGateway, IInvoiceRepository
charging     → IChargingSessionRepo
usermgmt     → IUserRepository
notification → INotificationRepo
```

Modules communicate across boundaries exclusively through **exported interfaces** or **application events** — internal implementation classes are never referenced directly.

---

## Getting Started

### Prerequisites
- Java 21
- Maven 3.8+

### Run

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080` with an H2 in-memory database. Sample zones, spaces, and pricing rules are seeded automatically.

### Test

```bash
mvn test
```

Includes unit tests, integration tests, and end-to-end scenario tests:
- `FullReservationFlowTest` — full lifecycle from booking to invoice
- `ConcurrentReservationTest` — validates pessimistic locking under race conditions
- `CancellationFlowTest` — reservation cancellation propagation
- `EVChargingFlowTest` — EV charging session lifecycle
- `ModularityTest` — Spring Modulith architecture validation

---

## REST API

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/users/register` | — | Register citizen or admin |
| POST | `/users/login` | — | Authenticate, receive JWT |
| GET | `/users/me` | JWT | Current user profile |
| GET | `/reservations/search` | JWT | Find available spaces with price estimates |
| POST | `/reservations` | JWT | Create reservation |
| GET | `/reservations/my` | JWT | My reservations |
| DELETE | `/reservations/{id}` | JWT | Cancel reservation |
| GET | `/zones` | — | List all parking zones |
| POST | `/zones` | ADMIN | Create zone |
| GET | `/zones/{id}/spaces` | — | Spaces in a zone |
| POST | `/charging/sessions/{id}/start` | JWT | Start EV charging |
| POST | `/charging/sessions/{id}/stop` | JWT | Stop EV charging |
| GET | `/billing/invoices` | JWT | My invoices |
| GET | `/pricing/rules` | — | Active pricing rules |

---

## Project Structure

```
src/main/java/com/parking/
├── ParkingApplication.java          # @SpringBootApplication @Modulithic
├── usermgmt/
├── zonemgmt/
├── pricing/
├── reservation/
├── billing/
├── charging/
└── notification/
    ├── package-info.java            # @ApplicationModule per module
    ├── <PublicTypes>.java           # Events, DTOs, exported interfaces
    └── internal/                   # Private implementation
```

---

## Notes

- The **mock payment gateway** always approves charges — suitable for demos and testing.
- A **License Plate Recognition webhook** (`LprWebhookController`) is wired into the charging module for future integration.
- OpenRouter AI integration exists in `ChargingStrategyService` for smart charging recommendations (requires `application-local.properties`).
- JWT secret in `application.properties` is a sample — regenerate for any non-local deployment: `openssl rand -base64 64`

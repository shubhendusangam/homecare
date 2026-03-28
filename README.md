# HomeCare — On-Demand Home Services Platform

A full-featured **Spring Boot** backend for an on-demand home services marketplace. Customers book services (cleaning, cooking, babysitting, elderly help), helpers accept and fulfill them, and admins manage the entire platform — all backed by real-time tracking, wallet payments, and multi-channel notifications.

[![Java](https://img.shields.io/badge/Java-17-blue)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/tests-172%20passing-brightgreen)](#testing)

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Module Overview](#module-overview)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [License](#license)

---

## Features

- **Three user roles** — Customer, Helper, Admin with JWT-based authentication & role-based access
- **Booking lifecycle** — Create → Assign → En Route → In Progress → Complete (with state machine validation)
- **Auto-assignment** — Nearest available helper is automatically assigned using haversine geo-distance
- **Real-time tracking** — WebSocket-based live helper location updates with ETA computation
- **Wallet & payments** — Top-up via Razorpay, pay bookings from wallet balance with hold/release flow
- **Refund engine** — Cancellation policy with full/partial refund support for both wallet and Razorpay payments
- **Multi-channel notifications** — In-app (WebSocket push), email (Thymeleaf templates), SMS
- **Review system** — Star ratings, review moderation, automatic helper rating recomputation
- **Admin dashboard** — Analytics, booking management, user ban/unban, service configuration, revenue reports
- **Scheduled jobs** — Auto-expire unaccepted bookings, booking reminders, daily reports (Quartz Scheduler)
- **Audit logging** — Structured event logging with MDC context (userId, requestId)
- **Built-in SPA** — Single-page admin/demo frontend served from static resources

---

## Architecture

```
┌─────────────┐     ┌──────────────────────────────────────────────────────┐
│   Frontend   │────▶│                  Spring Boot API                     │
│  (SPA / App) │◀────│                                                      │
└─────────────┘     │  ┌──────────┐ ┌─────────┐ ┌──────────┐ ┌─────────┐  │
      ▲             │  │ Booking  │ │ Payment │ │  Review  │ │  Admin  │  │
      │ WebSocket   │  │ Module   │ │ Module  │ │  Module  │ │ Module  │  │
      │             │  └────┬─────┘ └────┬────┘ └────┬─────┘ └────┬────┘  │
      │             │       │            │           │            │        │
      │             │  ┌────▼────────────▼───────────▼────────────▼────┐   │
      │             │  │              Core Module                      │   │
      │             │  │  (Security, Exceptions, Config, Entities)     │   │
      │             │  └──────────────────┬────────────────────────────┘   │
      │             │                     │                                │
      │             │  ┌──────────────────▼────────────────────────────┐   │
      │             │  │          H2 (dev) / PostgreSQL (prod)         │   │
      │             │  └───────────────────────────────────────────────┘   │
      │             └──────────────────────────────────────────────────────┘
      │
  ┌───┴───────────────────────────────────┐
  │  WebSocket (STOMP) — /ws              │
  │  • Live location tracking             │
  │  • Booking status updates             │
  │  • Helper status broadcasts           │
  │  • Real-time notifications            │
  └───────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2.5 |
| **Security** | Spring Security + JWT (jjwt 0.12.3) |
| **Persistence** | Spring Data JPA + Hibernate |
| **Database** | H2 (dev/test) · PostgreSQL (prod) |
| **Real-time** | Spring WebSocket (STOMP) |
| **Scheduling** | Quartz Scheduler |
| **Email** | Spring Mail + Thymeleaf templates |
| **Mapping** | MapStruct 1.5.5 |
| **Logging** | Logback + Logstash JSON encoder |
| **Build** | Maven |
| **Testing** | JUnit 5 + Mockito |

---

## Module Overview

| Module | Package | Responsibility |
|---|---|---|
| **User** | `com.homecare.user` | Authentication, registration, JWT, customer/helper profiles |
| **Booking** | `com.homecare.booking` | Booking CRUD, state machine, auto-assignment, Quartz jobs |
| **Payment** | `com.homecare.payment` | Wallet, Razorpay integration, hold/release, refunds |
| **Review** | `com.homecare.review` | Star ratings, moderation, helper rating recomputation |
| **Notification** | `com.homecare.notification` | Multi-channel delivery (WebSocket, email, SMS) |
| **Tracking** | `com.homecare.tracking` | Live location updates, ETA, location history |
| **Admin** | `com.homecare.admin` | Dashboard, analytics, user/service management |
| **Scheduler** | `com.homecare.scheduler` | Auto-expire, reminders, daily reports |
| **Core** | `com.homecare.core` | Security config, exceptions, enums, base entities, utilities |

---

## Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.8+**

### Run in Development Mode

```bash
# Clone the repository
git clone https://github.com/<your-username>/homecare.git
cd homecare

# Build and run (uses H2 in-memory database)
mvn spring-boot:run

# Or build the JAR first
mvn clean package -DskipTests
java -jar target/homecare.jar
```

The app starts at **http://localhost:8080** with:
- **H2 Console**: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:homecaredb`)
- **Demo Frontend**: http://localhost:8080/
- **Health Check**: http://localhost:8080/api/health

### Run with PostgreSQL (Production)

```bash
# Set environment variables
export JWT_SECRET=your-production-secret-min-32-chars
export RAZORPAY_KEY_ID=rzp_live_xxx
export MAIL_USERNAME=you@gmail.com
export MAIL_PASSWORD=app-password

# Run with prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## API Reference

All API endpoints are prefixed with `/api/v1`. Responses follow a uniform `ApiResponse<T>` envelope.

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register/customer` | — | Register a new customer |
| `POST` | `/auth/register/helper` | — | Register a new helper |
| `POST` | `/auth/login` | — | Login (returns JWT tokens) |
| `POST` | `/auth/refresh` | — | Refresh access token |
| `POST` | `/auth/logout` | ✅ | Revoke all refresh tokens |
| `POST` | `/auth/forgot-password` | — | Request password reset |
| `POST` | `/auth/reset-password` | — | Reset password with token |

### Bookings (Customer)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/bookings` | CUSTOMER, ADMIN | Create a booking |
| `GET` | `/bookings` | CUSTOMER, ADMIN | List own bookings |
| `GET` | `/bookings/{id}` | CUSTOMER, ADMIN | Get booking details |
| `DELETE` | `/bookings/{id}/cancel` | CUSTOMER, ADMIN | Cancel a booking |
| `GET` | `/bookings/available-helpers` | CUSTOMER, ADMIN | Find nearby helpers |

### Bookings (Helper)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/helpers/bookings` | HELPER | List assigned bookings |
| `GET` | `/helpers/bookings/pending` | HELPER | List nearby pending bookings |
| `PATCH` | `/helpers/bookings/{id}/accept` | HELPER | Accept a booking |
| `PATCH` | `/helpers/bookings/{id}/reject` | HELPER | Reject a booking |
| `PATCH` | `/helpers/bookings/{id}/start-travel` | HELPER | Start traveling |
| `PATCH` | `/helpers/bookings/{id}/start-job` | HELPER | Start the job |
| `PATCH` | `/helpers/bookings/{id}/complete` | HELPER | Complete the job |

### Wallet & Payments

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/wallet` | CUSTOMER | Get wallet balance |
| `GET` | `/wallet/transactions` | CUSTOMER | Transaction history |
| `POST` | `/wallet/topup/initiate` | CUSTOMER | Initiate Razorpay top-up |
| `POST` | `/wallet/topup/verify` | CUSTOMER | Verify top-up payment |
| `POST` | `/payments/booking/{id}/pay-wallet` | CUSTOMER | Pay booking via wallet |
| `POST` | `/payments/booking/{id}/initiate` | CUSTOMER | Initiate Razorpay payment |
| `POST` | `/payments/booking/{id}/verify` | CUSTOMER | Verify booking payment |
| `GET` | `/helpers/earnings` | HELPER | Earnings summary |

### Reviews

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/reviews` | CUSTOMER | Submit a review |
| `GET` | `/reviews/helpers/{helperId}` | ✅ | Get helper reviews |
| `GET` | `/reviews/me` | CUSTOMER | Get own reviews |

### Notifications

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/notifications/unread-count` | ✅ | Get unread count |
| `PATCH` | `/notifications/{id}/read` | ✅ | Mark as read |
| `PATCH` | `/notifications/read-all` | ✅ | Mark all as read |

### Tracking

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/tracking/{bookingId}/latest` | ✅ | Get latest helper location |
| `GET` | `/tracking/{bookingId}/history` | ✅ | Get location history |

### Admin

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/admin/dashboard` | ADMIN | Dashboard stats |
| `GET` | `/admin/analytics/bookings` | ADMIN | Booking analytics |
| `GET` | `/admin/analytics/revenue` | ADMIN | Revenue analytics |
| `GET` | `/admin/analytics/helpers` | ADMIN | Helper analytics |
| `GET` | `/admin/analytics/customers` | ADMIN | Customer analytics |
| `GET` | `/admin/bookings` | ADMIN | List all bookings |
| `GET` | `/admin/bookings/{id}` | ADMIN | Get booking detail |
| `PATCH` | `/admin/bookings/{id}/assign` | ADMIN | Assign helper |
| `POST` | `/admin/bookings/{id}/force-cancel` | ADMIN | Force cancel |
| `POST` | `/admin/bookings/{id}/force-complete` | ADMIN | Force complete |
| `GET` | `/admin/customers` | ADMIN | List customers |
| `GET` | `/admin/helpers` | ADMIN | List helpers |
| `PATCH` | `/admin/helpers/{id}/verify` | ADMIN | Verify helper |
| `PATCH` | `/admin/helpers/{id}/suspend` | ADMIN | Suspend helper |
| `PATCH` | `/admin/customers/{id}/ban` | ADMIN | Ban customer |
| `PUT` | `/admin/service-config/{serviceType}` | ADMIN | Update pricing |
| `PATCH` | `/admin/reviews/{id}/hide` | ADMIN | Hide review |
| `PATCH` | `/admin/reviews/{id}/flag` | ADMIN | Flag review |
| `GET` | `/admin/payments` | ADMIN | All transactions |
| `GET` | `/admin/payments/summary` | ADMIN | Payment summary |

### WebSocket Topics

| Destination | Description |
|---|---|
| `/topic/booking/{id}/location` | Live helper location for a booking |
| `/topic/booking/{id}/status` | Booking status change events |
| `/topic/helper-status` | Helper online/offline broadcasts |
| `/topic/helper-location` | Helper location broadcasts |
| `/topic/admin/bookings` | Admin live booking feed |
| `/user/queue/bookings` | Personal booking notifications |

---

## Configuration

Key configuration in `application.yml`:

```yaml
homecare:
  jwt:
    secret: ${JWT_SECRET:change-in-prod}   # Min 32 characters
    expiry-minutes: 1440                    # Access token expiry
    refresh-expiry-days: 30                 # Refresh token expiry

  booking:
    auto-assign-radius-km: 10              # Auto-assignment search radius
    auto-expire-minutes: 15                 # Unaccepted booking expiry
    max-advance-schedule-days: 30           # Max days ahead for scheduling

  payment:
    platform-fee: 0.15                     # 15% platform fee on bookings
    wallet-low-balance-alert: 100          # Low balance notification threshold
    razorpay-key-id: ${RAZORPAY_KEY_ID}    # Razorpay public key

  notification:
    email:
      enabled: ${MAIL_ENABLED:false}       # Enable/disable email sending
    sms:
      enabled: ${SMS_ENABLED:false}        # Enable/disable SMS sending
```

### Profiles

| Profile | Database | Payment Gateway | Use Case |
|---|---|---|---|
| `dev` (default) | H2 in-memory | Mock | Local development |
| `test` | H2 in-memory | Mock | Automated testing |
| `prod` | PostgreSQL | Razorpay | Production deployment |

---

## Testing

The project has a comprehensive test suite using **JUnit 5** and **Mockito**.

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=BookingServiceTest

# Run with verbose output
mvn test -Dsurefire.useFile=false
```

### Test Coverage

**172 tests** across **17 test classes** — all passing.

| Test Class | Tests | Module |
|---|---|---|
| `BookingStateMachineTest` | 28 | Booking state transitions |
| `BookingServiceTest` | 18 | Booking CRUD, cancel, complete, access control |
| `PaymentServiceTest` | 8 | Topup, wallet pay, refund, release |
| `WalletServiceTest` | 10 | Wallet CRUD, hold/release, refund guard |
| `PasswordConstraintValidatorTest` | 15 | Password policy validation |
| `AuthServiceTest` | 13 | Register, login, refresh, logout |
| `JwtUtilTest` | 11 | Token generation, parsing, validation |
| `JwtAuthenticationFilterTest` | 10 | JWT filter, MDC, banned user check |
| `ReviewServiceTest` | 10 | Submit, hide, flag, rating recomputation |
| `TrackingServiceTest` | 9 | Location updates, ETA, broadcasts |
| `GlobalExceptionHandlerTest` | 9 | HTTP status mapping per ErrorCode |
| `NotificationServiceImplTest` | 7 | Multi-channel notification delivery |
| `HelperServiceTest` | 7 | Profile, status transitions, location |
| `GeoUtilsTest` | 6 | Haversine distance computations |
| `BannedUserStoreTest` | 6 | In-memory ban/unban store |
| `CustomerServiceTest` | 5 | Profile get/update, partial updates |
| `HomeCareApplicationTests` | 1 | Spring context integration test |

### Regression Tests

The test suite includes dedicated regression tests for fixed bugs:
- **MDC leak prevention** — MDC cleared after filter chain (even on exceptions)
- **Cancel with PENDING payment** — No refund issued when payment hasn't been made
- **Wallet hold/release flow** — `releaseHold` called on job completion
- **HELD payment status** — Wallet payments set `HELD` (not `PAID`) until job completes
- **Admin booking access** — Admins can access any booking via `getBooking()`
- **Negative balance guard** — Wallet refund caps balance at zero

---

## Project Structure

```
src/main/java/com/homecare/
├── HomeCareApplication.java
├── admin/                    # Admin dashboard, analytics, user mgmt
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/              # AdminService, BannedUserStore, ServiceConfigCache
├── booking/                  # Booking lifecycle
│   ├── config/               # BookingConfig (pricing, settings)
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── mapper/
│   ├── repository/
│   ├── service/              # BookingService
│   └── statemachine/         # BookingStateMachine
├── core/                     # Shared infrastructure
│   ├── config/               # SecurityConfig, WebSocket, Auditing
│   ├── controller/           # HealthController
│   ├── dto/                  # ApiResponse, PagedResponse
│   ├── entity/               # BaseEntity
│   ├── enums/                # BookingStatus, PaymentStatus, ServiceType, etc.
│   ├── exception/            # GlobalExceptionHandler, BusinessException
│   ├── filter/               # RequestLoggingFilter
│   ├── logging/              # AuditEvent, structured logging
│   └── util/                 # GeoUtils
├── notification/             # Multi-channel notifications
│   ├── config/               # NotificationTemplates
│   ├── controller/
│   ├── dto/
│   ├── email/                # EmailService (Thymeleaf)
│   ├── entity/
│   ├── enums/                # NotificationType
│   ├── repository/
│   ├── service/              # NotificationService, NotificationServiceImpl
│   ├── sms/                  # SmsService, MockSmsService
│   └── websocket/            # WebSocketNotificationPusher
├── payment/                  # Wallet & payment processing
│   ├── config/               # PaymentConfig
│   ├── controller/
│   ├── dto/
│   ├── entity/               # Wallet, WalletTransaction, PaymentOrder
│   ├── enums/                # TransactionType, TransactionStatus
│   ├── gateway/              # PaymentGateway interface, MockPaymentGateway
│   ├── mapper/
│   ├── repository/
│   └── service/              # PaymentService, WalletService
├── review/                   # Review & rating system
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── mapper/
│   ├── repository/
│   └── service/              # ReviewService
├── scheduler/                # Quartz scheduled jobs
│   ├── BookingAutoExpireJob.java
│   ├── BookingReminderJob.java
│   ├── DailyReportJob.java
│   ├── HelperInactivityJob.java
│   ├── ScheduledBookingTriggerJob.java
│   └── QuartzConfig.java
├── tracking/                 # Real-time location tracking
│   ├── controller/
│   ├── dto/
│   ├── entity/               # LocationHistory
│   ├── repository/
│   └── service/              # TrackingService
└── user/                     # User management & auth
    ├── controller/
    ├── dto/
    ├── entity/               # User, CustomerProfile, HelperProfile, RefreshToken
    ├── enums/                # Role, HelperStatus
    ├── repository/
    ├── security/             # JwtUtil, JwtAuthenticationFilter, UserPrincipal
    ├── service/              # AuthService, CustomerService, HelperService
    └── validation/           # PasswordConstraintValidator

src/main/resources/
├── application.yml           # Shared config
├── application-dev.yml       # Dev profile (H2, debug logging)
├── application-prod.yml      # Prod profile (PostgreSQL)
├── data.sql                  # Seed data for dev
├── logback-spring.xml        # Structured logging config
├── static/                   # SPA frontend (HTML/CSS/JS)
└── templates/email/          # Thymeleaf email templates

src/test/
├── java/com/homecare/        # 17 test classes, 172 tests
└── resources/
    ├── application.yml       # Test config (H2, mock services)
    └── application-test.yml  # Test profile
```

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.


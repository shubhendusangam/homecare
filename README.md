# HomeCare — On-Demand Home Services Platform

A full-featured **Spring Boot** backend for an on-demand home services marketplace. Customers book services (cleaning, cooking, babysitting, elderly help), helpers accept and fulfill them, and admins manage the entire platform — all backed by real-time tracking, wallet payments, in-app chat, subscription plans, dispute resolution, referral rewards, and multi-channel notifications.

[![Java](https://img.shields.io/badge/Java-17-blue)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/tests-224%20passing-brightgreen)](#testing)

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
- **Auto-assignment** — Nearest available helper is automatically assigned using haversine geo-distance with bounding box pre-filter
- **Preferred helper booking** — Customers save favourite helpers and request them directly via `requestedHelperId`
- **Helper availability slots** — Weekly schedule (7-day grid) + one-off unavailable dates; booking auto-assignment respects working hours
- **Available time slots** — `GET /bookings/available-slots` shows per-hour helper availability for a given service, location, and date
- **Real-time tracking** — WebSocket-based live helper location updates with ETA computation
- **In-app chat** — STOMP WebSocket messaging between customer and helper during active bookings (ASSIGNED, HELPER\_EN\_ROUTE, IN\_PROGRESS), with REST history loading and read receipts
- **Wallet & payments** — Top-up via Razorpay, pay bookings from wallet balance with hold/release flow
- **Refund engine** — Cancellation policy with full/partial refund support for both wallet and Razorpay payments
- **Subscription plans** — Recurring service plans (weekly/biweekly/monthly) with auto-renewal via wallet debit, session tracking, and Quartz-based renewal jobs
- **Dispute resolution** — Structured disputes with evidence submission (text, image URL, location screenshot), admin assignment, and financial resolution (full/partial refund, no refund, re-service, warning)
- **Referral system** — Auto-generated referral codes, ₹50 referee credit on signup, ₹100 referrer credit after referee's first completed booking, with 30-day expiry window
- **Multi-channel notifications** — In-app (WebSocket push), email (Thymeleaf templates), SMS — email and SMS delivered asynchronously via dedicated thread pool
- **Review system** — Star ratings, review moderation, automatic helper rating recomputation
- **Admin dashboard** — Analytics, booking management, user ban/unban, service configuration, revenue reports, notification broadcasts
- **Scheduled jobs** — Auto-expire unaccepted bookings, booking reminders, subscription renewal, chat cleanup, daily reports (Quartz Scheduler)
- **Audit logging** — Structured event logging with MDC context (userId, requestId)
- **Built-in SPA** — Single-page admin/demo frontend served from static resources

---

## Architecture

```
┌─────────────┐     ┌──────────────────────────────────────────────────────┐
│   Frontend   │────▶│                  Spring Boot API                     │
│  (SPA / App) │◀────│                                                      │
└─────────────┘     │  ┌──────────┐ ┌─────────┐ ┌──────────┐ ┌─────────┐  │
      ▲             │  │ Booking  │ │ Payment │ │Subscript.│ │  Admin  │  │
      │ WebSocket   │  │ Module   │ │ Module  │ │  Module  │ │ Module  │  │
      │             │  └────┬─────┘ └────┬────┘ └────┬─────┘ └────┬────┘  │
      │             │  ┌────┴──┐ ┌──┴──┐ ┌──┴──┐ ┌──┴──┐ ┌──┴──┐         │
      │             │  │ Chat  │ │Disp.│ │ Rev.│ │Refer│ │Track│         │
      │             │  │Module │ │Mod. │ │Mod. │ │Mod. │ │Mod. │         │
      │             │  └───┬───┘ └──┬──┘ └──┬──┘ └──┬──┘ └──┬──┘         │
      │             │      │        │       │       │       │             │
      │             │  ┌───▼────────▼───────▼───────▼───────▼──────────┐  │
      │             │  │              Core Module                       │  │
      │             │  │  (Security, Exceptions, Config, Entities)      │  │
      │             │  └──────────────────┬─────────────────────────────┘  │
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
  │  • In-app chat messaging              │
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
| **Async** | Spring `@Async` with custom thread pool (`notificationExecutor`) |
| **Email** | Spring Mail + Thymeleaf templates |
| **Mapping** | Hand-written `@Component` mapper classes |
| **Logging** | Logback + Logstash JSON encoder |
| **Build** | Maven |
| **Testing** | JUnit 5 + Mockito |

---

## Module Overview

| Module | Package | Responsibility |
|---|---|---|
| **User** | `com.homecare.user` | Authentication, registration, JWT, customer/helper profiles, favourite helpers, helper availability slots |
| **Booking** | `com.homecare.booking` | Booking CRUD, state machine, auto-assignment (with availability filtering), available slots, Quartz jobs |
| **Payment** | `com.homecare.payment` | Wallet, Razorpay integration, hold/release, refunds, subscription debits |
| **Review** | `com.homecare.review` | Star ratings, moderation, helper rating recomputation |
| **Notification** | `com.homecare.notification` | Multi-channel delivery (WebSocket, async email, async SMS), admin broadcasts |
| **Tracking** | `com.homecare.tracking` | Live location updates, ETA, location history |
| **Chat** | `com.homecare.chat` | In-app STOMP messaging, chat history, read receipts, admin chat access |
| **Subscription** | `com.homecare.subscription` | Recurring service plans, customer subscriptions, auto-renewal, session tracking |
| **Dispute** | `com.homecare.dispute` | Dispute lifecycle, evidence submission, admin mediation, financial resolution |
| **Referral** | `com.homecare.referral` | Referral codes, signup/completion events, referrer credit, admin analytics |
| **Admin** | `com.homecare.admin` | Dashboard, analytics, user/service management, banned user store |
| **Scheduler** | `com.homecare.scheduler` | Auto-expire, reminders, subscription renewal, chat cleanup, daily reports |
| **Core** | `com.homecare.core` | Security config, async config, exceptions, enums, base entities, logging, utilities |

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
| `POST` | `/bookings` | CUSTOMER, ADMIN | Create a booking (optional `requestedHelperId`, `subscriptionId`) |
| `GET` | `/bookings` | CUSTOMER, ADMIN | List own bookings |
| `GET` | `/bookings/{id}` | CUSTOMER, ADMIN | Get booking details |
| `DELETE` | `/bookings/{id}/cancel` | CUSTOMER, ADMIN | Cancel a booking |
| `GET` | `/bookings/available-helpers` | CUSTOMER, ADMIN | Find nearby helpers (optional `scheduledAt` filter) |
| `GET` | `/bookings/available-slots` | CUSTOMER, ADMIN | Per-hour helper availability for service + location + date |

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

### Helper Availability

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/helpers/me/availability` | HELPER | Get own weekly schedule |
| `PUT` | `/helpers/me/availability` | HELPER | Set weekly schedule (7-day grid) |
| `POST` | `/helpers/me/unavailable-dates` | HELPER | Mark a specific date unavailable |
| `GET` | `/helpers/me/unavailable-dates` | HELPER | List own unavailable dates |
| `DELETE` | `/helpers/me/unavailable-dates/{id}` | HELPER | Remove unavailable date |
| `GET` | `/helpers/{helperId}/availability` | ✅ | View any helper's weekly schedule |

### Favourite Helpers

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/favourites/{helperId}` | CUSTOMER | Add helper to favourites |
| `DELETE` | `/favourites/{helperId}` | CUSTOMER | Remove from favourites |
| `GET` | `/favourites` | CUSTOMER | List favourite helpers (with live availability) |
| `PATCH` | `/favourites/{helperId}` | CUSTOMER | Update nickname or notes |

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

### Subscriptions

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/subscription-plans` | ✅ | List active subscription plans |
| `GET` | `/subscription-plans/{id}` | ✅ | Get plan details |
| `POST` | `/subscriptions` | CUSTOMER | Subscribe to a plan |
| `GET` | `/subscriptions` | CUSTOMER | List own subscriptions |
| `GET` | `/subscriptions/{id}` | CUSTOMER | Subscription detail |
| `DELETE` | `/subscriptions/{id}/cancel` | CUSTOMER | Cancel subscription |

### Chat

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/chat/{bookingId}/messages` | ✅ | Chat history (paginated, newest first) |
| `PATCH` | `/chat/{bookingId}/read` | ✅ | Mark all unread messages as read |
| `GET` | `/chat/{bookingId}/unread` | ✅ | Get unread message count |

### Disputes

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/disputes` | ✅ | Raise a dispute on a booking |
| `GET` | `/disputes/me` | ✅ | List own disputes (paginated) |
| `GET` | `/disputes/{id}` | ✅ | Get dispute detail |
| `POST` | `/disputes/{id}/evidence` | ✅ | Submit evidence (text, image URL, screenshot) |

### Referrals

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/referrals/my-code` | ✅ | Get own referral code (auto-creates if none) |
| `GET` | `/referrals/stats` | ✅ | Referral stats (total, successful, credits earned) |
| `GET` | `/referrals/history` | ✅ | Referral event history (paginated) |

### Reviews

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/reviews` | CUSTOMER | Submit a review |
| `GET` | `/reviews/helpers/{helperId}` | ✅ | Get helper reviews |
| `GET` | `/reviews/me` | CUSTOMER | Get own reviews |

### Notifications

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/notifications` | ✅ | List own notifications (paginated, unread first) |
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
| `GET` | `/admin/bookings/{id}/chat` | ADMIN | Read booking chat (for disputes) |
| `PATCH` | `/admin/bookings/{id}/assign` | ADMIN | Assign helper |
| `POST` | `/admin/bookings/{id}/force-cancel` | ADMIN | Force cancel |
| `POST` | `/admin/bookings/{id}/force-complete` | ADMIN | Force complete |
| `GET` | `/admin/users` | ADMIN | List/filter users |
| `PATCH` | `/admin/users/{id}/activate` | ADMIN | Activate user |
| `PATCH` | `/admin/users/{id}/deactivate` | ADMIN | Deactivate user |
| `PATCH` | `/admin/helpers/{id}/verify` | ADMIN | Verify helper |
| `PATCH` | `/admin/helpers/{id}/suspend` | ADMIN | Suspend helper |
| `PATCH` | `/admin/customers/{id}/ban` | ADMIN | Ban customer |
| `PUT` | `/admin/service-config/{serviceType}` | ADMIN | Update pricing |
| `PATCH` | `/admin/reviews/{id}/hide` | ADMIN | Hide review |
| `PATCH` | `/admin/reviews/{id}/flag` | ADMIN | Flag review |
| `GET` | `/admin/payments` | ADMIN | All transactions |
| `GET` | `/admin/payments/summary` | ADMIN | Payment summary |
| `POST` | `/admin/subscription-plans` | ADMIN | Create subscription plan |
| `PUT` | `/admin/subscription-plans/{id}` | ADMIN | Update plan |
| `DELETE` | `/admin/subscription-plans/{id}` | ADMIN | Deactivate plan |
| `GET` | `/admin/subscription-plans` | ADMIN | List all plans (incl. inactive) |
| `GET` | `/admin/subscriptions` | ADMIN | List all subscriptions (filter by status) |
| `GET` | `/admin/subscriptions/{id}` | ADMIN | Subscription detail |
| `POST` | `/admin/subscriptions/{id}/cancel` | ADMIN | Admin cancel subscription |
| `GET` | `/admin/disputes` | ADMIN | List disputes (filter: status, type, date) |
| `GET` | `/admin/disputes/{id}` | ADMIN | Full dispute with evidence |
| `PATCH` | `/admin/disputes/{id}/assign` | ADMIN | Assign dispute to self |
| `POST` | `/admin/disputes/{id}/resolve` | ADMIN | Resolve with decision |
| `GET` | `/admin/referrals/summary` | ADMIN | Referral summary (signups, conversions) |
| `GET` | `/admin/referrals` | ADMIN | All referral events (filter: status, date) |
| `POST` | `/admin/notifications/broadcast` | ADMIN | Broadcast notification to users/roles |

### WebSocket Topics

| Destination | Description |
|---|---|
| `/topic/booking/{id}/location` | Live helper location for a booking |
| `/topic/booking/{id}/status` | Booking status change events |
| `/topic/chat/{bookingId}` | In-app chat messages (customer ↔ helper) |
| `/topic/helper-status` | Helper online/offline broadcasts |
| `/topic/helper-location` | Helper location broadcasts |
| `/topic/admin/bookings` | Admin live booking feed |
| `/user/queue/bookings` | Personal booking notifications |

### STOMP Message Endpoints

| Destination | Description |
|---|---|
| `/app/chat/{bookingId}/send` | Send a chat message (payload: `{ content }`) |

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
    default-currency: INR

  tracking:
    location-update-interval-seconds: 10

  notification:
    email:
      from: ${MAIL_FROM:noreply@homecare.in}
      enabled: ${MAIL_ENABLED:false}       # Enable/disable email sending
    sms:
      enabled: ${SMS_ENABLED:false}        # Enable/disable SMS sending

  referral:
    referrer-credit: 100.00                # Credited to referrer after referee's first booking
    referee-credit: 50.00                  # Credited to referee on registration with code
    code-length: 8                         # Length of generated referral code
    signup-expiry-days: 30                 # Days for referee to complete first booking
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

**224 tests** across **24 test classes** — all passing.

| Test Class | Tests | Module |
|---|---|---|
| `BookingServiceTest` | 25 | Booking CRUD, cancel, complete, preferred helper, availability filter |
| `BookingStateMachineTest` | 4 | Booking state transitions |
| `ChatServiceTest` | 13 | Save/broadcast, history, mark as read, unread count, message cleanup |
| `SubscriptionServiceTest` | 12 | Subscribe, cancel, renew, session increment, wallet insufficient |
| `DisputeServiceTest` | 10 | Raise dispute, resolve, submit evidence |
| `ReferralServiceTest` | 15 | Code generation, create event, referrer credit, stats |
| `FavouriteHelperServiceTest` | 10 | Add/remove favourite, list, increment bookings together |
| `HelperAvailabilityServiceTest` | 12 | Weekly schedule, unavailable dates, availability check |
| `PaymentServiceTest` | 8 | Top-up, wallet pay, refund, release |
| `WalletServiceTest` | 10 | Wallet CRUD, hold/release, refund guard |
| `AuthServiceTest` | 12 | Register, login, refresh, logout |
| `JwtUtilTest` | 11 | Token generation, parsing, validation |
| `JwtAuthenticationFilterTest` | 10 | JWT filter, MDC, banned user check |
| `ReviewServiceTest` | 10 | Submit, hide, flag, rating recomputation |
| `TrackingServiceTest` | 9 | Location updates, ETA, broadcasts |
| `GlobalExceptionHandlerTest` | 9 | HTTP status mapping per ErrorCode |
| `NotificationServiceImplTest` | 7 | Multi-channel notification delivery, admin alerts |
| `AsyncNotificationDispatcherTest` | 6 | Async email and SMS dispatch |
| `GeoUtilsTest` | 8 | Haversine distance, bounding box computations |
| `HelperServiceTest` | 7 | Profile, status transitions, location |
| `BannedUserStoreTest` | 6 | In-memory ban/unban store |
| `CustomerServiceTest` | 5 | Profile get/update, partial updates |
| `PasswordConstraintValidatorTest` | 4 | Password policy validation |
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
├── HomeCareApplication.java          # @EnableAsync, @EnableJpaAuditing
├── admin/                            # Admin dashboard, analytics, user mgmt
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── service/                      # AdminService, BannedUserStore, ServiceConfigCache
├── booking/                          # Booking lifecycle
│   ├── config/                       # BookingConfig (pricing, settings)
│   ├── controller/                   # BookingController, HelperBookingController, AdminBookingController
│   ├── dto/
│   ├── entity/
│   ├── mapper/
│   ├── repository/
│   ├── service/                      # BookingService
│   └── statemachine/                 # BookingStateMachine
├── chat/                             # In-app messaging
│   ├── controller/                   # ChatController (STOMP + REST), AdminChatController
│   ├── dto/
│   ├── entity/                       # ChatMessage
│   ├── enums/                        # SenderRole
│   ├── mapper/
│   ├── repository/
│   └── service/                      # ChatService
├── core/                             # Shared infrastructure
│   ├── config/                       # SecurityConfig, WebSocketConfig, AsyncConfig, AuditingConfig, DataInitializer
│   ├── controller/                   # HealthController
│   ├── dto/                          # ApiResponse, PagedResponse
│   ├── entity/                       # BaseEntity
│   ├── enums/                        # BookingStatus, PaymentStatus, ServiceType, CyclePeriod,
│   │                                 #   SubscriptionStatus, DisputeType, DisputeStatus,
│   │                                 #   DisputeResolution, DisputeRaisedBy, EvidenceType,
│   │                                 #   ReferralStatus, ErrorCode, BookingType
│   ├── exception/                    # GlobalExceptionHandler, BusinessException
│   ├── filter/                       # RequestLoggingFilter
│   ├── logging/                      # AuditEvent, LoggingAspect, SensitiveDataMasker,
│   │                                 #   QuartzJobLoggingListener, WebSocketLoggingInterceptor,
│   │                                 #   SecurityEventLogger, Logged
│   └── util/                         # GeoUtils
├── dispute/                          # Dispute resolution
│   ├── controller/                   # DisputeController, AdminDisputeController
│   ├── dto/
│   ├── entity/                       # Dispute, DisputeEvidence
│   ├── mapper/
│   ├── repository/
│   └── service/                      # DisputeService
├── helper/                           # Placeholder (package-info.java only)
├── notification/                     # Multi-channel notifications
│   ├── config/                       # NotificationTemplates
│   ├── controller/                   # NotificationController, AdminNotificationController
│   ├── dto/
│   ├── email/                        # EmailService (Thymeleaf)
│   ├── entity/
│   ├── enums/                        # NotificationType
│   ├── repository/
│   ├── service/                      # NotificationService, NotificationServiceImpl,
│   │                                 #   AsyncNotificationDispatcher, NotificationQueryService
│   ├── sms/                          # SmsService, MockSmsService
│   └── websocket/                    # WebSocketNotificationPusher
├── payment/                          # Wallet & payment processing
│   ├── config/                       # PaymentConfig
│   ├── controller/                   # PaymentController, WalletController, HelperEarningsController,
│   │                                 #   AdminPaymentController
│   ├── dto/
│   ├── entity/                       # Wallet, WalletTransaction, PaymentOrder
│   ├── enums/                        # TransactionType (incl. DEBIT_SUBSCRIPTION), TransactionStatus
│   ├── gateway/                      # PaymentGateway interface, MockPaymentGateway
│   ├── mapper/
│   ├── repository/
│   └── service/                      # PaymentService, WalletService
├── referral/                         # Referral system
│   ├── config/                       # ReferralConfig
│   ├── controller/                   # ReferralController, AdminReferralController
│   ├── dto/
│   ├── entity/                       # ReferralCode, ReferralEvent
│   ├── mapper/
│   ├── repository/
│   └── service/                      # ReferralService
├── review/                           # Review & rating system
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── mapper/
│   ├── repository/
│   └── service/                      # ReviewService
├── scheduler/                        # Quartz scheduled jobs
│   ├── BookingAutoExpireJob.java
│   ├── BookingReminderJob.java
│   ├── ChatMessageCleanupJob.java    # Purges chat messages > 90 days
│   ├── DailyReportJob.java
│   ├── HelperInactivityJob.java
│   ├── ScheduledBookingTriggerJob.java
│   ├── SubscriptionRenewalJob.java   # Per-subscription one-shot renewal
│   ├── SubscriptionRenewalSweepJob.java  # Daily 1:00 AM cron catchup
│   ├── QuartzConfig.java
│   ├── entity/
│   └── repository/
├── subscription/                     # Subscription plans
│   ├── controller/                   # SubscriptionPlanController, SubscriptionController,
│   │                                 #   AdminSubscriptionPlanController, AdminSubscriptionController
│   ├── dto/
│   ├── entity/                       # SubscriptionPlan, CustomerSubscription
│   ├── mapper/
│   ├── repository/
│   └── service/                      # SubscriptionService
├── tracking/                         # Real-time location tracking
│   ├── controller/
│   ├── dto/
│   ├── entity/                       # LocationHistory
│   ├── repository/
│   └── service/                      # TrackingService
└── user/                             # User management & auth
    ├── controller/                   # AuthController, CustomerController, HelperController,
    │                                 #   FavouriteController, UserAdminController
    ├── dto/
    ├── entity/                       # User, CustomerProfile, HelperProfile, RefreshToken,
    │                                 #   FavouriteHelper, HelperAvailabilitySlot, HelperUnavailableDate
    ├── enums/                        # Role, HelperStatus
    ├── repository/
    ├── security/                     # JwtUtil, JwtAuthenticationFilter, UserPrincipal
    ├── service/                      # AuthService, CustomerService, HelperService,
    │                                 #   FavouriteHelperService, HelperAvailabilityService, UserAdminService
    └── validation/                   # PasswordConstraintValidator

src/main/resources/
├── application.yml                   # Shared config
├── application-dev.yml               # Dev profile (H2, debug logging)
├── application-prod.yml              # Prod profile (PostgreSQL)
├── homecare_data.sql                 # Seed data for dev (loaded by DataInitializer)
├── logback-spring.xml                # Structured logging config
├── static/                           # SPA frontend (HTML/CSS/JS)
└── templates/email/                  # Thymeleaf email templates

src/test/
├── java/com/homecare/               # 24 test classes, 224 tests
└── resources/
    ├── application.yml               # Test config (H2, mock services)
    └── application-test.yml          # Test profile
```

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.


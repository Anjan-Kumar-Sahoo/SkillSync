# Payment System Upgrade — Dedicated Microservice with Event-Driven Saga

> [!IMPORTANT]
> **Extraction (March 2026):** Payment has been extracted from User Service into a dedicated **Payment Service** (port 8086, `com.skillsync.payment`) with event-driven Saga orchestration via RabbitMQ. All changes compile and test successfully. ✅ Build verified: 12 payment-service tests pass, 16 user-service tests pass.

---

## Architecture Overview

```mermaid
graph TD
    A["Client / Frontend"] -->|"POST /api/payments/create-order"| B["PaymentController (payment-service)"]
    B -->|"X-User-Id from JWT"| C["PaymentService"]
    C -->|"Create Razorpay Order"| D["Razorpay API"]
    C -->|"Save Payment (CREATED)"| E["PaymentRepository (skillsync_payment)"]
    
    A -->|"POST /api/payments/verify"| B
    C -->|"Verify Signature"| D
    C -->|"Update to VERIFIED"| E
    C -->|"Delegate"| F["PaymentSagaOrchestrator"]
    
    F -->|"Mark SUCCESS_PENDING"| E
    F -->|"Publish Event"| MQ["RabbitMQ (payment.business.action)"]
    MQ -->|"Consume"| PEC["PaymentEventConsumer (user-service)"]
    PEC -->|"SESSION_BOOKING"| H["Session Gate (external)"]
    
    F -->|"Success"| I["Mark SUCCESS ✅"]
    F -->|"Failure"| J["Compensation"]
    J -->|"Mark COMPENSATED"| E
```

---

## Payment Status State Machine

```mermaid
stateDiagram-v2
    [*] --> CREATED : Order created
    CREATED --> VERIFIED : Signature valid
    CREATED --> FAILED : Signature/amount invalid
    VERIFIED --> SUCCESS_PENDING : Saga started
    SUCCESS_PENDING --> SUCCESS : Business action OK
    SUCCESS_PENDING --> COMPENSATED : Business action failed
```

| Status | Description |
|--------|-------------|
| `CREATED` | Razorpay order created, awaiting frontend checkout |
| `VERIFIED` | Razorpay signature verified successfully |
| `SUCCESS_PENDING` | Business action (saga step) in progress |
| `SUCCESS` | Payment fully completed — business action succeeded |
| `FAILED` | Payment verification failed |
| `COMPENSATED` | Payment verified but business action failed — compensation applied |

---

## Files Changed

### New Files

| File | Purpose |
|------|---------|
| [PaymentServiceApplication.java](file:///f:/SkillSync/payment-service/src/main/java/com/skillsync/payment/PaymentServiceApplication.java) | Spring Boot entry point for payment-service |
| [PaymentSagaOrchestrator.java](file:///f:/SkillSync/payment-service/src/main/java/com/skillsync/payment/service/PaymentSagaOrchestrator.java) | Event-driven Saga orchestration — publishes `payment.business.action` to RabbitMQ |
| [PaymentEventConsumer.java](file:///f:/SkillSync/user-service/src/main/java/com/skillsync/user/consumer/PaymentEventConsumer.java) | User Service consumer — handles `payment.business.action` events → triggers mentor approval |
| [PaymentCompletedEvent.java](file:///f:/SkillSync/payment-service/src/main/java/com/skillsync/payment/event/PaymentCompletedEvent.java) | RabbitMQ event DTO for payment lifecycle notifications |

### Modified Files

````carousel
#### PaymentStatus.java
```diff
 CREATED,
+VERIFIED,
+SUCCESS_PENDING,
 SUCCESS,
 FAILED,
+COMPENSATED
```
Added 3 new states to support saga lifecycle.
<!-- slide -->
#### Payment.java (Entity)
```diff
-@Column(nullable = false, length = 10)
+@Column(nullable = false, length = 20)
 private PaymentStatus status;

+@Column(nullable = false)
 private Long referenceId;

+@Enumerated(EnumType.STRING)
+@Column(nullable = false, length = 30)
+private ReferenceType referenceType;

+@Column(length = 500)
+private String compensationReason;
```
- `referenceId` now **non-nullable** — every payment must be linked to a business entity
- Added `referenceType` enum for traceability
- Added `compensationReason` for debugging failed sagas
<!-- slide -->
#### CreateOrderRequest.java (DTO)
```diff
+@NotNull(message = "Reference ID is required")
 Long referenceId,
+
+@NotNull(message = "Reference type is required")
+ReferenceType referenceType
```
Both fields are now **mandatory** — no payment without context.
<!-- slide -->
#### PaymentResponse.java (DTO)
```diff
 Long referenceId,
+String referenceType,
+String compensationReason,
 LocalDateTime createdAt,
```
API responses now include full reference mapping and compensation info.
<!-- slide -->
#### PaymentController.java
```diff
 @GetMapping("/check")
 public ResponseEntity<Boolean> checkPaymentStatus(
-        @RequestParam Long userId,
+        @RequestHeader("X-User-Id") Long userId,
         @RequestParam PaymentType type)
```
- **Security fix**: Removed `userId` from `@RequestParam` — all endpoints now use `X-User-Id` header
- `getPaymentByOrderId` now validates ownership
<!-- slide -->
#### PaymentService.java
Key changes:
- Verification now transitions: `CREATED → VERIFIED → (saga)`
- Delegates post-payment logic to `PaymentSagaOrchestrator`
- Added `validateReferenceMapping()` — ensures PaymentType/ReferenceType consistency
- Added `preventDuplicatePayment()` — checks active payments on same reference
- `getPaymentByOrderId()` now requires userId for ownership validation
- Enhanced idempotency: returns current state for `SUCCESS`, `COMPENSATED`, `SUCCESS_PENDING`
<!-- slide -->
#### MentorService.java
```diff
+@Transactional
+public void revertMentorApproval(Long mentorId) {
+    // Reverts status APPROVED → PENDING
+    // Reverts role ROLE_MENTOR → ROLE_USER
+}
```
Compensation method for saga rollback.
<!-- slide -->
#### GlobalExceptionHandler.java
- `PaymentException` now uses **dynamic HTTP status** from the exception
- Added `MissingRequestHeaderException` handler → returns `401` for missing `X-User-Id`
- Extracted `buildResponse()` helper with `LinkedHashMap` for consistent key ordering
<!-- slide -->
#### PaymentException.java
```diff
+private final HttpStatus httpStatus;
+
+public PaymentException(String errorCode, String message, HttpStatus httpStatus)
```
Supports per-error HTTP status codes (400, 401, 403, 404, 409, 500).
````

---

## Key Design Decisions

### 1. Saga Orchestration Pattern
- **Why not @Transactional for everything?** Business actions (mentor approval, auth-service calls) involve external services and message queues. A single transaction would hold DB locks too long and can't span services.
- **REQUIRES_NEW propagation** on saga state transitions ensures each step is independently committed, making the system resilient to partial failures.

### 2. Compensation over Rollback
- Razorpay payments **cannot be reversed** once confirmed. The system records the compensation reason and reverts internal state.
- Mentor approval revert is **best-effort** for the auth-service role change — if it fails, the mentor status is still reverted.

### 3. Reference Mapping
- **Every payment must have a `referenceId` + `referenceType`** — this is enforced at the DTO level with `@NotNull`.
- Enables: traceability, duplicate prevention, and debugging.

### 4. Security
- **No endpoint accepts userId from request params or body** — always from `X-User-Id` header (set by API Gateway from JWT).
- Missing header returns `401 UNAUTHORIZED`.
- Cross-user access attempts return `403 FORBIDDEN`.

### 5. Event-Driven Decoupling
- Payment Service **does not import or depend on** User Service — all coordination is via RabbitMQ `payment.business.action` events.
- `PaymentSagaOrchestrator` publishes events; `PaymentEventConsumer` in User Service consumes them.
- Business actions are modular — easy to add new payment types.
- This replaces the old direct method call coupling (`MentorService.approveMentor()`) with message-based coordination.

### 6. Payment Notifications
- Payment events (`payment.success`, `payment.failed`, `payment.compensated`) are published to RabbitMQ `payment.exchange`.
- Notification Service consumes these events and pushes user-friendly notifications via WebSocket.
- Notification failure does NOT affect payment flow — it is best-effort.

---

## Error Code Reference

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `ORDER_NOT_FOUND` | 404 | Payment order doesn't exist |
| `UNAUTHORIZED_ACCESS` | 403 | Payment doesn't belong to user |
| `SIGNATURE_INVALID` | 400 | Razorpay signature verification failed |
| `AMOUNT_MISMATCH` | 400 | Server-side amount doesn't match |
| `DUPLICATE_PAYMENT` | 409 | Payment already exists for this reference |
| `INVALID_REFERENCE` | 400 | PaymentType/ReferenceType mismatch |
| `PAYMENT_ALREADY_FAILED` | 400 | Cannot re-process failed payment |
| `ORDER_CREATION_FAILED` | 400 | Razorpay API failure |
| `PAYMENT_ERROR` | 400 | Generic payment error |

---

## Database Note

> [!WARNING]
> The `payments` table now lives in a **dedicated database** (`skillsync_payment`, schema: `payments`). The old `users.payments` table in `skillsync_user` is no longer used and should be dropped after data migration.
> `spring.jpa.hibernate.ddl-auto=update` is configured, so Hibernate will auto-create the table in the new database.

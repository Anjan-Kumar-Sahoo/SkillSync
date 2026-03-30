# SkillSync Frontend API Integration Contract

## 🧩 PART 1 — GLOBAL RULES

* **Base URL**: `http://localhost:8080` (Routed via API Gateway to corresponding microservices)
* **Format**: All requests and responses use `application/json`
* **Naming Convention**: `camelCase` for all JSON keys
* **Response Structure**:
  * **Success**: The HTTP status code (200-299) indicates success, returning the resource direct or payload inside a standard object wrapper.
  * **Error**: A standard standardized error object is returned on HTTP 4xx/5xx responses.
* **Missing/Null Fields**: Null values are explicitly returned. Optional fields might be omitted if undefined, but required fields are strictly present.

---

## 🧩 PART 2 — AUTHENTICATION DETAILS (CRITICAL)

### 1. JWT Structure
* **Header**: Sent in the `Authorization` header.
* **Format**: `Bearer <accessToken>`
* **Usage**: Required for all endpoints marked as **Auth Required: true**.

### 2. Token Behavior
* **Access Token**: Short-lived, valid for 15-60 minutes depending on environment.
* **Refresh Token**: Long-lived, valid for 7-30 days.

### 3. Refresh Flow
* **Trigger**: When the Frontend receives a `401 Unauthorized` response on any protected route.
* **Endpoint**: `POST /api/auth/refresh`
* **Request Body**:
  ```json
  { "refreshToken": "eyJhbG..." }
  ```
* **Success Response**:
  ```json
  { "accessToken": "eyJhbG...", "refreshToken": "eyJhbG..." }
  ```
* **Action**: Update global Redux state/localStorage with new tokens and transparently retry the original failed request.

### 4. 401 & 403 Handling
* **401 Unauthorized during Refresh**: If `/api/auth/refresh` itself throws 401 (token expired/blacklisted) — **Force Logout**: clear local storage and redirect user to `/login` with session expiry toast.
* **403 Forbidden**: Token is valid, but user lacks necessary role (e.g., Learner trying to access Admin endpoints). Route to a Not Authorized view.

---

## 🧩 PART 3 & PART 4 — API FORMAT & ENDPOINTS BY FEATURE

### 🔹 AUTH APIs

#### Endpoint: `POST /api/auth/register`
**Description**: Registers a new user. Default role is Learner.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "Password123!"
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Registration successful. Please verify email.",
  "email": "john@example.com"
}
```

#### Endpoint: `POST /api/auth/verify-otp`
**Description**: Verifies email address using the 6-digit OTP sent via RabbitMQ/Email.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "email": "john@example.com",
  "otp": "123456"
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Email verified successfully."
}
```

#### Endpoint: `POST /api/auth/resend-otp`
**Description**: Resends verification OTP to email.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "email": "john@example.com"
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "OTP resent successfully."
}
```

#### Endpoint: `POST /api/auth/login`
**Description**: Authenticates user via email/password.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "email": "john@example.com",
  "password": "Password123!"
}
```

**Success Response**: *(200 OK)*
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "user": {
    "id": "usr-123",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "ROLE_LEARNER",
    "emailVerified": true
  }
}
```

#### Endpoint: `POST /api/auth/oauth-login`
**Description**: Authenticates user via Google OAuth profile payload.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "provider": "google",
  "providerId": "10023000...",
  "email": "demo@gmail.com",
  "firstName": "Demo",
  "lastName": "User"
}
```

**Success Response**: *(200 OK)*
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "passwordSetupRequired": false,
  "user": {
    "id": "usr-125",
    "email": "demo@gmail.com",
    "role": "ROLE_LEARNER"
  }
}
```

#### Endpoint: `POST /api/auth/setup-password`
**Description**: Sets a password for purely OAuth-authenticated users.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "email": "demo@gmail.com",
  "password": "NewPassword123!"
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Password setup successfully."
}
```

#### Endpoint: `POST /api/auth/logout`
**Description**: Invalidates the user's refresh token on the server (adds to Redis blocklist).
**Auth Required**: true
**Headers**: `Authorization: Bearer <accessToken>`

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Logged out successfully"
}
```

---

### 🔹 USER / PROFILE APIs

#### Endpoint: `GET /api/users/profile`
**Description**: Retrieves the authenticated user's profile.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "usr-123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "role": "ROLE_LEARNER",
  "bio": "Enthusiastic developer",
  "skills": ["React", "Java"]
}
```

#### Endpoint: `PUT /api/users/profile`
**Description**: Updates basic user profile metadata.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "bio": "Enthusiastic developer"
}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "usr-123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "role": "ROLE_LEARNER",
  "bio": "Enthusiastic developer",
  "skills": ["React", "Java"]
}
```

#### Endpoint: `POST /api/users/skills`
**Description**: Add skills to the user profile.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "skills": ["React", "TypeScript", "Spring Boot"]
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Skills updated successfully",
  "skills": ["React", "TypeScript", "Spring Boot"]
}
```

---

### 🔹 MENTOR APIs

#### Endpoint: `GET /api/mentors`
**Description**: Search mentors with filtering and standard pagination structure.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "content": [
    {
      "id": "mnt-456",
      "userId": "usr-456",
      "firstName": "Jane",
      "lastName": "Smith",
      "headline": "Senior Staff Engineer",
      "hourlyRate": 80.0,
      "rating": 4.8,
      "skills": ["Java", "System Design"]
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 25,
  "totalPages": 3,
  "last": false
}
```

#### Endpoint: `GET /api/mentors/{mentorId}`
**Description**: Get detailed profile of a mentor, including scheduled availability slots.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "mnt-456",
  "userId": "usr-456",
  "firstName": "Jane",
  "headline": "Senior Staff Engineer",
  "bio": "I help people scale applications.",
  "hourlyRate": 80.0,
  "rating": 4.8,
  "reviewCount": 14,
  "skills": ["Java", "System Design"],
  "availableSlots": [
    {
      "id": "slot-1",
      "startTime": "2026-03-28T10:00:00Z",
      "endTime": "2026-03-28T11:00:00Z",
      "isBooked": false
    }
  ]
}
```

#### Endpoint: `POST /api/mentors/apply`
**Description**: Allows a Learner to submit an application to become a Mentor.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "headline": "Senior Staff Engineer",
  "bio": "Extensive experience in microservices...",
  "hourlyRate": 50.0,
  "skills": ["React", "Java"],
  "linkedInUrl": "https://linkedin.com/in/john"
}
```

**Success Response**: *(201 Created)*
```json
{
  "message": "Mentor application submitted successfully. Pending admin approval."
}
```

#### Endpoint: `POST /api/mentors/availability`
**Description**: Mentors set or overwrite their open time slots.
**Auth Required**: true (Role: `ROLE_MENTOR`)
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "slots": [
    {
      "startTime": "2026-03-28T10:00:00Z",
      "endTime": "2026-03-28T11:00:00Z"
    }
  ]
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Availability updated successfully"
}
```

#### Endpoint: `POST /api/mentors/{mentorId}/approve`
**Description**: Admin approves a pending mentor application.
**Auth Required**: true (Role: `ROLE_ADMIN`)
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Mentor approved successfully."
}
```

---

### 🔹 SESSION APIs

#### Endpoint: `POST /api/sessions`
**Description**: Learner requests a new session with a Mentor.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "mentorId": "mnt-456",
  "slotId": "slot-1",
  "topic": "System Design Mock Interview",
  "notes": "Looking specifically for scaling discussion."
}
```

**Success Response**: *(201 Created)*
```json
{
  "id": "ses-789",
  "mentorId": "mnt-456",
  "learnerId": "usr-123",
  "status": "PENDING",
  "startTime": "2026-03-28T10:00:00Z",
  "endTime": "2026-03-28T11:00:00Z",
  "amount": 80.0
}
```

#### Endpoint: `POST /api/sessions/{sessionId}/accept`
**Description**: Mentor accepts a pending session. Triggers realtime Websocket notification to learner.
**Auth Required**: true (Role: `ROLE_MENTOR`)
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "ses-789",
  "status": "ACCEPTED"
}
```

#### Endpoint: `POST /api/sessions/{sessionId}/reject`
**Description**: Mentor rejects a session request.
**Auth Required**: true (Role: `ROLE_MENTOR`)
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "ses-789",
  "status": "REJECTED"
}
```

#### Endpoint: `POST /api/sessions/{sessionId}/complete`
**Description**: Mentor marks an accepted session as completed post-call.
**Auth Required**: true (Role: `ROLE_MENTOR`)
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "ses-789",
  "status": "COMPLETED"
}
```

#### Endpoint: `GET /api/sessions`
**Description**: List user's sessions (automatically detects learner vs mentor context based on auth token).
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "content": [
    {
      "id": "ses-789",
      "mentorName": "Jane Smith",
      "learnerName": "John Doe",
      "status": "ACCEPTED",
      "startTime": "2026-03-28T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 🔹 PAYMENT APIs

#### Endpoint: `POST /api/payments/order`
**Description**: Creates a Razorpay order before initiating payment on the frontend.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "sessionId": "ses-789",
  "amount": 80.0,
  "currency": "USD"
}
```

**Success Response**: *(200 OK)*
```json
{
  "orderId": "order_KjkjJd...",
  "amount": 8000,
  "currency": "USD",
  "keyId": "rzp_test_..."
}
```

#### Endpoint: `POST /api/payments/verify`
**Description**: Verifies the Razorpay payment signature post transaction SDK UI cycle.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "razorpayOrderId": "order_KjkjJd...",
  "razorpayPaymentId": "pay_Kjk...",
  "razorpaySignature": "a3b2..."
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Payment verified successfully",
  "status": "COMPLETED",
  "sessionId": "ses-789"
}
```

---

### 🔹 REVIEW APIs

#### Endpoint: `POST /api/reviews`
**Description**: Submits a review for a completed session.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "sessionId": "ses-789",
  "mentorId": "mnt-456",
  "rating": 5,
  "comment": "Excellent guidance on distributed systems."
}
```

**Success Response**: *(201 Created)*
```json
{
  "id": "rev-999",
  "message": "Review submitted successfully."
}
```

#### Endpoint: `GET /api/reviews/mentor/{mentorId}`
**Description**: Fetch all paginated reviews for a specific mentor.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "content": [
    {
      "id": "rev-999",
      "learnerName": "John Doe",
      "rating": 5,
      "comment": "Excellent guidance.",
      "createdAt": "2026-03-30T14:00:00Z"
    }
  ],
  "page": 0,
  "size": 5,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 🔹 NOTIFICATION APIs

#### Endpoint: `GET /api/notifications`
**Description**: Get paginated notifications for the user.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "content": [
    {
      "id": "notif-123",
      "type": "SESSION_BOOKED",
      "message": "Your session has been confirmed.",
      "timestamp": "2026-03-27T10:00:00Z",
      "read": false
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### Endpoint: `GET /api/notifications/unread-count`
**Description**: Gets the unread notification count.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "unreadCount": 3
}
```

#### Endpoint: `PUT /api/notifications/{id}/read`
**Description**: Marks a specific notification as read.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Notification marked as read"
}
```

---

## 🧩 PART 5 — PAGINATION & FILTERING

Define clearly:

### Pagination format:
Select endpoints returning lists strictly use this structure:
```json
{
  "content": [
     // Objects
  ],
  "page": 0,
  "size": 10,
  "totalElements": 100,
  "totalPages": 10,
  "last": false
}
```

### Filters (Mentors Example):
Used as standard GET URL-encoded query parameters:
* `skill`
* `minPrice` / `maxPrice`
* `rating`
* `availability` (boolean string)

Provide sample query params lookup:
```
/api/mentors?skill=Java&page=0&size=10&rating=4
```

---

## 🧩 PART 6 — WEBSOCKET / REAL-TIME

* **WebSocket endpoint**: `ws://localhost:8080/ws`
* **Connection method**: SockJS with STOMP client
* **Destination**: `/topic/notifications/{userId}`

### Event payload example:

```json
{
  "id": "notif-890",
  "type": "SESSION_BOOKED",
  "message": "Your session is confirmed",
  "timestamp": "2026-03-27T14:30:00Z",
  "read": false
}
```

---

## 🧩 PART 7 — ERROR HANDLING (IMPORTANT)

**Global Error Format**:
Any response HTTP status > 299 maps to this strict formal failure payload.

### Error Response:
```json
{
  "status": 400,
  "message": "Validation error",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ]
}
```

**Common Status Constraints**:
* **400**: Bad Request. Maps directly to field-level errors if `errors[]` array present. 
* **401**: Unauthorized. Triggers Token Refresh layer logic or force logout.
* **403**: Forbidden. Show 'Insufficient permissions' UI.
* **404**: Not found. Standard 404 message.
* **500**: Internal Server Error. 

---

## 🧩 PART 8 — RATE LIMITING & EDGE CASES

* **Payment APIs Restrictions**: Rate limited logic restricts redundant Razorpay order generation logic to avoid split-second double charges on the user. Frontends must immediately lock inputs/buttons (disable state) until `/api/payments/verify` finishes its pass gracefully.
* **Auth Rate Limits**: Gateway applies aggressive rate-limits globally limiting brute force requests to `/api/auth/*` routes. Yields standard 429 when max bucket is triggered.
* **Expected Retry Behavior**: Use exponential back-off up to 3 times ONLY on `503 Service Unavailable` or `429 Too Many Requests`.

---

## 🧩 PART 9 — SAMPLE END-TO-END FLOW

Here is one fully verified example of a cross-system workflow logic combining tokens, calls, and realtime components:

1. **Login User**:
   * Request: `POST /api/auth/login` | payload `{"email":"learner@test.com", "password":"pass"}`
   * Consequence: Get `accessToken` and persist in Redux.

2. **Search Mentor**:
   * Request: `GET /api/mentors?skill=Java` 
   * Consequence: Returns list. You select Mentor `mnt-001` and an associated slot.

3. **Create Session**:
   * Request: `POST /api/sessions` | payload `{"mentorId":"mnt-001", "slotId":"slot-abc", "topic":"Help"}`
   * Consequence: Returns session `ses-999` marked as `PENDING`.

4. **Websocket Fires (Mentor Perspective)**:
   * Mentor receives STOMP message payload on `/topic/notifications/usr-mentorId` that a session is requested.
   * Mentor accepts session: `POST /api/sessions/ses-999/accept`

5. **Create Payment**:
   * Request: Learner initiates booking workflow via `POST /api/payments/order` | payload `{"sessionId":"ses-999", "amount": 80.0, "currency":"USD"}`
   * Consequence: Acquires `orderId`, popping open Razorpay frontend UI Modal lock.

6. **Verify Payment**:
   * Request: User submits valid card. SDK returns success properties.
   * Consequence: Invoke `POST /api/payments/verify` | payload `{"razorpayOrderId": "...", "razorpayPaymentId": "...", "razorpaySignature": "..."}`
   * Output: Complete execution cycle marked safely.

7. **Receive Realtime Confirmed Notification**:
   * Learner is hit with async WebSocket payload notifying `SESSION_BOOKED: Your session is confirmed.`

---

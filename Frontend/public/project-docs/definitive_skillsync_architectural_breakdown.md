# 🏗️ SkillSync: The Definitive Architectural & Logic Breakdown

This document provides a granular, step-by-step audit of the entire SkillSync ecosystem. It breaks down the logic, naming conventions, and runtime flow of every core component in your project.

---

## 📂 1. PROJECT STRUCTURE & PHILOSOPHY

SkillSync is architected as a **Distributed SaaS Platform** using the **Cloud-Native Microservices** pattern.

### The Backend Strategy:
*   **Unification**: All external traffic flows through the `api-gateway`.
*   **Separation of Concerns**: `auth-service` manages identity; `user-service` manages data; `skill-service` manages domain logic.
*   **Centralized Config**: Uses Spring Cloud Config to manage property files across services.
*   **Shared Cache**: Managed through a custom `skillsync-cache-common` module to ensure consistent Redis patterns.

---

## 🔐 2. SECURITY: THE "IDENTITY PIPELINE"

This is the most critical logic in your project. It is "Double-Gated."

### Step 1: The Gatekeeper (api-gateway)
*   **Logic**: The `JwtAuthenticationFilter` (Reactive) performs a "Signature Check." It doesn't ask the DB; it simply trusts the cryptographic signature of the token.
*   **Innovation**: It performs **Request Mutation**. It extracts the User ID and injects it as an `X-User-Id` header. This allows downstream services to stay "security-agnostic."

### Step 2: The Authority (auth-service)
*   **Logic**: Uses **HS256 Symmetric Signing**. The secret key is the "God Key."
*   **Manual Authentication**: You bypass Spring's default login form for a manual REST controller. This allows you to return tokens in the JSON response PLUS a secure cookie.
*   **Refresh Token Rotation**: CRITICAL STEP. Inside `AuthService.java`, the system deletes the old token upon use. This prevents an attacker from using a stolen refresh token indefinitely.

### Step 3: The Consumer (user-service)
*   **Logic**: It has no security configuration. It relies on `@RequestHeader("X-User-Id")`.
*   **Granular Word Analysis**: The use of `X-` prefix is the industry standard for custom headers. It identifies that the ID has been "pre-verified" by a proxy.

---

## 🌐 3. FRONTEND: THE "STATEFUL UI"

Your UI isn't just a skin; it's a dynamic state machine.

### The Interceptor Flow (src/services/axios.ts)
*   **Request Interceptor**: Automatically pulls the `accessToken` from Redux state and attaches it to the `Authorization` header.
*   **Response Interceptor**: This is your "Self-Healing" logic. If an API returns **401**, the interceptor automatically calls the `/refresh` endpoint, gets a new token, and **retries the original request** without the user ever seeing a flicker.

### State Management (Redux Toolkit)
*   **`authSlice.ts`**: Handles the persistent state of the user.
*   **`themeSlice.ts`**: Manages the "Premium Theme Studio" real-time styling.
*   **Logic**: Uses `extraReducers` for async thunks (login/logout), ensuring the UI updates automatically based on the API response.

---

## ⚙️ 4. MICROSERVICE INTERNALS (SERVICE-BY-SERVICE)

### Auth-Service (The Brain)
*   **`AuthController`**: Handlers for Login, Register, OTP Verify, and OAuth.
*   **`UserDetailsServiceImpl`**: The bridge to JPA. It maps your `AuthUser` entity to Spring's internal `UserDetails`.
*   **Naming Convention**: You use `DTO` (Data Transfer Object) consistently. Example: `LoginRequest` vs `AuthResponse`. This prevents your internal DB Entities from being exposed to the browser.

### User-Service (The Data)
*   **`UserProfileController`**: Manages BIOS, Avatar, and Social links.
*   **Cache Eviction**: Every time a user updates their profile, the service calls `cacheService.evict()`. 
*   **Granular Step**: This ensures that when the Gateway or Chat-Service asks for the user's profile from Redis, they get the fresh data, not the old one.

### Eureka & Config Server (The Skeleton)
*   **Logic**: Services register themselves with Eureka using a heart-beat mechanism. If `auth-service` dies, the Gateway is notified within seconds and stops routing traffic to it.

---

## 🚨 5. ERROR & EXCEPTION LOGIC

Your project uses a **Global Exception Handling Pattern**.
*   **`GlobalExceptionHandler.java`**: Uses `@RestControllerAdvice`.
*   **Logic**: It catches `RuntimeException`, `MethodArgumentNotValidException`, and custom business errors. 
*   **Result**: It returns a standardized JSON structure: `{ "message": "...", "status": 404 }`. This allows the Frontend (Axios) to handle all errors in a single catch block.

---

## 🏢 6. SCALABILITY & PRODUCTION AUDIT

### Real-World Readiness:
*   **Pros**: Microservices, Eureka, Redis Caching, Gateway Proxying. This is exactly how most SaaS companies (Series A/B) architect their systems.
*   **Gaps**:
    1.  **JWT Revocation**: You need a Redis-based blacklist.
    2.  **Rate Limiting**: You should move the rate-limiting logic to the Nginx/Cloudfront level in production.
    3.  **Circuit Breakers**: You could add `Resilience4j` to handle cases where `auth-service` is slow.

---

## 🏁 7. THE PROJECT IN A NUTSHELL (FOR REVIEW)

**"SkillSync is a full-stack, distributed microservices platform designed for horizontal scalability. It follows a stateless token-based security model enforced at an API Gateway, with a centralized identity service managing JWT lifecycles and Refresh Token Rotation. The React frontend is a 'Self-Healing' UI that utilizes Axios interceptors for silent token refreshes and Redux for real-time state synchronization, resulting in a production-grade, enterprise-ready SaaS architecture."**

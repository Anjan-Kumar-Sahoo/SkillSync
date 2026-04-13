# 🚀 SkillSync: End-to-End Execution Trace Analysis

This document provides a forensic, debugger-level trace of the SkillSync platform, from the Frontend UI to the Backend Database.

---

## 1. 🏗️ HIGH-LEVEL OVERVIEW

**Architecture**: Distributed Microservices (Cloud-Native)
**Communication**: REST API (Synchronous) + Reactive Gateway
**Security**: Stateless JWT (HS256) with Stateful Refresh Token Rotation
**Frontend**: React (Vite) + Redux Toolkit

---

## 2. 🔄 FULL REQUEST FLOW (STEP-BY-STEP)

### Feature: User Login
| Step | Layer | File | Method/Function | Logic |
| :--- | :--- | :--- | :--- | :--- |
| 1 | UI | `Login.tsx` | `handleSubmit` | Captures user email/password. |
| 2 | State | `authSlice.ts` | `loginUser` (Async Thunk) | Dispatches the API call. |
| 3 | API | `axios.ts` | `apiClient.post` | Sends POST to `/api/auth/login`. |
| 4 | Proxy | `gateway` | `JwtAuthenticationFilter` | Bypasses auth (it's a public path). |
| 5 | Backend | `AuthController` | `login(LoginRequest)` | Receives JSON payload. |
| 6 | Service| `AuthService` | `login` | Calls `manager.authenticate()`. |
| 7 | DB | `AuthUserRepository`| `findByEmail` | Fetches hashed password from PostgreSQL. |
| 8 | Logic | `BCrypt` | `matches()` | Compares raw password with DB hash. |
| 9 | Token | `JwtTokenProvider` | `generateAccessToken` | Signs JWT using HS256 secret. |
| 10| Resp | `AuthController` | `ResponseEntity.ok` | Returns JSON + `HttpOnly` cookie. |

---

## 3. 🔐 SECURITY FLOW (DEEP LEVEL)

### A. JWT Mechanics
*   **Generation**: Done in `JwtTokenProvider.java` using the `jjwt` library.
*   **Signature**: Symmetric (HS256). Both issuer and validator share the same secret string.
*   **Storage**: 
    *   **AccessToken**: Client-side memory (Redux) + JSON body.
    *   **RefreshToken**: `HttpOnly` Cookie (Prevents XSS theft).
*   **Validation**: Performed by the `JwtAuthenticationFilter`. If `expiry < now` or `signature` is invalid, a 401 is returned.

### B. THE FILTER CHAIN (Servlet Flow)
Request → `CorsFilter` → `LogoutFilter` → **`JwtAuthenticationFilter`** → `FilterSecurityInterceptor`

1.  **JwtAuthenticationFilter** (`OncePerRequestFilter`): 
    *   Examines the `Authorization` header.
    *   Calls `jwtTokenProvider.validateToken()`.
    *   Populates **SecurityContextHolder** via `setAuthentication()`.
2.  **FilterSecurityInterceptor**:
    *   Checks the `permitMatchers` defined in `SecurityConfig`.
    *   If path is `/api/auth/login`, it allows the request.

---

## 4. 🔁 INTERNAL CODE CALL TRACE

**Controller Lifecycle (Login)**:
```
AuthController.login(LoginRequest)
└── AuthService.login(LoginRequest)
    ├── AuthenticationManager.authenticate(UsernamePasswordAuthenticationToken)
    │   └── DaoAuthenticationProvider.authenticate()
    │       └── UserDetailsServiceImpl.loadUserByUsername(email)
    │           └── AuthUserRepository.findByEmail(email)
    ├── JwtTokenProvider.generateAccessToken(userId, email, role)
    └── RefreshTokenRepository.save(new RefreshToken)
```

---

## 5. 🧠 WHY-BASED EXPLANATION (CRITICAL)

### Q: Why HS256 (Symmetric) instead of RS256 (Asymmetric)?
*   **Answer**: SkillSync is a private microservice cluster. Since the Auth Service and Gateway are in the same trust boundary, managing a single secret is more efficient. RS256 is only needed if we allow external partners to verify our tokens.

### Q: Why Stateless JWT but a Persistent Refresh Token?
*   **Answer**: This is the **"Stateless Paradox."** We want the AccessToken to be fast and zero-latency (Stateless). We want the RefreshToken to be revokable (Stateful). By storing the RefreshToken in the DB, we can manually "Log out" a user by deleting their refresh session.

---

## 6. ❓ WH-QUESTIONS (INTERVIEW MODE)

### Module: Filter Chain
*   **Q**: What happens if `chain.doFilter()` is not called?
*   **A**: The request is halted; the user sees a blank response or a generic failure.
*   **Q**: Who clears the `SecurityContext` after a request?
*   **A**: Spring's internal `SecurityContextPersistenceFilter` ensures the `ThreadLocal` is wiped before the thread returns to the pool.

### Module: JWT
*   **Q**: What if the `jwt.secret` is leaked?
*   **A**: An attacker can forge any user role, including Admin. Corrective action involves immediate key rotation via environment variables.

---

## 7. 🧰 TECHNOLOGIES USED
*   **Spring Boot Starter Security**: Core framework.
*   **JJWT (0.12.6)**: Manual JWT generation and parsing.
*   **Spring Cloud Gateway**: Reactive edge security.
*   **PostgreSQL**: User and Token persistence.
*   **Redis**: Profile and domain caching.

---

## 8. ⚠️ HIDDEN MECHANISMS
*   **Auto-Configuration**: Spring Security automatically creates a `DaoAuthenticationProvider` because it finds your `UserDetailsService` and `PasswordEncoder` beans.
*   **Credentials Nulling**: Spring Security automatically clears the password from the `Authentication` object after successful validation to increase safety.

---

## 9. 🔥 EDGE CASES
*   **Token Expired during Request**: The `JwtAuthenticationFilter` throws an exception, caught by the **ETF (ExceptionTranslationFilter)**, returning a 401. The Frontend Axios interceptor then triggers a `/refresh` call.
*   **DB Connection Failure**: If PostgreSQL is down during login, the system returns a 500 via the `GlobalExceptionHandler`.

---

## 🧭 10. VISUAL FLOW

**Architecture Flow**:
`React (UI) --(JWT)--> API Gateway (Validate) --(Header)--> Microservice (Business Logic) --(JPA)--> PostgreSQL`

**Internal Security Flow**:
`Request --> FilterChain --> Context Set --> DispatcherServlet --> Interceptor --> Controller`

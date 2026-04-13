# 🔐 Complete Security & Request Lifecycle Documentation

---

## 1. 🧾 High-Level Architecture

The SkillSync platform utilizes a **Stateless Distributed Security Architecture** optimized for a microservices environment. 

*   **Authentication Method**: **JWT (JSON Web Token)**. The system employs a "Dual-Delivery" strategy where tokens are returned in both the JSON response body and as **HttpOnly, Secure, SameSite=None** cookies. This ensures compatibility across multiple subdomains (`skillsync.rangaraju.dev` vs `api.skillsync.mraks.dev`).
*   **Authorization Model**: **RBAC (Role-Based Access Control)**. Roles (`ROLE_LEARNER`, `ROLE_MENTOR`, `ROLE_ADMIN`) are embedded as claims within the JWT and propagated through the system via custom HTTP headers.
*   **Real-World Analogy**: Imagine a high-security airport.
    *   **The Auth Service** is the passport office that verifies your identity and issues a digital passport (JWT).
    *   **The API Gateway** is the security checkpoint. It verifies your passport and stamps your "Internal Boarding Pass" (Custom Headers) with your identity and clearance level.
    *   **Downstream Services** (User Service, Skill Service) are the individual airport lounges. They don't check your passport again; they simply look at the stamp on your boarding pass (Headers) provided by the Security Checkpoint (Gateway).

---

## 2. 🧑‍💻 Actors Involved

1.  **Browser**: The client environment (React/Vite). Responsible for storing tokens in `localStorage` and sending them via Axios.
2.  **API Gateway (Spring Cloud Gateway)**: The entry point (running on **Netty**). It performs JWT validation, rate limiting, and request routing.
3.  **Auth Service (Spring Boot/Tomcat)**: The identity provider. Handles login, registration, password hashing (BCrypt), and JWT generation.
4.  **Downstream Services (User/Skill/Session)**: Business logic containers. They trust the identity provided by the Gateway.
5.  **PostgreSQL**: The source of truth for user credentials and profiles.
6.  **Redis**: Used for caching and session state (where applicable).

---

## 3. 🌐 FULL REQUEST LIFECYCLE

### 👉 1. The Browser Trigger
When a user clicks a button (e.g., "Change Profile"), the Frontend triggers an Axios call.
*   **Axios Interceptor**: Before the request leaves, an interceptor injects the `Authorization: Bearer <token>` header from Redux state.
*   **DNS Resolution**: `api.skillsync.mraks.dev` is resolved to the IP of the Load Balancer or API Gateway.

### 👉 2. The Gateway Entry (Netty Internals)
Unlike traditional services, the Gateway uses **Netty** (Event Loop model).
*   **Selector Loop**: Netty's worker thread picks up the TCP packet.
*   **HttpServerCodec**: Decodes the byte stream into an HTTP request object.
*   **Gateway Filters**: The request hits the `GlobalFilter` chain.
    *   **JwtAuthenticationFilter (Custom)**: Extracts the token, validates the signature using the `HMAC-SHA256` key, and extracts claims (`sub`, `email`, `role`).
    *   **Request Mutation**: The Gateway creates a *new* internal request, removing any "spoofed" `X-User-Id` headers and injecting the verified details from the JWT.

### 👉 3. Downstream Routing
The Gateway uses **LoadBalancerClient (Caffeine-backed)** to find the `user-service` instance via **Eureka**. It then forwards the mutated request (with `X-User-Id` header).

### 👉 4. Inside the Microservice (Tomcat Internals)
The request reaches a service like `user-service`.
*   **Connector**: Tomcat's `Http11NioProtocol` connector accepts the connection.
*   **Processor**: An `Http11Processor` reads the headers.
*   **CoyoteAdapter**: Converts the internal Coyote request to a `HttpServletRequest`.
*   **DispatcherServlet**: The central "Front Controller" receives the request.
    *   It queries `HandlerMapping` to find the controller method.
    *   It uses `HandlerAdapter` to execute the method.
    *   **Argument Resolver**: The `@RequestHeader("X-User-Id")` is resolved automatically by Spring MVC from the headers injected by the Gateway.

---

## 4. 🔗 SPRING SECURITY FILTER CHAIN

In the **Auth Service**, Spring Security is fully active to handle the login/authentication logic.

### ✅ Default Filter Chain Execution Order:
1.  **Disable CSRF**: Since we use JWTs (which are resistant to CSRF if stored correctly and sent via headers), CSRF is disabled via `AbstractHttpConfigurer::disable`.
2.  **SecurityContextPersistenceFilter**: Checks if a `SecurityContext` exists in the `HttpSession` (Stateless policy means this is usually empty, but it's the first stop).
3.  **HeaderWriterFilter**: Adds security headers (X-Frame-Options, X-Content-Type-Options) to the response.
4.  **LogoutFilter**: Handles `/logout` if configured.
5.  **JwtAuthenticationFilter (Custom)**: 
    *   Checks `Authorization` header OR `accessToken` cookie.
    *   If present, validates the token.
    *   Calls `UserDetailsService` to load user details.
    *   Creates a `UsernamePasswordAuthenticationToken` and places it in the `SecurityContextHolder`.
6.  **UsernamePasswordAuthenticationFilter**: Standard filter for `/login`. It intercepts POST requests to the login path, extracts credentials, and passes them to the `AuthenticationManager`.
7.  **FilterSecurityInterceptor**: The "Final Boss." It checks the authenticated user against the required permissions (Authorization).

---

## 5. 🔑 AUTHENTICATION FLOW (LOGIN STEP-BY-STEP)

When a user posts to `/api/auth/login`:

1.  **Controller Entry**: `AuthController.login(LoginRequest)` is called.
2.  **Manager Delegation**: The controller calls `authService.login()`, which invokes `authenticationManager.authenticate()`.
3.  **Provider Logic**: `DaoAuthenticationProvider` is triggered.
    *   It calls `UserDetailsServiceImpl.loadUserByUsername(email)`.
    *   It compares the raw password from the request with the BCrypt-hashed password from the DB.
4.  **Success**: If valid, an `Authentication` object is stored in the `SecurityContext`.
5.  **Token Generation**: `JwtTokenProvider` generates access and refresh tokens.
6.  **Response Construction**: The tokens are added to the JSON body and set as `HttpOnly` cookies via `Set-Cookie` headers.

---

## 6. 🎟️ TOKEN GENERATION (JWT)

Generated using the `jjwt` library:
*   **Header**: `{"alg": "HS256", "typ": "JWT"}`
*   **Payload (Claims)**: 
    *   `sub`: User ID (String)
    *   `email`: User's email
    *   `role`: User's primary role (e.g., `ROLE_MENTOR`)
    *   `iat`: Issued at time
    *   `exp`: Expiration time (typically 24h for access, 7d for refresh)
*   **Signature**: `HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)`

---

## 7. 🧠 TOKEN STORAGE & USAGE

### Frontend:
*   **Redux (AuthSlice)**: Holds the token in memory for fast access.
*   **LocalStorage**: Persists the token across page refreshes (`ss_access_token`).
*   **Silent Refresh**: When a `401 Unauthorized` is received, an Axios interceptor catches it, calls `/api/auth/refresh` using the stored refresh token, updates the `localStorage`, and retries the original failed request.

### Backend:
*   **Gateway**: Validates signature and expiration.
*   **Services**: Use `@RequestHeader` to consume the identity.

---

## 8. 🧵 THREAD & SECURITY CONTEXT

The system utilizes `ThreadLocal` storage via **SecurityContextHolder**.
*   **Scope**: The `Authentication` object is bound to the specific thread handling the request.
*   **Lifecycle**:
    1.  Filter sets the `Authentication` object.
    2.  Controller/Service accesses it via `SecurityContextHolder.getContext().getAuthentication()`.
    3.  Once the response is sent, the `SecurityContextPersistenceFilter` clears the `ThreadLocal` to prevent "Thread Poisoning" (crucial when using Thread Pools where threads are reused).

---

## 9. 🚨 ERROR & EXCEPTION FLOW

1.  **Missing Token**: Gateway returns `401 Unauthorized` immediately.
2.  **Expired Token**: `JwtTokenProvider` throws `ExpiredJwtException`. The filter catches this and the `ExceptionTranslationFilter` (or Gateway `onError`) returns `401`.
3.  **Forbidden Role**: If a `ROLE_LEARNER` tries to access `/api/admin/**`, the `FilterSecurityInterceptor` detects the mismatch and throws an `AccessDeniedException`, resulting in a `403 Forbidden`.
4.  **GlobalExceptionHandler**: Standardized JSON error responses ensure the frontend can parse the failure reason.

---

## 10. 🔒 SECURITY MECHANISMS

*   **BCrypt**: Passwords are never stored in plain text. A salt is generated automatically per password, and 12 rounds of hashing are used.
*   **CORS**: Configured at the Gateway level to allow specific origins (`skillsync.mraks.dev`, etc.) while blocking others.
*   **Statelessness**: No `HttpSession` is stored on the server side, allowing the backend to scale horizontally effortlessly.
*   **Internal Blocking**: The Gateway explicitly blocks any request starting with `/api/auth/internal/**` to prevent external users from calling service-to-service internal APIs.

---

## 11. 📊 SEQUENCE DIAGRAM (TEXT)

```text
[ Browser ] -> [ Nginx ] -> [ API Gateway ] -> [ Auth Service ]
    |             |              |                  |
    |---(Login)-->|------------->|                  |
    |             |              |---(Authenticate)--> [ DB ]
    |             |              |                  |    |
    | <--(Tokens)-|<---(Tokens)--|<-------(Success)------|
    |             |              |                  |
[ Browser State ] (store JWT in localStorage)
    |
    |---(Get Profile + JWT)----->| (Validate JWT)
    |             |              |---(Inject Headers)--> [ User Service ]
    |             |              |                          |
    | <---(200 OK)---------------|<-----------(JSON)--------|
```

---

## 12. 🖥️ FRONTEND SECURITY FLOW (STORY MODE)

1.  **User logs in**: `AuthService.login()` sends credentials.
2.  **Success**: Backend sends JWT. Frontend stores it in `localStorage` and Redux.
3.  **Navigation**: User goes to `/dashboard`. A **Route Guard** (`ProtectedRoute.tsx`) checks `state.auth.isAuthenticated`.
4.  **Data Fetch**: Dashboard calls `/api/users/me`. Axios interceptor adds `Bearer <JWT>`.
5.  **Validation**: Gateway verifies JWT, injects `X-User-Id: 101`.
6.  **Service Action**: `UserController` reads `X-User-Id`, fetches user 101 data, and returns it.
7.  **Logout**: User clicks Logout. `authSlice` clears `localStorage`, Redux, and calls backend `/api/auth/logout` to clear cookies.

---

## ⚙️ JVM & TOMCAT MICRO-STEPS

For every request to the `auth-service` (Tomcat):

1.  `NioEndpoint`: The worker thread (e.g., `http-nio-8081-exec-1`) is pulled from the pool.
2.  `CoyoteAdapter.service()`: The entry point into the Servlet world.
3.  `FilterChain.doFilter()`: Sequential execution of filters.
4.  `ReflectiveMethodInvocation`: Spring uses reflection to call the Controller method (`AuthController.login`).
5.  `JIT Compiler`: Frequent security checks and token parsing code sections are optimized into native machine code (HotSpot).
6.  `GC (G1)`: Claims objects and temporary byte buffers from JWT parsing are short-lived and cleaned up in Young Generation (Eden space).

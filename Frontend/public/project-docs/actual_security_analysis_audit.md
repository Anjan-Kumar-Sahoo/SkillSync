# 🔐 SkillSync: Actual Security & JVM Internals Audit

This document is a forensic analysis of the **actual** security mechanisms implemented in the SkillSync project. It ignores theoretical Spring Security features and focuses only on what is present in the codebase.

---

## 1. 🌐 THE ACTUAL REQUEST FLOW (TOMCAT TO CONTROLLER)

When a request (e.g., `GET /api/users/me`) hits the backend, the flow is:

1.  **Tomcat Selector**: A Tomcat worker thread picks up the request. `HttpServletRequest` and `HttpServletResponse` are instantiated by Tomcat's `CoyoteAdapter`.
2.  **DelegatingFilterProxy**: This is a standard Servlet Filter that acts as a bridge. It looks up a Bean named `springSecurityFilterChain`.
3.  **FilterChainProxy**: The central entry point for Spring Security. It holds the list of `SecurityFilterChain` beans.
4.  **Active Security Filters**: (See Section 3 for the specific list).
5.  **DispatcherServlet**: Once all filters call `chain.doFilter()`, the request hits the "Front Controller."
6.  **HandlerMapping**: DispatcherServlet looks for `@RestController` mapping.
7.  **Controller Entry**: The method (e.g., `getMyProfile`) is executed.

---

## 2. 🔗 FILTER CHAIN INTERNALS (WHO CALLS WHOM?)

The magic happens inside the `doFilter(request, response, chain)` method.

*   **Logic**: Every filter performs its task (e.g., `JwtAuthenticationFilter` checks for a token).
*   **The Chain**: If the filter is satisfied, it **MUST** call `chain.doFilter(request, response)`.
*   **WHAT IF**: If `chain.doFilter()` is NOT called (e.g., on a 401 error), the request is "halted." It never reaches the Controller, and the response is sent back immediately.

---

## 3. ✅ USED vs ❌ NOT USED FILTERS

| Component | Status | Where/How | Impact of Removal |
| :--- | :--- | :--- | :--- |
| **HeaderWriterFilter** | **USED** | Default | Browser loses Clickjacking/MIME protection. |
| **CorsFilter** | **USED** | `SecurityConfig` | Frontend (React) will get "CORS Error" and fail to fetch data. |
| **CsrfFilter** | **NOT USED** | Disabled in `SecurityConfig` | None (Project uses JWT, which is stateless). |
| **LogoutFilter** | **USED** | Default | Standard logout logic would break. |
| **JwtAuthenticationFilter** | **USED** | **Custom Class** | No API call would be authenticated; all requests get 403. |
| **AnonymousAuthFilter** | **USED** | Default | `SecurityContext` would be null for public pages, causing NullPointerErrors. |
| **ExceptionTranslationFilter**| **USED** | Default | Filter-level errors would return 500 instead of 401/403. |
| **FilterSecurityInterceptor** | **USED** | Final Link | `@PreAuthorize` and `requestMatchers` would stop working. |
| **BasicAuthFilter** | **NOT USED** | Not configured | None. |
| **UsernamePasswordFilter** | **NOT USED** | Replaced | We use manual JSON login in `AuthController`. |

---

## 4. 🔑 AUTHENTICATION COMPONENTS (ACTUAL CODE)

*   **`UsernamePasswordAuthenticationToken`**: Used inside `AuthService.login` to wrap the email/password before passing to the manager.
*   **`AuthenticationManager`**: Managed by Spring. Triggered manually in `AuthService`.
*   **`UserDetailsServiceImpl`**: Implements `UserDetailsService`. It's the bridge to the PostgreSQL DB.
*   **`BCryptPasswordEncoder`**: Defined as a `@Bean` in `SecurityConfig`. Used for hashing during registration and comparison during login.

---

## 5. 🧠 SECURITY CONTEXT & JVM THREADING

The project uses the default **`MODE_THREADLOCAL`**.

*   **JVM Internals**: The `SecurityContext` is stored in a `ThreadLocal` variable. This means the authenticated user is "globally accessible" within the **same thread** handling the request.
*   **Thread Hygiene**: Spring Security automatically clears the `ThreadLocal` at the end of the request via the `SecurityContextPersistenceFilter` (or modern equivalent).
*   **Thread Pool Behavior**: Because Tomcat reuses threads, if the context wasn't cleared, "User A" might find themselves logged in as "User B" on the next request. This project prevents this via automatic cleanup.

---

## 6. 🚨 EXCEPTION FLOW (CRITICAL)

In this project, exceptions are handled differently depending on **where** they happen:

### Scenario A: Exception in Filter (e.g., Expired JWT)
*   **Flow**: `JwtAuthenticationFilter` → `ExceptionTranslationFilter`.
*   **Reality**: Since the request hasn't reached Spring MVC yet, **`@ControllerAdvice` cannot see this exception.**
*   **Result**: The error is handled by the `AuthenticationEntryPoint` (default returning 401/403).

### Scenario B: Exception in Controller
*   **Flow**: Controller → `GlobalExceptionHandler` (`@RestControllerAdvice`).
*   **Reality**: This is where `ResourceNotFoundException` or `BadCredentialsException` (thrown from service) are caught and converted to JSON.

---

## 7. 🔁 REFRESH TOKEN REALITIES

*   **Mechanism**: Database-backed rotation.
*   **Flow**: When refreshing, the old Refresh Token is **DELETED** and a new one is created.
*   **Security Risk**: The project does NOT implement "Sliding Window" detection. If a Refresh Token is stolen, the attacker can refresh until the token expires naturally (7 days). However, single-use rotation (deleting on use) limits the window of opportunity.

---

## 8. 🧠 REDIS USAGE

Contrary to theoretical "Redis Blacklists," this project uses Redis for **Cache Management**, not for Security Blocking.
*   **Where**: `CacheService` is used to store user profiles.
*   **Security link**: When a user's **Role** is updated in `AuthService`, Redis is told to delete that profile cache so the Gateway doesn't use old role data.

---

## 📊 FINAL ARCHITECTURE SUMMARY

*   **Type**: Stateless Microservices Security.
*   **Source of Truth**: The `auth-service` issues tokens; the `api-gateway` validates them.
*   **Weakness**: Once an `AccessToken` is issued, there is no way to revoke it (statelessness). It is valid until it expires (24h).
*   **Strength**: Extremely scalable. The `user-service` and `skill-service` don't even need to talk to a database or Auth Service to verify a user; they simply trust the `X-User-Id` header from the Gateway.

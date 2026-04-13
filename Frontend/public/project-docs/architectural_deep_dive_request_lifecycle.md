# 🏗️ Spring Boot & Security: The Internal Request Lifecycle

This document is a technical forensic audit of how a request moves through the JVM, Tomcat, and Spring Security layers in the SkillSync project.

---

## 🧭 PART 1: TOMCAT & SERVLET FLOW

1.  **TCP Connection**: Client hits port 8080/443. Tomcat's **Connector** (Coyote) accepts the connection.
2.  **Request Parsing**: The **CoyoteAdapter** parses the raw HTTP bytes into `HttpServletRequest` and `HttpServletResponse` objects (Servlet API).
3.  **Container Routing**:
    *   **Engine** → **Host** (e.g., api.skillsync.com) → **Context** (The Auth-Service app).
4.  **The Filter Hand-off**: Before reaching any Servlet, the request must pass through the **FilterChain**.

---

## 🧩 PART 2: FILTERS vs. CONTROLLERS

*   **Filters**: Part of the **Servlet Container**. They wrap the request.
*   **Interceptors**: Part of **Spring MVC**. They wrap the handler.
*   **Logic**: Security happens in **Filters** because we want to block unauthorized access at the "Gate," before Spring MVC even allocates memory for a controller call.

---

## 🔗 PART 3: THE DELEGATING PROXY

**The Problem**: Tomcat doesn't know about Spring Beans.
**The Solution**: **`DelegatingFilterProxy`**.
1.  Tomcat calls this proxy (Standard Servlet Filter).
2.  The proxy looks up the `springSecurityFilterChain` Bean inside the Spring Context.
3.  Control is passed to **`FilterChainProxy`**, which executes the security sub-chain.

---

## 🔐 PART 4: SPRING SECURITY FILTER CHAIN (ORDER OF EXECUTION)

1.  **WebAsyncManagerIntegrationFilter**: Ensures security context is available in async requests.
2.  **SecurityContextPersistenceFilter**: Loads the context (usually empty in your stateless JWT app).
3.  **HeaderWriterFilter**: Adds `X-Frame-Options` and `X-XSS-Protection`.
4.  **CorsFilter**: Handles **Preflight OPTIONS** requests.
5.  **LogoutFilter**: Intercepts `/logout`.
6.  **JwtAuthenticationFilter (YOUR CUSTOM FILTER)**: 
    *   **Logic**: Extracts JWT → Validates → Populates `SecurityContext`.
    *   **Position**: Usually added `.addFilterBefore(UsernamePasswordAuthenticationFilter.class)`.
7.  **AnonymousAuthenticationFilter**: If no user is logged in, it creates an "Anonymous" user so the system doesn't crash on nulls.
8.  **ExceptionTranslationFilter**: (The "Try-Catch" block). It catches security exceptions from later filters.
9.  **FilterSecurityInterceptor**: The "Final Gate." It checks if the current user has the `ROLE_MENTOR` required for the URL.

---

## 🧠 PART 5: THE SECURITY CONTEXT (JVM INTERNALS)

*   **SecurityContextHolder**: A static utility.
*   **ThreadLocal**: The engine. It stores data that is unique to the **current thread**.
*   **Lifecycle**: 
    1.  Request starts → Thread 101 picked from pool.
    2.  Filter sets `User` in `ThreadLocal`.
    3.  Controller reads user.
    4.  **CLEANUP**: Filter clears `ThreadLocal`. 
    *   **WHY?**: If not cleared, and Thread 101 is reused for a different user, the second user might see the first user's data.

---

## 🔑 PART 6: THE AUTHENTICATION FLOW

When you call `authenticationManager.authenticate()`:
1.  **Manager**: `ProviderManager` iterates through providers.
2.  **Provider**: `DaoAuthenticationProvider` is selected.
3.  **Service**: It calls your `UserDetailsServiceImpl.loadUserByUsername()`.
4.  **Verification**: It compares the BCrypt hash with the user's input.
5.  **Success**: Returns a fully populated `Authentication` object (with roles).

---

## 🔐 PART 7: JWT INTERNAL FLOW

1.  **Extraction**: `bearerToken.substring(7)`.
2.  **Validation**: Claims are parsed. Signature is checked using the **HS256 Secret**.
3.  **Context Setting**:
    ```java
    SecurityContextHolder.getContext().setAuthentication(authentication);
    ```
    Now, the request is officially "Authenticated."

---

## 🔁 PART 8: REFRESH TOKEN & REDIS

*   **Rotation**: On every refresh, the **old Token is deleted** from PostgreSQL/Redis and a new one generated.
*   **Why dangerous?**: Refresh tokens are long-lived. If stolen, they allow permanent access.
*   **Blacklist Strategy**: When a user logs out, we take their current **AccessToken ID (jti)** and store it in Redis with a TTL. Any request with that jti is blocked.

---

## 🚨 PART 9: EXCEPTION HANDLING

1.  **AuthenticationException**: Thrown if JWT is invalid (401).
2.  **AccessDeniedException**: Thrown if user has no role (403).
3.  **ETF (ExceptionTranslationFilter)**: It handles these.
    *   *Analogy*: ETF is like a customer service agent standing right before the exit. If security rejects you, ETF catches you and gives you an "Official Denial Notice" (JSON response).

---

## 📊 PART 10: AUTHORIZATION (VOTERS)

*   **AccessDecisionManager**: Makes the final "Permit" or "Deny" call.
*   **AffirmativeBased**: (Default). If even **ONE** Voter says "Yes," access is granted.
*   **Voter**: Each voter checks one thing (e.g., "Does he have the Role?").

---

## 🏁 FINAL SUMMARY
1.  **Tomcat** receives raw bytes and creates a Request.
2.  **DelegatingFilterProxy** hands off to Spring Security.
3.  **Your JWT Filter** validates the token and stores it in **ThreadLocal**.
4.  **Authorization Filters** check roles.
5.  **DispatcherServlet** finds your Controller.
6.  **Controller** executes and returns JSON.
7.  **Filter Chain Cleanup** wipes the thread memory.

# 🗄️ SkillSync: Comprehensive Storage & Persistence Audit

This document traces every byte of data in the SkillSync ecosystem, identifying its storage medium, persistence level, and security policy.

---

## 🖥️ 1. CLIENT-SIDE STORAGE (THE BROWSER)

| Data Type | Medium | Persistence Policy | Security Policy |
| :--- | :--- | :--- | :--- |
| **Access Token** | **LocalStorage** / Redux | Persists across tabs/reloads. | Vulnerable to XSS; short TTL (24h). |
| **Refresh Token**| **Cookies** | Persists until expiry (7d). | `HttpOnly` (No JS access), `Secure`, `SameSite=None`. |
| **Theme Config** | **LocalStorage** | Permanent until cleared. | Low risk; non-sensitive. |
| **User Profile** | **Redux Store (RAM)** | Wiped on tab close/refresh. | Masked (No passwords/PII stored). |

---

## ⚡ 2. INFRASTRUCTURE STORAGE (DATABASE & CACHE)

### A. Database Storage (PostgreSQL)
*   **Auth Service DB**: Stores `AuthUser` (hashed passwords) and the `RefreshToken` table.
*   **User Service DB**: Stores `UserProfile`, BIOS, and Social links.
*   **Policy**: Permanent relational storage. Primary source of truth.

### B. Redis Cache Storage
*   **What's inside?**: Serialized User Profile objects (`user:profile:<id>`).
*   **Policy**: **Cache-Aside Pattern**. 
    *   *TTL*: Data expires naturally.
    *   *Eviction*: Manual eviction via `cacheService.evict()` during profile/role updates.
*   **Why?**: To prevent thousands of "Get Profile" hits from slowing down the PostgreSQL database.

---

## 🧠 3. SERVER-SIDE STORAGE (RAM & IN-MEMORY)

### A. The JVM Heap (RAM)
*   **SecurityContext**: Stores the authenticated user object for the duration of a request.
*   **Medium**: `ThreadLocal`. 
*   **Policy**: **Zero-Persistence**. The data exists only for the millisecond the request is active. It is wiped immediately after.

### B. Tomcat Request Buffers
*   **What?**: Incoming JSON payloads (`LoginRequest`) are momentarily buffered in RAM before being mapped to POJOs.

---

## 🔐 4. TOKEN STORAGE & SECURITY POLICIES

### Access Token (The "Stateless" Guest)
*   **Storage**: Nowhere on the server.
*   **Verification**: Only exists in CPU registers during signature checking.
*   **Policy**: "Verify, don't store."

### Refresh Token (The "Stateful" Anchor)
*   **Storage**: **PostgreSQL `refresh_token` table**.
*   **Logic**: Every refresh token is linked to a `User_ID`.
*   **Revocation Policy**: When a user logs out, the row is **deleted**. This is the only way to "Kill" a session in a stateless architecture.

---

## 📂 5. CONFIGURATION & LOG STORAGE

### A. Spring Cloud Config (Git/Filesystem)
*   **What?**: Centralized `.yml` and `.properties` files.
*   **Medium**: External Git repository or filesystem on the Config Server instance.

### B. Environment Variables (OS Storage)
*   **What?**: `JWT_SECRET`, `DB_PASSWORD`, `SMTP_PASSWORD`.
*   **Policy**: Stored in the OS memory of the container/process. This is the **most secure** place for sensitive keys because they never hit the disk in plain text.

### C. Log Storage (Standard Out / Files)
*   **What?**: Application logs (`log.info()`).
*   **Medium**: In local dev: Terminal (RAM). In production: Filesystem or ELK Stack (Disk).

---

## 📊 6. STORAGE SUMMARY ARCHITECTURE

```text
USER EVENT
    ↓
[ RAM ] (Redux/Context) -> Ephemeral, Zero-Latency
    ↓
[ CACHE ] (Redis) -> Short-term, High-Performance
    ↓
[ DISK ] (PostgreSQL) -> Permanent, Source of Truth
    ↓
[ COOKIES ] (Secure Storage) -> Cross-Request Auth Link
```

---

## 🏁 FINAL PERSISTENCE SUMMARY
SkillSync follows the **"Distributed State"** principle. Sensitive credentials and long-lived sessions are pinned to the **Database** for control. High-frequency data (Profiles) is pinned to **Redis** for speed. Identity is pinned to **Client Memory (JWT)** for scalability. This multi-layered storage strategy ensures that the system is both fast and secure.

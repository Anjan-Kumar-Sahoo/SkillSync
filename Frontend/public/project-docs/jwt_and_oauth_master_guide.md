# 🔐 JWT & OAuth: The Senior Architect's Master Guide

This guide breaks down complex security concepts into digestible, interview-ready modules. 

---

## ✈️ Case 1: JWT Only (Stateless Authentication)

**Simple Analogy**: A JWT is like an **Airport Boarding Pass**. The airline (Server) verifies your ID once and gives you a pass. You don't show your ID at the gate or the lounge; you just show the pass. The gate agent (Filter) only needs to see the airline's official stamp (Signature) to trust you.

### 🔍 What & Why
*   **What is it?**: A JSON Web Token is a compact, URL-safe string containing "claims" (User ID, Roles, Expiry).
*   **Problem it Solves**: It solves the **"Memory Crisis"** of stateful sessions. In old apps, the server had to remember every user. In JWT apps, the server has "amnesia"—it only trusts the token the user brings.

### 🔁 Step-by-Step Flow
1.  **Login**: User sends credentials.
2.  **Signing**: Server verifies credentials and signs a token with a `SECRET_KEY`.
3.  **Delivery**: Server sends the JWT to the browser.
4.  **Storage**: Browser stores it (LocalStore or Cookie).
5.  **API Call**: For every request, browser sends the token in the `Authorization` header.
6.  **Verification**: The server (or Gateway) checks the signature. If it matches the secret, access is granted.

### ❓ Interview Qs
*   **What if the secret key is leaked?**: The entire system is compromised; an attacker can forge "Admin" tokens.
*   **Can we store passwords inside a JWT?**: **NO**. JWTs are only Base64 encoded (not encrypted). Anyone can read the contents; they just can't change them without breaking the signature.

---

## 🔑 Case 2: OAuth Only (Delegated Authorization)

**Simple Analogy**: OAuth is like a **Hotel Valet Key**. You give the valet a special key that only starts the car and opens the door. It does **not** open the trunk or glovebox. You are giving the valet "Delegated Access" to your car without giving them your master key.

### 🔍 What & Why
*   **What is it?**: OAuth is a framework that allows a third-party app (e.g., SkillSync) to access your data on another service (e.g., Google) without seeing your password.
*   **Problem it Solves**: It eliminates the need for users to share passwords across different websites.

### 🔁 Step-by-Step Flow
1.  **Registration**: App registers with Google (gets Client ID/Secret).
2.  **Redirect**: App sends user to Google's "Login with Google" page.
3.  **Approval**: User logs into Google and clicks "Approve."
4.  **Code Exchange**: Google sends an "Authorization Code" back to the App.
5.  **Token Exchange**: The App sends that code + Secret to Google.
6.  **Success**: Google returns an `AccessToken` to the App.

### ❓ Interview Qs
*   **Why have a Code Exchange step? why not just send the token?**: Security. The "Code" is sent via the browser (public). The "Token" is exchanged server-to-server (private). This prevents hackers from sniffing the token in the browser history.

---

## 🏗️ Case 3: JWT + OAuth Combined (The Hybrid Flow)

**Industry Standard**: This is what 90% of modern SaaS apps (including SkillSync's OAuth stubs) use.

### 🔍 Why use both together?
*   OAuth handles the **Identity Check** (using Google/Github).
*   JWT handles the **Session Preservation** within your own app.

### 🔁 Complete Working Flow
1.  **Start**: User clicks "Login with Google."
2.  **Handshake**: Google verifies the user and gives the **Auth-Service** a temporary permission.
3.  **App Verification**: **Auth-Service** checks if this email exists in the SkillSync DB.
4.  **The Switch**: Once the Auth-Service trusts the Google identity, it **issues its own JWT** (AccessToken) to the browser.
5.  **The Result**: For all future calls (to `user-service`), the browser uses the **SkillSync JWT**, not the Google token.

### 🏢 Real Project Implementation (SkillSync)
In your project:
*   **AuthService.loginWithOAuth**: Receives profile from the provider.
*   **Identity Mapping**: The provider's ID is linked to our internal `AuthUser`.
*   **Session Lifecycle**: We issue our own HS256 tokens so we can manage roles (MENTOR/LEARNER) that Google doesn't know about.

---

## ⚖️ COMPARISON: JWT vs OAuth

| Feature | JWT (The Pass) | OAuth (The Key) |
| :--- | :--- | :--- |
| **Type** | Token Standard | Authorization Framework |
| **Purpose** | Identity Verification | Access Delegation |
| **Storage** | Client-side (SPAs) | Server-side (APIs) |
| **Analogy** | Boarding Pass | Valet Key |

---

## ⚠️ Common Mistakes & Best Practices

1.  **Mistake**: Long expiration (TTL). 
    *   *Fix*: Use short Access Tokens (15m) and long Refresh Tokens (7d).
2.  **Mistake**: Storing tokens in `localStorage`. 
    *   *Fix*: LocalStorage is vulnerable to XSS. Use **HttpOnly Cookies** whenever possible.
3.  **Mistake**: No "Secret Rotation".
    *   *Fix*: Change your `jwt.secret` periodically and use a managed store like AWS Secrets Manager.

---

## 🧠 Advanced Interview "Power Answers"

**Q: How do you log a user out in a pure stateless JWT system?**
*   **Answer**: "By design, you can't instantly revoke a stateless JWT. However, we solve this in SkillSync using **Refresh Token Rotation**. When a user logs out, we delete their refresh token from the database. While their current access token might work for another few minutes, they will be unable to generate a new one, effectively ending their session."

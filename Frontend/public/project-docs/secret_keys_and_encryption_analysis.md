# 🔐 SkillSync Deep-Dive: Symmetric vs Asymmetric Secret Keys

This document provides a senior-level architectural analysis of secret key mechanisms within the **SkillSync** ecosystem, contrasting theoretical cryptography with production-grade implementation.

---

## 1. 🧾 Core Concepts (The Foundation)

In modern web security, we use two fundamentally different ways to "scramble" data:

### 🏠 Symmetric Encryption (Secret Key)
*   **Concept**: One single key is used for both locking (encrypting) and unlocking (decrypting).
*   **Analogy**: A traditional house door. Both you and the person entering use the exact same metal key.
*   **Pros**: Extremely fast; low CPU usage.
*   **Cons**: The "Key Distribution" problem. How do you give the key to a stranger without someone else stealing it during the handoff?

### 📮 Asymmetric Encryption (Public/Private Key)
*   **Concept**: Two keys that are mathematically linked.
    *   **Public Key**: Can be given to anyone. Used only for locking.
    *   **Private Key**: Keep it secret. Used only for unlocking.
*   **Analogy**: A physical mailbox. Anyone can drop a letter in (Public Key), but only the owner has the key to open it (Private Key).
*   **Pros**: Solves the trust problem. I don't need to know you to receive secret data from you.
*   **Cons**: Computationally expensive (100x–1000x slower than symmetric).

---

## 2. ⚙️ The Hybrid Workflow in SkillSync

SkillSync (and most modern sites) uses **Hybrid Encryption**. We use Asymmetric to exchange a secret "Session Key," and then switch to Symmetric for the actual heavy lifting.

### 🌐 Step-by-Step HTTPS (TLS) Handshake
1.  **Browser Hello**: The browser connects to the SkillSync Backend and asks for its **Public Key** (embedded in the SSL Certificate).
2.  **Server Cert**: The server sends the cert. The browser checks with a Certificate Authority (CA) to ensure the server is really `api.skillsync.mraks.dev`.
3.  **Secret Handshake**: The browser generates a random "Session Key" (Symmetric). It encrypts this key using the server's **Public Key** and sends it over.
4.  **Key Extraction**: Only the server (using its hidden **Private Key**) can decrypt and see that session key.
5.  **Secure Tunnel**: Now both have the same Symmetric key. They "switch" to fast AES encryption for the rest of the user's session.

---

## 🔑 3. JWT Signing: Symmetric Internal Logic

In the **SkillSync `auth-service`**, the code uses **HMAC-SHA256 (HS256)** for JWTs. This is a **Symmetric** operation.

*   **Generation**: The `auth-service` takes the user data + `JWT_SECRET` -> generates a signature string.
*   **Validation**: When you call the `user-service`, your request goes through the **API Gateway**. The Gateway has the *exact same* `JWT_SECRET`. It re-signs the data and compares it.
*   **Why Symmetric?**: Because both the `auth-service` and `gateway` are internal to our infrastructure. It's faster and requires managing only one secret string.

---

## 🏗️ 4. Where Keys are Managed (KMS & Best Practices)

| Component | Key Type | Where it Lives | Who Manages it |
| :--- | :--- | :--- | :--- |
| **TLS/SSL Certificate** | Asymmetric | Nginx / Load Balancer | DevOps/CA (Let's Encrypt) |
| **JWT Secret** | Symmetric | Env Variables / AWS Secrets Manager | Backend Architect |
| **Password Hashing** | One-Way Hash | Database (Salts) | Application Logic |
| **S3 File Uploads** | Symmetric | AWS KMS | Infrastructure Engineer |

---

## 🚨 5. "What If" Scenarios (Security Analysis)

### Q: What if the private key of the SSL certificate is leaked?
*   **Impact**: High. An attacker can set up a "Man-in-the-Middle" (MITM) server that looks exactly like SkillSync. They can decrypt every request meant for the real server.
*   **Action**: Revoke the certificate immediately via the CA (CRL/OCSP) and issue a new one.

### Q: What if the `JWT_SECRET` is leaked?
*   **Impact**: Critical. This is the "God Key." An attacker can generate an "Admin" token for themselves and bypass all security.
*   **Action**: Rotate the environment variable across all services and force all users to log in again (since old tokens will now be invalid).

### Q: Why don't we use RSA (Asymmetric) for the whole API?
*   **Impact**: Performance. Every API call would take 50ms–100ms longer. On a mobile network, the lag would be unbearable. Symmetric (AES) handles this in microseconds.

---

## 🔄 6. Complete Implementation Summary

1.  **User Accesses Frontend**: Handshake agreements establish a **Symmetric Session Key** for the HTTPS tunnel.
2.  **Login Request**: Credentials travel securely inside that tunnel.
3.  **Token Generation**: Backend picks the `JWT_SECRET` (Symmetric) and signs a JSON object.
4.  **Client Persistence**: Client stores the token. It is an "opaque" bearer string.
5.  **API Call**: Token is sent in the `Authorization` header.
6.  **Gateway Validation**: The Gateway uses the **same symmetric secret** to prove the token hasn't been tampered with.

---

## 🏁 Final Architectural Verdict

In **SkillSync**:
*   **TLS** is **Asymmetric** to build the initial bridge of trust with the browser.
*   **Data Transfer** is **Symmetric** for raw speed during the session.
*   **JWT** is **Symmetric (HS256)** because the issuer and validator are both internal and trusted. 

**Best Practice**: Never hardcode these. Use `System.getenv("JWT_SECRET")` and ensure that in Production, these are injected by a secure vault (like AWS KMS or HashiCorp Vault).

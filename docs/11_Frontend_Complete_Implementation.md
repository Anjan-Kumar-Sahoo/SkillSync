# Frontend Complete Implementation

Date: 2026-04-06
Status: Presentation-ready

## 1. Frontend objective
Deliver a role-based web application that is easy to maintain and aligned with the backend microservices contract.

## 2. Frontend stack
- React 18 + TypeScript
- Vite build tooling
- Redux Toolkit for auth and UI state
- React Query for server state, caching, and mutations
- Axios interceptors for auth propagation and standardized error handling
- Tailwind CSS for utility-first UI composition

## 3. Layered frontend architecture
1. Page layer: route-level screens in src/pages
2. Reusable components: layout, modal, and UI elements in src/components
3. State layer: Redux slices and React Query hooks
4. Service layer: typed API wrappers in src/services
5. HTTP layer: shared Axios client with interceptors

## 4. Routing model
Public routes:
- /login
- /register
- /verify-otp
- /forgot-password
- /reset-password
- /setup-password

Protected routes:
- /learner
- /mentor
- /admin
- /sessions
- /mentors
- /groups
- /profile
- /settings

Role-based redirect:
- /dashboard -> learner/mentor/admin home

## 5. Authentication and password flows
### 5.1 Registration
- User submits email
- OTP verification via verify-otp page
- Setup and complete account

### 5.2 Forgot password
- User enters email on forgot-password page
- App routes to reset-password page
- User enters OTP and new password
- Password reset completes and user returns to login

### 5.3 OAuth password setup
- setup-password page uses one password field
- Includes visibility toggle and live password constraints

## 6. Profile module updates
- Profile edit no longer auto-submits on edit transition
- Avatar persistence now uses profile avatarUrl update contract
- User-service backend supports avatarUrl in update payload

## 7. Session UX update
- Learner-only mentor search action is hidden for mentor users in session empty states

## 8. API integration pattern
- Services encapsulate endpoint calls
- Components avoid direct Axios calls when service wrappers exist
- Mutations invalidate query caches on success

## 9. Demo checklist (frontend)
- Login and role-aware dashboard redirect
- Forgot password OTP reset flow
- Profile edit including avatar URL
- Mentor dashboard and sessions screen behavior

## 10. Known operational note
Current deployment path is manual due to CI minute quota:
- Push code
- Build/push changed backend image(s)
- Pull and restart services on EC2

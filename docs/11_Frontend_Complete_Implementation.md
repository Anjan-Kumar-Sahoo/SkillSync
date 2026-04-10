# Frontend Complete Implementation

Date: 2026-04-10
Status: Presentation-ready (production fixes synced)

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
- /admin/users
- /admin/mentor-approvals
- /admin/skills
- /admin/groups
- /sessions
- /mentors
- /groups
- /profile
- /settings
- /settings/password

Admin route guard:
- `/admin*` routes are now frontend role-guarded to `ROLE_ADMIN`

Role-based redirect:
- /dashboard -> learner/mentor/admin home

## 5. Authentication and password flows
### 5.1 Registration
- User submits email
- OTP verification via verify-otp page
- Setup and complete account

### 5.2 Forgot password
- User enters email on forgot-password page
- OTP is delivered over email
- App routes to reset-password page
- User verifies OTP first on reset-password page
- New password field is unlocked only after OTP verification
- Password reset completes and user returns to login

### 5.3 In-app password change
- Settings page is password-only
- Uses OTP flow end-to-end: send OTP -> verify OTP -> save new password
- Uses endpoints `POST /api/auth/forgot-password`, `POST /api/auth/verify-password-reset-otp`, and `POST /api/auth/reset-password`
- No confirm-password field in UI
- Password checklist is live and submit remains disabled until all rules are met

### 5.4 OAuth password setup
- setup-password page uses one password field
- Includes visibility toggle and live password constraints

## 6. Profile module updates
- Profile edit no longer auto-submits on edit transition
- Avatar persistence now uses profile avatarUrl update contract
- User-service backend supports avatarUrl in update payload
- Profile no longer exposes Notification Preferences or Security & Privacy quick links
- Password updates are centralized under `/settings` and `/settings/password`

## 7. Session UX update
- Learner-only mentor search action is hidden for mentor users in session empty states
- Mentor discovery skill filter now loads from paginated skill catalog API
- Mentor search applies backend filters (`skill`, `rating`, `minPrice`, `maxPrice`, `search`)
- Duplicate booking prevention added on both backend and frontend guardrails
- Learner booking CTA disables with exact message: `Session already booked for this slot` (same mentor + date + time)
- Mentor availability duplicate slot protection returns `Slot already exists`
- Booking duration is fixed by selected mentor slot (no dynamic duration dropdown)
- Mentor dashboard totals exclude `CANCELLED`, and earnings widgets auto-refresh
- Mentor availability page shows info banner: `Sessions may last between 30 minutes to 2 hours depending on discussion.`

## 8. Groups and messaging (admin)
- Admin can open any group chat without joining
- Admin can read and send messages as super viewer
- Group listing now shows admin `Open Chat` actions
- Group detail no longer forces admin join flow

## 9. API integration pattern
- Services encapsulate endpoint calls
- Components avoid direct Axios calls when service wrappers exist
- Mutations invalidate query caches on success
- Password reset contracts are centralized in auth-service OTP endpoints

## 10. Demo checklist (frontend)
- Login and role-aware dashboard redirect
- Forgot password OTP flow with verify-before-reset
- In-app settings password OTP flow with live checklist and no confirm-password
- Profile edit including avatar URL
- Mentor dashboard counts after cancellation/completion changes
- Duplicate slot/booking validation behavior (learner + mentor flows)
- Admin group chat visibility and send-message behavior

## 11. Known operational note
Current deployment path is manual due to CI minute quota:
- Push code
- Build/push changed backend image(s)
- Pull and restart services on EC2

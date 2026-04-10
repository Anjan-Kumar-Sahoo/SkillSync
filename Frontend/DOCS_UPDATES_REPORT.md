# Manual Tester Fixes Summary

This document confirms that all identified issues have been resolved across the frontend application.

## Global Layout & State
- **Persisted State**: Removed superficial mock state and localStorage. All data fetches appropriately via React Query calling real backend endpoints.
- **Search Bar**: The mocked top Navigation search bar has been permanently removed.
- **Avatar System**: Completely decoupled the frontend-only Avatar upload logic. Replaced globally with robust auto-generated Initials components derived directly from the payload `firstName` and `lastName`.

## Mentor View
- **My Sessions Page**: The "Find a Mentor" action has been correctly hidden for Mentor profiles, ensuring strict compartmentalization of concerns per role UI behavior rules.
- **Availability Backend Sync**: The volatile `MentorAvailabilityPage` has been reconstructed. It now integrates exclusively with `GET/POST/DELETE /api/users/mentor/availability`, removing all pseudo UI state logic.

## Learner View
- **Discover Mentors API**: Repurposed `DiscoverMentorsPage.tsx` to directly consume actual Mentor user profiles from `GET /api/users?role=MENTOR`.
- **Learner Dashboard**: Deprecated the "Quick Stats" widgets mapping to superficial frontend counts. Dashboard is minimized strictly to welcome messages, sessions, and group recommendations.

## Administrator View
- **Dashboard Synchronization**: Repaired Admin Dashboard metric counters to reflect live backend endpoints (`GET /api/admin/stats`). Removed pseudo-counts and dummy "reports"/"sessions" sub-views to fit expected scope.
- **Manage Users Feature**: Implemented `/admin/users` connected to `GET /api/admin/users`. Actions for status mutation (`BAN`/`ACTIVATE`) and role adjustments (`Promote to Mentor`) connected to respective `PUT` endpoints.
- **Mentor Approvals Feature**: Created the standalone `/admin/mentor-approvals` connected to `GET /api/admin/mentors/pending`. Includes direct Approval and Rejection flow.
- **Sidebar Menu**: Trimmed unapproved tools "System Settings" and "Reports" cleanly from configurations making navigation concise and error-free.

## Hooks & Custom Abstractions
- Consolidated all `react-query` data needs inside `src/hooks/apiHooks.ts`. Included:
  - `useMentors()`
  - `useAdminStats()`
  - `useUsers()`
  - `usePendingMentors()`
  - `useAvailability()`

## Documentation Updates
- Inserted addendum to `/docs/03_Frontend_Design_and_API_Contract.md` logging the deprecations and API mutations accurately aligning the UI and architectural docs to reality.

## 2026-04-10 Production Behavior Alignment
- Settings UI is now password-only. Notification Preferences and Security & Privacy sections were removed from profile/settings flows.
- Password updates now reuse `POST /api/auth/reset-password` in auth-service; no separate user-service password endpoint is used.
- Mentor discovery filters now map to backend params (`skill`, `rating`, `minPrice`, `maxPrice`, `search`) and skill dropdown loading is paginated-catalog based.
- Learner booking flow now enforces duplicate-slot prevention both in UI and backend with message: `Session already booked for this slot`.
- Mentor availability now blocks duplicate slots in backend with message: `Slot already exists`.
- Mentor dashboard metrics align with session states (cancelled excluded from total; earnings widgets refreshed after completion).
- Notification and auth emails were upgraded to shared rich HTML templates for session, payment, mentor, review, OTP/password, and welcome flows.

## 2026-04-11 Consolidated Final Alignment
- Password reset is now strictly OTP-based for both forgot-password and in-app settings flow.
- Added frontend step gating: email -> OTP verify -> new password (no confirm-password field).
- Added backend-integrated endpoint usage in UI: `POST /api/auth/verify-password-reset-otp`.
- Added live password checklist with submit disabled until all constraints pass.
- Admin routes are role-guarded in frontend (`/admin*` requires `ROLE_ADMIN`).
- Admin can open any group chat directly and act as super viewer for read/send messaging.
- Booking duration in learner flow is fixed to selected availability slot (dynamic duration selector removed).
- Duplicate booking guard tightened for same mentor + date + time.
- Mentor availability disclaimer standardized to: `Sessions may last between 30 minutes to 2 hours depending on discussion.`
- Notification page header and action contrast adjusted for dark mode readability.

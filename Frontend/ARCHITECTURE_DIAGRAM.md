# SkillSync Frontend - Architecture Diagram & Quick Reference

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          USER INTERFACE LAYER                        │
│                          (React Components)                          │
│  ┌──────────────┬──────────────┬──────────────┬──────────────────┐  │
│  │   Learner    │    Mentor    │    Admin     │   Auth & Error   │  │
│  │  Dashboard   │  Dashboard   │  Dashboard   │     Pages        │  │
│  └──────────────┴──────────────┴──────────────┴──────────────────┘  │
│  ┌──────────────┬──────────────┬──────────────┬──────────────────┐  │
│  │   Mentor     │   Sessions   │   Groups     │  Notifications   │  │
│  │ Discovery    │  Management  │ Management   │   Center         │  │
│  └──────────────┴──────────────┴──────────────┴──────────────────┘  │
│  ┌──────────────┬──────────────┬──────────────┬──────────────────┐  │
│  │  User        │  Settings    │   Reviews    │   Payment        │  │
│  │  Profile     │  (Password)  │  & Ratings   │   Checkout       │  │
│  └──────────────┴──────────────┴──────────────┴──────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    APPLICATION LOGIC LAYER                           │
│                   (Redux Slices + React Query)                       │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐  │
│  │   Auth      │ │   UI State  │ │  Sessions   │ │  Mentors    │  │
│  │   State     │ │   State     │ │   State     │ │   State     │  │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐  │
│  │   Groups    │ │ Notifications│ │  Reviews    │ │React Query  │  │
│  │   State     │ │   State     │ │   State     │ │  Caching    │  │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│                      DOMAIN SERVICE LAYER                            │
│                  (Axios-based API Clients)                           │
│  ┌──────────────┬──────────────┬──────────────┬──────────────────┐  │
│  │   Session    │   Mentor     │   Group      │   Review         │  │
│  │   Service    │   Service    │   Service    │   Service        │  │
│  └──────────────┴──────────────┴──────────────┴──────────────────┘  │
│  ┌──────────────┬──────────────┬──────────────────────────────────┐  │
│  │Notification  │   User       │  Axios Interceptors             │  │
│  │   Service    │   Service    │  (Auth, Error Handling)         │  │
│  └──────────────┴──────────────┴──────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│               HTTP CLIENT LAYER (Axios)                              │
│  • Automatic JWT attachment                                          │
│  • 401 error handling with token refresh                             │
│  • Request/response transformation                                   │
│  • Global error handling (toasts)                                    │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│           BACKEND API LAYER (Spring Boot Microservices)              │
│  ┌──────────────┬──────────────┬──────────────┬──────────────────┐  │
│  │   API        │  Session     │   User       │   Payment        │  │
│  │  Gateway     │  Service     │   Service    │   Service        │  │
│  │  (Port 8080) │  (Port 8085) │  (Port 8082) │  (Port 8087)     │  │
│  └──────────────┴──────────────┴──────────────┴──────────────────┘  │
│  ┌──────────────┬──────────────┬──────────────────────────────────┐  │
│  │ Notification │ Auth Service │ Other Services                   │  │
│  │   Service    │ (Port 8081)  │                                  │  │
│  └──────────────┴──────────────┴──────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow Diagram

### Authentication Flow
```
Login Page
    ↓
Enter Credentials
    ↓
POST /api/auth/login
    ↓
Backend Returns: {token, refreshToken, user}
    ↓
Store in Redux + localStorage
    ↓
Axios Sets: Authorization: Bearer <token>
    ↓
Redirect to Dashboard
```

### Session Booking Flow
```
Mentor Discovery Page (GET /api/mentors/search)
    ↓
User Selects Mentor → Mentor Profile Page
    ↓
Click "Book Session" → Opens Booking Modal
    ↓
Select Date/Duration → POST /api/sessions
    ↓
Session Created (Status: REQUESTED)
    ↓
Duplicate slot validation (backend + UI guardrails)
    ↓
Notification to Mentor
    ↓
Redirect to "My Sessions"
```

### Session Acceptance Flow (Mentor)
```
Mentor Dashboard
    ↓
View "Pending Session Requests"
    ↓
Click "Accept" → PUT /api/sessions/{id} {status: ACCEPTED}
    ↓
Session Status Updated
    ↓
Learner Receives Notification
```

### State Management Flow
```
User Interaction (Click Button)
    ↓
Component Dispatch Redux Action
    ↓
Reducer Updates State
    ↓
Component Selector Gets Updated State
    ↓
Component Re-renders
```

### API Data Flow
```
Component Hook (useQuery/useMutation)
    ↓
Call Domain Service Method
    ↓
Service Calls axios.get/post/put/delete
    ↓
Axios Interceptor Adds Auth Token
    ↓
Request Sent to Backend
    ↓
Response Interceptor Handles 401 (refresh if needed)
    ↓
Service Returns Typed Data
    ↓
React Query Caches Result
    ↓
Component Receives Data in Hook Return
```

---

## File Structure Tree

```
SkillSync/Frontend/
├── src/
│   ├── pages/                          # All page components
│   │   ├── admin/
│   │   │   └── AdminDashboardPage.tsx
│   │   ├── auth/
│   │   │   ├── LoginPage.tsx
│   │   │   ├── RegisterPage.tsx
│   │   │   ├── VerifyOtpPage.tsx
│   │   │   ├── SetupPasswordPage.tsx
│   │   │   └── UnauthorizedPage.tsx
│   │   ├── error/
│   │   │   └── ServerErrorPage.tsx
│   │   ├── groups/
│   │   │   ├── GroupsPage.tsx
│   │   │   └── GroupDetailPage.tsx
│   │   ├── learner/
│   │   │   └── LearnerDashboardPage.tsx
│   │   ├── mentor/
│   │   │   └── MentorDashboardPage.tsx
│   │   ├── mentors/
│   │   │   ├── DiscoverMentorsPage.tsx
│   │   │   └── MentorDetailPage.tsx
│   │   ├── notifications/
│   │   │   └── NotificationsPage.tsx
│   │   ├── payment/
│   │   │   └── CheckoutPage.tsx
│   │   ├── profile/
│   │   │   └── UserProfilePage.tsx
│   │   ├── sessions/
│   │   │   └── MySessionsPage.tsx
│   │   ├── settings/
│   │   │   └── SettingsPage.tsx
│   │   └── LandingPage.tsx
│   │
│   ├── components/                     # Reusable components
│   │   ├── layout/
│   │   │   ├── PageLayout.tsx
│   │   │   ├── AuthLayout.tsx
│   │   │   ├── Navbar.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   ├── ProtectedRoute.tsx
│   │   │   └── AuthLoader.tsx
│   │   ├── modals/
│   │   │   └── ReviewModal.tsx
│   │   └── ui/
│   │       └── Toast.tsx
│   │
│   ├── store/                          # Redux state management
│   │   ├── index.ts
│   │   └── slices/
│   │       ├── authSlice.ts
│   │       ├── uiSlice.ts
│   │       ├── sessionsSlice.ts
│   │       ├── mentorsSlice.ts
│   │       ├── groupsSlice.ts
│   │       ├── notificationsSlice.ts
│   │       └── reviewsSlice.ts
│   │
│   ├── services/                       # API service layer
│   │   ├── axios.ts
│   │   ├── sessionService.ts
│   │   ├── mentorService.ts
│   │   ├── groupService.ts
│   │   ├── reviewService.ts
│   │   ├── notificationService.ts
│   │   └── userService.ts
│   │
│   ├── features/                       # Feature-specific logic
│   │   ├── admin/
│   │   ├── auth/
│   │   ├── dashboard/
│   │   ├── groups/
│   │   ├── mentors/
│   │   ├── notifications/
│   │   ├── payment/
│   │   └── sessions/
│   │
│   ├── hooks/                          # Custom React hooks
│   ├── types/                          # TypeScript types
│   ├── utils/                          # Utility functions
│   ├── assets/
│   ├── App.tsx
│   ├── App.css
│   ├── main.tsx
│   └── index.css
│
├── docs/
│   ├── 11_Frontend_Complete_Implementation.md
│   └── ... (other docs)
│
├── IMPLEMENTATION_CHECKLIST.md
├── package.json
├── tsconfig.json
├── vite.config.ts
├── tailwind.config.js
├── eslint.config.js
└── ...
```

---

## Redux State Shape

```typescript
RootState = {
  auth: {
    user: {
      id: number
      name: string
      email: string
      role: 'ROLE_LEARNER' | 'ROLE_MENTOR' | 'ROLE_ADMIN'
      profileImage?: string
      isEmailVerified: boolean
    }
    token: string | null
    refreshToken: string | null
    isLoading: boolean
    error: string | null
  }

  ui: {
    sidebarOpen: boolean
    notificationsOpen: boolean
    theme: 'light' | 'dark'
    loading: boolean
    error: string | null
  }

  sessions: {
    sessions: SessionData[]
    upcomingSessions: SessionData[]
    completedSessions: SessionData[]
    selectedSession: SessionData | null
    isLoading: boolean
    totalElements: number
    currentPage: number
  }

  mentors: {
    mentors: MentorData[]
    selectedMentor: MentorData | null
    myMentorProfile: MentorData | null
    isLoading: boolean
    totalElements: number
    filters: { skill, rating, price }
  }

  groups: {
    groups: GroupData[]
    myGroups: GroupData[]
    selectedGroup: GroupData | null
    isLoading: boolean
    searchQuery: string
  }

  notifications: {
    notifications: NotificationData[]
    unreadCount: number
    isLoading: boolean
  }

  reviews: {
    reviews: ReviewData[]
    mentorReviews: ReviewData[]
    myReviews: ReviewData[]
    isLoading: boolean
  }
}
```

---

## API Endpoint Coverage

### Sessions Service
```
GET    /api/sessions                 → Get user sessions
GET    /api/sessions/:id             → Get session details
GET    /api/sessions/mentor          → Get mentor's sessions (for mentors)
POST   /api/sessions                 → Create session request
PUT    /api/sessions/:id             → Update session status
DELETE /api/sessions/:id             → Cancel session
```

### Mentors Service
```
GET    /api/mentors/search           → List mentors with filters (skill/rating/minPrice/maxPrice/search)
GET    /api/mentors/:id              → Get mentor profile
GET    /api/mentors/me               → Get my mentor profile
POST   /api/mentors                  → Apply as mentor
PUT    /api/mentors/:id              → Update mentor profile
GET    /api/mentors/me/availability  → Get availability
POST   /api/mentors/me/availability  → Add availability slot
DELETE /api/mentors/me/availability/:slotId → Delete availability slot
```

### Groups Service
```
GET    /api/groups                   → List public groups
GET    /api/groups/my                → Get my groups
GET    /api/groups/:id               → Get group details
POST   /api/groups                   → Create group
PUT    /api/groups/:id               → Update group
DELETE /api/groups/:id               → Delete group
POST   /api/groups/:id/join          → Join group
POST   /api/groups/:id/leave         → Leave group
GET    /api/groups/:id/members       → Get group members
GET    /api/groups/:id/discussions   → Get discussions
POST   /api/groups/:id/discussions   → Post discussion
```

### Reviews Service
```
GET    /api/reviews                  → Get reviews
GET    /api/reviews/my               → Get my reviews
GET    /api/mentors/:id/reviews      → Get mentor's reviews
POST   /api/reviews                  → Submit review
PUT    /api/reviews/:id              → Update review
DELETE /api/reviews/:id              → Delete review
```

### Users Service
```
GET    /api/users/me                 → Get my profile
GET    /api/users/:id                → Get user profile
PUT    /api/users/me                 → Update profile
POST   /api/users/me/upload-image    → Upload profile image
POST   /api/auth/reset-password      → Password reset (OTP mode or authenticated current-password mode)
```

### Notifications Service
```
GET    /api/notifications            → Get notifications
GET    /api/notifications/unread     → Get unread notifications
PUT    /api/notifications/:id/read   → Mark as read
PUT    /api/notifications/read-all   → Mark all as read
DELETE /api/notifications/:id        → Delete notification
DELETE /api/notifications            → Clear all
```

---

## Key Dependencies

```json
{
  "react": "^19.2.4",
  "react-dom": "^19.2.4",
  "react-router-dom": "^7.13.2",
  "redux": "via @reduxjs/toolkit",
  "@reduxjs/toolkit": "^2.11.2",
  "react-redux": "^9.2.0",
  "@tanstack/react-query": "^5.95.2",
  "axios": "^1.13.6",
  "react-hook-form": "^7.72.0",
  "jwt-decode": "^4.0.0",
  "tailwindcss": "^4.2.2",
  "@stomp/stompjs": "^7.3.0",
  "sockjs-client": "^1.6.1"
}
```

---

## Development Commands

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint

# Type check
tsc -b
```

---

## Environment Variables

```env
# Development
VITE_API_URL=http://localhost:8080

# Production
VITE_API_URL=https://api.skillsync.mraks.dev
```

---

## Quick Start

1. **Install & Run**:
   ```bash
   npm install
   npm run dev
   ```

2. **Access**: http://localhost:5173

3. **Login**: Use test credentials from backend

4. **Explore**:
   - Learner: Dashboard → Browse Mentors → Book Session
   - Mentor: Dashboard → Manage Sessions → View Earnings
   - Admin: Dashboard → Manage Users → Approve Mentors

---

## Support & Resources

- **API Docs**: `/docs/03_Frontend_Design_and_API_Contract.md`
- **Architecture**: `/docs/11_Frontend_Complete_Implementation.md`
- **Checklist**: `/Frontend/IMPLEMENTATION_CHECKLIST.md`
- **Backend**: Spring Boot Microservices on `api.skillsync.mraks.dev`

---

**Created on**: April 4, 2026  
**Status**: ✅ Production Ready

# 03 Frontend Design and API Contract



---

## Content from: doc3_frontend_design.md

# 📄 DOCUMENT 3: FRONTEND DESIGN (REACT + EXCEPTIONS)

> [!IMPORTANT]
> **Architecture Update (March 2026):** The following backend services have been merged to simplify the architecture:
> - **Mentor Service + Group Service → User Service** (port 8082)
> - **Review Service → Session Service** (port 8085)
>
> **CQRS + Redis Caching (March 2026):** Backend services now use Redis distributed caching with the CQRS pattern.
>
> 🚀 **Frontend Completed (March 2026):** The React 18 frontend is now fully scaffolded and operational.
> - **Tech**: React 18, Vite, TypeScript, Tailwind v4
> - **State Management**: Redux Toolkit for Auth (JWT token persistence), React Query for Data fetching
> - **Pages Built**: Auth (Login, Register), Learner Dashboard, Mentor Discovery, 
>   My Sessions (multi-tab mapping), Checkout (Razorpay SDK Flow), and Mentor Dashboard (availability logic).

## SkillSync — React Frontend Architecture

---

## 3.1 Tech Stack

| Layer | Technology | Rationale |
|---|---|---|
| **Framework** | React 18 + TypeScript | Type safety, ecosystem maturity, concurrent features |
| **State Management** | Redux Toolkit | Predictable global state, DevTools, RTK Query integration |
| **Server State** | React Query (TanStack Query v5) | Caching, background refetch, pagination, optimistic updates |
| **HTTP Client** | Axios | Interceptors for JWT, request/response transformation |
| **Routing** | React Router v6 | Nested routes, lazy loading, route guards |
| **Styling** | Tailwind CSS v3 + Headless UI | Utility-first, accessible components, rapid prototyping |
| **Forms** | React Hook Form + Zod | Performant forms with schema-based validation |
| **Notifications** | React Hot Toast | Lightweight, customizable toast notifications |
| **WebSocket** | SockJS + STOMP.js | Real-time notifications from backend |
| **Payments** | Razorpay Web SDK | Checkout UI for mentor fee & session booking |
| **Testing** | Jest + React Testing Library | Component & integration testing |
| **E2E Testing** | Playwright | Cross-browser end-to-end testing |
| **Build Tool** | Vite | Fast HMR, optimized builds |
| **Linting** | ESLint + Prettier | Code quality and formatting |

---

## 3.2 Folder Structure

```
src/
├── app/
│   ├── store.ts                    # Redux store configuration
│   ├── rootReducer.ts              # Combined reducers
│   └── hooks.ts                    # Typed useAppDispatch, useAppSelector
│
├── assets/
│   ├── images/
│   ├── icons/
│   └── fonts/
│
├── components/
│   ├── atoms/                      # Smallest reusable pieces
│   │   ├── Button/
│   │   │   ├── Button.tsx
│   │   │   ├── Button.test.tsx
│   │   │   └── index.ts
│   │   ├── Input/
│   │   ├── Badge/
│   │   ├── Avatar/
│   │   ├── Spinner/
│   │   ├── StarRating/
│   │   └── StatusBadge/
│   │
│   ├── molecules/                  # Composed atomic components
│   │   ├── SearchBar/
│   │   ├── MentorCard/
│   │   ├── SessionCard/
│   │   ├── ReviewCard/
│   │   ├── NotificationItem/
│   │   ├── GroupCard/
│   │   ├── SkillTag/
│   │   ├── FilterPanel/
│   │   └── PaginationBar/
│   │
│   ├── organisms/                  # Complex UI sections
│   │   ├── Navbar/
│   │   ├── Sidebar/
│   │   ├── Footer/
│   │   ├── MentorGrid/
│   │   ├── SessionList/
│   │   ├── ReviewSection/
│   │   ├── NotificationPanel/
│   │   └── GroupDiscussion/
│   │
│   └── templates/                  # Page layout wrappers
│       ├── DashboardLayout/
│       ├── AuthLayout/
│       └── AdminLayout/
│
├── features/                       # Feature-based modules
│   ├── auth/
│   │   ├── components/
│   │   │   ├── LoginForm.tsx
│   │   │   ├── RegisterForm.tsx
│   │   │   └── ForgotPasswordForm.tsx
│   │   ├── hooks/
│   │   │   └── useAuth.ts
│   │   ├── services/
│   │   │   └── authApi.ts
│   │   ├── slices/
│   │   │   └── authSlice.ts
│   │   ├── types/
│   │   │   └── auth.types.ts
│   │   └── pages/
│   │       ├── LoginPage.tsx
│   │       ├── RegisterPage.tsx
│   │       └── ForgotPasswordPage.tsx
│   │
│   ├── dashboard/
│   │   ├── components/
│   │   │   ├── LearnerDashboard.tsx
│   │   │   ├── MentorDashboard.tsx
│   │   │   ├── StatsCard.tsx
│   │   │   └── UpcomingSessions.tsx
│   │   └── pages/
│   │       └── DashboardPage.tsx
│   │
│   ├── mentor/
│   │   ├── components/
│   │   │   ├── MentorProfileCard.tsx
│   │   │   ├── MentorFilters.tsx
│   │   │   ├── MentorSearchResults.tsx
│   │   │   ├── AvailabilityEditor.tsx
│   │   │   └── MentorApplicationForm.tsx
│   │   ├── hooks/
│   │   │   ├── useMentorSearch.ts
│   │   │   └── useMentorProfile.ts
│   │   ├── services/
│   │   │   └── mentorApi.ts
│   │   ├── slices/
│   │   │   └── mentorSlice.ts
│   │   └── pages/
│   │       ├── MentorDiscoveryPage.tsx
│   │       ├── MentorProfilePage.tsx
│   │       └── MentorApplicationPage.tsx
│   │
│   ├── session/
│   │   ├── components/
│   │   │   ├── BookSessionModal.tsx
│   │   │   ├── SessionCard.tsx
│   │   │   ├── SessionDetailsModal.tsx
│   │   │   └── SessionStatusBadge.tsx
│   │   ├── hooks/
│   │   │   └── useSessions.ts
│   │   ├── services/
│   │   │   └── sessionApi.ts
│   │   ├── slices/
│   │   │   └── sessionSlice.ts
│   │   └── pages/
│   │       └── MySessionsPage.tsx
│   │
│   ├── group/
│   │   ├── components/
│   │   │   ├── CreateGroupForm.tsx
│   │   │   ├── GroupMemberList.tsx
│   │   │   ├── DiscussionThread.tsx
│   │   │   └── PostDiscussionForm.tsx
│   │   ├── services/
│   │   │   └── groupApi.ts
│   │   └── pages/
│   │       ├── GroupListPage.tsx
│   │       ├── GroupDetailPage.tsx
│   │       └── CreateGroupPage.tsx
│   │
│   ├── review/
│   │   ├── components/
│   │   │   ├── ReviewForm.tsx
│   │   │   ├── ReviewList.tsx
│   │   │   └── RatingDistribution.tsx
│   │   └── services/
│   │       └── reviewApi.ts
│   │
│   ├── notification/
│   │   ├── components/
│   │   │   ├── NotificationBell.tsx
│   │   │   ├── NotificationDropdown.tsx
│   │   │   └── NotificationItem.tsx
│   │   ├── hooks/
│   │   │   └── useNotifications.ts
│   │   ├── services/
│   │   │   └── notificationApi.ts
│   │   └── slices/
│   │       └── notificationSlice.ts
│   │
│   ├── payment/
│   │   ├── components/
│   │   │   ├── CheckoutModal.tsx
│   │   │   └── PaymentHistoryTable.tsx
│   │   ├── hooks/
│   │   │   └── useRazorpay.ts
│   │   ├── services/
│   │   │   └── paymentApi.ts
│   │   └── pages/
│   │       └── PaymentHistoryPage.tsx
│   │
│   ├── profile/
│   │   ├── components/
│   │   │   ├── ProfileForm.tsx
│   │   │   ├── AvatarUpload.tsx
│   │   │   └── SkillSelector.tsx
│   │   └── pages/
│   │       └── ProfilePage.tsx
│   │
│   └── admin/
│       ├── components/
│       │   ├── UserTable.tsx
│       │   ├── MentorApprovalList.tsx
│       │   ├── GroupModerationList.tsx
│       │   └── PlatformStats.tsx
│       └── pages/
│           ├── AdminDashboardPage.tsx
│           ├── UserManagementPage.tsx
│           ├── MentorApprovalPage.tsx
│           └── GroupModerationPage.tsx
│
├── lib/
│   ├── api/
│   │   ├── axiosInstance.ts        # Axios config + interceptors
│   │   ├── apiClient.ts           # Generic API call helpers
│   │   └── endpoints.ts           # API endpoint constants
│   ├── websocket/
│   │   ├── stompClient.ts         # WebSocket connection manager
│   │   └── useWebSocket.ts        # WebSocket React hook
│   └── utils/
│       ├── formatters.ts          # Date, currency formatters
│       ├── validators.ts          # Zod schemas
│       └── constants.ts           # App constants
│
├── guards/
│   ├── AuthGuard.tsx              # Redirect if not authenticated
│   ├── RoleGuard.tsx              # Redirect if insufficient role
│   └── GuestGuard.tsx             # Redirect if already authenticated
│
├── errors/
│   ├── ErrorBoundary.tsx          # Global error boundary
│   ├── ApiError.ts                # API error class
│   ├── errorHandler.ts            # Centralized error handler
│   └── ErrorFallback.tsx          # Error UI component
│
├── types/
│   ├── api.types.ts               # API response types
│   ├── user.types.ts
│   ├── mentor.types.ts
│   ├── session.types.ts
│   ├── group.types.ts
│   ├── review.types.ts
│   └── notification.types.ts
│
├── App.tsx                         # Root app with providers
├── Router.tsx                      # Route definitions
├── main.tsx                        # Entry point
└── index.css                       # Tailwind directives
```

---

## 3.3 Routing

### Route Definitions

```tsx
// Router.tsx
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import { AuthGuard } from './guards/AuthGuard';
import { RoleGuard } from './guards/RoleGuard';
import { GuestGuard } from './guards/GuestGuard';
import { DashboardLayout } from './components/templates/DashboardLayout';
import { AuthLayout } from './components/templates/AuthLayout';
import { AdminLayout } from './components/templates/AdminLayout';
import { Spinner } from './components/atoms/Spinner';

// Lazy-loaded pages
const LoginPage = lazy(() => import('./features/auth/pages/LoginPage'));
const RegisterPage = lazy(() => import('./features/auth/pages/RegisterPage'));
const ForgotPasswordPage = lazy(() => import('./features/auth/pages/ForgotPasswordPage'));
const DashboardPage = lazy(() => import('./features/dashboard/pages/DashboardPage'));
const MentorDiscoveryPage = lazy(() => import('./features/mentor/pages/MentorDiscoveryPage'));
const MentorProfilePage = lazy(() => import('./features/mentor/pages/MentorProfilePage'));
const MentorApplicationPage = lazy(() => import('./features/mentor/pages/MentorApplicationPage'));
const MySessionsPage = lazy(() => import('./features/session/pages/MySessionsPage'));
const GroupListPage = lazy(() => import('./features/group/pages/GroupListPage'));
const GroupDetailPage = lazy(() => import('./features/group/pages/GroupDetailPage'));
const CreateGroupPage = lazy(() => import('./features/group/pages/CreateGroupPage'));
const ProfilePage = lazy(() => import('./features/profile/pages/ProfilePage'));
const AdminDashboardPage = lazy(() => import('./features/admin/pages/AdminDashboardPage'));
const UserManagementPage = lazy(() => import('./features/admin/pages/UserManagementPage'));
const MentorApprovalPage = lazy(() => import('./features/admin/pages/MentorApprovalPage'));
const GroupModerationPage = lazy(() => import('./features/admin/pages/GroupModerationPage'));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage'));

const LazyLoad = ({ children }: { children: React.ReactNode }) => (
  <Suspense fallback={<Spinner fullScreen />}>{children}</Suspense>
);

export const router = createBrowserRouter([
  // Public/Guest routes
  {
    element: <GuestGuard><AuthLayout /></GuestGuard>,
    children: [
      { path: '/login', element: <LazyLoad><LoginPage /></LazyLoad> },
      { path: '/register', element: <LazyLoad><RegisterPage /></LazyLoad> },
      { path: '/forgot-password', element: <LazyLoad><ForgotPasswordPage /></LazyLoad> },
    ],
  },

  // Authenticated routes
  {
    element: <AuthGuard><DashboardLayout /></AuthGuard>,
    children: [
      { path: '/', element: <LazyLoad><DashboardPage /></LazyLoad> },
      { path: '/profile', element: <LazyLoad><ProfilePage /></LazyLoad> },
      { path: '/mentors', element: <LazyLoad><MentorDiscoveryPage /></LazyLoad> },
      { path: '/mentors/:id', element: <LazyLoad><MentorProfilePage /></LazyLoad> },
      { path: '/mentors/apply', element: <LazyLoad><MentorApplicationPage /></LazyLoad> },
      { path: '/sessions', element: <LazyLoad><MySessionsPage /></LazyLoad> },
      { path: '/groups', element: <LazyLoad><GroupListPage /></LazyLoad> },
      { path: '/groups/create', element: <LazyLoad><CreateGroupPage /></LazyLoad> },
      { path: '/groups/:id', element: <LazyLoad><GroupDetailPage /></LazyLoad> },
      { path: '/payments/history', element: <LazyLoad><PaymentHistoryPage /></LazyLoad> },
    ],
  },

  // Admin routes
  {
    element: (
      <AuthGuard>
        <RoleGuard allowedRoles={['ROLE_ADMIN']}>
          <AdminLayout />
        </RoleGuard>
      </AuthGuard>
    ),
    children: [
      { path: '/admin', element: <LazyLoad><AdminDashboardPage /></LazyLoad> },
      { path: '/admin/users', element: <LazyLoad><UserManagementPage /></LazyLoad> },
      { path: '/admin/mentors', element: <LazyLoad><MentorApprovalPage /></LazyLoad> },
      { path: '/admin/groups', element: <LazyLoad><GroupModerationPage /></LazyLoad> },
    ],
  },

  // 404
  { path: '*', element: <LazyLoad><NotFoundPage /></LazyLoad> },
]);
```

### Route Guards

```tsx
// guards/AuthGuard.tsx
import { Navigate, useLocation } from 'react-router-dom';
import { useAppSelector } from '../app/hooks';

export const AuthGuard = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated, isLoading } = useAppSelector((state) => state.auth);
  const location = useLocation();

  if (isLoading) return <Spinner fullScreen />;
  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location }} replace />;

  return <>{children}</>;
};

// guards/RoleGuard.tsx
import { Navigate } from 'react-router-dom';
import { useAppSelector } from '../app/hooks';

interface RoleGuardProps {
  allowedRoles: string[];
  children: React.ReactNode;
}

export const RoleGuard = ({ allowedRoles, children }: RoleGuardProps) => {
  const { user } = useAppSelector((state) => state.auth);

  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};
```

---

## 3.4 Screens

### 3.4.1 Authentication Screens

| Screen | Route | Description |
|---|---|---|
| Login | `/login` | Email + password, "Remember me", links to register/forgot-password |
| Register | `/register` | Email, password, first name, last name, confirm password |
| Forgot Password | `/forgot-password` | Email input, sends reset link |

### 3.4.2 Dashboard

| Screen | Route | Role | Content |
|---|---|---|---|
| Learner Dashboard | `/` | LEARNER | Upcoming sessions, recommended mentors, active groups, quick actions |
| Mentor Dashboard | `/` | MENTOR | Pending requests, upcoming sessions, rating summary, earnings overview |
| Admin Dashboard | `/admin` | ADMIN | Platform stats, pending approvals count, recent activity |

### 3.4.3 Mentor Screens

| Screen | Route | Description |
|---|---|---|
| Mentor Discovery | `/mentors` | Search bar, filter sidebar, paginated mentor grid, sort controls |
| Mentor Profile | `/mentors/:id` | Full profile, skills, availability calendar, reviews, "Book Session" CTA |
| Mentor Application | `/mentors/apply` | Multi-step form: bio, experience, rate, skills selection, **Razorpay payment ($9)** |

### 3.4.4 Session Screens

| Screen | Route | Description |
|---|---|---|
| My Sessions | `/sessions` | Tab-based: Upcoming, Pending, Completed, Cancelled + filter by date range |
| Book Session Modal | (overlay) | Date picker, time slot selection, topic, description, confirm, **Razorpay payment ($9)** |
| Session Detail Modal | (overlay) | Full session info, status, actions (accept/reject/cancel/complete/review) |

### 3.4.5 Group Screens

| Screen | Route | Description |
|---|---|---|
| Group List | `/groups` | Grid of group cards, search, filter by skill, "Create Group" button |
| Group Detail | `/groups/:id` | Members list, discussion threads, join/leave button |
| Create Group | `/groups/create` | Name, description, skill tags, max members |

### 3.4.6 Admin Screens

| Screen | Route | Description |
|---|---|---|
| Admin Dashboard | `/admin` | KPI cards, charts (users growth, sessions/day), quick actions |
| User Management | `/admin/users` | Searchable table, activate/deactivate, role filter |
| Mentor Approval | `/admin/mentors` | Pending applications list, approve/reject with reason |
| Group Moderation | `/admin/groups` | Flag/delete groups, remove inappropriate discussions |

### 3.4.7 Common Screens

| Screen | Route | Description |
|---|---|---|
| Profile | `/profile` | Edit profile form, avatar upload, skill management |
| Payment History | `/payments/history` | Table of past payments (mentor fees, session bookings) |
| 404 | `*` | "Page not found" with link to dashboard |

---

## 3.5 Component Architecture

### Atomic Design Hierarchy

```
┌──────────────────────────────────────────────────────┐
│                    TEMPLATES                          │
│  DashboardLayout, AuthLayout, AdminLayout            │
│  ┌────────────────────────────────────────────────┐  │
│  │                  ORGANISMS                      │  │
│  │  Navbar, Sidebar, MentorGrid, SessionList      │  │
│  │  ┌──────────────────────────────────────────┐  │  │
│  │  │              MOLECULES                    │  │  │
│  │  │  MentorCard, SessionCard, FilterPanel     │  │  │
│  │  │  ┌────────────────────────────────────┐  │  │  │
│  │  │  │            ATOMS                    │  │  │  │
│  │  │  │  Button, Input, Badge, Avatar,      │  │  │  │
│  │  │  │  Spinner, StarRating, StatusBadge   │  │  │  │
│  │  │  └────────────────────────────────────┘  │  │  │
│  │  └──────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

### Key Component Examples

```tsx
// components/molecules/MentorCard/MentorCard.tsx
interface MentorCardProps {
  mentor: MentorSummary;
  onBookSession?: (mentorId: string) => void;
}

export const MentorCard: React.FC<MentorCardProps> = ({ mentor, onBookSession }) => {
  return (
    <div className="bg-white rounded-xl shadow-md hover:shadow-lg transition-shadow p-6 border border-gray-100">
      <div className="flex items-start gap-4">
        <Avatar src={mentor.avatarUrl} name={mentor.firstName} size="lg" />
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900">
            {mentor.firstName} {mentor.lastName}
          </h3>
          <p className="text-sm text-gray-500">{mentor.experienceYears}+ years experience</p>
          <div className="flex items-center gap-1 mt-1">
            <StarRating value={mentor.avgRating} readOnly />
            <span className="text-sm text-gray-400">({mentor.totalReviews})</span>
          </div>
        </div>
        <div className="text-right">
          <p className="text-2xl font-bold text-indigo-600">${mentor.hourlyRate}</p>
          <p className="text-xs text-gray-400">/hour</p>
        </div>
      </div>

      <div className="flex flex-wrap gap-2 mt-4">
        {mentor.skills.slice(0, 4).map((skill) => (
          <SkillTag key={skill.id} name={skill.name} />
        ))}
        {mentor.skills.length > 4 && (
          <Badge variant="ghost">+{mentor.skills.length - 4} more</Badge>
        )}
      </div>

      <div className="flex justify-between items-center mt-4 pt-4 border-t border-gray-100">
        <StatusBadge status={mentor.isAvailable ? 'available' : 'unavailable'} />
        <Button
          variant="primary"
          size="sm"
          onClick={() => onBookSession?.(mentor.id)}
          disabled={!mentor.isAvailable}
        >
          Book Session
        </Button>
      </div>
    </div>
  );
};
```

---

## 3.6 State Management Design

### Redux Store Shape

```typescript
// app/store.ts
import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../features/auth/slices/authSlice';
import mentorReducer from '../features/mentor/slices/mentorSlice';
import sessionReducer from '../features/session/slices/sessionSlice';
import notificationReducer from '../features/notification/slices/notificationSlice';
import uiReducer from './uiSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    mentor: mentorReducer,
    session: sessionReducer,
    notification: notificationReducer,
    ui: uiReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: ['auth/setTokens'],
      },
    }),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

### Auth Slice

```typescript
// features/auth/slices/authSlice.ts
import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';

interface AuthState {
  user: UserSummary | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: ApiErrorPayload | null;
}

const initialState: AuthState = {
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  isLoading: true, // Initially true to check stored token
  error: null,
};

export const login = createAsyncThunk(
  'auth/login',
  async (credentials: LoginRequest, { rejectWithValue }) => {
    try {
      const response = await authApi.login(credentials);
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      return response;
    } catch (error) {
      return rejectWithValue(extractApiError(error));
    }
  }
);

export const refreshAccessToken = createAsyncThunk(
  'auth/refresh',
  async (_, { getState, rejectWithValue }) => {
    try {
      const { auth } = getState() as { auth: AuthState };
      const response = await authApi.refreshToken({ refreshToken: auth.refreshToken! });
      localStorage.setItem('accessToken', response.accessToken);
      return response;
    } catch (error) {
      return rejectWithValue(extractApiError(error));
    }
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout: (state) => {
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
      state.isLoading = false;
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
    },
    clearError: (state) => {
      state.error = null;
    },
    initializeAuth: (state) => {
      const accessToken = localStorage.getItem('accessToken');
      const refreshToken = localStorage.getItem('refreshToken');
      if (accessToken && refreshToken) {
        state.accessToken = accessToken;
        state.refreshToken = refreshToken;
        state.isAuthenticated = true;
        // User data fetched separately via /api/users/me
      }
      state.isLoading = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.isLoading = false;
        state.isAuthenticated = true;
        state.user = action.payload.user;
        state.accessToken = action.payload.accessToken;
        state.refreshToken = action.payload.refreshToken;
      })
      .addCase(login.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as ApiErrorPayload;
      });
  },
});

export const { logout, clearError, initializeAuth } = authSlice.actions;
export default authSlice.reducer;
```

### State Management Decision Matrix

| Data Type | Tool | Reason |
|---|---|---|
| Auth state (user, tokens) | Redux Toolkit | Global, persisted, needed everywhere |
| UI state (modals, sidebars) | Redux Toolkit | Cross-component coordination |
| Mentor search results | React Query | Server state, needs cache invalidation |
| Session list | React Query | Server state, needs background refresh |
| Notifications | Redux + WebSocket | Hybrid: REST initial load + WebSocket push |
| Form state | React Hook Form | Local, ephemeral, high-frequency updates |

---

## 3.7 API Integration Layer

### Axios Instance with JWT Interceptor

```typescript
// lib/api/axiosInstance.ts
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { store } from '../../app/store';
import { logout, refreshAccessToken } from '../../features/auth/slices/authSlice';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor — attach JWT
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = store.getState().auth.accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // Attach correlation ID for tracing
    config.headers['X-Correlation-ID'] = crypto.randomUUID();
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor — handle token refresh
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (token) {
      prom.resolve(token);
    } else {
      prom.reject(error);
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // If 401 and not already retrying
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue the request while refresh is in progress
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`;
              resolve(apiClient(originalRequest));
            },
            reject: (err: any) => reject(err),
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const result = await store.dispatch(refreshAccessToken()).unwrap();
        const newToken = result.accessToken;
        processQueue(null, newToken);
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        store.dispatch(logout());
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
```

### API Service Layer Example

```typescript
// features/mentor/services/mentorApi.ts
import { apiClient } from '../../../lib/api/axiosInstance';
import { MentorSearchRequest, MentorProfileResponse, PaginatedResponse } from '../../../types';

export const mentorApi = {
  search: async (params: MentorSearchRequest): Promise<PaginatedResponse<MentorProfileResponse>> => {
    const { data } = await apiClient.get('/api/mentors/search', { params });
    return data;
  },

  getById: async (id: string): Promise<MentorProfileResponse> => {
    const { data } = await apiClient.get(`/api/mentors/${id}`);
    return data;
  },

  apply: async (payload: MentorApplicationRequest): Promise<void> => {
    await apiClient.post('/api/mentors/apply', payload);
  },

  getAvailability: async (): Promise<AvailabilitySlot[]> => {
    const { data } = await apiClient.get('/api/mentors/me/availability');
    return data;
  },

  addAvailability: async (slot: AvailabilitySlotRequest): Promise<AvailabilitySlot> => {
    const { data } = await apiClient.post('/api/mentors/me/availability', slot);
    return data;
  },

  removeAvailability: async (slotId: string): Promise<void> => {
    await apiClient.delete(`/api/mentors/me/availability/${slotId}`);
  },
};
```

### React Query Integration

```typescript
// features/mentor/hooks/useMentorSearch.ts
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { mentorApi } from '../services/mentorApi';

export const useMentorSearch = (filters: MentorSearchRequest) => {
  return useQuery({
    queryKey: ['mentors', 'search', filters],
    queryFn: () => mentorApi.search(filters),
    staleTime: 30 * 1000,    // 30 seconds before refetch
    gcTime: 5 * 60 * 1000,   // 5 minutes garbage collection
    retry: 2,                 // Retry failed requests twice
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 10000),
    keepPreviousData: true,   // Keep old data while fetching new page
  });
};

export const useMentorProfile = (mentorId: string) => {
  return useQuery({
    queryKey: ['mentors', mentorId],
    queryFn: () => mentorApi.getById(mentorId),
    enabled: !!mentorId,
    staleTime: 60 * 1000,
  });
};
```

---

## 3.8 Exception Handling (Frontend)

### 3.8.1 API Error Class

```typescript
// errors/ApiError.ts
export interface ApiErrorPayload {
  timestamp: string;
  status: number;
  error: string;      // Error code
  message: string;     // Human-readable message
  path: string;
  details?: Record<string, string>;
}

export class ApiError extends Error {
  public readonly status: number;
  public readonly errorCode: string;
  public readonly path: string;
  public readonly details?: Record<string, string>;

  constructor(payload: ApiErrorPayload) {
    super(payload.message);
    this.name = 'ApiError';
    this.status = payload.status;
    this.errorCode = payload.error;
    this.path = payload.path;
    this.details = payload.details;
  }

  get isValidationError(): boolean {
    return this.errorCode === 'VALIDATION_ERROR';
  }

  get isAuthError(): boolean {
    return this.status === 401;
  }

  get isNotFound(): boolean {
    return this.status === 404;
  }

  get isConflict(): boolean {
    return this.status === 409;
  }

  get isServerError(): boolean {
    return this.status >= 500;
  }
}
```

### 3.8.2 Centralized Error Handler

```typescript
// errors/errorHandler.ts
import { AxiosError } from 'axios';
import toast from 'react-hot-toast';
import { ApiError, ApiErrorPayload } from './ApiError';

export const extractApiError = (error: unknown): ApiErrorPayload => {
  if (error instanceof AxiosError && error.response?.data) {
    return error.response.data as ApiErrorPayload;
  }

  // Network error (no response from server)
  if (error instanceof AxiosError && !error.response) {
    return {
      timestamp: new Date().toISOString(),
      status: 0,
      error: 'NETWORK_ERROR',
      message: 'Unable to connect to the server. Please check your internet connection.',
      path: error.config?.url || '',
    };
  }

  // Unknown error
  return {
    timestamp: new Date().toISOString(),
    status: 500,
    error: 'UNKNOWN_ERROR',
    message: error instanceof Error ? error.message : 'An unexpected error occurred',
    path: '',
  };
};

export const handleApiError = (error: unknown): void => {
  const apiError = extractApiError(error);

  switch (apiError.error) {
    case 'NETWORK_ERROR':
      toast.error('Network error. Please check your connection.', {
        id: 'network-error',           // Prevent duplicate toasts
        duration: 5000,
      });
      break;

    case 'VALIDATION_ERROR':
      // Validation errors are usually shown inline on forms
      // Only toast if no form context
      if (apiError.details) {
        const firstError = Object.values(apiError.details)[0];
        toast.error(firstError);
      } else {
        toast.error(apiError.message);
      }
      break;

    case 'AUTH_TOKEN_EXPIRED':
    case 'AUTH_TOKEN_INVALID':
      // Handled by axios interceptor (silent refresh)
      break;

    case 'ACCESS_DENIED':
      toast.error('You do not have permission to perform this action.');
      break;

    case 'RESOURCE_NOT_FOUND':
      toast.error(apiError.message);
      break;

    case 'SESSION_CONFLICT':
      toast.error('This time slot is already booked. Please choose another time.');
      break;

    case 'RATE_LIMIT_EXCEEDED':
      toast.error('Too many requests. Please wait a moment and try again.');
      break;

    case 'SERVICE_UNAVAILABLE':
      toast.error('Service temporarily unavailable. Please try again in a few minutes.', {
        duration: 8000,
      });
      break;

    default:
      if (apiError.status >= 500) {
        toast.error('Something went wrong on our end. We\'re working on it!');
      } else {
        toast.error(apiError.message || 'An error occurred.');
      }
  }
};
```

### 3.8.3 Global Error Boundary

```tsx
// errors/ErrorBoundary.tsx
import React, { Component, ErrorInfo, ReactNode } from 'react';
import { ErrorFallback } from './ErrorFallback';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // Log to error reporting service (e.g., Sentry)
    console.error('ErrorBoundary caught:', error, errorInfo);
    this.props.onError?.(error, errorInfo);
    
    // In production, send to error tracking
    if (import.meta.env.PROD) {
      // Sentry.captureException(error, { extra: { componentStack: errorInfo.componentStack } });
    }
  }

  handleReset = (): void => {
    this.setState({ hasError: false, error: null });
  };

  render(): ReactNode {
    if (this.state.hasError) {
      return this.props.fallback || (
        <ErrorFallback
          error={this.state.error!}
          onReset={this.handleReset}
        />
      );
    }
    return this.props.children;
  }
}

// errors/ErrorFallback.tsx
interface ErrorFallbackProps {
  error: Error;
  onReset: () => void;
}

export const ErrorFallback: React.FC<ErrorFallbackProps> = ({ error, onReset }) => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-lg p-8 text-center">
        <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg className="w-8 h-8 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
        </div>
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Something went wrong</h2>
        <p className="text-gray-500 mb-6 text-sm">
          {import.meta.env.DEV ? error.message : 'An unexpected error occurred. Please try again.'}
        </p>
        <div className="flex gap-3 justify-center">
          <button onClick={onReset}
            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition">
            Try Again
          </button>
          <button onClick={() => window.location.href = '/'}
            className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition">
            Go Home
          </button>
        </div>
      </div>
    </div>
  );
};
```

### 3.8.4 Form-Level Error Handling

```tsx
// Example: Login form with inline validation errors
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginForm: React.FC = () => {
  const dispatch = useAppDispatch();
  const { error: serverError } = useAppSelector((state) => state.auth);
  
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      await dispatch(login(data)).unwrap();
    } catch (err) {
      const apiError = err as ApiErrorPayload;
      
      // Map server validation errors to form fields
      if (apiError.error === 'VALIDATION_ERROR' && apiError.details) {
        Object.entries(apiError.details).forEach(([field, message]) => {
          setError(field as keyof LoginFormData, { message });
        });
      } else if (apiError.error === 'ACCOUNT_LOCKED') {
        setError('root', { message: 'Account locked. Please try again in 15 minutes.' });
      } else if (apiError.error === 'AUTHENTICATION_FAILED') {
        setError('root', { message: 'Invalid email or password.' });
      } else {
        handleApiError(err);
      }
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {errors.root && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
          {errors.root.message}
        </div>
      )}
      
      <Input
        label="Email"
        type="email"
        {...register('email')}
        error={errors.email?.message}
      />
      
      <Input
        label="Password"
        type="password"
        {...register('password')}
        error={errors.password?.message}
      />
      
      <Button type="submit" fullWidth loading={isSubmitting}>
        Sign In
      </Button>
    </form>
  );
};
```

### 3.8.5 React Query Error Handling

```typescript
// Global React Query error handler
// App.tsx

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        const apiError = extractApiError(error);
        // Don't retry auth errors or validation errors
        if ([401, 403, 400, 404, 422].includes(apiError.status)) return false;
        // Retry server errors up to 3 times
        return failureCount < 3;
      },
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
      staleTime: 30 * 1000,
    },
    mutations: {
      retry: false,
      onError: (error) => {
        handleApiError(error);
      },
    },
  },
  queryCache: new QueryCache({
    onError: (error, query) => {
      // Only show toast for errors that aren't handled at the component level
      if (query.meta?.showErrorToast !== false) {
        handleApiError(error);
      }
    },
  }),
});
```

### 3.8.6 Retry Mechanism

```typescript
// lib/utils/retry.ts

export const withRetry = async <T>(
  fn: () => Promise<T>,
  options: {
    maxRetries?: number;
    initialDelay?: number;
    maxDelay?: number;
    shouldRetry?: (error: unknown) => boolean;
  } = {}
): Promise<T> => {
  const {
    maxRetries = 3,
    initialDelay = 1000,
    maxDelay = 10000,
    shouldRetry = (error) => {
      const apiError = extractApiError(error);
      return apiError.status >= 500 || apiError.error === 'NETWORK_ERROR';
    },
  } = options;

  let lastError: unknown;
  
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      
      if (attempt === maxRetries || !shouldRetry(error)) {
        throw error;
      }

      const delay = Math.min(initialDelay * 2 ** attempt, maxDelay);
      const jitter = delay * 0.1 * Math.random(); // Add jitter
      await new Promise((resolve) => setTimeout(resolve, delay + jitter));
    }
  }

  throw lastError;
};
```

---

## 3.9 WebSocket Integration

```typescript
// lib/websocket/stompClient.ts
import SockJS from 'sockjs-client';
import { Client, IMessage } from '@stomp/stompjs';
import { store } from '../../app/store';
import { addNotification } from '../../features/notification/slices/notificationSlice';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8088/ws';

let stompClient: Client | null = null;

export const connectWebSocket = (userId: string): void => {
  const token = store.getState().auth.accessToken;

  stompClient = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    
    onConnect: () => {
      console.log('WebSocket connected');
      // Subscribe to user-specific notification channel
      stompClient?.subscribe(`/user/${userId}/notifications`, (message: IMessage) => {
        const notification = JSON.parse(message.body);
        store.dispatch(addNotification(notification));
        
        // Show toast for new notification
        toast(notification.title, {
          icon: '🔔',
          duration: 4000,
        });
      });
    },
    
    onDisconnect: () => {
      console.log('WebSocket disconnected');
    },
    
    onStompError: (frame) => {
      console.error('WebSocket STOMP error:', frame.headers['message']);
    },
  });

  stompClient.activate();
};

export const disconnectWebSocket = (): void => {
  stompClient?.deactivate();
  stompClient = null;
};
```

---

## 3.10 UI/UX Flows

### Flow 1: Session Booking

```
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│  1. Mentor    │     │  2. Mentor    │     │  3. Book      │
│  Discovery    │────▶│  Profile      │────▶│  Session      │
│  Page         │     │  Page         │     │  Modal        │
│               │     │               │     │               │
│ • Search bar  │     │ • Full bio    │     │ • Date picker │
│ • Filters     │     │ • Skills      │     │ • Time slots  │
│ • Mentor grid │     │ • Reviews     │     │ • Topic input │
│ • Pagination  │     │ • Availability│     │ • Confirm btn │
└───────────────┘     │ • Book CTA    │     └───────┬───────┘
                      └───────────────┘             │
                                                    │ On submit
                                                    ▼
                      ┌───────────────┐     ┌───────────────┐
                      │  5. Sessions  │     │  4. Success   │
                      │  List (status │◀────│  Toast +      │
                      │  = REQUESTED) │     │  Redirect     │
                      └───────────────┘     └───────────────┘
```

### Flow 2: Mentor Discovery with Filters

```
User types in search bar
        │
        ▼
┌─────────────────┐     Debounce 300ms     ┌─────────────────┐
│ Update URL      │────────────────────────▶│ Trigger API     │
│ query params    │                         │ call via         │
│ (?skill=Java    │                         │ React Query     │
│  &minRating=4)  │                         └────────┬────────┘
└─────────────────┘                                  │
                                                     ▼
                               ┌───────────────────────────────┐
                               │ Show results:                 │
                               │ • Loading skeleton while fetch│
                               │ • Mentor cards (cached)       │
                               │ • Empty state if no results   │
                               │ • Error state if API fails    │
                               │   → "Retry" button            │
                               └───────────────────────────────┘
```

### Flow 3: Login with Error Handling

```
┌───────────┐     Submit        ┌──────────┐     API Call    ┌──────────┐
│ Login     │──────────────────▶│ Validate │────────────────▶│ Auth     │
│ Form      │                   │ (Zod)    │                 │ Service  │
│           │                   │          │                 │          │
│ email     │                   │ Client   │                 │          │
│ password  │                   │ errors?  │                 │          │
│           │                   └──────┬───┘                 └────┬─────┘
└───────────┘                          │                          │
                                       │                          │
                              ┌────────┴────────┐       ┌────────┴────────┐
                              │ Yes: Show       │       │ 200: Store JWT  │
                              │ inline errors   │       │  → redirect to  │
                              │ (field-level)   │       │  dashboard      │
                              └─────────────────┘       │                 │
                                                        │ 401: Show       │
                                                        │  "Invalid       │
                                                        │  credentials"   │
                                                        │                 │
                                                        │ 403: Show       │
                                                        │  "Account       │
                                                        │  locked"        │
                                                        │                 │
                                                        │ 500/Network:    │
                                                        │  Toast error +  │
                                                        │  retry button   │
                                                        └─────────────────┘
```

---

> [!NOTE]
> The frontend follows a strict **separation of concerns**:
> - **Components** handle rendering only
> - **Hooks** handle data fetching and business logic
> - **Services** handle API communication
> - **Slices** handle global state
> - **Errors** module handles all error presentation


---

## Content from: FRONTEND_API_CONTRACT.md

# SkillSync Frontend API Integration Contract

## 🧩 PART 1 — GLOBAL RULES

* **Base URL (Production)**: `https://skillsync.mraks.dev` (same-origin routes like `/api/*`, `/auth/*` proxied to backend)
* **Base URL (Local Dev)**: `http://localhost:8080` (via Vite proxy / Docker gateway)
* **Format**: All requests and responses use `application/json`
* **Naming Convention**: `camelCase` for all JSON keys
* **Response Structure**:
  * **Success**: The HTTP status code (200-299) indicates success, returning the resource direct or payload inside a standard object wrapper.
  * **Error**: A standard standardized error object is returned on HTTP 4xx/5xx responses.
* **Missing/Null Fields**: Null values are explicitly returned. Optional fields might be omitted if undefined, but required fields are strictly present.

---

## 🧩 PART 2 — AUTHENTICATION DETAILS (CRITICAL)

### 1. JWT Structure
* **Header**: Sent in the `Authorization` header.
* **Format**: `Bearer <accessToken>`
* **Usage**: Required for all endpoints marked as **Auth Required: true**.

### 2. Token Behavior
* **Access Token**: Short-lived, valid for 15-60 minutes depending on environment.
* **Refresh Token**: Long-lived, valid for 7-30 days.

### 3. Refresh Flow
* **Trigger**: When the Frontend receives a `401 Unauthorized` response on any protected route.
* **Endpoint**: `POST /api/auth/refresh`
* **Request Body**:
  ```json
  { "refreshToken": "eyJhbG..." }
  ```
* **Success Response**:
  ```json
  { "accessToken": "eyJhbG...", "refreshToken": "eyJhbG..." }
  ```
* **Action**: Update global Redux state/localStorage with new tokens and transparently retry the original failed request.

### 4. 401 & 403 Handling
* **401 Unauthorized during Refresh**: If `/api/auth/refresh` itself throws 401 (token expired/blacklisted) — **Force Logout**: clear local storage and redirect user to `/login` with session expiry toast.
* **403 Forbidden**: Token is valid, but user lacks necessary role (e.g., Learner trying to access Admin endpoints). Route to a Not Authorized view.

---

## 🧩 PART 3 & PART 4 — API FORMAT & ENDPOINTS BY FEATURE

### 🔹 AUTH APIs

#### Endpoint: `POST /api/auth/register`
**Description**: Registers a new user. Default role is Learner.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "Password123!"
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Registration successful. Please verify email.",
  "email": "john@example.com"
}
```

#### Endpoint: `POST /api/auth/verify-otp`
**Description**: Verifies email address using the 6-digit OTP sent via RabbitMQ/Email.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "email": "john@example.com",
  "otp": "123456"
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Email verified successfully."
}
```

#### Endpoint: `POST /api/auth/resend-otp`
**Description**: Resends verification OTP to email.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "email": "john@example.com"
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "OTP resent successfully."
}
```

#### Endpoint: `POST /api/auth/login`
**Description**: Authenticates user via email/password.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "email": "john@example.com",
  "password": "Password123!"
}
```

**Success Response**: *(200 OK)*
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "user": {
    "id": "usr-123",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "ROLE_LEARNER",
    "emailVerified": true
  }
}
```

#### Endpoint: `POST /api/auth/oauth-login`
**Description**: Authenticates user via Google OAuth profile payload.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "provider": "google",
  "providerId": "10023000...",
  "email": "demo@gmail.com",
  "firstName": "Demo",
  "lastName": "User"
}
```

**Success Response**: *(200 OK)*
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "passwordSetupRequired": false,
  "user": {
    "id": "usr-125",
    "email": "demo@gmail.com",
    "role": "ROLE_LEARNER"
  }
}
```

#### Endpoint: `POST /api/auth/setup-password`
**Description**: Sets a password for purely OAuth-authenticated users.
**Auth Required**: false
**Headers**: None

**Request Body**:
```json
{
  "email": "demo@gmail.com",
  "password": "NewPassword123!"
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Password setup successfully."
}
```

#### Endpoint: `POST /api/auth/logout`
**Description**: Invalidates the user's refresh token on the server (adds to Redis blocklist).
**Auth Required**: true
**Headers**: `Authorization: Bearer <accessToken>`

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Logged out successfully"
}
```

---

### 🔹 USER / PROFILE APIs

#### Endpoint: `GET /api/users/profile`
**Description**: Retrieves the authenticated user's profile.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "usr-123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "role": "ROLE_LEARNER",
  "bio": "Enthusiastic developer",
  "skills": ["React", "Java"]
}
```

#### Endpoint: `PUT /api/users/profile`
**Description**: Updates basic user profile metadata.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "bio": "Enthusiastic developer"
}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "usr-123",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "role": "ROLE_LEARNER",
  "bio": "Enthusiastic developer",
  "skills": ["React", "Java"]
}
```

#### Endpoint: `POST /api/users/skills`
**Description**: Add skills to the user profile.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "skills": ["React", "TypeScript", "Spring Boot"]
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Skills updated successfully",
  "skills": ["React", "TypeScript", "Spring Boot"]
}
```

---

### 🔹 MENTOR APIs

#### Endpoint: `GET /api/mentors`
**Description**: Search mentors with filtering and standard pagination structure.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "content": [
    {
      "id": "mnt-456",
      "userId": "usr-456",
      "firstName": "Jane",
      "lastName": "Smith",
      "headline": "Senior Staff Engineer",
      "hourlyRate": 80.0,
      "rating": 4.8,
      "skills": ["Java", "System Design"]
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 25,
  "totalPages": 3,
  "last": false
}
```

#### Endpoint: `GET /api/mentors/{mentorId}`
**Description**: Get detailed profile of a mentor, including scheduled availability slots.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "mnt-456",
  "userId": "usr-456",
  "firstName": "Jane",
  "headline": "Senior Staff Engineer",
  "bio": "I help people scale applications.",
  "hourlyRate": 80.0,
  "rating": 4.8,
  "reviewCount": 14,
  "skills": ["Java", "System Design"],
  "availableSlots": [
    {
      "id": "slot-1",
      "startTime": "2026-03-28T10:00:00Z",
      "endTime": "2026-03-28T11:00:00Z",
      "isBooked": false
    }
  ]
}
```

#### Endpoint: `POST /api/mentors/apply`
**Description**: Allows a Learner to submit an application to become a Mentor.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "headline": "Senior Staff Engineer",
  "bio": "Extensive experience in microservices...",
  "hourlyRate": 50.0,
  "skills": ["React", "Java"],
  "linkedInUrl": "https://linkedin.com/in/john"
}
```

**Success Response**: *(201 Created)*
```json
{
  "message": "Mentor application submitted successfully. Pending admin approval."
}
```

#### Endpoint: `POST /api/mentors/availability`
**Description**: Mentors set or overwrite their open time slots.
**Auth Required**: true (Role: `ROLE_MENTOR`)
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "slots": [
    {
      "startTime": "2026-03-28T10:00:00Z",
      "endTime": "2026-03-28T11:00:00Z"
    }
  ]
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Availability updated successfully"
}
```

#### Endpoint: `POST /api/mentors/{mentorId}/approve`
**Description**: Admin approves a pending mentor application.
**Auth Required**: true (Role: `ROLE_ADMIN`)
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Mentor approved successfully."
}
```

---

### 🔹 SESSION APIs

#### Endpoint: `POST /api/sessions`
**Description**: Learner requests a new session with a Mentor.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "mentorId": "mnt-456",
  "slotId": "slot-1",
  "topic": "System Design Mock Interview",
  "notes": "Looking specifically for scaling discussion."
}
```

**Success Response**: *(201 Created)*
```json
{
  "id": "ses-789",
  "mentorId": "mnt-456",
  "learnerId": "usr-123",
  "status": "PENDING",
  "startTime": "2026-03-28T10:00:00Z",
  "endTime": "2026-03-28T11:00:00Z",
  "amount": 80.0
}
```

#### Endpoint: `POST /api/sessions/{sessionId}/accept`
**Description**: Mentor accepts a pending session. Triggers realtime Websocket notification to learner.
**Auth Required**: true (Role: `ROLE_MENTOR`)
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "ses-789",
  "status": "ACCEPTED"
}
```

#### Endpoint: `POST /api/sessions/{sessionId}/reject`
**Description**: Mentor rejects a session request.
**Auth Required**: true (Role: `ROLE_MENTOR`)
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "ses-789",
  "status": "REJECTED"
}
```

#### Endpoint: `POST /api/sessions/{sessionId}/complete`
**Description**: Mentor marks an accepted session as completed post-call.
**Auth Required**: true (Role: `ROLE_MENTOR`)
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "id": "ses-789",
  "status": "COMPLETED"
}
```

#### Endpoint: `GET /api/sessions`
**Description**: List user's sessions (automatically detects learner vs mentor context based on auth token).
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "content": [
    {
      "id": "ses-789",
      "mentorName": "Jane Smith",
      "learnerName": "John Doe",
      "status": "ACCEPTED",
      "startTime": "2026-03-28T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 🔹 PAYMENT APIs

#### Endpoint: `POST /api/payments/order`
**Description**: Creates a Razorpay order before initiating payment on the frontend.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "sessionId": "ses-789",
  "amount": 80.0,
  "currency": "USD"
}
```

**Success Response**: *(200 OK)*
```json
{
  "orderId": "order_KjkjJd...",
  "amount": 8000,
  "currency": "USD",
  "keyId": "rzp_test_..."
}
```

#### Endpoint: `POST /api/payments/verify`
**Description**: Verifies the Razorpay payment signature post transaction SDK UI cycle.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "razorpayOrderId": "order_KjkjJd...",
  "razorpayPaymentId": "pay_Kjk...",
  "razorpaySignature": "a3b2..."
}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Payment verified successfully",
  "status": "COMPLETED",
  "sessionId": "ses-789"
}
```

---

### 🔹 REVIEW APIs

#### Endpoint: `POST /api/reviews`
**Description**: Submits a review for a completed session.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "sessionId": "ses-789",
  "mentorId": "mnt-456",
  "rating": 5,
  "comment": "Excellent guidance on distributed systems."
}
```

**Success Response**: *(201 Created)*
```json
{
  "id": "rev-999",
  "message": "Review submitted successfully."
}
```

#### Endpoint: `GET /api/reviews/mentor/{mentorId}`
**Description**: Fetch all paginated reviews for a specific mentor.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "content": [
    {
      "id": "rev-999",
      "learnerName": "John Doe",
      "rating": 5,
      "comment": "Excellent guidance.",
      "createdAt": "2026-03-30T14:00:00Z"
    }
  ],
  "page": 0,
  "size": 5,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 🔹 NOTIFICATION APIs

#### Endpoint: `GET /api/notifications`
**Description**: Get paginated notifications for the user.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "content": [
    {
      "id": "notif-123",
      "type": "SESSION_BOOKED",
      "message": "Your session has been confirmed.",
      "timestamp": "2026-03-27T10:00:00Z",
      "read": false
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### Endpoint: `GET /api/notifications/unread-count`
**Description**: Gets the unread notification count.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "unreadCount": 3
}
```

#### Endpoint: `PUT /api/notifications/{id}/read`
**Description**: Marks a specific notification as read.
**Auth Required**: true
**Headers**: `Authorization: Bearer <token>`

**Request Body**:
```json
{}
```

**Success Response**: *(200 OK)*
```json
{
  "message": "Notification marked as read"
}
```

---

## 🧩 PART 5 — PAGINATION & FILTERING

Define clearly:

### Pagination format:
Select endpoints returning lists strictly use this structure:
```json
{
  "content": [
     // Objects
  ],
  "page": 0,
  "size": 10,
  "totalElements": 100,
  "totalPages": 10,
  "last": false
}
```

### Filters (Mentors Example):
Used as standard GET URL-encoded query parameters:
* `skill`
* `minPrice` / `maxPrice`
* `rating`
* `availability` (boolean string)

Provide sample query params lookup:
```
/api/mentors?skill=Java&page=0&size=10&rating=4
```

---

## 🧩 PART 6 — WEBSOCKET / REAL-TIME

* **WebSocket endpoint**: `ws://localhost:8080/ws`
* **Connection method**: SockJS with STOMP client
* **Destination**: `/topic/notifications/{userId}`

### Event payload example:

```json
{
  "id": "notif-890",
  "type": "SESSION_BOOKED",
  "message": "Your session is confirmed",
  "timestamp": "2026-03-27T14:30:00Z",
  "read": false
}
```

---

## 🧩 PART 7 — ERROR HANDLING (IMPORTANT)

**Global Error Format**:
Any response HTTP status > 299 maps to this strict formal failure payload.

### Error Response:
```json
{
  "status": 400,
  "message": "Validation error",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ]
}
```

**Common Status Constraints**:
* **400**: Bad Request. Maps directly to field-level errors if `errors[]` array present. 
* **401**: Unauthorized. Triggers Token Refresh layer logic or force logout.
* **403**: Forbidden. Show 'Insufficient permissions' UI.
* **404**: Not found. Standard 404 message.
* **500**: Internal Server Error. 

---

## 🧩 PART 8 — RATE LIMITING & EDGE CASES

* **Payment APIs Restrictions**: Rate limited logic restricts redundant Razorpay order generation logic to avoid split-second double charges on the user. Frontends must immediately lock inputs/buttons (disable state) until `/api/payments/verify` finishes its pass gracefully.
* **Auth Rate Limits**: Gateway applies aggressive rate-limits globally limiting brute force requests to `/api/auth/*` routes. Yields standard 429 when max bucket is triggered.
* **Expected Retry Behavior**: Use exponential back-off up to 3 times ONLY on `503 Service Unavailable` or `429 Too Many Requests`.

---

## 🧩 PART 9 — SAMPLE END-TO-END FLOW

Here is one fully verified example of a cross-system workflow logic combining tokens, calls, and realtime components:

1. **Login User**:
   * Request: `POST /api/auth/login` | payload `{"email":"learner@test.com", "password":"pass"}`
   * Consequence: Get `accessToken` and persist in Redux.

2. **Search Mentor**:
   * Request: `GET /api/mentors?skill=Java` 
   * Consequence: Returns list. You select Mentor `mnt-001` and an associated slot.

3. **Create Session**:
   * Request: `POST /api/sessions` | payload `{"mentorId":"mnt-001", "slotId":"slot-abc", "topic":"Help"}`
   * Consequence: Returns session `ses-999` marked as `PENDING`.

4. **Websocket Fires (Mentor Perspective)**:
   * Mentor receives STOMP message payload on `/topic/notifications/usr-mentorId` that a session is requested.
   * Mentor accepts session: `POST /api/sessions/ses-999/accept`

5. **Create Payment**:
   * Request: Learner initiates booking workflow via `POST /api/payments/order` | payload `{"sessionId":"ses-999", "amount": 80.0, "currency":"USD"}`
   * Consequence: Acquires `orderId`, popping open Razorpay frontend UI Modal lock.

6. **Verify Payment**:
   * Request: User submits valid card. SDK returns success properties.
   * Consequence: Invoke `POST /api/payments/verify` | payload `{"razorpayOrderId": "...", "razorpayPaymentId": "...", "razorpaySignature": "..."}`
   * Output: Complete execution cycle marked safely.

7. **Receive Realtime Confirmed Notification**:
   * Learner is hit with async WebSocket payload notifying `SESSION_BOOKED: Your session is confirmed.`

---

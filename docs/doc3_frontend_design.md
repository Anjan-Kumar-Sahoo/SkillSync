# 📄 DOCUMENT 3: FRONTEND DESIGN (REACT + EXCEPTIONS)

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
| Mentor Application | `/mentors/apply` | Multi-step form: bio, experience, rate, skills selection |

### 3.4.4 Session Screens

| Screen | Route | Description |
|---|---|---|
| My Sessions | `/sessions` | Tab-based: Upcoming, Pending, Completed, Cancelled + filter by date range |
| Book Session Modal | (overlay) | Date picker, time slot selection, topic, description, confirm |
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

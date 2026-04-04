# SkillSync Frontend Architecture - Complete Implementation Guide

**Date**: April 4, 2026  
**Version**: 1.0  
**Status**: Production-Ready

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Project Structure](#project-structure)
3. [State Management](#state-management)
4. [Service Layer](#service-layer)
5. [Pages & Routing](#pages--routing)
6. [Component System](#component-system)
7. [Authentication Flow](#authentication-flow)
8. [API Integration](#api-integration)
9. [Styling & Theming](#styling--theming)
10. [Best Practices](#best-practices)
11. [Development Guidelines](#development-guidelines)
12. [Deployment & DevOps](#deployment--devops)

---

## Architecture Overview

SkillSync Frontend follows a **modular, feature-based architecture** with strict separation of concerns:

```
Presentation Layer (Pages/Components)
         ↓
Application Logic Layer (Redux Slices + Hooks)
         ↓
Domain Service Layer (API Services)
         ↓
HTTP Client Layer (Axios with Interceptors)
         ↓
Backend API (Microservices)
```

### Key Principles

- **Separation of Concerns**: UI, business logic, and data access are strictly separated
- **Reusability**: Components, services, and hooks are designed for maximum reusability
- **Type Safety**: Full TypeScript with strict mode enabled
- **Scalability**: Modular structure allows easy addition of new features
- **Maintainability**: Clear naming conventions and consistent patterns throughout

---

## Project Structure

```
src/
├── assets/              # Images, icons, fonts
├── components/
│   ├── layout/         # Page layouts and navigation
│   │   ├── Navbar.tsx
│   │   ├── Sidebar.tsx
│   │   ├── PageLayout.tsx
│   │   ├── AuthLayout.tsx
│   │   ├── ProtectedRoute.tsx
│   │   └── AuthLoader.tsx
│   ├── modals/         # Reusable modal components
│   │   └── ReviewModal.tsx
│   └── ui/             # Reusable UI components
│       └── Toast.tsx
├── features/           # Feature-specific logic (hooks, utilities)
│   ├── admin/
│   ├── auth/
│   ├── dashboard/
│   ├── groups/
│   ├── mentors/
│   ├── notifications/
│   ├── payment/
│   └── sessions/
├── hooks/              # Custom React hooks
├── pages/              # Page components (one per route)
│   ├── admin/
│   │   └── AdminDashboardPage.tsx
│   ├── auth/
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   ├── VerifyOtpPage.tsx
│   │   └── SetupPasswordPage.tsx
│   ├── error/
│   │   └── ServerErrorPage.tsx
│   ├── groups/
│   │   ├── GroupsPage.tsx
│   │   └── GroupDetailPage.tsx
│   ├── learner/
│   │   └── LearnerDashboardPage.tsx
│   ├── mentor/
│   │   └── MentorDashboardPage.tsx
│   ├── mentors/
│   │   ├── DiscoverMentorsPage.tsx
│   │   └── MentorDetailPage.tsx
│   ├── notifications/
│   │   └── NotificationsPage.tsx
│   ├── payment/
│   │   └── CheckoutPage.tsx
│   ├── profile/
│   │   └── UserProfilePage.tsx
│   ├── sessions/
│   │   └── MySessionsPage.tsx
│   ├── settings/
│   │   └── SettingsPage.tsx
│   ├── LandingPage.tsx
│   └── App.tsx
├── services/           # API service layer (by domain)
│   ├── axios.ts        # Axios instance with interceptors
│   ├── sessionService.ts
│   ├── mentorService.ts
│   ├── groupService.ts
│   ├── reviewService.ts
│   ├── notificationService.ts
│   └── userService.ts
├── store/              # Redux store configuration
│   ├── index.ts        # Store setup
│   └── slices/         # Redux slices (by feature)
│       ├── authSlice.ts
│       ├── uiSlice.ts
│       ├── sessionsSlice.ts
│       ├── mentorsSlice.ts
│       ├── groupsSlice.ts
│       ├── notificationsSlice.ts
│       └── reviewsSlice.ts
├── types/              # TypeScript types and interfaces
├── utils/              # Utility functions
├── main.tsx            # Entry point
├── index.css           # Global styles
└── App.css             # App-specific styles
```

---

## State Management

We use **Redux Toolkit** for global state management and **React Query** for server state.

### Redux Store Structure

```typescript
RootState {
  auth: {
    user: UserData | null
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
  sessions: { /* SessionData[] and metadata */ }
  mentors: { /* MentorData[] and filters */ }
  groups: { /* GroupData[] */ }
  notifications: { /* NotificationData[] and unread count */ }
  reviews: { /* ReviewData[] */ }
}
```

### Redux Slices Created

1. **authSlice**: User authentication state (existing)
2. **uiSlice**: Global UI state (theme, sidebar, modals)
3. **sessionsSlice**: User sessions and session management
4. **mentorsSlice**: Mentor profiles and discovery filters
5. **groupsSlice**: Learning groups and memberships
6. **notificationsSlice**: System notifications
7. **reviewsSlice**: Mentor reviews and ratings

### Usage Example

```typescript
import { useAppSelector, useAppDispatch } from '../store';
import { setTheme } from '../store/slices/uiSlice';

const Component = () => {
  const theme = useAppSelector(state => state.ui.theme);
  const dispatch = useAppDispatch();
  
  const toggleTheme = () => {
    dispatch(setTheme(theme === 'light' ? 'dark' : 'light'));
  };
};
```

---

## Service Layer

Each domain has a dedicated service class that handles API communication:

### Service Pattern

```typescript
class DomainService {
  async getItems(page: number): Promise<PaginatedResponse<Item>>
  async getItemById(id: number): Promise<Item>
  async createItem(payload): Promise<Item>
  async updateItem(id: number, payload): Promise<Item>
  async deleteItem(id: number): Promise<void>
}

export default new DomainService();
```

### Domain Services

1. **sessionService.ts**: Session booking, status updates
2. **mentorService.ts**: Mentor profiles, discovery, availability
3. **groupService.ts**: Groups, discussions, memberships
4. **reviewService.ts**: Reviews and ratings
5. **notificationService.ts**: Notifications and preferences
6. **userService.ts**: User profiles, settings, preferences

### Service Usage

```typescript
import sessionService from '../services/sessionService';

const { data: sessions } = useQuery({
  queryKey: ['sessions'],
  queryFn: () => sessionService.getSessions('ACCEPTED', 0, 10),
});
```

---

## Pages & Routing

### Complete Route Map

| Path | Component | Role | Description |
|------|-----------|------|-------------|
| `/` | LandingPage | Public | Marketing/intro page |
| `/login` | LoginPage | Public | User login |
| `/register` | RegisterPage | Public | User registration |
| `/verify-otp` | VerifyOtpPage | Public | OTP verification |
| `/setup-password` | SetupPasswordPage | Public | Initial password setup |
| `/dashboard` | DashboardRedirect | Auth | Role-based redirect |
| `/learner` | LearnerDashboardPage | Learner | Learner dashboard |
| `/mentor` | MentorDashboardPage | Mentor | Mentor dashboard |
| `/admin` | AdminDashboardPage | Admin | Admin panel |
| `/mentors` | DiscoverMentorsPage | Auth | Browse mentors |
| `/mentors/:id` | MentorDetailPage | Auth | Mentor details & booking |
| `/sessions` | MySessionsPage | Auth | User's sessions |
| `/groups` | GroupsPage | Auth | Explore & manage groups |
| `/groups/:id` | GroupDetailPage | Auth | Group discussions |
| `/notifications` | NotificationsPage | Auth | Notification center |
| `/profile` | UserProfilePage | Auth | User profile management |
| `/settings` | SettingsPage | Auth | Settings & preferences |
| `/checkout` | CheckoutPage | Auth | Payment checkout |
| `/unauthorized` | UnauthorizedPage | Public | 403 error page |
| `/500` | ServerErrorPage | Public | 500 error page |

### Route Guards

All `/dashboard/*` routes are protected by `ProtectedRoute` component which:
- Checks if user is authenticated  
- Loads user from localStorage or refresh token
- Redirects to login if not authenticated
- Handles 401 responses with token refresh

---

## Component System

### Layout Components

**PageLayout**: Wraps all protected pages
```typescript
<PageLayout>
  <YourContent />
</PageLayout>
```

Features:
- Navbar with notifications
- Sidebar navigation
- Responsive design
- Mobile menu

**AuthLayout**: Wraps auth pages
- Centered form layout
- No navbar/sidebar
- Responsive

### UI Components

**Toast**: Notification system
```typescript
const { showToast } = useToast();
showToast({ 
  message: 'Success!', 
  type: 'success' // 'success' | 'error' | 'info' | 'warning'
});
```

**ReviewModal**: Modal for submitting reviews
- Star rating
- Comment input
- Validation

---

## Authentication Flow

### Login Flow

```
1. User visits /login
2. Enters credentials
3. API returns: { token, refreshToken, user }
4. Stored in Redux + localStorage
5. Axios interceptor attaches token to all requests
6. User redirected to dashboard
```

### Token Refresh

When access token expires:
```
1. API returns 401 Unauthorized
2. Axios interceptor catches it
3. Calls refresh-token endpoint with refreshToken
4. Gets new access token
5. Retries original request
6. If refresh fails, redirects to login
```

### Protected Routes

```typescript
<ProtectedRoute /> component:
- Checks Redux auth state
- Verifies token isn't expired
- Loads user if needed
- Renders protected pages or redirects
```

---

## API Integration

### Axios Setup

```typescript
// src/services/axios.ts
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 10000,
  withCredentials: true,
});

// Request interceptor: attach token
api.interceptors.request.use(config => {
  const token = store.getState().auth.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: handle 401 + refresh
api.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      // Handle token refresh
    }
    return Promise.reject(error);
  }
);
```

### Error Handling

```typescript
try {
  const response = await api.get('/api/endpoint');
  // Handle success
} catch (error: any) {
  const message = error.response?.data?.message || 'An error occurred';
  showToast({ message, type: 'error' });
}
```

### Request Types

```typescript
// API endpoint response wrapper
interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

// Example usage
const { data } = await api.get('/api/mentors?page=0&size=10');
const mentors: PaginatedResponse<MentorData> = data;
```

---

## Styling & Theming

### Technology: Tailwind CSS v4

**Key Files**:
- `src/index.css`: Global styles + CSS variables
- `src/App.css`: App-specific styles
- Page components: Inline Tailwind classes

### Color Palette

Consistent throughout the app:
- Primary: `blue-600` (actions)
- Success: `green-600` (confirmations)
- Warning: `orange-600` (alerts)
- Error: `red-600` (errors)
- Neutral: `gray-*` (backgrounds, text)

### Responsive Breakpoints

```
sm: 640px
md: 768px
lg: 1024px
xl: 1280px
```

### Usage Example

```tsx
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  <Card className="bg-white rounded-lg p-6 shadow-sm border border-gray-200" />
</div>
```

---

## Best Practices

### 1. Component Design

✅ DO:
- Keep components small and focused
- Use TypeScript interfaces for props
- Lift state up when needed
- Use custom hooks for logic

❌ DON'T:
- Pass prop drilling multiple levels
- Mix business logic with UI
- Create giant components

### 2. Data Fetching

✅ DO:
- Use React Query for server state
- Use Redux for UI state
- Create reusable service methods
- Handle loading/error states

❌ DON'T:
- Fetch in useEffect without dependencies array
- Store API data in Redux
- Ignore error cases

### 3. Error Handling

✅ DO:
- Use toast notifications for user feedback
- Log errors for debugging
- Provide fallback UI
- Handle 401/403 properly

❌ DON'T:
- Silent failures
- Generic error messages
- Let errors crash the app

### 4. Performance

✅ DO:
- Code split pages with React.lazy
- Memoize expensive components
- Optimize re-renders
- Use virtualization for large lists

❌ DON'T:
- Fetch all data at once
- Create functions in render
- Render all thousands of items

### 5. Security

✅ DO:
- Store tokens securely
- Use HTTPOnly cookies if possible
- Validate input on client
- Use HTTPS in production
- Check authorization for each route

❌ DON'T:
- Store sensitive data in localStorage unencrypted
- Trust client-side authorization alone
- Expose API keys
- Skip backend validation

---

## Development Guidelines

### Adding a New Page

1. **Create page component** in `src/pages/feature/FeaturePage.tsx`
   ```typescript
   const FeaturePage = () => {
     return (
       <PageLayout>
         <div>Your content</div>
       </PageLayout>
     );
   };
   export default FeaturePage;
   ```

2. **Create service** in `src/services/featureService.ts` if needed
   ```typescript
   class FeatureService {
     async getFeatures() { /* ... */ }
   }
   export default new FeatureService();
   ```

3. **Create Redux slice** in `src/store/slices/featureSlice.ts` if needed
   ```typescript
   const featureSlice = createSlice({
     name: 'feature',
     initialState,
     reducers: { /* ... */ },
   });
   ```

4. **Add route** to `src/App.tsx`
   ```typescript
   <Route path="/feature" element={<FeaturePage />} />
   ```

5. **Add navigation link** in Navbar/Sidebar

### Adding a New Service Endpoint

1. Create method in service class:
   ```typescript
   async getItem(id: number): Promise<ItemData> {
     const res = await api.get(`/api/items/${id}`);
     return res.data;
   }
   ```

2. Use in component with React Query:
   ```typescript
   const { data, isLoading, error } = useQuery({
     queryKey: ['item', id],
     queryFn: () => itemService.getItem(id),
   });
   ```

### Environment Configuration

**`.env.development`**:
```
VITE_API_URL=http://localhost:8080
```

**`.env.production`**:
```
VITE_API_URL=https://api.skillsync.mraks.dev
```

---

## Deployment & DevOps

### Build Process

```bash
npm run build
# Outputs to dist/
# - HTML, CSS, JS all optimized
# - Code splitting applied
# - Source maps generated
```

### Production Checklist

- [ ] Environment variables set correctly
- [ ] API URL points to production gateway
- [ ] Error monitoring configured (Sentry)
- [ ] Analytics configured (Google Analytics)
- [ ] CORS configured properly
- [ ] Security headers set
- [ ] CSP policy defined
- [ ] Cache-busting headers set
- [ ] 404 fallback configured
- [ ] SSL certificate valid

### Docker Deployment

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build
RUN npm install -g serve
EXPOSE 3000
CMD ["serve", "-s", "dist", "-l", "3000"]
```

### Vercel Deployment

```json
{
  "buildCommand": "npm run build",
  "outputDirectory": "dist",
  "env": {
    "VITE_API_URL": "@vite_api_url_production"
  }
}
```

---

## Testing Strategy

### Unit Tests (Jest)

```typescript
describe('UserService', () => {
  it('should fetch user profile', async () => {
    const user = await userService.getMyProfile();
    expect(user).toBeDefined();
  });
});
```

### Component Tests (React Testing Library)

```typescript
describe('LoginPage', () => {
  it('should display login form', () => {
    render(<LoginPage />);
    expect(screen.getByText('Login')).toBeInTheDocument();
  });
});
```

### E2E Tests (Cypress)

```typescript
describe('Login Flow', () => {
  it('should login successfully', () => {
    cy.visit('/login');
    cy.get('input[name="email"]').type('user@example.com');
    cy.get('button[type="submit"]').click();
    cy.url().should('include', '/dashboard');
  });
});
```

---

## Troubleshooting

### Common Issues & Solutions

**Issue**: CORS errors in production
- **Solution**: Update API URL to match backend domain, configure backend CORS

**Issue**: 401 errors after token refresh
- **Solution**: Check refresh token endpointlogic, verify token expiry times

**Issue**: State not persisting on refresh
- **Solution**: Setup localStorage persistence for auth tokens

**Issue**: API calls not being intercepted
- **Solution**: Ensure axios instance is imported correctly, check interceptor setup

---

## Next Steps

1. ✅ Create all Redux slices (DONE)
2. ✅ Create domain services (DONE)
3. ✅ Create all pages (DONE)
4. ✅ Update routing (DONE)
5. ⏳ Add unit tests
6. ⏳ Add E2E tests
7. ⏳ Implement WebSocket for real-time notifications
8. ⏳ Setup error monitoring (Sentry)
9. ⏳ Optimize performance (profiling, bundle size)
10. ⏳ Add analytics

---

## Conclusion

This frontend architecture is production-ready, scalable, and maintainable. Follow these guidelines to keep the codebase clean and organized as you add new features.

For questions or improvements, refer to the backend API documentation and system architecture docs.

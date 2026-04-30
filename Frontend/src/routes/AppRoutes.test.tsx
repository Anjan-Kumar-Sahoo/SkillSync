import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { MemoryRouter } from 'react-router-dom';
import authReducer from '../store/slices/authSlice';
import { ThemeProvider } from '../context/ThemeContext';
import AppRoutes from './AppRoutes';

type AuthRole = 'ROLE_ADMIN' | 'ROLE_MENTOR' | 'ROLE_LEARNER' | null;

type TestAuthState = {
  user: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    role: Exclude<AuthRole, null>;
  } | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  role: AuthRole;
};

const unauthenticatedAuthState: TestAuthState = {
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  role: null,
};

const createAuthenticatedState = (role: Exclude<AuthRole, null>): TestAuthState => ({
  user: {
    id: 1,
    firstName: 'Test',
    lastName: 'User',
    email: 'test@skillsync.dev',
    role,
  },
  accessToken: 'token',
  refreshToken: 'refresh',
  isAuthenticated: true,
  role,
});

const renderAppRoutes = ({ route, authState = unauthenticatedAuthState }: { route: string; authState?: TestAuthState }) => {
  const store = configureStore({
    reducer: {
      auth: authReducer,
    } as any,
    preloadedState: {
      auth: authState,
    } as any,
  });

  return render(
    <Provider store={store}>
      <ThemeProvider>
        <MemoryRouter initialEntries={[route]}>
          <AppRoutes />
        </MemoryRouter>
      </ThemeProvider>
    </Provider>,
  );
};

function mockPage(label: string) {
  return function MockedPage() {
    return <div>{label}</div>;
  };
}

jest.mock('../pages/LandingPage.tsx', () => ({ __esModule: true, default: mockPage('LandingPage') }));
jest.mock('../pages/PptLandingPage.tsx', () => ({ __esModule: true, default: mockPage('PptLandingPage') }));
jest.mock('../pages/docs/LearnDocsPage', () => ({ __esModule: true, default: mockPage('LearnDocsPage') }));

jest.mock('../pages/auth/LoginPage', () => ({ __esModule: true, default: mockPage('LoginPage') }));
jest.mock('../pages/auth/ForgotPasswordPage', () => ({ __esModule: true, default: mockPage('ForgotPasswordPage') }));
jest.mock('../pages/auth/RegisterPage', () => ({ __esModule: true, default: mockPage('RegisterPage') }));
jest.mock('../pages/auth/VerifyOtpPage', () => ({ __esModule: true, default: mockPage('VerifyOtpPage') }));
jest.mock('../pages/auth/ResetPasswordPage', () => ({ __esModule: true, default: mockPage('ResetPasswordPage') }));
jest.mock('../pages/auth/SetupPasswordPage', () => ({ __esModule: true, default: mockPage('SetupPasswordPage') }));
jest.mock('../pages/auth/UnauthorizedPage', () => ({ __esModule: true, default: mockPage('UnauthorizedPage') }));
jest.mock('../pages/error/ServerErrorPage', () => ({ __esModule: true, default: mockPage('ServerErrorPage') }));

jest.mock('../pages/learner/LearnerDashboardPage', () => ({ __esModule: true, default: mockPage('LearnerDashboardPage') }));
jest.mock('../pages/mentor/MentorDashboardPage', () => ({ __esModule: true, default: mockPage('MentorDashboardPage') }));
jest.mock('../pages/mentor/MentorAvailabilityPage', () => ({ __esModule: true, default: mockPage('MentorAvailabilityPage') }));
jest.mock('../pages/mentor/EarningsPage', () => ({ __esModule: true, default: mockPage('EarningsPage') }));

jest.mock('../pages/admin/AdminDashboardPage', () => ({ __esModule: true, default: mockPage('AdminDashboardPage') }));
jest.mock('../pages/admin/UsersCenterPage', () => ({ __esModule: true, default: mockPage('UsersCenterPage') }));
jest.mock('../pages/admin/MentorApprovalsPage', () => ({ __esModule: true, default: mockPage('MentorApprovalsPage') }));
jest.mock('../pages/admin/AdminSkillsPage', () => ({ __esModule: true, default: mockPage('AdminSkillsPage') }));
jest.mock('../pages/admin/AdminGroupsPage', () => ({ __esModule: true, default: mockPage('AdminGroupsPage') }));

jest.mock('../pages/mentors/DiscoverMentorsPage', () => ({ __esModule: true, default: mockPage('DiscoverMentorsPage') }));
jest.mock('../pages/mentors/MentorDetailPage', () => ({ __esModule: true, default: mockPage('MentorDetailPage') }));
jest.mock('../pages/sessions/MySessionsPage', () => ({ __esModule: true, default: mockPage('MySessionsPage') }));
jest.mock('../pages/payment/CheckoutPage', () => ({ __esModule: true, default: mockPage('CheckoutPage') }));
jest.mock('../pages/profile/UserProfilePage', () => ({ __esModule: true, default: mockPage('UserProfilePage') }));
jest.mock('../pages/groups/GroupsPage', () => ({ __esModule: true, default: mockPage('GroupsPage') }));
jest.mock('../pages/groups/GroupDetailPage', () => ({ __esModule: true, default: mockPage('GroupDetailPage') }));
jest.mock('../pages/notifications/NotificationsPage', () => ({ __esModule: true, default: mockPage('NotificationsPage') }));
jest.mock('../pages/settings/SettingsPage', () => ({ __esModule: true, default: mockPage('SettingsPage') }));
jest.mock('../pages/support/HelpCenterPage', () => ({ __esModule: true, default: mockPage('HelpCenterPage') }));

describe('AppRoutes', () => {
  it('renders public landing route', () => {
    renderAppRoutes({ route: '/' });
    expect(screen.getByText('LandingPage')).toBeInTheDocument();
  });

  it('renders login route inside auth branch', () => {
    renderAppRoutes({ route: '/login' });
    expect(screen.getByText('LoginPage')).toBeInTheDocument();
  });

  it('redirects /dashboard to mentor dashboard for mentor role', () => {
    renderAppRoutes({
      route: '/dashboard',
      authState: createAuthenticatedState('ROLE_MENTOR'),
    });

    expect(screen.getByText('MentorDashboardPage')).toBeInTheDocument();
  });

  it('redirects unauthorized admin access to unauthorized page', () => {
    renderAppRoutes({
      route: '/admin',
      authState: createAuthenticatedState('ROLE_LEARNER'),
    });

    expect(screen.getByText('UnauthorizedPage')).toBeInTheDocument();
  });

  it('redirects unauthenticated protected route access to login', () => {
    renderAppRoutes({ route: '/sessions' });
    expect(screen.getByText('LoginPage')).toBeInTheDocument();
  });
});

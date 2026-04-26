import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '../../tests/utils/renderWithProviders';

import AdminDashboardPage from './admin/AdminDashboardPage';
import ForgotPasswordPage from './auth/ForgotPasswordPage';
import LoginPage from './auth/LoginPage';
import RegisterPage from './auth/RegisterPage';
import LearnerDashboardPage from './learner/LearnerDashboardPage';
import MentorDashboardPage from './mentor/MentorDashboardPage';
import DiscoverMentorsPage from './mentors/DiscoverMentorsPage';
import NotificationsPage from './notifications/NotificationsPage';
import CheckoutPage from './payment/CheckoutPage';
import SettingsPage from './settings/SettingsPage';
import HelpCenterPage from './support/HelpCenterPage';
import api from '../services/axios';
import notificationService from '../services/notificationService';

jest.mock('@react-oauth/google', () => ({
  useGoogleLogin: () => jest.fn(),
}));

jest.mock('../components/layout/PageLayout', () => ({
  __esModule: true,
  default: ({ children }: { children: React.ReactNode }) => <div data-testid="page-layout">{children}</div>,
}));

jest.mock('../services/axios', () => ({
  __esModule: true,
  API_BASE_URL: 'https://api.skillsync.dev',
  default: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

jest.mock('../services/notificationService', () => ({
  __esModule: true,
  default: {
    getNotifications: jest.fn().mockResolvedValue({ content: [], totalElements: 0, page: 0, size: 20 }),
    getUnreadCount: jest.fn().mockResolvedValue(0),
    markAsRead: jest.fn().mockResolvedValue(undefined),
    markAllAsRead: jest.fn().mockResolvedValue(undefined),
    deleteNotification: jest.fn().mockResolvedValue(undefined),
    clearAllNotifications: jest.fn().mockResolvedValue(undefined),
    subscribeToNotifications: jest.fn(() => jest.fn()),
  },
}));

const mockedApi = api as jest.Mocked<typeof api>;
const mockedNotificationService = notificationService as jest.Mocked<typeof notificationService>;
const originalFetch = global.fetch;

type AuthRole = 'ROLE_ADMIN' | 'ROLE_MENTOR' | 'ROLE_LEARNER';

const createAuthState = (role: 'ROLE_ADMIN' | 'ROLE_MENTOR' | 'ROLE_LEARNER') => ({
  user: {
    id: 1,
    firstName: 'Mock',
    lastName: 'User',
    email: 'mock@skillsync.dev',
    role,
  },
  role,
  accessToken: 'access',
  refreshToken: 'refresh',
  isAuthenticated: true,
});

type RouteEntry =
  | string
  | {
      pathname: string;
      state?: Record<string, unknown>;
    };

type SmokeCase = {
  name: string;
  Component: React.ComponentType;
  route: string;
  authRole?: AuthRole;
  initialEntries?: RouteEntry[];
  assertContent: () => void | Promise<void>;
  assertSideEffects?: () => void | Promise<void>;
};

const wasApiGetCalledFor = (pathFragment: string): boolean =>
  mockedApi.get.mock.calls.some(([url]) => String(url).includes(pathFragment));

describe('core pages smoke coverage', () => {
  const pages: SmokeCase[] = [
    {
      name: 'LoginPage',
      Component: LoginPage,
      route: '/login',
      assertContent: () => {
        expect(screen.getByRole('heading', { name: /Welcome back/i })).toBeInTheDocument();
        expect(screen.getByLabelText('Email')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Sign In/i })).toBeInTheDocument();
      },
    },
    {
      name: 'RegisterPage',
      Component: RegisterPage,
      route: '/register',
      assertContent: () => {
        expect(screen.getByRole('heading', { name: /Register an account/i })).toBeInTheDocument();
        expect(screen.getByLabelText('Email')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Verify Email/i })).toBeInTheDocument();
      },
    },
    {
      name: 'ForgotPasswordPage',
      Component: ForgotPasswordPage,
      route: '/forgot-password',
      assertContent: () => {
        expect(screen.getByRole('heading', { name: /Forgot Password/i })).toBeInTheDocument();
        expect(screen.getByText(/Enter your account email/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Send Reset OTP/i })).toBeInTheDocument();
      },
    },
    {
      name: 'CheckoutPage',
      Component: CheckoutPage,
      route: '/checkout',
      authRole: 'ROLE_LEARNER',
      initialEntries: [
        {
          pathname: '/checkout',
          state: {
            mentorId: 1,
            mentorName: 'Mentor Mock',
            startTime: '2026-04-16T10:00:00Z',
            hourlyRate: 1200,
          },
        },
      ],
      assertContent: () => {
        expect(screen.getByRole('heading', { name: /Complete Your Booking/i })).toBeInTheDocument();
        expect(screen.getByText('Mentor Mock')).toBeInTheDocument();
        expect(screen.getByText(/Total Amount/i)).toBeInTheDocument();
        expect(screen.getByLabelText('Cardholder Name')).toBeInTheDocument();
      },
    },
    {
      name: 'LearnerDashboardPage',
      Component: LearnerDashboardPage,
      route: '/learner',
      authRole: 'ROLE_LEARNER',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Welcome back, Mock!/i })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: /Upcoming Sessions/i })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: /Recommended Mentors/i })).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/sessions/learner')).toBe(true));
        await waitFor(() => expect(wasApiGetCalledFor('/api/mentors/search')).toBe(true));
      },
    },
    {
      name: 'MentorDashboardPage',
      Component: MentorDashboardPage,
      route: '/mentor',
      authRole: 'ROLE_MENTOR',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Mentor Dashboard/i })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: /Action Required/i })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: /Upcoming Sessions/i })).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/mentors/me')).toBe(true));
        await waitFor(() => expect(wasApiGetCalledFor('/api/sessions/mentor')).toBe(true));
      },
    },
    {
      name: 'AdminDashboardPage',
      Component: AdminDashboardPage,
      route: '/admin',
      authRole: 'ROLE_ADMIN',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Admin Dashboard/i })).toBeInTheDocument();
        expect(screen.getByText('Total Users')).toBeInTheDocument();
        expect(screen.getByText('Approved Mentors')).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/admin/stats')).toBe(true));
      },
    },
    {
      name: 'DiscoverMentorsPage',
      Component: DiscoverMentorsPage,
      route: '/mentors',
      authRole: 'ROLE_LEARNER',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Discover Mentors/i })).toBeInTheDocument();
        expect(screen.getByText(/Filter by Skills/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Apply/i })).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/mentors/search')).toBe(true));
        await waitFor(() => expect(wasApiGetCalledFor('/api/skills')).toBe(true));
      },
    },
    {
      name: 'NotificationsPage',
      Component: NotificationsPage,
      route: '/notifications',
      authRole: 'ROLE_LEARNER',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Notifications/i })).toBeInTheDocument();
        expect(screen.getByText(/All notifications read/i)).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(mockedNotificationService.getNotifications).toHaveBeenCalledWith(0, 50));
      },
    },
    {
      name: 'SettingsPage',
      Component: SettingsPage,
      route: '/settings',
      authRole: 'ROLE_LEARNER',
      assertContent: () => {
        expect(screen.getByRole('heading', { name: /Change Password/i })).toBeInTheDocument();
        expect(screen.getByText(/Step 1: Email/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Send OTP/i })).toBeInTheDocument();
      },
    },
    {
      name: 'HelpCenterPage',
      Component: HelpCenterPage,
      route: '/help',
      authRole: 'ROLE_LEARNER',
      assertContent: () => {
        expect(screen.getByRole('heading', { name: /Help Center/i })).toBeInTheDocument();
        expect(screen.getAllByText(/academyskillsync@gmail.com/i).length).toBeGreaterThan(0);
      },
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();

    (window as unknown as { Razorpay: jest.Mock }).Razorpay = jest.fn().mockImplementation(() => ({ open: jest.fn() }));

    mockedApi.get.mockImplementation(async (url: string) => {
      if (url.includes('/api/admin/stats')) {
        return {
          data: {
            totalUsers: 25,
            totalMentors: 8,
            totalSessions: 112,
            pendingMentorApprovals: 3,
          },
        } as never;
      }

      if (url.includes('/api/sessions/learner')) {
        return {
          data: {
            content: [],
            totalElements: 0,
            number: 0,
            size: 50,
            last: true,
          },
        } as never;
      }

      if (url.includes('/api/groups/my')) {
        return { data: [] } as never;
      }

      if (url.includes('/api/mentors/me')) {
        return {
          data: {
            id: 99,
            firstName: 'Mentor',
            lastName: 'Mock',
            avgRating: 0,
            totalSessions: 0,
          },
        } as never;
      }

      if (url.includes('/api/sessions/mentor')) {
        return {
          data: {
            content: [],
            totalElements: 0,
            number: 0,
            size: 200,
            last: true,
          },
        } as never;
      }

      if (url.includes('/api/reviews/mentor/')) {
        return { data: { content: [], totalElements: 0 } } as never;
      }

      if (url.includes('/api/notifications/unread/count')) {
        return { data: { count: 0 } } as never;
      }

      if (url.includes('/api/skills')) {
        return {
          data: {
            content: [{ id: 1, name: 'React' }],
            totalElements: 1,
            page: 0,
            size: 200,
            last: true,
          },
        } as never;
      }

      return {
        data: {
          content: [],
          totalElements: 0,
          page: 0,
          size: 10,
          count: 0,
          number: 0,
          last: true,
        },
      } as never;
    });

    mockedApi.post.mockResolvedValue({ data: {} } as never);
    mockedApi.put.mockResolvedValue({ data: {} } as never);
    mockedApi.delete.mockResolvedValue({ data: {} } as never);

    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      text: async () => '# SkillSync docs',
      json: async () => ({}),
      arrayBuffer: async () => new ArrayBuffer(8),
    } as Response);
  });

  afterAll(() => {
    global.fetch = originalFetch;
  });

  pages.forEach(({ name, Component, route, authRole, initialEntries, assertContent, assertSideEffects }) => {
    it(`renders ${name} with meaningful UI anchors`, async () => {
      const preloadedState = authRole
        ? {
            auth: createAuthState(authRole),
          }
        : undefined;

      renderWithProviders(<Component />, {
        route,
        initialEntries,
        preloadedState,
      });

      await assertContent();

      if (assertSideEffects) {
        await assertSideEffects();
      }
    });
  });
});

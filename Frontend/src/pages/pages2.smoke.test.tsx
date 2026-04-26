/// <reference types="jest" />
import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import { renderWithProviders } from '../../tests/utils/renderWithProviders';

import AdminGroupsPage from './admin/AdminGroupsPage';
import AdminSkillsPage from './admin/AdminSkillsPage';
import MentorApprovalsPage from './admin/MentorApprovalsPage';
import UsersCenterPage from './admin/UsersCenterPage';
import VerifyOtpPage from './auth/VerifyOtpPage';
import ResetPasswordPage from './auth/ResetPasswordPage';
import SetupPasswordPage from './auth/SetupPasswordPage';
import GroupsPage from './groups/GroupsPage';
import GroupDetailPage from './groups/GroupDetailPage';
import LearnDocsPage from './docs/LearnDocsPage';
import MentorProfilePage from './mentors/MentorProfilePage';
import MentorAvailabilityPage from './mentor/MentorAvailabilityPage';
import EarningsPage from './mentor/EarningsPage';
import MySessionsPage from './sessions/MySessionsPage';
import UserProfilePage from './profile/UserProfilePage';
import PptLandingPage from './PptLandingPage';
import MentorDetailPage from './mentors/MentorDetailPage';
import api from '../services/axios';

// Mocking Layout to avoid nested routing/sidebar issues during smoke tests
jest.mock('../components/layout/PageLayout', () => ({
  __esModule: true,
  default: ({ children }: { children: React.ReactNode }) => <div data-testid="page-layout">{children}</div>,
}));

// Properly mock axios instance
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

const mockedApi = api as jest.Mocked<typeof api>;

type AuthRole = 'ROLE_ADMIN' | 'ROLE_MENTOR' | 'ROLE_LEARNER';

const createAuthState = (role: AuthRole) => ({
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

type SmokeCase = {
  name: string;
  Component: React.ComponentType<any>;
  route: string;
  routePath?: string;
  authRole?: AuthRole;
  initialEntries?: any[];
  assertContent: () => void | Promise<void>;
  assertSideEffects?: () => void | Promise<void>;
};

const wasApiGetCalledFor = (pathFragment: string): boolean =>
  mockedApi.get.mock.calls.some(([url]) => String(url).includes(pathFragment));

describe('extended pages smoke coverage', () => {
  const pages: SmokeCase[] = [
    {
      name: 'AdminGroupsPage',
      Component: AdminGroupsPage,
      route: '/admin/groups',
      authRole: 'ROLE_ADMIN',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Manage Groups/i })).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/Search by name or description/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /\+ Create Group/i })).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/groups')).toBe(true));
      },
    },
    {
      name: 'AdminSkillsPage',
      Component: AdminSkillsPage,
      route: '/admin/skills',
      authRole: 'ROLE_ADMIN',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Manage Skills/i })).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/Skill Name/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Add Skill/i })).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/skills')).toBe(true));
      },
    },
    {
      name: 'MentorApprovalsPage',
      Component: MentorApprovalsPage,
      route: '/admin/approvals',
      authRole: 'ROLE_ADMIN',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Mentor Approvals/i })).toBeInTheDocument();
        expect(screen.getByText(/Review and manage pending mentor applications/i)).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/admin/mentors/pending')).toBe(true));
      },
    },
    {
      name: 'UsersCenterPage',
      Component: UsersCenterPage,
      route: '/admin/users',
      authRole: 'ROLE_ADMIN',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Manage Users/i })).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/Type email to search/i)).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/admin/users')).toBe(true));
      },
    },
    {
      name: 'VerifyOtpPage',
      Component: VerifyOtpPage,
      routePath: '/verify-otp',
      route: '/verify-otp',
      initialEntries: [{ pathname: '/verify-otp', state: { email: 'test@example.com' } }],
      assertContent: async () => {
        expect(await screen.findByText(/Verify your email/i)).toBeInTheDocument();
        expect(screen.getAllByRole('textbox')).toHaveLength(6);
        expect(screen.getByRole('button', { name: /Verify/i })).toBeInTheDocument();
      },
    },
    {
      name: 'ResetPasswordPage',
      Component: ResetPasswordPage,
      routePath: '/reset-password',
      route: '/reset-password',
      initialEntries: [{ pathname: '/reset-password', state: { email: 'test@example.com' } }],
      assertContent: async () => {
        expect(await screen.findByText(/Reset your password/i)).toBeInTheDocument();
        expect(screen.getAllByRole('textbox')).toHaveLength(6);
      },
    },
    {
      name: 'SetupPasswordPage',
      Component: SetupPasswordPage,
      routePath: '/setup-password',
      route: '/setup-password',
      initialEntries: [{ pathname: '/setup-password', state: { email: 'test@example.com' } }],
      assertContent: async () => {
        expect(await screen.findByText(/Set your password/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Save Password/i })).toBeInTheDocument();
      },
    },
    {
      name: 'GroupsPage',
      Component: GroupsPage,
      route: '/groups',
      authRole: 'ROLE_LEARNER',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Learning Groups/i })).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/Search groups\.\.\./i)).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/groups')).toBe(true));
      },
    },
    {
      name: 'GroupDetailPage',
      Component: GroupDetailPage,
      routePath: '/groups/:id',
      route: '/groups/1',
      authRole: 'ROLE_LEARNER',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Mock Group/i })).toBeInTheDocument();
        expect(screen.getByText(/Description/i)).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/groups/1')).toBe(true));
      },
    },
    {
      name: 'LearnDocsPage',
      Component: LearnDocsPage,
      route: '/docs',
      authRole: 'ROLE_LEARNER',
      assertContent: async () => {
        expect(await screen.findByText(/Learn Every Part of the Platform from One Place/i)).toBeInTheDocument();
        expect(screen.getByText(/SkillSync Documentation Hub/i)).toBeInTheDocument();
      },
    },
    {
      name: 'MentorProfilePage',
      Component: MentorProfilePage,
      routePath: '/mentors/:id/profile',
      route: '/mentors/1/profile',
      authRole: 'ROLE_LEARNER',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Mock Mentor/i })).toBeInTheDocument();
        expect(screen.getByText(/Fullstack Developer/i)).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/mentors/1')).toBe(true));
      },
    },
    {
      name: 'MentorAvailabilityPage',
      Component: MentorAvailabilityPage,
      route: '/mentor/availability',
      authRole: 'ROLE_MENTOR',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /^Availability$/i })).toBeInTheDocument();
        expect(screen.getByText(/Set the weekly time windows/i)).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/mentors/me/availability')).toBe(true));
      },
    },
    {
      name: 'EarningsPage',
      Component: EarningsPage,
      route: '/mentor/earnings',
      authRole: 'ROLE_MENTOR',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /^Earnings$/i })).toBeInTheDocument();
        expect(screen.getByText(/Earnings are now calculated/i)).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/sessions/mentor')).toBe(true));
      },
    },
    {
      name: 'MySessionsPage',
      Component: MySessionsPage,
      route: '/sessions',
      authRole: 'ROLE_LEARNER',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /My Sessions/i })).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/sessions/learner')).toBe(true));
      },
    },
    {
      name: 'UserProfilePage',
      Component: UserProfilePage,
      route: '/profile',
      authRole: 'ROLE_LEARNER',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /My Profile/i })).toBeInTheDocument();
      },
    },
    {
      name: 'PptLandingPage',
      Component: PptLandingPage,
      route: '/presentation',
      assertContent: async () => {
        expect((await screen.findAllByText(/Peer To Peer Learning Platform/i)).length).toBeGreaterThan(0);
        expect(await screen.findByRole('heading', { name: /Documentation/i })).toBeInTheDocument();
      },
    },
    {
      name: 'MentorDetailPage',
      Component: MentorDetailPage,
      routePath: '/mentors/:id',
      route: '/mentors/1',
      authRole: 'ROLE_LEARNER',
      assertContent: async () => {
        expect(await screen.findByRole('heading', { name: /Mock Mentor/i })).toBeInTheDocument();
      },
      assertSideEffects: async () => {
        await waitFor(() => expect(wasApiGetCalledFor('/api/mentors/1')).toBe(true));
      },
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();

    // Polyfill window methods
    (window as any).Razorpay = jest.fn().mockImplementation(() => ({ open: jest.fn() }));
    (window as any).scrollTo = jest.fn();

    mockedApi.get.mockImplementation(async (url: string) => {
      if (url.includes('/api/mentors/1') || url.includes('/api/mentors/me')) {
        return {
          data: {
            id: 1,
            userId: 1,
            firstName: 'Mock',
            lastName: 'Mentor',
            bio: 'Bio',
            headline: 'Fullstack Developer',
            hourlyRate: 50,
            avgRating: 4.5,
            totalSessions: 10,
            skills: ['React', 'Node.js']
          }
        } as never;
      }
      if (url.includes('/api/groups/1')) {
        return { data: { id: 1, name: 'Mock Group', description: 'Description', category: 'General', memberCount: 10, isJoined: true } } as never;
      }
      if (url.includes('/api/mentors/me/earnings')) {
        return { data: { totalEarnings: 500, pendingPayout: 100, completedSessions: 5 } } as never;
      }
      if (url.includes('/api/notifications/unread/count')) {
        return { data: { count: 0 } } as never;
      }

      return {
        data: {
          content: [],
          totalElements: 0,
          page: 0,
          size: 10,
          number: 0,
          last: true,
        },
      } as never;
    });

    mockedApi.post.mockResolvedValue({ data: {} } as never);

    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      text: async () => '# Documentation',
    } as Response);
  });

  pages.forEach(({ name, Component, route, routePath, authRole, initialEntries, assertContent, assertSideEffects }) => {
    it(`renders ${name} with meaningful UI anchors`, async () => {
      const preloadedState = authRole
        ? {
          auth: createAuthState(authRole),
        }
        : undefined;

      const ui = routePath ? (
        <Routes>
          <Route path={routePath} element={<Component />} />
        </Routes>
      ) : (
        <Component />
      );

      renderWithProviders(ui, {
        route,
        initialEntries: initialEntries || [route],
        preloadedState,
      });

      await assertContent();
      if (assertSideEffects) {
        await assertSideEffects();
      }
    });
  });
});

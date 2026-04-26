import { waitFor } from '@testing-library/react';
import Navbar from './Navbar';
import { renderWithProviders } from '../../../tests/utils/renderWithProviders';
import api from '../../services/axios';
import notificationService from '../../services/notificationService';

const unsubscribe = jest.fn();

jest.mock('../../services/axios', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
  },
}));

jest.mock('../../services/notificationService', () => ({
  __esModule: true,
  default: {
    subscribeToNotifications: jest.fn(() => unsubscribe),
  },
}));

jest.mock('../ui/ThemeToggleButton', () => ({
  __esModule: true,
  default: () => <button>Theme toggle</button>,
}));

describe('Navbar', () => {
  const mockedApi = api as jest.Mocked<typeof api>;
  const mockedNotifications = notificationService as jest.Mocked<typeof notificationService>;

  beforeEach(() => {
    jest.clearAllMocks();
    mockedApi.get.mockResolvedValue({ data: { count: 3 } } as any);
  });

  it('subscribes to notifications and renders unread badge for authenticated user', async () => {
    const { getByText, unmount } = renderWithProviders(<Navbar />, {
      preloadedState: {
        auth: {
          user: { id: 7, firstName: 'Lara', lastName: 'Dev', email: 'lara@skillsync.dev', role: 'ROLE_LEARNER' },
          accessToken: 'token',
          refreshToken: 'refresh',
          isAuthenticated: true,
          role: 'ROLE_LEARNER',
        },
      },
    });

    await waitFor(() => {
      expect(mockedNotifications.subscribeToNotifications).toHaveBeenCalled();
      expect(getByText('3')).toBeInTheDocument();
    });

    unmount();
    expect(unsubscribe).toHaveBeenCalled();
  });

  it('does not subscribe and does not crash when user is missing', async () => {
    const { getByText } = renderWithProviders(<Navbar />, {
      preloadedState: {
        auth: { user: null, accessToken: null, refreshToken: null, isAuthenticated: false, role: null },
      },
    });

    await waitFor(() => {
      expect(getByText('Theme toggle')).toBeInTheDocument();
      expect(mockedNotifications.subscribeToNotifications).not.toHaveBeenCalled();
    });
  });

  it('falls back to unread count zero when API errors', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('request failed'));
    const { queryByText } = renderWithProviders(<Navbar />, {
      preloadedState: {
        auth: {
          user: { id: 11, firstName: 'A', lastName: 'B', email: 'a@b.dev', role: 'ROLE_ADMIN' },
          accessToken: 'token',
          refreshToken: 'refresh',
          isAuthenticated: true,
          role: 'ROLE_ADMIN',
        },
      },
    });

    await waitFor(() => {
      expect(queryByText('9+')).not.toBeInTheDocument();
    });
  });

  it('calls queryClient.invalidateQueries when notification received', async () => {
    const { queryClient } = renderWithProviders(<Navbar />, {
      preloadedState: {
        auth: {
          user: { id: 1, firstName: 'Test', lastName: 'User', email: 'test@skillsync.dev', role: 'ROLE_LEARNER' },
          accessToken: 'token',
          refreshToken: 'refresh',
          isAuthenticated: true,
          role: 'ROLE_LEARNER',
        },
      },
    });
    // Simulate notification callback
    const subscribe = notificationService.subscribeToNotifications as jest.Mock;
    const callback = subscribe.mock.calls[0][0];
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');
    callback();
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['notifications'] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['unread-notifications'] });
    invalidateSpy.mockRestore();
  });
});

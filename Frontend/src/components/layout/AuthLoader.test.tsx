import { waitFor } from '@testing-library/react';
import AuthLoader from './AuthLoader';
import { renderWithProviders } from '../../../tests/utils/renderWithProviders';
import api from '../../services/axios';

jest.mock('../../services/axios', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

describe('AuthLoader', () => {
  const mockedApi = api as jest.Mocked<typeof api>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('skips auth bootstrap for public routes', async () => {
    const { getByText } = renderWithProviders(
      <AuthLoader>
        <div>Public child</div>
      </AuthLoader>,
      { route: '/login' },
    );

    await waitFor(() => {
      expect(getByText('Public child')).toBeInTheDocument();
      expect(mockedApi.get).not.toHaveBeenCalled();
      expect(mockedApi.post).not.toHaveBeenCalled();
    });
  });

  it('loads current user on protected routes when user is missing', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: { id: 1, firstName: 'User', lastName: 'Loaded', email: 'loaded@skillsync.dev', role: 'ROLE_LEARNER' },
    } as any);

    const { getByText, store } = renderWithProviders(
      <AuthLoader>
        <div>Private child</div>
      </AuthLoader>,
      {
        route: '/sessions',
        preloadedState: {
          auth: { user: null, accessToken: null, refreshToken: 'refresh', isAuthenticated: false, role: null },
        },
      },
    );

    await waitFor(() => {
      expect(getByText('Private child')).toBeInTheDocument();
      expect(mockedApi.get).toHaveBeenCalledWith('/api/auth/me', expect.any(Object));
      expect(store.getState().auth.user?.email).toBe('loaded@skillsync.dev');
    });
  });

  it('attempts refresh then retries user fetch after initial 401', async () => {
    mockedApi.get.mockRejectedValueOnce({ response: { status: 401 } } as any);
    mockedApi.post.mockResolvedValueOnce({ data: { ok: true } } as any);
    mockedApi.get.mockResolvedValueOnce({
      data: { id: 2, firstName: 'Retry', lastName: 'User', email: 'retry@skillsync.dev', role: 'ROLE_MENTOR' },
    } as any);

    const { getByText, store } = renderWithProviders(
      <AuthLoader>
        <div>Private child</div>
      </AuthLoader>,
      {
        route: '/mentor',
        preloadedState: {
          auth: { user: null, accessToken: null, refreshToken: 'refresh', isAuthenticated: false, role: null },
        },
      },
    );

    await waitFor(() => {
      expect(getByText('Private child')).toBeInTheDocument();
      expect(mockedApi.post).toHaveBeenCalledWith('/api/auth/refresh', undefined, expect.any(Object));
      expect(store.getState().auth.user?.email).toBe('retry@skillsync.dev');
    });
  });

  it('logs error if user not authenticated on load (catch block)', async () => {
    mockedApi.get.mockRejectedValueOnce({ response: { status: 403 } });
    mockedApi.post.mockRejectedValueOnce({});
    const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

    renderWithProviders(
      <AuthLoader>
        <div>Private child</div>
      </AuthLoader>,
      {
        route: '/sessions',
        preloadedState: {
          auth: { user: null, accessToken: null, refreshToken: 'refresh', isAuthenticated: false, role: null },
        },
      },
    );
    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('User not authenticated on load - requires login');
    });
    errorSpy.mockRestore();
  });

  it('logs error if user not authenticated on load (else block)', async () => {
    const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    renderWithProviders(
      <AuthLoader>
        <div>Private child</div>
      </AuthLoader>,
      {
        route: '/sessions',
        preloadedState: {
          auth: { user: null, accessToken: null, refreshToken: null, isAuthenticated: false, role: null },
        },
      },
    );
    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('User not authenticated on load - requires login');
    });
    errorSpy.mockRestore();
  });
});

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { renderWithProviders } from '../../utils/renderWithProviders';
import { server } from '../../mocks/server';
import LoginPage from '../../../src/pages/auth/LoginPage';

const originalFetch = global.fetch;
const mockUseGoogleLogin = jest.fn();

jest.mock('@react-oauth/google', () => ({
  useGoogleLogin: (options: { onSuccess?: (value: { access_token: string }) => void }) => {
    mockUseGoogleLogin.mockImplementation(() => {
      options?.onSuccess?.({ access_token: 'mock-google-token' });
    });
    return mockUseGoogleLogin;
  },
}));

describe('LoginPage (MSW integration)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = originalFetch;
  });

  test('renders login form correctly', () => {
    renderWithProviders(<LoginPage />);
    expect(screen.getByRole('heading', { level: 2, name: /welcome back/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /google/i })).toBeInTheDocument();
  });

  test('shows validation errors for empty inputs', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />);

    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
    expect(await screen.findByText(/password is required/i)).toBeInTheDocument();
  });


  test('shows an error message when credentials are invalid', async () => {
    server.use(
      http.post('*/api/auth/login', () =>
        HttpResponse.json(
          { message: 'Invalid credentials. Please try again.' },
          { status: 401 }
        )
      )
    );

    const user = userEvent.setup();
    renderWithProviders(<LoginPage />);

    await user.type(screen.getByLabelText(/email/i), 'wrong@test.com');
    await user.type(screen.getByLabelText(/password/i), 'wrongpass');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText(/error connecting to server/i)).toBeInTheDocument();
  });

  test('renders safely when session_expired reason query exists', () => {
    renderWithProviders(<LoginPage />, { route: '/login?reason=session_expired' });
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });
});

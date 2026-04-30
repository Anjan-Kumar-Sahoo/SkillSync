import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../utils/renderWithProviders';
import RegisterPage from '../../../src/pages/auth/RegisterPage';
import api from '../../../src/services/axios';

jest.mock('../../../src/services/axios', () => ({
  __esModule: true,
  default: {
    post: jest.fn(),
  },
}));

const mockUseGoogleLogin = jest.fn();
const originalFetch = global.fetch;
const mockedApi = api as jest.Mocked<typeof api>;

jest.mock('@react-oauth/google', () => ({
  useGoogleLogin: (options: any) => {
    mockUseGoogleLogin.mockImplementation(() => {
      if (options && options.onSuccess) {
        options.onSuccess({ access_token: 'mock-google-token' });
      }
    });
    return mockUseGoogleLogin;
  },
}));

describe('RegisterPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = originalFetch;
    mockedApi.post.mockReset();
  });

  test('renders initial verification step', () => {
    renderWithProviders(<RegisterPage />);
    expect(screen.getByRole('heading', { level: 2, name: /register an account/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /verify email/i })).toBeInTheDocument();
  });

  test('shows validation errors for empty email on verify step', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);

    await user.click(screen.getByRole('button', { name: /verify email/i }));
    
    expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
  });

  test('initiates registration: user already exists', async () => {
    mockedApi.post.mockImplementation((url: string) => {
      if (url === '/api/auth/initiate-registration') {
        return Promise.resolve({ data: { exists: true, message: 'Email already registered' } } as any);
      }
      return Promise.reject({ response: { status: 404 } });
    });
    
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText(/email/i), 'existing@test.com');
    await user.click(screen.getByRole('button', { name: /verify email/i }));

    expect(await screen.findByText(/this email is already registered/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /go to login/i })).toBeInTheDocument();
  });

  test('full registration flow completes successfully', async () => {
    mockedApi.post.mockImplementation((url: string) => {
      if (url === '/api/auth/initiate-registration') {
        return Promise.resolve({ data: { exists: false, message: 'OTP sent' } } as any);
      }

      if (url === '/api/auth/verify-otp') {
        return Promise.resolve({ data: { success: true } } as any);
      }

      if (url === '/api/auth/complete-registration') {
        return Promise.resolve({
          data: {
            user: {
              id: 1,
              firstName: 'New',
              lastName: 'User',
              role: 'ROLE_LEARNER',
              email: 'newuser@test.com',
            },
            accessToken: 'token123',
            refreshToken: 'ref123'
          }
        } as any);
      }

      return Promise.reject({ response: { status: 404 } });
    });

    const user = userEvent.setup();
    const { store } = renderWithProviders(<RegisterPage />);

    // Step 1: Request OTP
    await user.type(screen.getByLabelText(/email/i), 'newuser@test.com');
    await user.click(screen.getByRole('button', { name: /verify email/i }));

    // Expect OTP input fields to appear
    expect(await screen.findByText(/otp sent on your email: newuser@test.com/i)).toBeInTheDocument();

    // Step 2: Enter OTP
    // There are 6 inputs, they all have id `otp-0`, `otp-1`, etc.
    const container = screen.getByText(/otp sent on your email/i).parentElement!;
    // get all text inputs in the container
    const otpInputs = within(container).getAllByRole('textbox');
    expect(otpInputs).toHaveLength(6);

    for (let i = 0; i < 6; i++) {
        await user.type(otpInputs[i], (i + 1).toString());
    }

    // Submit OTP
    await user.click(screen.getByRole('button', { name: /verify otp/i }));

    // Step 3: Complete Profile form should render
    expect(await screen.findByRole('heading', { level: 2, name: /complete profile/i })).toBeInTheDocument();

    // Fill profile
    await user.type(screen.getByLabelText(/first name/i), 'New');
    await user.type(screen.getByLabelText(/last name/i), 'User');
    
    // We need to meet password constraints
    const passwordInput = screen.getByLabelText(/create password/i);
    await user.type(passwordInput, 'ValidPassword123!');

    // Show/hide password test
    const showPasswordBtn = screen.getByRole('button', { name: /show password/i });
    await user.click(showPasswordBtn);
    expect(passwordInput).toHaveAttribute('type', 'text');
    await user.click(screen.getByRole('button', { name: /hide password/i }));
    expect(passwordInput).toHaveAttribute('type', 'password');

    // Submit registration
    await user.click(screen.getByRole('button', { name: /finish registration/i }));

    // Verify it authenticated
    await waitFor(() => {
      expect(store.getState().auth.isAuthenticated).toBe(true);
      expect(store.getState().auth.user?.firstName).toBe('New');
    });
  });

  test('validates incomplete OTP', async () => {
    mockedApi.post.mockImplementation((url: string) => {
      if (url === '/api/auth/initiate-registration') {
        return Promise.resolve({ data: { exists: false, message: 'OTP sent' } } as any);
      }

      return Promise.reject({ response: { status: 404 } });
    });

    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText(/email/i), 'newuser@test.com');
    await user.click(screen.getByRole('button', { name: /verify email/i }));
    
    expect(await screen.findByText(/otp sent on your email/i)).toBeInTheDocument();

    // Only enter 3 digits
    const container = screen.getByText(/otp sent on your email/i).parentElement!;
    const otpInputs = within(container).getAllByRole('textbox');
    for (let i = 0; i < 3; i++) {
        await user.type(otpInputs[i], '1');
    }

    const verifyBtn = screen.getByRole('button', { name: /verify otp/i });
    await user.click(verifyBtn);

    // Toast would appear here, but we check if we're still on the verify step
    expect(screen.getByText(/otp sent on your email/i)).toBeInTheDocument();
  });

  test('handles verify mutation error', async () => {
    mockedApi.post.mockImplementation((url: string) => {
      if (url === '/api/auth/initiate-registration') {
        return Promise.resolve({ data: { exists: false, message: 'OTP sent' } } as any);
      }

      if (url === '/api/auth/verify-otp') {
        return Promise.reject({ response: { status: 400, data: { message: 'Invalid OTP' } } });
      }

      return Promise.reject({ response: { status: 404 } });
    });

    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText(/email/i), 'newuser@test.com');
    await user.click(screen.getByRole('button', { name: /verify email/i }));
    
    expect(await screen.findByText(/otp sent on your email/i)).toBeInTheDocument();

    // Enter 6 digits
    const container = screen.getByText(/otp sent on your email/i).parentElement!;
    const otpInputs = within(container).getAllByRole('textbox');
    for (let i = 0; i < 6; i++) {
        await user.type(otpInputs[i], '1');
    }

    const verifyBtn = screen.getByRole('button', { name: /verify otp/i });
    await user.click(verifyBtn);

    // Wait and verify we are still on the verify step because it failed
    await waitFor(() => {
        expect(screen.getByRole('button', { name: /verify otp/i })).toBeInTheDocument();
    });
  });

  test('handles google login in register page', async () => {
      mockedApi.post.mockImplementation((url: string) => {
        if (url === '/api/auth/oauth-login') {
          return Promise.resolve({
            data: {
              user: {
                id: 5,
                role: 'ROLE_LEARNER',
                firstName: 'GoogleG',
                lastName: 'G',
                email: 'google@test.com',
              },
              accessToken: 'g-acc',
              refreshToken: 'g-ref',
              passwordSetupRequired: false
            }
          } as any);
        }

        return Promise.reject({ response: { status: 404 } });
      });
  
      global.fetch = jest.fn((input: RequestInfo | URL, init?: RequestInit) => {
        const url = typeof input === 'string'
          ? input
          : input instanceof URL
            ? input.toString()
            : input.url;

        if (url.includes('googleapis.com/oauth2/v3/userinfo')) {
          return Promise.resolve({
            json: async () => ({
              sub: '12345',
              email: 'google@test.com',
              given_name: 'Google',
              family_name: 'G'
            })
          } as any);
        }

        return originalFetch(input as any, init as any);
      }) as any;
  
      const user = userEvent.setup();
      const { store } = renderWithProviders(<RegisterPage />);
  
      const googleBtn = screen.getByRole('button', { name: /continue with google/i });
      await user.click(googleBtn);
  
      await waitFor(() => {
        expect(store.getState().auth.isAuthenticated).toBe(true);
        expect(store.getState().auth.user?.firstName).toBe('GoogleG');
      });
  });

  test('password constraints validation prevents submission', async () => {
    mockedApi.post.mockImplementation((url: string) => {
      if (url === '/api/auth/initiate-registration') {
        return Promise.resolve({ data: { exists: false, message: 'OTP sent' } } as any);
      }

      if (url === '/api/auth/verify-otp') {
        return Promise.resolve({ data: { success: true } } as any);
      }

      if (url === '/api/auth/complete-registration') {
        return Promise.resolve({ data: {} } as any);
      }

      return Promise.reject({ response: { status: 404 } });
    });
    
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />);

    // Mock completing verification
    await user.type(screen.getByLabelText(/email/i), 'newuser@test.com');
    await user.click(screen.getByRole('button', { name: /verify email/i }));
    await screen.findByText(/otp sent on your email/i);
    
    const container = screen.getByText(/otp sent on your email/i).parentElement!;
    const otpInputs = within(container).getAllByRole('textbox');
    for (let i = 0; i < 6; i++) {
        await user.type(otpInputs[i], '1');
    }
    await user.click(screen.getByRole('button', { name: /verify otp/i }));
    expect(await screen.findByRole('heading', { level: 2, name: /complete profile/i })).toBeInTheDocument();

    // Fill profile with WEAK password
    await user.type(screen.getByLabelText(/first name/i), 'New');
    await user.type(screen.getByLabelText(/last name/i), 'User');
    await user.type(screen.getByLabelText(/create password/i), 'weak'); // Will fail constraints

    await user.click(screen.getByRole('button', { name: /finish registration/i }));

    // Verify it stays there
    expect(screen.getByRole('heading', { level: 2, name: /complete profile/i })).toBeInTheDocument();
  });
});

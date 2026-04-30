import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../mocks/server';
import { renderWithProviders } from '../utils/renderWithProviders';
import RegisterPage from '../../src/pages/auth/RegisterPage';
import LoginPage from '../../src/pages/auth/LoginPage';

jest.mock('@react-oauth/google', () => ({
  useGoogleLogin: () => jest.fn(),
}));

describe('E2E Flow: Register -> OTP -> Login', () => {
  test('completes registration handshake and then signs in', async () => {
    let registrationCompleted = false;

    server.use(
      http.post('*/api/auth/initiate-registration', () =>
        HttpResponse.json({ exists: false, message: 'OTP sent' })
      ),
      http.post('*/api/auth/verify-otp', () => HttpResponse.json({ success: true })),
      http.post('*/api/auth/complete-registration', async () => {
        registrationCompleted = true;
        // Return without user to force login step after registration completion.
        return HttpResponse.json({});
      }),
      http.post('*/api/auth/login', async ({ request }) => {
        const payload = (await request.json()) as { email?: string };

        return HttpResponse.json({
          user: {
            id: 808,
            role: 'ROLE_LEARNER',
            firstName: 'Reg',
            lastName: 'Flow',
            email: payload.email,
          },
          accessToken: 'post-reg-access',
          refreshToken: 'post-reg-refresh',
        });
      })
    );

    const user = userEvent.setup();
    const { unmount } = renderWithProviders(<RegisterPage />);

    await user.type(screen.getByLabelText(/email/i), 'new.flow@skillsync.dev');
    await user.click(screen.getByRole('button', { name: /verify email/i }));

    expect(await screen.findByText(/otp sent on your email/i)).toBeInTheDocument();

    const otpInputs = screen.getAllByLabelText(/otp digit/i);
    expect(otpInputs).toHaveLength(6);

    for (let i = 0; i < otpInputs.length; i += 1) {
      await user.type(otpInputs[i], String((i + 1) % 10));
    }

    await user.click(screen.getByRole('button', { name: /verify otp/i }));

    expect(await screen.findByRole('heading', { level: 2, name: /complete profile/i })).toBeInTheDocument();

    await user.type(screen.getByLabelText(/first name/i), 'Reg');
    await user.type(screen.getByLabelText(/last name/i), 'Flow');
    await user.type(screen.getByLabelText(/create password/i), 'ValidPassword123!');
    await user.click(screen.getByRole('button', { name: /finish registration/i }));

    await waitFor(() => {
      expect(registrationCompleted).toBe(true);
    });

    unmount();

    const { store } = renderWithProviders(<LoginPage />);

    await user.type(screen.getByLabelText(/email/i), 'new.flow@skillsync.dev');
    await user.type(screen.getByLabelText(/password/i), 'ValidPassword123!');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(store.getState().auth.isAuthenticated).toBe(true);
      expect(store.getState().auth.user?.email).toBe('new.flow@skillsync.dev');
    });
  });
});

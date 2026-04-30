import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '../mocks/server';
import { renderWithProviders } from '../utils/renderWithProviders';
import LoginPage from '../../src/pages/auth/LoginPage';
import CheckoutPage from '../../src/pages/payment/CheckoutPage';

type RazorpayHandlerPayload = {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
};

type RazorpayOptions = {
  handler: (response: RazorpayHandlerPayload) => void;
  modal: {
    ondismiss: () => void | Promise<void>;
  };
};

jest.mock('@react-oauth/google', () => ({
  useGoogleLogin: () => jest.fn(),
}));

describe('E2E Flow: Login -> Book Session -> Payment -> Verify', () => {
  test('completes the full learner checkout flow', async () => {
    let cancelled = false;
    let razorpayOptions: RazorpayOptions | null = null;
    const mockRazorpayOpen = jest.fn();

    window.Razorpay = jest.fn().mockImplementation((options: RazorpayOptions) => {
      razorpayOptions = options;
      return { open: mockRazorpayOpen };
    }) as unknown as typeof window.Razorpay;

    server.use(
      http.post('*/api/auth/login', async ({ request }) => {
        const payload = (await request.json()) as { email?: string };
        return HttpResponse.json({
          user: {
            id: 301,
            role: 'ROLE_LEARNER',
            firstName: 'Flow',
            lastName: 'User',
            email: payload.email,
          },
          accessToken: 'flow-access',
          refreshToken: 'flow-refresh',
        });
      }),
      http.post('*/api/sessions', () => HttpResponse.json({ id: 7001 })),
      http.post('*/api/payments/create-order', () =>
        HttpResponse.json({
          orderId: 'order_flow_7001',
          amount: 2100,
          currency: 'INR',
          keyId: 'rzp_flow_key',
        })
      ),
      http.post('*/api/payments/verify', () => HttpResponse.json({ status: 'success' })),
      http.put('*/api/sessions/:id/cancel', () => {
        cancelled = true;
        return HttpResponse.json({ status: 'cancelled' });
      })
    );

    const user = userEvent.setup();
    const { store: loginStore, unmount } = renderWithProviders(<LoginPage />);

    await user.type(screen.getByLabelText(/email/i), 'flow.learner@skillsync.dev');
    await user.type(screen.getByLabelText(/password/i), 'ValidPassword123!');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(loginStore.getState().auth.isAuthenticated).toBe(true);
      expect(loginStore.getState().auth.user?.email).toBe('flow.learner@skillsync.dev');
    });

    const authState = loginStore.getState().auth;
    unmount();

    renderWithProviders(<CheckoutPage />, {
      initialEntries: [
        {
          pathname: '/checkout',
          state: {
            mentorId: 901,
            mentorName: 'Mentor Prime',
            startTime: '2026-09-10T09:30:00Z',
            hourlyRate: 2000,
          },
        },
      ],
      preloadedState: { auth: authState },
    });

    await user.click(screen.getByRole('button', { name: /confirm payment/i }));

    await waitFor(() => {
      expect(mockRazorpayOpen).toHaveBeenCalledTimes(1);
      expect(razorpayOptions).not.toBeNull();
    });

    act(() => {
      razorpayOptions?.handler({
        razorpay_order_id: 'order_flow_7001',
        razorpay_payment_id: 'pay_flow_7001',
        razorpay_signature: 'sig_flow_7001',
      });
    });

    await waitFor(() => {
      expect(screen.queryByText(/verifying transaction/i)).not.toBeInTheDocument();
    });

    expect(cancelled).toBe(false);
  });
});

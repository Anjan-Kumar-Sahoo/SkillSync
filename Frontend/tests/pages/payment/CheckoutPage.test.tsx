import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { renderWithProviders } from '../../utils/renderWithProviders';
import { server } from '../../mocks/server';
import CheckoutPage from '../../../src/pages/payment/CheckoutPage';

type RazorpayHandlerPayload = {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
};

type RazorpayOptions = {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  handler: (response: RazorpayHandlerPayload) => void;
  modal: {
    ondismiss: () => void | Promise<void>;
  };
  theme: {
    color: string;
  };
};

type RazorpayInstance = {
  open: jest.Mock;
};

describe('CheckoutPage (MSW integration)', () => {
  let mockRazorpayOpen: jest.Mock;
  let razorpayOptions: RazorpayOptions | null;
  let cancelledSessionIds: string[];

  const mockLocationState = {
    mentorId: 101,
    mentorName: 'Jane Smith',
    startTime: '2026-08-16T10:00:00Z',
    hourlyRate: 1000,
  };

  const initialEntries = [{ pathname: '/checkout', state: mockLocationState }];

  beforeEach(() => {
    jest.clearAllMocks();
    razorpayOptions = null;
    cancelledSessionIds = [];
    mockRazorpayOpen = jest.fn();

    window.Razorpay = jest.fn().mockImplementation((options: RazorpayOptions): RazorpayInstance => {
      razorpayOptions = options;
      return { open: mockRazorpayOpen };
    }) as unknown as typeof window.Razorpay;

    server.use(
      http.post('*/api/sessions', () => HttpResponse.json({ id: 999 })),
      http.post('*/api/payments/create-order', () =>
        HttpResponse.json({
          orderId: 'order_123',
          amount: 1050,
          currency: 'INR',
          keyId: 'rzp_test_key',
        })
      ),
      http.post('*/api/payments/verify', () => HttpResponse.json({ status: 'success' })),
      http.put('*/api/sessions/:id/cancel', ({ params }) => {
        cancelledSessionIds.push(String(params.id));
        return HttpResponse.json({ status: 'cancelled' });
      })
    );
  });

  test('renders checkout summary correctly', () => {
    renderWithProviders(<CheckoutPage />, { initialEntries });

    expect(screen.getByRole('heading', { level: 1, name: /complete your booking/i })).toBeInTheDocument();
    expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    expect(screen.getByText('₹1000.00')).toBeInTheDocument();
    expect(screen.getByText('₹50.00')).toBeInTheDocument();
    expect(screen.getByText('₹1050.00')).toBeInTheDocument();
  });

  test('redirects out if accessed without location state', () => {
    const { container } = renderWithProviders(<CheckoutPage />, { initialEntries: ['/checkout'] });
    expect(container).toBeEmptyDOMElement();
  });

  test('handles successful payment flow and verification', async () => {
    const user = userEvent.setup();
    renderWithProviders(<CheckoutPage />, { initialEntries });

    await user.click(screen.getByRole('button', { name: /confirm payment/i }));

    await waitFor(() => {
      expect(mockRazorpayOpen).toHaveBeenCalledTimes(1);
      expect(razorpayOptions).not.toBeNull();
    });

    act(() => {
      razorpayOptions?.handler({
        razorpay_order_id: 'order_123',
        razorpay_payment_id: 'pay_123',
        razorpay_signature: 'sig_123',
      });
    });

    expect(await screen.findByText(/verifying transaction/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.queryByText(/verifying transaction/i)).not.toBeInTheDocument();
    });

    expect(cancelledSessionIds).toEqual([]);
  });

  test('handles payment modal dismiss and rolls session back', async () => {
    const user = userEvent.setup();
    renderWithProviders(<CheckoutPage />, { initialEntries });

    await user.click(screen.getByRole('button', { name: /confirm payment/i }));

    await waitFor(() => expect(mockRazorpayOpen).toHaveBeenCalledTimes(1));

    await act(async () => {
      await razorpayOptions?.modal.ondismiss();
    });

    await waitFor(() => {
      expect(cancelledSessionIds).toContain('999');
      expect(screen.getByRole('button', { name: /confirm payment/i })).toBeInTheDocument();
    });
  });

  test('stops flow when session reservation API fails', async () => {
    server.use(
      http.post('*/api/sessions', () =>
        HttpResponse.json({ message: 'Mentor not available' }, { status: 400 })
      )
    );

    const user = userEvent.setup();
    renderWithProviders(<CheckoutPage />, { initialEntries });

    await user.click(screen.getByRole('button', { name: /confirm payment/i }));

    await waitFor(() => {
      expect(mockRazorpayOpen).not.toHaveBeenCalled();
      expect(cancelledSessionIds).toEqual([]);
      expect(screen.getByRole('button', { name: /confirm payment/i })).toBeInTheDocument();
    });
  });

  test('rolls back session when order creation fails', async () => {
    server.use(
      http.post('*/api/payments/create-order', () =>
        HttpResponse.json({ message: 'Order rejected' }, { status: 500 })
      )
    );

    const user = userEvent.setup();
    renderWithProviders(<CheckoutPage />, { initialEntries });

    await user.click(screen.getByRole('button', { name: /confirm payment/i }));

    await waitFor(() => {
      expect(cancelledSessionIds).toContain('999');
      expect(mockRazorpayOpen).not.toHaveBeenCalled();
    });
  });

  test('rolls back session when payment verification fails', async () => {
    server.use(
      http.post('*/api/payments/verify', () =>
        HttpResponse.json({ message: 'Verification failed' }, { status: 400 })
      )
    );

    const user = userEvent.setup();
    renderWithProviders(<CheckoutPage />, { initialEntries });

    await user.click(screen.getByRole('button', { name: /confirm payment/i }));

    await waitFor(() => {
      expect(mockRazorpayOpen).toHaveBeenCalledTimes(1);
      expect(razorpayOptions).not.toBeNull();
    });

    act(() => {
      razorpayOptions?.handler({
        razorpay_order_id: 'order_123',
        razorpay_payment_id: 'pay_123',
        razorpay_signature: 'sig_123',
      });
    });

    await waitFor(() => {
      expect(cancelledSessionIds).toContain('999');
    });
  });

  test('allows switching between card and PayPal methods', async () => {
    const user = userEvent.setup();
    renderWithProviders(<CheckoutPage />, { initialEntries });

    await user.click(screen.getByText('PayPal'));
    expect(screen.queryByPlaceholderText(/john doe/i)).not.toBeInTheDocument();

    await user.click(screen.getByText('Credit or Debit Card'));
    expect(screen.getByPlaceholderText(/john doe/i)).toBeInTheDocument();
  });
});

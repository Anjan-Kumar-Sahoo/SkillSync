import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderHook } from '@testing-library/react';
import { ToastProvider, useToast } from './Toast';

const Trigger = ({ type = 'success' }: { type?: 'success' | 'error' | 'info' }) => {
  const { showToast } = useToast();
  return <button onClick={() => showToast({ message: 'Toast message', type })}>show</button>;
};

describe('Toast', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('shows and auto-hides a toast message', async () => {
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    render(
      <ToastProvider>
        <Trigger />
      </ToastProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'show' }));
    expect(screen.getByText('Toast message')).toBeInTheDocument();

    act(() => {
      jest.advanceTimersByTime(3000);
    });

    await waitFor(() => {
      expect(screen.queryByText('Toast message')).not.toBeInTheDocument();
    });
  });

  it('applies error style variant when requested', async () => {
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    render(
      <ToastProvider>
        <Trigger type="error" />
      </ToastProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'show' }));
    const toast = screen.getByText('Toast message');
    expect(toast.className).toContain('bg-error');
  });

  it('throws when hook is used outside provider', () => {
    expect(() => renderHook(() => useToast())).toThrow('useToast must be used within ToastProvider');
  });
});

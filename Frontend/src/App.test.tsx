import { render, screen } from '@testing-library/react';
import App from './App';

jest.mock('./components/ui/Toast', () => ({
  ToastProvider: ({ children }: { children: React.ReactNode }) => <div data-testid="toast-provider">{children}</div>,
}));

jest.mock('./components/ui/ActionConfirm', () => ({
  ActionConfirmProvider: ({ children }: { children: React.ReactNode }) => <div data-testid="confirm-provider">{children}</div>,
}));

jest.mock('./components/layout/AuthLoader', () => ({
  __esModule: true,
  default: ({ children }: { children: React.ReactNode }) => <div data-testid="auth-loader">{children}</div>,
}));

jest.mock('./routes/AppRoutes', () => ({
  __esModule: true,
  default: () => <div>AppRoutes content</div>,
}));

describe('App', () => {
  it('composes providers and renders routes', () => {
    render(<App />);

    expect(screen.getByTestId('toast-provider')).toBeInTheDocument();
    expect(screen.getByTestId('confirm-provider')).toBeInTheDocument();
    expect(screen.getByTestId('auth-loader')).toBeInTheDocument();
    expect(screen.getByText('AppRoutes content')).toBeInTheDocument();
  });
});

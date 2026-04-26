import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ThemeToggleButton from './ThemeToggleButton';

const toggleTheme = jest.fn();
let mockIsDark = false;

jest.mock('../../context/ThemeContext', () => ({
  useTheme: () => ({
    isDark: mockIsDark,
    toggleTheme,
  }),
}));

describe('ThemeToggleButton', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockIsDark = false;
  });

  it('renders with light mode label and aria semantics', () => {
    render(<ThemeToggleButton />);

    const button = screen.getByRole('button', { name: 'Switch to dark mode' });
    expect(button).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByText('Light Mode')).toBeInTheDocument();
  });

  it('hides label when showLabel is false and still toggles theme', async () => {
    const user = userEvent.setup();
    render(<ThemeToggleButton showLabel={false} className="test-class" />);

    const button = screen.getByRole('button', { name: 'Switch to dark mode' });
    expect(button).toHaveClass('test-class');
    expect(screen.queryByText('Light Mode')).not.toBeInTheDocument();

    await user.click(button);
    expect(toggleTheme).toHaveBeenCalledTimes(1);
  });

  it('renders dark mode icon and accessible label when dark theme is active', () => {
    mockIsDark = true;
    render(<ThemeToggleButton />);

    const button = screen.getByRole('button', { name: 'Switch to light mode' });
    expect(button).toHaveAttribute('aria-pressed', 'true');
    expect(button).toHaveAttribute('title', 'Switch to light mode');
    expect(screen.getByText('Dark Mode')).toBeInTheDocument();
    expect(screen.getByText('dark_mode')).toBeInTheDocument();
  });
});

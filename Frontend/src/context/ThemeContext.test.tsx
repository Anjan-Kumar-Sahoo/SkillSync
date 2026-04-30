import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderHook } from '@testing-library/react';
import { ThemeProvider, useTheme } from './ThemeContext';

const ThemeConsumer = () => {
  const { theme, isDark, toggleTheme, setTheme } = useTheme();
  return (
    <div>
      <span data-testid="theme-value">{theme}</span>
      <span data-testid="is-dark">{String(isDark)}</span>
      <button onClick={toggleTheme}>toggle</button>
      <button onClick={() => setTheme('light')}>light</button>
    </div>
  );
};

describe('ThemeContext', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.className = '';
    document.documentElement.removeAttribute('data-theme');
  });

  it('defaults to light theme and applies root attributes', () => {
    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('theme-value')).toHaveTextContent('light');
    expect(screen.getByTestId('is-dark')).toHaveTextContent('false');
    expect(document.documentElement).toHaveClass('theme-light');
    expect(document.documentElement).toHaveAttribute('data-theme', 'light');
    expect(window.localStorage.getItem('skillsync.theme')).toBe('light');
  });

  it('loads dark theme from localStorage and toggles back to light', async () => {
    window.localStorage.setItem('skillsync.theme', 'dark');
    const user = userEvent.setup();

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('theme-value')).toHaveTextContent('dark');
    expect(document.documentElement).toHaveClass('theme-dark');

    await user.click(screen.getByRole('button', { name: 'toggle' }));

    expect(screen.getByTestId('theme-value')).toHaveTextContent('light');
    expect(window.localStorage.getItem('skillsync.theme')).toBe('light');
  });

  it('throws when useTheme is used outside ThemeProvider', () => {
    expect(() => renderHook(() => useTheme())).toThrow('useTheme must be used within ThemeProvider');
  });

  it('returns light theme if window is undefined', () => {
    const originalWindow = global.window;
    delete (global as any).window;
    const { ThemeProvider: RealThemeProvider } = jest.requireActual('./ThemeContext');
    let themeValue = undefined;
    function TestComponent() {
      const { theme } = jest.requireActual('./ThemeContext').useTheme();
      themeValue = theme;
      return null;
    }
    render(
      <RealThemeProvider>
        <TestComponent />
      </RealThemeProvider>
    );
    expect(themeValue).toBe('light');
    global.window = originalWindow;
  });

  it('sets colorScheme style on document root', () => {
    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );
    expect(document.documentElement.style.colorScheme).toBe('light');
  });
});

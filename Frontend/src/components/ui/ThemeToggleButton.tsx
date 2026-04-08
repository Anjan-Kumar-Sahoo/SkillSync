import { useTheme } from '../../context/ThemeContext';

type ThemeToggleButtonProps = {
  className?: string;
  showLabel?: boolean;
};

const ThemeToggleButton = ({ className = '', showLabel = true }: ThemeToggleButtonProps) => {
  const { isDark, toggleTheme } = useTheme();

  const label = isDark ? 'Dark Mode' : 'Light Mode';

  return (
    <button
      type="button"
      onClick={toggleTheme}
      aria-pressed={isDark}
      aria-label={`Switch to ${isDark ? 'light' : 'dark'} mode`}
      title={`Switch to ${isDark ? 'light' : 'dark'} mode`}
      className={`group inline-flex items-center gap-2 rounded-full border border-outline-variant/40 bg-surface-container-lowest/90 px-3 py-2 text-on-surface shadow-[0_10px_24px_rgba(15,23,42,0.18)] backdrop-blur-md transition-all hover:-translate-y-0.5 hover:shadow-[0_14px_28px_rgba(15,23,42,0.22)] active:translate-y-0 ${className}`}
    >
      <span className="relative inline-flex h-6 w-11 items-center rounded-full bg-surface-container-high transition-colors">
        <span
          className={`absolute h-5 w-5 rounded-full bg-gradient-to-br from-primary to-primary-container shadow-md transition-transform duration-300 ${
            isDark ? 'translate-x-5' : 'translate-x-1'
          }`}
        />
        <span className="sr-only">Theme</span>
      </span>

      {showLabel && (
        <span className="hidden sm:inline text-xs font-bold tracking-wide text-on-surface-variant group-hover:text-on-surface">
          {label}
        </span>
      )}

      <span className="material-symbols-outlined text-[18px] text-on-surface-variant group-hover:text-on-surface transition-colors">
        {isDark ? 'dark_mode' : 'light_mode'}
      </span>
    </button>
  );
};

export default ThemeToggleButton;

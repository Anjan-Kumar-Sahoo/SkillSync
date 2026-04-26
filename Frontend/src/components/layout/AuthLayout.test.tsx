import { Route, Routes } from 'react-router-dom';
import { renderWithProviders } from '../../../tests/utils/renderWithProviders';
import AuthLayout from './AuthLayout';

jest.mock('../ui/ThemeToggleButton', () => ({
  __esModule: true,
  default: () => <button>Theme toggle</button>,
}));

describe('AuthLayout', () => {
  it('renders outlet and theme toggle', () => {
    const { getByText } = renderWithProviders(
      <Routes>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<div>Login form</div>} />
        </Route>
      </Routes>,
      { route: '/login' },
    );

    expect(getByText('Theme toggle')).toBeInTheDocument();
    expect(getByText('Login form')).toBeInTheDocument();
  });
});

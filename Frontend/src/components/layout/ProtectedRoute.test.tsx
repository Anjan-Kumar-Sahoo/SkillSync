import { Route, Routes } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import { renderWithProviders } from '../../../tests/utils/renderWithProviders';

describe('ProtectedRoute', () => {
  it('redirects unauthenticated users to login', () => {
    const { getByText } = renderWithProviders(
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/private" element={<div>Private page</div>} />
        </Route>
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>,
      {
        route: '/private',
        preloadedState: {
          auth: { user: null, accessToken: null, refreshToken: null, isAuthenticated: false, role: null },
        },
      },
    );

    expect(getByText('Login page')).toBeInTheDocument();
  });

  it('renders protected outlet when authenticated', () => {
    const { getByText } = renderWithProviders(
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/private" element={<div>Private page</div>} />
        </Route>
      </Routes>,
      {
        route: '/private',
        preloadedState: {
          auth: {
            user: { id: 1, firstName: 'A', lastName: 'B', email: 'a@b.dev', role: 'ROLE_LEARNER' },
            accessToken: 'token',
            refreshToken: 'refresh',
            isAuthenticated: true,
            role: 'ROLE_LEARNER',
          },
        },
      },
    );

    expect(getByText('Private page')).toBeInTheDocument();
  });
});

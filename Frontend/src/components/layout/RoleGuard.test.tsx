import { Route, Routes } from 'react-router-dom';
import RoleGuard from './RoleGuard';
import { renderWithProviders } from '../../../tests/utils/renderWithProviders';

describe('RoleGuard', () => {
  it('redirects to login when not authenticated', () => {
    const { getByText } = renderWithProviders(
      <Routes>
        <Route element={<RoleGuard allowedRoles={['ROLE_ADMIN']} />}>
          <Route path="/admin" element={<div>Admin page</div>} />
        </Route>
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>,
      {
        route: '/admin',
        preloadedState: {
          auth: { user: null, accessToken: null, refreshToken: null, isAuthenticated: false, role: null },
        },
      },
    );

    expect(getByText('Login page')).toBeInTheDocument();
  });

  it('redirects to unauthorized when role is not allowed', () => {
    const { getByText } = renderWithProviders(
      <Routes>
        <Route element={<RoleGuard allowedRoles={['ROLE_ADMIN']} />}>
          <Route path="/admin" element={<div>Admin page</div>} />
        </Route>
        <Route path="/unauthorized" element={<div>Unauthorized page</div>} />
      </Routes>,
      {
        route: '/admin',
        preloadedState: {
          auth: {
            user: { id: 2, firstName: 'M', lastName: 'N', email: 'm@n.dev', role: 'ROLE_MENTOR' },
            accessToken: 'token',
            refreshToken: 'refresh',
            isAuthenticated: true,
            role: 'ROLE_MENTOR',
          },
        },
      },
    );

    expect(getByText('Unauthorized page')).toBeInTheDocument();
  });

  it('renders outlet when role is allowed', () => {
    const { getByText } = renderWithProviders(
      <Routes>
        <Route element={<RoleGuard allowedRoles={['ROLE_ADMIN']} />}>
          <Route path="/admin" element={<div>Admin page</div>} />
        </Route>
      </Routes>,
      {
        route: '/admin',
        preloadedState: {
          auth: {
            user: { id: 3, firstName: 'A', lastName: 'D', email: 'a@d.dev', role: 'ROLE_ADMIN' },
            accessToken: 'token',
            refreshToken: 'refresh',
            isAuthenticated: true,
            role: 'ROLE_ADMIN',
          },
        },
      },
    );

    expect(getByText('Admin page')).toBeInTheDocument();
  });
});

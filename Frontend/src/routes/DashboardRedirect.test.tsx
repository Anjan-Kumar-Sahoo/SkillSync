import { render, screen } from '@testing-library/react';
import { Route, Routes, MemoryRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../store/slices/authSlice';
import DashboardRedirect from './DashboardRedirect';

type AuthRole = 'ROLE_ADMIN' | 'ROLE_MENTOR' | 'ROLE_LEARNER' | null;

const renderRedirect = (role: AuthRole) => {
  const store = configureStore({
    reducer: {
      auth: authReducer,
    } as any,
    preloadedState: {
      auth: {
        user: role
          ? { id: 1, firstName: 'Route', lastName: 'User', email: 'route@skillsync.dev', role }
          : null,
        accessToken: role ? 'token' : null,
        refreshToken: role ? 'refresh' : null,
        isAuthenticated: Boolean(role),
        role,
      },
    } as any,
  });

  return render(
    <Provider store={store}>
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="/dashboard" element={<DashboardRedirect />} />
          <Route path="/admin" element={<div>Admin destination</div>} />
          <Route path="/mentor" element={<div>Mentor destination</div>} />
          <Route path="/learner" element={<div>Learner destination</div>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );
};

describe('DashboardRedirect', () => {
  it('routes admins to admin dashboard', () => {
    renderRedirect('ROLE_ADMIN');
    expect(screen.getByText('Admin destination')).toBeInTheDocument();
  });

  it('routes mentors to mentor dashboard', () => {
    renderRedirect('ROLE_MENTOR');
    expect(screen.getByText('Mentor destination')).toBeInTheDocument();
  });

  it('routes learners to learner dashboard', () => {
    renderRedirect('ROLE_LEARNER');
    expect(screen.getByText('Learner destination')).toBeInTheDocument();
  });

  it('defaults unknown role to learner dashboard', () => {
    renderRedirect(null);
    expect(screen.getByText('Learner destination')).toBeInTheDocument();
  });
});

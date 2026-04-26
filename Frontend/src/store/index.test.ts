import { store } from './index';
import { setCredentials, logout } from './slices/authSlice';

describe('store', () => {
  it('exposes expected slices and handles dispatches', () => {
    const initial = store.getState();
    expect(initial).toHaveProperty('auth');
    expect(initial).toHaveProperty('ui');
    expect(initial).toHaveProperty('sessions');
    expect(initial).toHaveProperty('mentors');
    expect(initial).toHaveProperty('groups');
    expect(initial).toHaveProperty('notifications');
    expect(initial).toHaveProperty('reviews');

    store.dispatch(
      setCredentials({
        user: { id: 1, firstName: 'A', lastName: 'B', email: 'a@b.dev', role: 'ROLE_LEARNER' },
        accessToken: 'token',
        refreshToken: 'refresh',
      }),
    );

    expect(store.getState().auth.isAuthenticated).toBe(true);

    store.dispatch(logout());
    expect(store.getState().auth.isAuthenticated).toBe(false);
  });
});

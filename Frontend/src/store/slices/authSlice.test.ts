import authReducer, { setCredentials } from './authSlice';

describe('authSlice', () => {
  it('sets isAuthenticated and role when user is present', () => {
    const initialState = { isAuthenticated: false, user: null, role: null };
    const user = { id: 1, role: 'ROLE_ADMIN' };
    const nextState = authReducer(initialState, setCredentials({ user }));
    expect(nextState.isAuthenticated).toBe(true);
    expect(nextState.role).toBe('ROLE_ADMIN');
  });
});

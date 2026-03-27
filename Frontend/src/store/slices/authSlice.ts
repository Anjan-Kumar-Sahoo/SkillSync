import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { UserSummary } from '../../types';

type UserRole = 'ROLE_LEARNER' | 'ROLE_MENTOR' | 'ROLE_ADMIN' | null;

interface AuthState {
  user: UserSummary | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  role: UserRole;
}

const initialState: AuthState = {
  user: null,
  accessToken: localStorage.getItem('skillsync_access_token'),
  refreshToken: localStorage.getItem('skillsync_refresh_token'),
  isAuthenticated: !!localStorage.getItem('skillsync_access_token'),
  role: null,
};

interface SetCredentialsPayload {
  user: UserSummary | null;
  accessToken: string;
  refreshToken: string;
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials(state, action: PayloadAction<SetCredentialsPayload>) {
      const { user, accessToken, refreshToken } = action.payload;
      state.user = user;
      state.accessToken = accessToken;
      state.refreshToken = refreshToken;
      state.isAuthenticated = true;
      if (user) {
        state.role = user.role as UserRole;
      }

      // Persist to localStorage
      localStorage.setItem('skillsync_access_token', accessToken);
      localStorage.setItem('skillsync_refresh_token', refreshToken);
    },
    logout(state) {
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
      state.role = null;

      localStorage.removeItem('skillsync_access_token');
      localStorage.removeItem('skillsync_refresh_token');
    },
  },
});

export const { setCredentials, logout } = authSlice.actions;
export default authSlice.reducer;

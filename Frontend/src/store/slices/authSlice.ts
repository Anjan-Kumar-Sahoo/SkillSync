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
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
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

      // Removed localStorage persistence for security (XSS mitigation)
    },
    logout(state) {
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
      state.role = null;

      // Removed localStorage persistence for security
    },
    updateUserName(state, action: PayloadAction<{ firstName: string; lastName: string }>) {
      if (!state.user) {
        return;
      }

      state.user = {
        ...state.user,
        firstName: action.payload.firstName,
        lastName: action.payload.lastName,
      };
    },
  },
});

export const { setCredentials, logout, updateUserName } = authSlice.actions;
export default authSlice.reducer;

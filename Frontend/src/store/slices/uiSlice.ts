import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

interface UIState {
  sidebarOpen: boolean;
  notificationsOpen: boolean;
  theme: 'light' | 'dark';
  loading: boolean;
  error: string | null;
}

const initialState: UIState = {
  sidebarOpen: true,
  notificationsOpen: false,
  theme: 'light',
  loading: false,
  error: null,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSidebar: (state) => {
      state.sidebarOpen = !state.sidebarOpen;
    },
    setSidebarOpen: (state, action: PayloadAction<boolean>) => {
      state.sidebarOpen = action.payload;
    },
    toggleNotifications: (state) => {
      state.notificationsOpen = !state.notificationsOpen;
    },
    setNotificationsOpen: (state, action: PayloadAction<boolean>) => {
      state.notificationsOpen = action.payload;
    },
    setTheme: (state, action: PayloadAction<'light' | 'dark'>) => {
      state.theme = action.payload;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
});

export const {
  toggleSidebar,
  setSidebarOpen,
  toggleNotifications,
  setNotificationsOpen,
  setTheme,
  setLoading,
  setError,
  clearError,
} = uiSlice.actions;

export default uiSlice.reducer;

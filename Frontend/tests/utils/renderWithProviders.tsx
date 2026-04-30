import React from 'react';
import { Provider } from 'react-redux';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { configureStore } from '@reduxjs/toolkit';
import { render, type RenderOptions } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import authReducer from '../../src/store/slices/authSlice';
import uiReducer from '../../src/store/slices/uiSlice';
import sessionsReducer from '../../src/store/slices/sessionsSlice';
import mentorsReducer from '../../src/store/slices/mentorsSlice';
import groupsReducer from '../../src/store/slices/groupsSlice';
import notificationsReducer from '../../src/store/slices/notificationsSlice';
import reviewsReducer from '../../src/store/slices/reviewsSlice';
import { ThemeProvider } from '../../src/context/ThemeContext';
import { ToastProvider } from '../../src/components/ui/Toast';
import { ActionConfirmProvider } from '../../src/components/ui/ActionConfirm';

type ExtendedOptions = {
  route?: string;
  initialEntries?: any[];
  preloadedState?: Record<string, unknown>;
} & Omit<RenderOptions, 'queries'>;

export const createTestStore = (preloadedState?: Record<string, unknown>) =>
  configureStore({
    reducer: {
      auth: authReducer,
      ui: uiReducer,
      sessions: sessionsReducer,
      mentors: mentorsReducer,
      groups: groupsReducer,
      notifications: notificationsReducer,
      reviews: reviewsReducer,
    } as any,
    preloadedState: preloadedState as any,
  });

export const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        refetchOnMount: false,
        refetchOnReconnect: false,
        refetchOnWindowFocus: false,
        gcTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  });

export const renderWithProviders = (ui: React.ReactElement, options: ExtendedOptions = {}) => {
  const { route = '/', initialEntries, preloadedState, ...renderOptions } = options;
  const store = createTestStore(preloadedState);
  const queryClient = createTestQueryClient();

  const entriesToUse = initialEntries || [route];

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <MemoryRouter initialEntries={entriesToUse}>
      <ThemeProvider>
        <ToastProvider>
          <ActionConfirmProvider>
            <Provider store={store}>
              <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            </Provider>
          </ActionConfirmProvider>
        </ToastProvider>
      </ThemeProvider>
    </MemoryRouter>
  );

  return {
    store,
    queryClient,
    ...render(ui, { wrapper: Wrapper, ...renderOptions }),
  };
};

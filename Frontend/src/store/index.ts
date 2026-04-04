import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import uiReducer from './slices/uiSlice';
import sessionsReducer from './slices/sessionsSlice';
import mentorsReducer from './slices/mentorsSlice';
import groupsReducer from './slices/groupsSlice';
import notificationsReducer from './slices/notificationsSlice';
import reviewsReducer from './slices/reviewsSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    ui: uiReducer,
    sessions: sessionsReducer,
    mentors: mentorsReducer,
    groups: groupsReducer,
    notifications: notificationsReducer,
    reviews: reviewsReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

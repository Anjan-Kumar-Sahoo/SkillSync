import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

export interface NotificationData {
  id: number;
  userId: number;
  type:
    | 'SESSION_REQUEST'
    | 'SESSION_REQUESTED'
    | 'SESSION_REQUESTED_CONFIRMATION'
    | 'SESSION_ACCEPTED'
    | 'SESSION_APPROVED'
    | 'SESSION_REJECTED'
    | 'SESSION_CANCELLED'
    | 'SESSION_COMPLETED'
    | 'MENTOR_APPROVED'
    | 'REVIEW_RECEIVED'
    | 'SYSTEM'
    | 'GROUP_INVITE';
  title: string;
  message: string;
  isRead: boolean;
  relatedEntityType?: string;
  relatedEntityId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface NotificationsState {
  notifications: NotificationData[];
  unreadCount: number;
  isLoading: boolean;
  error: string | null;
  totalElements: number;
}

const initialState: NotificationsState = {
  notifications: [],
  unreadCount: 0,
  isLoading: false,
  error: null,
  totalElements: 0,
};

const notificationsSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    setNotifications: (state, action: PayloadAction<NotificationData[]>) => {
      state.notifications = action.payload;
      state.unreadCount = action.payload.filter(n => !n.isRead).length;
    },
    addNotification: (state, action: PayloadAction<NotificationData>) => {
      state.notifications.unshift(action.payload);
      if (!action.payload.isRead) {
        state.unreadCount += 1;
      }
    },
    markAsRead: (state, action: PayloadAction<number>) => {
      const notification = state.notifications.find(n => n.id === action.payload);
      if (notification && !notification.isRead) {
        notification.isRead = true;
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
    },
    markAllAsRead: (state) => {
      state.notifications.forEach(n => {
        n.isRead = true;
      });
      state.unreadCount = 0;
    },
    removeNotification: (state, action: PayloadAction<number>) => {
      const notification = state.notifications.find(n => n.id === action.payload);
      if (notification && !notification.isRead) {
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
      state.notifications = state.notifications.filter(n => n.id !== action.payload);
    },
    clearNotifications: (state) => {
      state.notifications = [];
      state.unreadCount = 0;
    },
    setNotificationsLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
    setNotificationsError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    setNotificationsTotalElements: (state, action: PayloadAction<number>) => {
      state.totalElements = action.payload;
    },
  },
});

export const {
  setNotifications,
  addNotification,
  markAsRead,
  markAllAsRead,
  removeNotification,
  clearNotifications,
  setNotificationsLoading,
  setNotificationsError,
  setNotificationsTotalElements,
} = notificationsSlice.actions;

export default notificationsSlice.reducer;

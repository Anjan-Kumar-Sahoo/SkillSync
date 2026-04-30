import notificationsReducer, { addNotification, markAsRead, markAllAsRead, removeNotification } from './notificationsSlice';

describe('notificationsSlice', () => {
  it('increments unreadCount when adding unread notification', () => {
    const initialState: any = { notifications: [], unreadCount: 0, isLoading: false, error: null, totalElements: 0 };
    const nextState = notificationsReducer(initialState, addNotification({
      id: 1,
      userId: 1,
      type: 'SYSTEM',
      title: 'Test',
      message: 'Test Message',
      isRead: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }));
    expect(nextState.unreadCount).toBe(1);
  });

  it('marks notification as read and decrements unreadCount', () => {
    const initialState: any = {
      notifications: [{
        id: 1,
        userId: 1,
        type: 'SYSTEM',
        title: 'Test',
        message: 'Test Message',
        isRead: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }],
      unreadCount: 1,
      isLoading: false,
      error: null,
      totalElements: 1
    };
    const nextState = notificationsReducer(initialState, markAsRead(1));
    expect(nextState.notifications[0].isRead).toBe(true);
    expect(nextState.unreadCount).toBe(0);
  });

  it('marks all as read and resets unreadCount', () => {
    const baseNotification = {
      userId: 1,
      type: 'SYSTEM' as const,
      title: 'Test',
      message: 'Test Message',
      isRead: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    const initialState: any = {
      notifications: [
        { ...baseNotification, id: 1 },
        { ...baseNotification, id: 2 }
      ],
      unreadCount: 2,
      isLoading: false,
      error: null,
      totalElements: 2
    };
    const nextState = notificationsReducer(initialState, markAllAsRead());
    expect(nextState.notifications.every(n => n.isRead)).toBe(true);
    expect(nextState.unreadCount).toBe(0);
  });

  it('removes notification and updates unreadCount if not read', () => {
    const initialState: any = {
      notifications: [{
        id: 1,
        userId: 1,
        type: 'SYSTEM',
        title: 'Test',
        message: 'Test Message',
        isRead: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }],
      unreadCount: 1,
      isLoading: false,
      error: null,
      totalElements: 1
    };
    const nextState = notificationsReducer(initialState, removeNotification(1));
    expect(nextState.unreadCount).toBe(0);
  });
});

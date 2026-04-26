import notificationsReducer, { addNotification, markAsRead, markAllAsRead, removeNotification } from './notificationsSlice';

describe('notificationsSlice', () => {
  it('increments unreadCount when adding unread notification', () => {
    const initialState = { notifications: [], unreadCount: 0 };
    const nextState = notificationsReducer(initialState, addNotification({ id: 1, isRead: false }));
    expect(nextState.unreadCount).toBe(1);
  });

  it('marks notification as read and decrements unreadCount', () => {
    const initialState = { notifications: [{ id: 1, isRead: false }], unreadCount: 1 };
    const nextState = notificationsReducer(initialState, markAsRead(1));
    expect(nextState.notifications[0].isRead).toBe(true);
    expect(nextState.unreadCount).toBe(0);
  });

  it('marks all as read and resets unreadCount', () => {
    const initialState = { notifications: [{ id: 1, isRead: false }, { id: 2, isRead: false }], unreadCount: 2 };
    const nextState = notificationsReducer(initialState, markAllAsRead());
    expect(nextState.notifications.every(n => n.isRead)).toBe(true);
    expect(nextState.unreadCount).toBe(0);
  });

  it('removes notification and updates unreadCount if not read', () => {
    const initialState = { notifications: [{ id: 1, isRead: false }], unreadCount: 1 };
    const nextState = notificationsReducer(initialState, removeNotification(1));
    expect(nextState.unreadCount).toBe(0);
  });
});

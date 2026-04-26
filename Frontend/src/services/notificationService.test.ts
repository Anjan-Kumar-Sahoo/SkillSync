import api from './axios';
import notificationService from './notificationService';
import { Client } from '@stomp/stompjs';

const unsubscribeMock = jest.fn();
const deactivateMock = jest.fn();

let messageCallback: ((message: { body: string }) => void) | null = null;

jest.mock('./axios', () => ({
  __esModule: true,
  API_BASE_URL: 'https://api.skillsync.dev',
  default: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

jest.mock('@stomp/stompjs', () => {
  return {
    Client: jest.fn().mockImplementation((config) => {
      const client: any = {
        active: false,
        subscribe: jest.fn((_destination: string, callback: (message: { body: string }) => void) => {
          messageCallback = callback;
          return { unsubscribe: unsubscribeMock };
        }),
        activate: jest.fn(function () {
          this.active = true;
          if (typeof this.onConnect === 'function') {
            this.onConnect();
          }
        }),
        deactivate: deactivateMock,
        onConnect: config.onConnect,
        onStompError: config.onStompError,
        onWebSocketError: config.onWebSocketError,
      };
      return client;
    }),
  };
});

describe('notificationService', () => {
  const mockedApi = api as jest.Mocked<typeof api>;
  const mockedClient = Client as unknown as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
    messageCallback = null;
    (notificationService as any).stompClient = null;
    (notificationService as any).subscription = null;
    (notificationService as any).listeners.clear();
  });

  it('gets notifications and unread notifications', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { content: [{ id: 1, isRead: false }, { id: 2, isRead: true }] } } as any);
    mockedApi.get.mockResolvedValueOnce({ data: { content: [{ id: 10, isRead: false }] } } as any);

    const notifications = await notificationService.getNotifications(1, 50);
    const unread = await notificationService.getUnreadNotifications();

    expect(mockedApi.get).toHaveBeenNthCalledWith(1, '/api/notifications?page=1&size=50');
    expect(notifications.content).toHaveLength(2);
    expect(unread).toEqual([{ id: 10, isRead: false }]);
  });

  it('gets unread count with default fallback', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { count: 4 } } as any);
    mockedApi.get.mockResolvedValueOnce({ data: {} } as any);

    const count = await notificationService.getUnreadCount();
    const fallback = await notificationService.getUnreadCount();

    expect(count).toBe(4);
    expect(fallback).toBe(0);
  });

  it('marks as read and falls back to alternate endpoint when primary fails', async () => {
    mockedApi.post.mockRejectedValueOnce(new Error('old endpoint missing'));
    mockedApi.put.mockResolvedValueOnce({ data: {} } as any);

    await notificationService.markAsRead(99);

    expect(mockedApi.post).toHaveBeenCalledWith('/api/notifications/read/99');
    expect(mockedApi.put).toHaveBeenCalledWith('/api/notifications/99/read', {});
  });

  it('clears notifications using direct endpoint and fallback deletion', async () => {
    mockedApi.delete.mockResolvedValueOnce({ data: {} } as any);
    await notificationService.clearAllNotifications();
    expect(mockedApi.delete).toHaveBeenCalledWith('/api/notifications/all');

    mockedApi.delete.mockRejectedValueOnce(new Error('legacy backend'));
    mockedApi.get.mockResolvedValueOnce({ data: { content: [{ id: 1 }, { id: 2 }] } } as any);
    mockedApi.delete.mockResolvedValue({ data: {} } as any);

    await notificationService.clearAllNotifications();

    expect(mockedApi.get).toHaveBeenCalledWith('/api/notifications?page=0&size=200');
    expect(mockedApi.delete).toHaveBeenCalledWith('/api/notifications/1');
    expect(mockedApi.delete).toHaveBeenCalledWith('/api/notifications/2');
  });

  it('subscribes, handles websocket notifications, parse errors, and disconnects', () => {
    const listener = jest.fn();
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);

    const unsubscribe = notificationService.subscribeToNotifications(listener);

    expect(mockedClient).toHaveBeenCalledWith(
      expect.objectContaining({
        brokerURL: 'wss://api.skillsync.dev/ws/notifications',
      }),
    );

    expect(messageCallback).not.toBeNull();
    messageCallback?.({ body: JSON.stringify({ id: 5, title: 'Hi' }) });
    expect(listener).toHaveBeenCalledWith({ id: 5, title: 'Hi' });

    messageCallback?.({ body: '{invalid-json}' });
    expect(warnSpy).toHaveBeenCalled();

    unsubscribe();

    expect(unsubscribeMock).toHaveBeenCalled();
    expect(deactivateMock).toHaveBeenCalled();

    warnSpy.mockRestore();
  });

  it('reuses active websocket connection when multiple listeners subscribe', () => {
    const firstUnsub = notificationService.subscribeToNotifications(jest.fn());
    notificationService.subscribeToNotifications(jest.fn());

    expect(mockedClient).toHaveBeenCalledTimes(1);

    firstUnsub();
  });

  it('handles repeated unsubscribe safely after client teardown', () => {
    const unsubscribe = notificationService.subscribeToNotifications(jest.fn());

    unsubscribe();
    unsubscribe();

    expect(deactivateMock).toHaveBeenCalledTimes(1);
  });

  it('calls dedicated mark-all and delete endpoints', async () => {
    mockedApi.put.mockResolvedValueOnce({ data: {} } as any);
    mockedApi.delete.mockResolvedValueOnce({ data: {} } as any);

    await notificationService.markAllAsRead();
    await notificationService.deleteNotification(44);

    expect(mockedApi.put).toHaveBeenCalledWith('/api/notifications/read-all', {});
    expect(mockedApi.delete).toHaveBeenCalledWith('/api/notifications/44');
  });

  it('logs STOMP error when onStompError is triggered', () => {
    const consoleSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    (notificationService as any).ensureWebSocketConnection();
    const client = (notificationService as any).stompClient;
    client.onStompError({ headers: { message: 'stomp error' } });
    expect(consoleSpy).toHaveBeenCalledWith('Notification WebSocket STOMP error', 'stomp error');
    consoleSpy.mockRestore();
  });

  it('logs WebSocket error when onWebSocketError is triggered', () => {
    const consoleSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    (notificationService as any).ensureWebSocketConnection();
    const client = (notificationService as any).stompClient;
    client.onWebSocketError();
    expect(consoleSpy).toHaveBeenCalledWith('Notification WebSocket connection error');
    consoleSpy.mockRestore();
  });
});

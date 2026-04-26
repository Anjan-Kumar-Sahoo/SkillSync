type LoadedNotificationService = {
  service: any;
  clientCtor: jest.Mock;
};

const loadNotificationServiceWithBaseUrl = async (baseUrl: string): Promise<LoadedNotificationService> => {
  jest.resetModules();

  const clientCtor = jest.fn().mockImplementation((config) => {
    const client: any = {
      active: false,
      subscribe: jest.fn(() => ({ unsubscribe: jest.fn() })),
      activate: jest.fn(function () {
        this.active = true;
        if (typeof this.onConnect === 'function') {
          this.onConnect();
        }
      }),
      deactivate: jest.fn(),
      onConnect: config.onConnect,
      onStompError: config.onStompError,
      onWebSocketError: config.onWebSocketError,
    };

    return client;
  });

  jest.doMock('./axios', () => ({
    __esModule: true,
    API_BASE_URL: baseUrl,
    default: {
      get: jest.fn(),
      post: jest.fn(),
      put: jest.fn(),
      delete: jest.fn(),
    },
  }));

  jest.doMock('@stomp/stompjs', () => ({
    Client: clientCtor,
  }));

  const service = (await import('./notificationService')).default;

  return {
    service,
    clientCtor,
  };
};

describe('notificationService websocket URL branches', () => {
  afterEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
  });

  it('uses ws protocol when API base URL is http', async () => {
    const loaded = await loadNotificationServiceWithBaseUrl('http://localhost:8080');

    const unsubscribe = loaded.service.subscribeToNotifications(jest.fn());

    expect(loaded.clientCtor).toHaveBeenCalledWith(
      expect.objectContaining({
        brokerURL: 'ws://localhost:8080/ws/notifications',
      }),
    );

    unsubscribe();
  });

  it('falls back to direct URL suffix when API base URL has no http scheme', async () => {
    const loaded = await loadNotificationServiceWithBaseUrl('/gateway');

    const unsubscribe = loaded.service.subscribeToNotifications(jest.fn());

    expect(loaded.clientCtor).toHaveBeenCalledWith(
      expect.objectContaining({
        brokerURL: '/gateway/ws/notifications',
      }),
    );

    unsubscribe();
  });
});

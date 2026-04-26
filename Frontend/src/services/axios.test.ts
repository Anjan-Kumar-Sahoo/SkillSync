type LoadedModule = {
  api: any;
  API_BASE_URL: string;
  apiInstance: any;
  responseSuccessHandler: ((response: any) => any) | undefined;
  requestHandler: ((config: any) => any) | undefined;
  responseErrorHandler: ((error: any) => Promise<any>) | undefined;
  dispatch: jest.Mock;
  getState: jest.Mock;
  logout: jest.Mock;
  setCredentials: jest.Mock;
};

type LoadAxiosModuleOptions = {
  refreshToken?: string | null;
  accessToken?: string | null;
  isProd?: boolean;
  apiUrl?: string;
};

const loadAxiosModule = async (options: LoadAxiosModuleOptions = {}): Promise<LoadedModule> => {
  jest.resetModules();

  if (options.isProd === undefined) {
    delete process.env.JEST_IMPORT_META_PROD;
  } else {
    process.env.JEST_IMPORT_META_PROD = options.isProd ? 'true' : 'false';
  }

  if (options.apiUrl === undefined) {
    delete process.env.JEST_IMPORT_META_VITE_API_URL;
  } else {
    process.env.JEST_IMPORT_META_VITE_API_URL = options.apiUrl;
  }

  let responseSuccessHandler: ((response: any) => any) | undefined;
  let requestHandler: ((config: any) => any) | undefined;
  let responseErrorHandler: ((error: any) => Promise<any>) | undefined;

  const apiInstance: any = jest.fn().mockResolvedValue({ data: { retried: true } });
  apiInstance.interceptors = {
    request: {
      use: jest.fn((handler: (config: any) => any) => {
        requestHandler = handler;
      }),
    },
    response: {
      use: jest.fn((success: (response: any) => any, error: (err: any) => Promise<any>) => {
        responseSuccessHandler = success;
        responseErrorHandler = error;
      }),
    },
  };
  apiInstance.post = jest.fn();
  apiInstance.get = jest.fn();

  const axiosCreate = jest.fn(() => apiInstance);

  jest.doMock('axios', () => ({
    __esModule: true,
    default: {
      create: axiosCreate,
    },
  }));

  const dispatch = jest.fn();
  const getState = jest.fn(() => ({
    auth: {
      refreshToken: options.refreshToken === undefined ? 'refresh-token' : options.refreshToken,
      accessToken: options.accessToken === undefined ? 'access-token' : options.accessToken,
    },
  }));

  jest.doMock('../store', () => ({
    store: {
      dispatch,
      getState,
    },
  }));

  const logout = jest.fn(() => ({ type: 'auth/logout' }));
  const setCredentials = jest.fn((payload) => ({ type: 'auth/setCredentials', payload }));

  jest.doMock('../store/slices/authSlice', () => ({
    logout,
    setCredentials,
  }));

  const module = await import('./axios');

  return {
    api: module.default,
    API_BASE_URL: module.API_BASE_URL,
    apiInstance,
    responseSuccessHandler,
    requestHandler,
    responseErrorHandler,
    dispatch,
    getState,
    logout,
    setCredentials,
  };
};

describe('axios service', () => {
  beforeEach(() => {
    window.history.replaceState({}, '', '/');
  });

  afterEach(() => {
    jest.useRealTimers();
    delete process.env.JEST_IMPORT_META_PROD;
    delete process.env.JEST_IMPORT_META_VITE_API_URL;
  });

  it('registers a passthrough request interceptor and sets API base url', async () => {
    const loaded = await loadAxiosModule();

    expect(loaded.API_BASE_URL).toBeTruthy();
    expect(loaded.requestHandler).toBeDefined();
    expect(loaded.requestHandler?.({ url: '/api/test' })).toEqual({ url: '/api/test' });
  });

  it('normalizes API base url in production when VITE_API_URL is misconfigured', async () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    
    const ipConfigured = await loadAxiosModule({
      isProd: true,
      apiUrl: 'https://35.153.59.2:8080',
    });
    expect(ipConfigured.API_BASE_URL).toBe('https://api.skillsync.mraks.dev');

    const frontendDomainConfigured = await loadAxiosModule({
      isProd: true,
      apiUrl: 'https://skillsync.mraks.dev',
    });
    expect(frontendDomainConfigured.API_BASE_URL).toBe('https://api.skillsync.mraks.dev');

    const fallbackInProd = await loadAxiosModule({ isProd: true, apiUrl: '' });
    expect(fallbackInProd.API_BASE_URL).toBe('https://api.skillsync.mraks.dev');
    
    warnSpy.mockRestore();
  });

  it('parses JSON payloads safely in response success interceptor', async () => {
    const loaded = await loadAxiosModule();

    const objectPayload = loaded.responseSuccessHandler!({ data: { ok: true } });
    expect(objectPayload?.data).toEqual({ ok: true });

    const whitespacePayload = loaded.responseSuccessHandler!({ data: '   ' });
    expect(whitespacePayload?.data).toBe('   ');

    const plainStringPayload = loaded.responseSuccessHandler!({ data: 'not-json' });
    expect(plainStringPayload?.data).toBe('not-json');

    const jsonObjectPayload = loaded.responseSuccessHandler!({ data: '{"ok":true}' });
    expect(jsonObjectPayload?.data).toEqual({ ok: true });

    const jsonArrayPayload = loaded.responseSuccessHandler!({ data: '[1,2,3]' });
    expect(jsonArrayPayload?.data).toEqual([1, 2, 3]);

    const invalidJsonPayload = loaded.responseSuccessHandler!({ data: '{"broken":' });
    expect(invalidJsonPayload?.data).toBe('{"broken":');
  });

  it('skips global redirect handling for auth endpoints with 401', async () => {
    const loaded = await loadAxiosModule();
    const error = {
      config: { url: '/api/auth/login' },
      response: { status: 401 },
    };

    await expect(loaded.responseErrorHandler!(error)).rejects.toBe(error);
    expect(loaded.dispatch).not.toHaveBeenCalled();
  });

  it('skips global auth redirect handling when caller opts out', async () => {
    const loaded = await loadAxiosModule();
    const error = {
      config: { url: '/api/sessions', _skipAuthRedirect: true },
      response: { status: 401 },
    };

    await expect(loaded.responseErrorHandler!(error)).rejects.toBe(error);
    expect(loaded.dispatch).not.toHaveBeenCalled();
  });

  it('refreshes session on 401 and retries original request', async () => {
    const loaded = await loadAxiosModule();

    loaded.apiInstance.post.mockResolvedValueOnce({
      data: {
        user: { id: 1, role: 'ROLE_LEARNER' },
        accessToken: 'new-access',
        refreshToken: 'new-refresh',
      },
    });

    const originalRequest = { url: '/api/sessions', headers: { Authorization: 'Bearer old' } };
    const error = {
      config: originalRequest,
      response: { status: 401 },
    };

    await loaded.responseErrorHandler!(error);

    expect(loaded.apiInstance.post).toHaveBeenCalledWith(
      '/api/auth/refresh',
      { refreshToken: 'refresh-token' },
      expect.any(Object),
    );
    expect(loaded.setCredentials).toHaveBeenCalled();
    expect(loaded.dispatch).toHaveBeenCalled();
    expect(loaded.apiInstance).toHaveBeenCalledWith(expect.objectContaining({ url: '/api/sessions' }));
  });

  it('queues concurrent 401 requests while refresh is in progress', async () => {
    const loaded = await loadAxiosModule();

    let resolveRefresh: ((value: any) => void) | null = null;
    loaded.apiInstance.post.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveRefresh = resolve;
        }),
    );

    const firstRequest = loaded.responseErrorHandler!({
      config: { url: '/api/sessions/first', headers: { Authorization: 'Bearer old' } },
      response: { status: 401 },
    });

    const secondRequest = loaded.responseErrorHandler!({
      config: { url: '/api/sessions/second' },
      response: { status: 401 },
    });

    // TypeScript CFG assumes resolveRefresh is still null here.
    (resolveRefresh as unknown as (value: any) => void)({
      data: {
        user: { id: 1, role: 'ROLE_LEARNER' },
        accessToken: 'queue-access',
        refreshToken: 'queue-refresh',
      },
    });

    await Promise.all([firstRequest, secondRequest]);

    expect(loaded.apiInstance).toHaveBeenCalledWith(expect.objectContaining({ url: '/api/sessions/first' }));
    expect(loaded.apiInstance).toHaveBeenCalledWith(expect.objectContaining({ url: '/api/sessions/second' }));
  });

  it('refreshes without sending refresh payload when token is absent', async () => {
    const loaded = await loadAxiosModule({ refreshToken: null });

    loaded.apiInstance.post.mockResolvedValueOnce({
      data: {
        user: { id: 2, role: 'ROLE_ADMIN' },
        accessToken: 'new-access',
        refreshToken: 'new-refresh',
      },
    });

    await loaded.responseErrorHandler!({
      config: { url: '/api/secure', headers: {} },
      response: { status: 401 },
    });

    expect(loaded.apiInstance.post).toHaveBeenCalledWith('/api/auth/refresh', undefined, expect.any(Object));
  });

  it('retries request even when refresh response does not include user payload', async () => {
    const loaded = await loadAxiosModule();

    loaded.apiInstance.post.mockResolvedValueOnce({ data: { accessToken: 'token-only' } });

    await loaded.responseErrorHandler!({
      config: { url: '/api/groups', headers: {} },
      response: { status: 401 },
    });

    expect(loaded.setCredentials).not.toHaveBeenCalled();
    expect(loaded.apiInstance).toHaveBeenCalledWith(expect.objectContaining({ url: '/api/groups' }));
  });

  it('falls back to empty and current token values when refresh response omits tokens', async () => {
    const loaded = await loadAxiosModule({ refreshToken: 'persisted-refresh' });

    loaded.apiInstance.post.mockResolvedValueOnce({
      data: {
        user: { id: 10, role: 'ROLE_MENTOR' },
      },
    });

    await loaded.responseErrorHandler!({
      config: { url: '/api/mentor/secure', headers: {} },
      response: { status: 401 },
    });

    expect(loaded.setCredentials).toHaveBeenCalledWith(
      expect.objectContaining({
        user: { id: 10, role: 'ROLE_MENTOR' },
        accessToken: '',
        refreshToken: 'persisted-refresh',
      }),
    );
  });

  it('falls back to /api/auth/me after failed refresh and retries request when sanity check succeeds', async () => {
    const loaded = await loadAxiosModule();

    loaded.apiInstance.post.mockRejectedValueOnce({ response: { status: 401 } });
    loaded.apiInstance.get.mockResolvedValueOnce({ data: { id: 99, role: 'ROLE_MENTOR' } });

    const originalRequest = { url: '/api/groups', headers: {} };
    const error = {
      config: originalRequest,
      response: { status: 401 },
    };

    await loaded.responseErrorHandler!(error);

    expect(loaded.apiInstance.get).toHaveBeenCalledWith('/api/auth/me', expect.any(Object));
    expect(loaded.setCredentials).toHaveBeenCalled();
    expect(loaded.dispatch).toHaveBeenCalled();
    expect(loaded.apiInstance).toHaveBeenCalledWith(expect.objectContaining({ url: '/api/groups' }));
    expect(window.location.pathname + window.location.search).toBe('/');
  });

  it('uses empty token fallbacks when sanity-check user is restored without persisted tokens', async () => {
    const loaded = await loadAxiosModule({ refreshToken: null, accessToken: null });

    loaded.apiInstance.post.mockRejectedValueOnce({ response: { status: 401 } });
    loaded.apiInstance.get.mockResolvedValueOnce({ data: { id: 55, role: 'ROLE_LEARNER' } });

    await loaded.responseErrorHandler!({
      config: { url: '/api/learn', headers: {} },
      response: { status: 401 },
    });

    expect(loaded.setCredentials).toHaveBeenCalledWith(
      expect.objectContaining({
        user: { id: 55, role: 'ROLE_LEARNER' },
        accessToken: '',
        refreshToken: '',
      }),
    );
  });

  it('logs out when sanity-check user response is empty after refresh failure', async () => {
    const loaded = await loadAxiosModule();

    loaded.apiInstance.post.mockRejectedValueOnce({ response: { status: 401 } });
    loaded.apiInstance.get.mockResolvedValueOnce({ data: null });

    await expect(
      loaded.responseErrorHandler!({
        config: { url: '/api/secure', headers: {} },
        response: { status: 401 },
      }),
    ).rejects.toBeTruthy();

    expect(loaded.logout).toHaveBeenCalled();
    expect(window.location.pathname + window.location.search).toBe('/login?reason=session_expired');
  });

  it('logs out and redirects to login when refresh and sanity check fail', async () => {
    const loaded = await loadAxiosModule();

    loaded.apiInstance.post.mockRejectedValueOnce({ response: { status: 403 } });
    loaded.apiInstance.get.mockRejectedValueOnce(new Error('me failed'));

    const originalRequest = { url: '/api/secure', headers: {} };
    const error = { config: originalRequest, response: { status: 401 } };

    await expect(loaded.responseErrorHandler!(error)).rejects.toBeTruthy();

    expect(loaded.logout).toHaveBeenCalled();
    expect(loaded.dispatch).toHaveBeenCalledWith({ type: 'auth/logout' });
    expect(window.location.pathname + window.location.search).toBe('/login?reason=session_expired');
  });

  it('does not force logout when refresh fails with non-session status', async () => {
    const loaded = await loadAxiosModule();

    loaded.apiInstance.post.mockRejectedValueOnce({ response: { status: 500 } });

    await expect(
      loaded.responseErrorHandler!({
        config: { url: '/api/secure', headers: {} },
        response: { status: 401 },
      }),
    ).rejects.toBeTruthy();

    expect(loaded.logout).not.toHaveBeenCalled();
  });

  it('redirects to unauthorized for 403 and to 500 for server errors', async () => {
    const loaded = await loadAxiosModule();

    await expect(loaded.responseErrorHandler!({ config: { url: '/api/users' }, response: { status: 403 } })).rejects.toBeTruthy();
    expect(window.location.pathname + window.location.search).toBe('/unauthorized');

    await expect(loaded.responseErrorHandler!({ config: { url: '/api/users' }, response: { status: 500 } })).rejects.toBeTruthy();
    expect(window.location.pathname + window.location.search).toBe('/500');
  });

  it('resolves missing config/url safely for non-auth failures', async () => {
    const loaded = await loadAxiosModule();

    await expect(loaded.responseErrorHandler!({ response: { status: 500 } })).rejects.toBeTruthy();
    expect(window.location.pathname + window.location.search).toBe('/500');
  });

  it('retries 429 and 503 responses with exponential backoff up to 3 attempts', async () => {
    jest.useFakeTimers();
    const loaded = await loadAxiosModule();

    const retryableRequest = { url: '/api/notifications', _retryCount: 0, headers: {} };

    const pending429 = loaded.responseErrorHandler!({ config: retryableRequest, response: { status: 429 } });
    jest.advanceTimersByTime(1000);
    await pending429;

    const pending503 = loaded.responseErrorHandler!({ config: { ...retryableRequest, _retryCount: 1 }, response: { status: 503 } });
    jest.advanceTimersByTime(2000);
    await pending503;

    expect(loaded.apiInstance).toHaveBeenCalledTimes(2);

    await expect(
      loaded.responseErrorHandler!({ config: { ...retryableRequest, _retryCount: 3 }, response: { status: 503 } }),
    ).rejects.toBeTruthy();
  });

  it('retries original request after removing Authorization header', async () => {
    const loaded = await loadAxiosModule();
    loaded.apiInstance.post.mockResolvedValueOnce({
      data: {
        user: { id: 1, role: 'ROLE_LEARNER' },
        accessToken: 'new-access',
        refreshToken: 'new-refresh',
      },
    });
    const originalRequest = { url: '/api/secure', headers: { Authorization: 'Bearer old' } };
    const error = {
      config: originalRequest,
      response: { status: 401 },
    };
    await loaded.responseErrorHandler!(error);
    expect(originalRequest.headers.Authorization).toBeUndefined();
    expect(loaded.apiInstance).toHaveBeenCalledWith(expect.objectContaining({ url: '/api/secure' }));
  });
});

import axios from 'axios';
import { store } from '../store';
import { setCredentials, logout } from '../store/slices/authSlice';

const api = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' },
});

// REQUEST INTERCEPTOR — attach access token
api.interceptors.request.use((config) => {
  const token = store.getState().auth.accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// RESPONSE INTERCEPTOR — handle 401 with silent token refresh + retry
let isRefreshing = false;
let failedQueue: Array<{ resolve: (token: string) => void; reject: (err: any) => void }> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token!);
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Skip refresh loop for the refresh endpoint itself
    if (error.response?.status === 401 && !originalRequest._retry && originalRequest.url !== '/api/auth/refresh') {
      originalRequest._retry = true;

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        });
      }

      isRefreshing = true;
      const refreshToken = store.getState().auth.refreshToken;

      if (!refreshToken) {
        store.dispatch(logout());
        window.location.href = '/login?reason=session_expired';
        return Promise.reject(error);
      }

      try {
        const { data } = await axios.post('/api/auth/refresh', { refreshToken });
        store.dispatch(setCredentials({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          user: store.getState().auth.user,
        }));
        processQueue(null, data.accessToken);
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        store.dispatch(logout());
        localStorage.clear();
        window.location.href = '/login?reason=session_expired';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // 403 — role forbidden
    if (error.response?.status === 403) {
      window.location.href = '/unauthorized';
    }

    // 429 / 503 — exponential backoff retry (max 3 times)
    const retryCount = originalRequest._retryCount || 0;
    if ([429, 503].includes(error.response?.status) && retryCount < 3) {
      originalRequest._retryCount = retryCount + 1;
      const delay = Math.pow(2, retryCount) * 1000;
      await new Promise((res) => setTimeout(res, delay));
      return api(originalRequest);
    }

    return Promise.reject(error);
  }
);

export default api;

import axios from 'axios';
import { store } from '../store';
import { logout } from '../store/slices/authSlice';

// Strongly force HTTPS in production to prevent Vercel ENV leaks pointing to raw IPs.
const isProd = import.meta.env.PROD;
let configuredUrl = import.meta.env.VITE_API_URL;

// Correct legacy or raw IP configurations inside production builds automatically.
if (isProd && configuredUrl && configuredUrl.includes('35.153.59.2')) {
    configuredUrl = 'https://api.skillsync.mraks.dev';
}

// CRITICAL: If configuredUrl points to the frontend domain (skillsync.mraks.dev, Vercel),
// redirect to the actual API domain (api.skillsync.mraks.dev, EC2). This handles
// misconfigured VITE_API_URL in Vercel deployment settings.
if (isProd && configuredUrl && new URL(configuredUrl).hostname === 'skillsync.mraks.dev') {
    console.warn('[CORS FIX] Detected misconfigured API URL pointing to frontend domain. Redirecting to API Gateway...');
    configuredUrl = 'https://api.skillsync.mraks.dev';
}

const API_BASE_URL = configuredUrl || (isProd ? 'https://api.skillsync.mraks.dev' : 'http://localhost:8080');

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

// REQUEST INTERCEPTOR — we no longer manually attach the token.
// The browser will automatically send the HttpOnly 'accessToken' cookie.
api.interceptors.request.use((config) => {
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

      try {
        await api.post('/api/auth/refresh');
        
        // Refresh was successful, but tokens are handled by cookies, not response body
        // Just retry the queue.
        processQueue(null, '');
        
        // Re-run original request without altering authorization header
        delete originalRequest.headers.Authorization;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        store.dispatch(logout());
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

    // 500 — server error fallback message (skip if caller opted out)
    if (error.response?.status >= 500 && error.response?.status !== 503 && !originalRequest._skipErrorRedirect) {
      window.location.href = '/500';
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

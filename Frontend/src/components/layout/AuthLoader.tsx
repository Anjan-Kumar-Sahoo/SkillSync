import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { setCredentials } from '../../store/slices/authSlice';
import { useLocation } from 'react-router-dom';
import api from '../../services/axios';
import type { RootState } from '../../store';
import type { ReactNode } from 'react';
import type { AxiosRequestConfig } from 'axios';

type InternalRequestOptions = AxiosRequestConfig & {
  _skipErrorRedirect: boolean;
  _skipAuthRedirect: boolean;
};

const internalRequestOptions: InternalRequestOptions = {
  _skipErrorRedirect: true,
  _skipAuthRedirect: true,
};

export const AuthLoader = ({ children }: { children: ReactNode }) => {
  const dispatch = useDispatch();
  const location = useLocation();
  const user = useSelector((state: RootState) => state.auth.user);
  const accessToken = useSelector((state: RootState) => state.auth.accessToken);
  const refreshToken = useSelector((state: RootState) => state.auth.refreshToken);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;

    const initAuth = async () => {
      const path = location.pathname;
      const isPublicPath =
        path === '/' ||
        path === '/ppt' ||
        path === '/feppt' ||
        path === '/skillsync_study_guide.html' ||
        path === '/login' ||
        path === '/register' ||
        path === '/verify-otp' ||
        path === '/reset-password' ||
        path === '/setup-password' ||
        path === '/forgot-password';

      if (isPublicPath) {
        if (mounted) setLoading(false);
        return;
      }

      // If user isn't loaded yet, try to fetch identity using cookies
      if (!user) {
        const loadCurrentUser = async () => {
          const { data } = await api.get('/api/auth/me', internalRequestOptions);

          if (mounted && data) {
            dispatch(setCredentials({
              accessToken: accessToken ?? undefined,
              refreshToken: refreshToken ?? undefined,
              user: data,
            }));
          }
        };

        try {
          // Use /api/auth/me which extracts user from JWT cookie and returns UserSummary with role
          await loadCurrentUser();
        } catch (error) {
          const status =
            typeof error === 'object' && error !== null && 'response' in error
              ? (error as { response?: { status?: number } }).response?.status
              : undefined;

          if (status === 401) {
            try {
              // Access token may be expired (24h). Try refresh cookie (7d) and retry /me once.
              await api.post(
                '/api/auth/refresh',
                undefined,
                internalRequestOptions,
              );
              await loadCurrentUser();
            } catch {
              console.error('User not authenticated on load - requires login');
            }
          } else {
            console.error('User not authenticated on load - requires login');
          }
        }
      }
      
      if (mounted) setLoading(false);
    };

    initAuth();

    return () => { mounted = false; };
  }, [dispatch, user, accessToken, refreshToken, location.pathname]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-surface">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
      </div>
    );
  }

  return <>{children}</>;
};

export default AuthLoader;

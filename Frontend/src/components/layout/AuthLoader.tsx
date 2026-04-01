import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { setCredentials } from '../../store/slices/authSlice';
import api from '../../services/axios';
import type { RootState } from '../../store';
import type { ReactNode } from 'react';

export const AuthLoader = ({ children }: { children: ReactNode }) => {
  const dispatch = useDispatch();
  const user = useSelector((state: RootState) => state.auth.user);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;

    const initAuth = async () => {
      // If user isn't loaded yet, try to fetch profile using cookies
      if (!user) {
        try {
          const { data } = await api.get('/api/users/profile');
          if (mounted) {
            dispatch(setCredentials({ accessToken: '', refreshToken: '', user: data }));
          }
        } catch (error) {
          console.error('User not authenticated on load - requires login');
        }
      }
      
      if (mounted) setLoading(false);
    };

    initAuth();

    return () => { mounted = false; };
  }, [dispatch, user]);

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

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation } from '@tanstack/react-query';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { setCredentials } from '../../store/slices/authSlice';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';
import logo from '../../assets/skillsync-logo.png';

const LoginPage = () => {
  const { register, handleSubmit, formState: { errors } } = useForm();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { showToast } = useToast();
  const [searchParams, setSearchParams] = useSearchParams();

  useEffect(() => {
    if (searchParams.get('reason') === 'session_expired') {
      showToast({ message: 'Your session expired. Please sign in again.', type: 'error' });
      searchParams.delete('reason');
      setSearchParams(searchParams, { replace: true });
    }
  }, [searchParams, setSearchParams, showToast]);

  const loginMutation = useMutation({
    mutationFn: async (data: any) => {
      const response = await api.post('/api/auth/login', data);
      return response.data;
    },
    onSuccess: (data) => {
      dispatch(setCredentials({
        user: data.user,
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      }));
      
      const role = data.user.role;
      if (role === 'ROLE_ADMIN') navigate('/admin');
      else if (role === 'ROLE_MENTOR') navigate('/mentor');
      else navigate('/learner');
    },
    onError: () => {
      showToast({ message: 'Invalid credentials. Please try again.', type: 'error' });
    }
  });

  const oauthMutation = useMutation({
    mutationFn: async (profile: any) => {
      const response = await api.post('/api/auth/oauth-login', profile);
      return response.data;
    },
    onSuccess: (data, variables) => {
      if (data.passwordSetupRequired) {
        navigate('/setup-password', { state: { email: variables.email } });
      } else {
        dispatch(setCredentials({
          user: data.user,
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
        }));
        const role = data.user.role;
        if (role === 'ROLE_ADMIN') navigate('/admin');
        else if (role === 'ROLE_MENTOR') navigate('/mentor');
        else navigate('/learner');
      }
    },
    onError: () => {
      showToast({ message: 'OAuth login failed.', type: 'error' });
    }
  });

  const onSubmit = (data: any) => {
    loginMutation.mutate(data);
  };

  const handleGoogleLogin = () => {
    // Mock Google Profile for Viva Demo
    const mockProfile = {
      provider: 'google',
      providerId: 'google-12345',
      email: 'demo@google.com',
      firstName: 'Demo',
      lastName: 'User'
    };
    oauthMutation.mutate(mockProfile);
  };

  return (
    <div className="flex flex-col items-center">
      <img src={logo} alt="SkillSync" className="w-20 h-20 mb-4 hover:scale-105 transition duration-300" onError={(e: any) => { e.target.src = 'https://via.placeholder.com/80?text=Logo'; }} />
      <h1 className="text-3xl font-extrabold tracking-tight text-on-surface mb-2">SkillSync</h1>
      <p className="text-sm text-on-surface-variant font-medium mb-8 text-center px-4">Accelerate your growth through expert mentorship.</p>

      <div className="w-full bg-surface-container-lowest p-8 md:p-10 rounded-xl shadow-sm border border-outline-variant/15 transition-all">
        <h2 className="text-xl font-bold text-on-surface mb-6">Welcome back</h2>
        
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="text-sm font-semibold text-on-surface-variant block mb-1">Email</label>
            <input 
              type="email" 
              {...register('email', { required: 'Email is required' })} 
              className="w-full h-12 px-4 bg-surface-container-low border border-outline-variant/30 rounded-xl focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all duration-200" 
              placeholder="name@example.com"
            />
            {errors.email && <p className="text-xs text-error mt-1">{errors.email.message as string}</p>}
          </div>

          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="text-sm font-semibold text-on-surface-variant">Password</label>
              <Link to="/forgot-password" className="text-sm font-bold text-primary hover:underline">Forgot password?</Link>
            </div>
            <input 
              type="password" 
              {...register('password', { required: 'Password is required' })} 
              className="w-full h-12 px-4 bg-surface-container-low border border-outline-variant/30 rounded-xl focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all duration-200" 
              placeholder="••••••••"
            />
            {errors.password && <p className="text-xs text-error mt-1">{errors.password.message as string}</p>}
          </div>

          {loginMutation.isError && <p className="text-xs text-error mt-2">Error connecting to server. Please try again.</p>}

          <button 
            type="submit" 
            disabled={loginMutation.isPending} 
            className="mt-6 flex items-center justify-center w-full h-12 gradient-btn text-white font-bold rounded-xl shadow-md hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all duration-500 disabled:opacity-70 disabled:scale-100"
          >
            {loginMutation.isPending ? (
              <span className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></span>
            ) : (
              <>
                Sign In 
                <svg className="w-5 h-5 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3"/>
                </svg>
              </>
            )}
          </button>
        </form>

        <div className="mt-6 flex items-center justify-center space-x-4">
          <div className="flex-1 h-px bg-outline-variant/30"></div>
          <span className="text-xs font-semibold text-on-surface-variant tracking-wider uppercase">OR CONTINUE WITH</span>
          <div className="flex-1 h-px bg-outline-variant/30"></div>
        </div>

        <button 
          onClick={handleGoogleLogin} 
          disabled={oauthMutation.isPending}
          className="mt-6 flex items-center justify-center w-full h-12 bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-bold rounded-xl shadow-sm border border-outline-variant/30 transition-all duration-200"
        >
          {oauthMutation.isPending ? (
            <span className="animate-spin rounded-full h-5 w-5 border-b-2 border-on-surface"></span>
          ) : (
            <>
              <svg className="w-5 h-5 mr-3" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
              </svg>
              Google
            </>
          )}
        </button>

        <p className="mt-6 text-center text-sm font-semibold text-on-surface-variant">
          Don't have an account? <Link to="/register" className="text-primary hover:underline">Sign up</Link>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;

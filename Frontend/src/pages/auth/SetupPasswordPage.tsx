import { useForm } from 'react-hook-form';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';

const SetupPasswordPage = () => {
  const { register, handleSubmit, watch, formState: { errors } } = useForm();
  const location = useLocation();
  const navigate = useNavigate();
  const { showToast } = useToast();
  
  const email = location.state?.email;

  const setupMutation = useMutation({
    mutationFn: async (data: any) => {
      const response = await api.post('/api/auth/setup-password', data);
      return response.data;
    },
    onSuccess: () => {
      showToast({ message: 'Password set! You can now log in.', type: 'success' });
      navigate('/login');
    },
    onError: () => {
      showToast({ message: 'Failed to set password. Please try again.', type: 'error' });
    }
  });

  const onSubmit = (data: any) => {
    if (!email) {
      showToast({ message: 'Session invalid. Please try logging in again.', type: 'error' });
      navigate('/login');
      return;
    }
    setupMutation.mutate({ email, password: data.password });
  };

  const passwordValidation = {
    required: 'Password is required',
    minLength: { value: 8, message: 'Minimum 8 characters' },
    pattern: {
      value: /^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/,
      message: 'Need 1 uppercase, 1 digit, 1 special char'
    }
  };

  return (
    <div className="flex flex-col items-center">
      <div className="w-full bg-surface-container-lowest p-8 md:p-10 rounded-xl shadow-sm border border-outline-variant/15 transition-all">
        <h2 className="text-xl font-bold text-on-surface mb-2">Set your password</h2>
        <p className="text-sm text-on-surface-variant font-medium mb-6">
          You signed in with Google. Please set a password to also enable email login.
        </p>
        
        {email && (
          <p className="text-sm text-on-surface font-semibold mb-4 bg-surface px-3 py-2 rounded-md">
            {email}
          </p>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="text-sm font-semibold text-on-surface-variant block mb-1">Password</label>
            <input 
              type="password" 
              {...register('password', passwordValidation)} 
              className="w-full h-12 px-4 bg-surface-container-low border border-outline-variant/30 rounded-xl focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all duration-200" 
            />
            {errors.password && <p className="text-xs text-error mt-1">{errors.password.message as string}</p>}
          </div>

          <div>
            <label className="text-sm font-semibold text-on-surface-variant block mb-1">Confirm Password</label>
            <input 
              type="password" 
              {...register('confirmPassword', { 
                required: 'Required',
                validate: (val) => val === watch('password') || 'Passwords do not match'
              })} 
              className="w-full h-12 px-4 bg-surface-container-low border border-outline-variant/30 rounded-xl focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all duration-200" 
            />
            {errors.confirmPassword && <p className="text-xs text-error mt-1">{errors.confirmPassword.message as string}</p>}
          </div>

          <button 
            type="submit" 
            disabled={setupMutation.isPending || !email} 
            className="mt-6 flex items-center justify-center w-full h-12 gradient-btn text-white font-bold rounded-xl shadow-md hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all duration-500 disabled:opacity-70 disabled:scale-100"
          >
            {setupMutation.isPending ? (
              <span className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></span>
            ) : 'Save Password'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default SetupPasswordPage;

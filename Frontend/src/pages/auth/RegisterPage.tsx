import { useForm } from 'react-hook-form';
import { useMutation } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';
import logo from '../../assets/skillsync-logo.png';

const RegisterPage = () => {
  const { register, handleSubmit, watch, formState: { errors } } = useForm();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const registerMutation = useMutation({
    mutationFn: async (data: any) => {
      const response = await api.post('/api/auth/register', data);
      return response.data;
    },
    onSuccess: (_, variables) => {
      showToast({ message: 'Registration successful. Please verify your email.' });
      navigate('/verify-otp', { state: { email: variables.email } });
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Registration failed. Try again.';
      showToast({ message, type: 'error' });
    }
  });

  const onSubmit = (data: any) => {
    registerMutation.mutate({
      firstName: data.firstName,
      lastName: data.lastName,
      email: data.email,
      password: data.password
    });
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
      <div className="flex items-center gap-3 mb-6 group transition-all">
        <img 
          src={logo} 
          alt="SkillSync Logo" 
          className="w-12 h-12 object-contain hover:scale-110 transition duration-500" 
          onError={(e: any) => { e.target.src = 'https://via.placeholder.com/48?text=S'; }} 
        />
        <h1 className="text-4xl font-black tracking-tighter text-on-surface text-center">SkillSync</h1>
      </div>

      <div className="w-full bg-surface-container-lowest p-8 md:p-10 rounded-xl shadow-sm border border-outline-variant/15 transition-all">
        <h2 className="text-xl font-bold text-on-surface mb-6">Create your account</h2>
        
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-semibold text-on-surface-variant block mb-1">First Name</label>
              <input 
                {...register('firstName', { required: 'Required' })} 
                className="w-full h-12 px-4 bg-surface-container-low border border-outline-variant/30 rounded-xl focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all duration-200" 
              />
              {errors.firstName && <p className="text-xs text-error mt-1">{errors.firstName.message as string}</p>}
            </div>
            <div>
              <label className="text-sm font-semibold text-on-surface-variant block mb-1">Last Name</label>
              <input 
                {...register('lastName', { required: 'Required' })} 
                className="w-full h-12 px-4 bg-surface-container-low border border-outline-variant/30 rounded-xl focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all duration-200" 
              />
              {errors.lastName && <p className="text-xs text-error mt-1">{errors.lastName.message as string}</p>}
            </div>
          </div>

          <div>
            <label className="text-sm font-semibold text-on-surface-variant block mb-1">Email</label>
            <input 
              type="email" 
              {...register('email', { required: 'Required' })} 
              className="w-full h-12 px-4 bg-surface-container-low border border-outline-variant/30 rounded-xl focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all duration-200" 
            />
            {errors.email && <p className="text-xs text-error mt-1">{errors.email.message as string}</p>}
          </div>

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
            disabled={registerMutation.isPending} 
            className="mt-6 flex items-center justify-center w-full h-12 gradient-btn text-white font-bold rounded-xl shadow-md hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all duration-500 disabled:opacity-70 disabled:scale-100"
          >
            {registerMutation.isPending ? (
              <span className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></span>
            ) : 'Register'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm font-semibold text-on-surface-variant">
          Already have an account? <Link to="/login" className="text-primary hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  );
};

export default RegisterPage;

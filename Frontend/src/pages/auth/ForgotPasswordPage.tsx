import { useForm } from 'react-hook-form';
import { useMutation } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';

interface ForgotPasswordForm {
  email: string;
}

const ForgotPasswordPage = () => {
  const { register, handleSubmit, formState: { errors }, reset } = useForm<ForgotPasswordForm>();
  const { showToast } = useToast();

  const forgotPasswordMutation = useMutation({
    mutationFn: async (data: ForgotPasswordForm) => {
      const response = await api.post('/api/auth/forgot-password', data, { _skipErrorRedirect: true } as any);
      return response.data;
    },
    onSuccess: () => {
      showToast({
        message: 'If your email exists, a reset OTP has been sent.',
        type: 'success',
      });
      reset();
    },
    onError: () => {
      showToast({
        message: 'Could not process request right now. Please try again.',
        type: 'error',
      });
    },
  });

  const onSubmit = (data: ForgotPasswordForm) => {
    forgotPasswordMutation.mutate(data);
  };

  return (
    <div className="w-full max-w-md bg-surface-container-lowest p-8 md:p-10 rounded-xl shadow-sm border border-outline-variant/15">
      <h2 className="text-xl font-bold text-on-surface mb-2">Forgot Password</h2>
      <p className="text-sm text-on-surface-variant mb-6">
        Enter your account email and we will send you a password reset OTP.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="text-sm font-semibold text-on-surface-variant block mb-1">Email</label>
          <input
            type="email"
            {...register('email', {
              required: 'Email is required',
              pattern: { value: /^\S+@\S+\.\S+$/, message: 'Enter a valid email' },
            })}
            className="w-full h-12 px-4 bg-surface-container-low border border-outline-variant/30 rounded-xl focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all"
            placeholder="name@example.com"
          />
          {errors.email && <p className="text-xs text-error mt-1">{errors.email.message}</p>}
        </div>

        <button
          type="submit"
          disabled={forgotPasswordMutation.isPending}
          className="w-full h-12 gradient-btn text-white font-bold rounded-xl shadow-md disabled:opacity-70"
        >
          {forgotPasswordMutation.isPending ? 'Sending OTP...' : 'Send Reset OTP'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm font-semibold text-on-surface-variant">
        Back to <Link to="/login" className="text-primary hover:underline">Sign In</Link>
      </p>
    </div>
  );
};

export default ForgotPasswordPage;

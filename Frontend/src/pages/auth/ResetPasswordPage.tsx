import { useEffect, useRef, useState } from 'react';
import type { KeyboardEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';

const ResetPasswordPage = () => {
  const [otp, setOtp] = useState<string[]>(Array(6).fill(''));
  const [timeLeft, setTimeLeft] = useState(300);
  const [otpVerified, setOtpVerified] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const inputRefs = useRef<(HTMLInputElement | null)[]>(Array(6).fill(null));

  const location = useLocation();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const email = location.state?.email || '';
  const hasValidEmail = /^\S+@\S+\.\S+$/.test(email);

  useEffect(() => {
    if (hasValidEmail) return;
    showToast({ message: 'Session expired. Please restart forgot password.', type: 'error' });
    navigate('/forgot-password', { replace: true });
  }, [hasValidEmail, navigate, showToast]);

  useEffect(() => {
    if (timeLeft <= 0) return;
    const timer = setInterval(() => {
      setTimeLeft((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [timeLeft]);

  const constraints = [
    { label: 'At least 8 characters', met: newPassword.length >= 8 },
    { label: 'One uppercase letter', met: /[A-Z]/.test(newPassword) },
    { label: 'One lowercase letter', met: /[a-z]/.test(newPassword) },
    { label: 'One number', met: /\d/.test(newPassword) },
    { label: 'One special character', met: /[^A-Za-z0-9]/.test(newPassword) },
  ];
  const allConstraintsMet = constraints.every((c) => c.met);

  const verifyOtpMutation = useMutation({
    mutationFn: async (payload: { email: string; otp: string }) => {
      const response = await api.post('/api/auth/verify-password-reset-otp', payload, { _skipErrorRedirect: true } as any);
      return response.data;
    },
    onSuccess: () => {
      setOtpVerified(true);
      showToast({ message: 'OTP verified. You can now set a new password.', type: 'success' });
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Invalid OTP. Please try again.';
      showToast({ message, type: 'error' });
    },
  });

  const resetMutation = useMutation({
    mutationFn: async (payload: { email: string; otp: string; newPassword: string }) => {
      const response = await api.post('/api/auth/reset-password', payload, { _skipErrorRedirect: true } as any);
      return response.data;
    },
    onSuccess: () => {
      showToast({ message: 'Password reset successfully. Please log in.', type: 'success' });
      navigate('/login');
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to reset password. Please try again.';
      showToast({ message, type: 'error' });
    },
  });

  const resendMutation = useMutation({
    mutationFn: async () => {
      const response = await api.post('/api/auth/forgot-password', { email }, { _skipErrorRedirect: true } as any);
      return response.data;
    },
    onSuccess: () => {
      setTimeLeft(300);
      setOtp(Array(6).fill(''));
      setOtpVerified(false);
      showToast({ message: 'Reset OTP sent successfully.', type: 'success' });
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to send reset OTP.';
      showToast({ message, type: 'error' });
    },
  });

  const handleOtpChange = (index: number, value: string) => {
    if (isNaN(Number(value))) return;
    const next = [...otp];
    next[index] = value.slice(-1);
    setOtp(next);
    if (value && index < 5) inputRefs.current[index + 1]?.focus();
  };

  const handleOtpKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      if (otp[index] === '' && index > 0) {
        inputRefs.current[index - 1]?.focus();
      } else {
        const next = [...otp];
        next[index] = '';
        setOtp(next);
      }
    }
    if (e.key === 'Enter') handleSubmit();
  };

  const handleSubmit = () => {
    const otpCode = otp.join('');

    if (!otpVerified) {
      showToast({ message: 'Verify OTP first.', type: 'error' });
      return;
    }

    if (otpCode.length !== 6) {
      showToast({ message: 'Please enter all 6 OTP digits.', type: 'error' });
      return;
    }

    if (!allConstraintsMet) {
      showToast({ message: 'Please satisfy all password constraints.', type: 'error' });
      return;
    }

    resetMutation.mutate({ email, otp: otpCode, newPassword });
  };

  const handleVerifyOtp = () => {
    const otpCode = otp.join('');
    if (otpCode.length !== 6) {
      showToast({ message: 'Please enter all 6 OTP digits.', type: 'error' });
      return;
    }

    verifyOtpMutation.mutate({ email, otp: otpCode });
  };

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const isOtpComplete = otp.every((digit) => digit !== '');

  return (
    <div className="flex flex-col items-center">
      <div className="w-full bg-surface-container-lowest p-8 md:p-10 rounded-xl shadow-sm border border-outline-variant/15 transition-all">
        <h2 className="text-xl font-bold text-on-surface mb-2">Reset your password</h2>
        <p className="text-sm text-on-surface-variant font-medium mb-6">
          Enter the 6-digit OTP sent to <span className="font-bold text-on-surface">{email || 'your email'}</span> and set a new password.
        </p>

        <div className="flex justify-between mb-6 space-x-2">
          {otp.map((digit, idx) => (
            <input
              key={idx}
              ref={(el) => {
                inputRefs.current[idx] = el;
              }}
              type="text"
              inputMode="numeric"
              maxLength={1}
              value={digit}
              onChange={(e) => handleOtpChange(idx, e.target.value)}
              onKeyDown={(e) => handleOtpKeyDown(idx, e)}
              className="w-12 h-14 md:w-14 md:h-16 text-center text-xl font-bold bg-surface-container-low border border-outline-variant/30 rounded-xl focus:ring-2 focus:ring-primary focus:border-primary outline-none transition-all duration-200"
              disabled={otpVerified}
            />
          ))}
        </div>

        {!otpVerified ? (
          <button
            onClick={handleVerifyOtp}
            disabled={!isOtpComplete || verifyOtpMutation.isPending || timeLeft === 0}
            className="mt-2 flex items-center justify-center w-full h-12 gradient-btn text-white font-bold rounded-xl shadow-md hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all duration-500 disabled:opacity-70 disabled:scale-100 disabled:shadow-none"
          >
            {verifyOtpMutation.isPending ? <span className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></span> : 'Verify OTP'}
          </button>
        ) : (
          <>
            <div className="mb-3">
              <label className="text-sm font-semibold text-on-surface-variant block mb-1">New Password</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full h-12 pl-4 pr-12 bg-surface-container-low border border-outline-variant/30 rounded-xl focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-all duration-200"
                  placeholder="Enter your new password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 w-9 h-9 rounded-lg hover:bg-surface-container transition-colors text-on-surface-variant"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  <span className="material-symbols-outlined text-[20px]">{showPassword ? 'visibility_off' : 'visibility'}</span>
                </button>
              </div>
            </div>

            <div className="mb-6 rounded-xl border border-outline-variant/20 bg-surface-container-low p-4 space-y-2">
              {constraints.map((constraint) => (
                <p
                  key={constraint.label}
                  className={`flex items-center text-sm font-medium ${constraint.met ? 'text-emerald-600' : 'text-on-surface-variant'}`}
                >
                  <span className="material-symbols-outlined text-[16px] mr-2">{constraint.met ? 'check_circle' : 'radio_button_unchecked'}</span>
                  {constraint.label}
                </p>
              ))}
            </div>

            <button
              onClick={handleSubmit}
              disabled={!allConstraintsMet || resetMutation.isPending}
              className="mt-2 flex items-center justify-center w-full h-12 gradient-btn text-white font-bold rounded-xl shadow-md hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all duration-500 disabled:opacity-70 disabled:scale-100 disabled:shadow-none"
            >
              {resetMutation.isPending ? <span className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></span> : 'Reset Password'}
            </button>
          </>
        )}

        <div className="mt-8 flex flex-col items-center space-y-2">
          <p className="text-sm font-semibold text-on-surface-variant">
            {otpVerified ? 'OTP verified' : (timeLeft > 0 ? `${formatTime(timeLeft)} remaining` : 'Code expired')}
          </p>
          <button
            onClick={() => {
              if (timeLeft > 0 || resendMutation.isPending) return;
              resendMutation.mutate();
            }}
            disabled={otpVerified || timeLeft > 0 || resendMutation.isPending}
            className={`text-sm font-bold ${timeLeft > 0 ? 'text-outline hover:no-underline' : 'text-primary hover:underline'}`}
          >
            {resendMutation.isPending ? 'Sending...' : 'Resend OTP'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ResetPasswordPage;

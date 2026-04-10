import { useEffect, useRef, useState } from 'react';
import type { KeyboardEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useSelector } from 'react-redux';
import api from '../../services/axios';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import type { RootState } from '../../store';

const SettingsPage = () => {
  const { showToast } = useToast();
  const userEmail = useSelector((state: RootState) => state.auth.user?.email || '');

  const [step, setStep] = useState<'email' | 'otp' | 'password'>('email');
  const [email, setEmail] = useState(userEmail);
  const [otp, setOtp] = useState<string[]>(Array(6).fill(''));
  const [timeLeft, setTimeLeft] = useState(0);
  const [newPassword, setNewPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const inputRefs = useRef<(HTMLInputElement | null)[]>(Array(6).fill(null));

  useEffect(() => {
    if (timeLeft <= 0) return;
    const timer = setInterval(() => setTimeLeft((prev) => prev - 1), 1000);
    return () => clearInterval(timer);
  }, [timeLeft]);

  const constraints = [
    { label: 'Minimum 8 characters', met: newPassword.length >= 8 },
    { label: 'One uppercase letter', met: /[A-Z]/.test(newPassword) },
    { label: 'One lowercase letter', met: /[a-z]/.test(newPassword) },
    { label: 'One number', met: /\d/.test(newPassword) },
    { label: 'One special character', met: /[^A-Za-z0-9]/.test(newPassword) },
  ];
  const allConstraintsMet = constraints.every((rule) => rule.met);
  const otpCode = otp.join('');
  const isOtpComplete = otp.every((digit) => digit !== '');

  const sendOtpMutation = useMutation({
    mutationFn: async () => {
      const response = await api.post('/api/auth/forgot-password', { email }, { _skipErrorRedirect: true } as any);
      return response.data;
    },
    onSuccess: () => {
      showToast({ message: 'Password reset OTP sent to your email.', type: 'success' });
      setStep('otp');
      setOtp(Array(6).fill(''));
      setTimeLeft(300);
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to send OTP';
      showToast({ message, type: 'error' });
    },
  });

  const verifyOtpMutation = useMutation({
    mutationFn: async () => {
      const response = await api.post(
        '/api/auth/verify-password-reset-otp',
        { email, otp: otpCode },
        { _skipErrorRedirect: true } as any,
      );
      return response.data;
    },
    onSuccess: () => {
      showToast({ message: 'OTP verified. Set your new password.', type: 'success' });
      setStep('password');
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Invalid OTP';
      showToast({ message, type: 'error' });
    },
  });

  const changePasswordMutation = useMutation({
    mutationFn: async () => {
      const response = await api.post(
        '/api/auth/reset-password',
        {
          email,
          otp: otpCode,
          newPassword,
        },
        { _skipErrorRedirect: true } as any,
      );
      return response.data;
    },
    onSuccess: (data: any) => {
      showToast({ message: data?.message || 'Password updated successfully', type: 'success' });
      setStep('email');
      setOtp(Array(6).fill(''));
      setTimeLeft(0);
      setNewPassword('');
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to change password';
      showToast({ message, type: 'error' });
    },
  });

  const handleSendOtp = (e: React.FormEvent) => {
    e.preventDefault();

    if (!email.trim()) {
      showToast({ message: 'Email is required', type: 'error' });
      return;
    }

    sendOtpMutation.mutate();
  };

  const handleOtpChange = (index: number, value: string) => {
    if (isNaN(Number(value))) return;
    const next = [...otp];
    next[index] = value.slice(-1);
    setOtp(next);

    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleOtpKeyDown = (index: number, event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Backspace') {
      if (otp[index] === '' && index > 0) {
        inputRefs.current[index - 1]?.focus();
      } else {
        const next = [...otp];
        next[index] = '';
        setOtp(next);
      }
    }
    if (event.key === 'Enter') {
      handleVerifyOtp();
    }
  };

  const handleVerifyOtp = () => {
    if (!isOtpComplete) {
      showToast({ message: 'Please enter all 6 OTP digits', type: 'error' });
      return;
    }
    verifyOtpMutation.mutate();
  };

  const handleResendOtp = () => {
    if (timeLeft > 0 || sendOtpMutation.isPending) return;
    sendOtpMutation.mutate();
  };

  const handleChangePassword = (e: React.FormEvent) => {
    e.preventDefault();

    if (!isOtpComplete) {
      showToast({ message: 'OTP is required', type: 'error' });
      return;
    }

    if (!allConstraintsMet) {
      showToast({ message: 'Please satisfy all password constraints', type: 'error' });
      return;
    }

    changePasswordMutation.mutate();
  };

  const formatTime = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    const remaining = seconds % 60;
    return `${minutes}:${remaining < 10 ? '0' : ''}${remaining}`;
  };

  return (
    <PageLayout>
      <div className="max-w-2xl mx-auto">
        <div className="bg-gradient-to-r from-indigo-600 to-blue-600 rounded-lg p-8 text-white">
          <h1 className="text-3xl font-bold">Change Password</h1>
          <p className="text-indigo-100 mt-2">Secure your account with OTP verification.</p>
        </div>

        <div className="bg-surface-container-lowest rounded-lg p-6 shadow-sm border border-outline-variant/20 mt-6">
          <div className="mb-5 flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-on-surface-variant">
            <span className={step === 'email' ? 'text-primary' : ''}>Step 1: Email</span>
            <span>•</span>
            <span className={step === 'otp' ? 'text-primary' : ''}>Step 2: Verify OTP</span>
            <span>•</span>
            <span className={step === 'password' ? 'text-primary' : ''}>Step 3: New Password</span>
          </div>

          {step === 'email' && (
            <form onSubmit={handleSendOtp} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-on-surface mb-1">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-4 py-2 border border-outline-variant/30 rounded-lg bg-surface-container-low text-on-surface focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={sendOtpMutation.isPending}
                className="w-full bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700 transition disabled:opacity-50"
              >
                {sendOtpMutation.isPending ? 'Sending OTP...' : 'Send OTP'}
              </button>
            </form>
          )}

          {step === 'otp' && (
            <div className="space-y-4">
              <div className="flex justify-between gap-2">
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
                    className="w-12 h-14 text-center text-xl font-bold border border-outline-variant/30 rounded-xl bg-surface-container-low text-on-surface focus:ring-2 focus:ring-primary"
                  />
                ))}
              </div>

              <button
                type="button"
                onClick={handleVerifyOtp}
                disabled={!isOtpComplete || verifyOtpMutation.isPending}
                className="w-full bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700 transition disabled:opacity-50"
              >
                {verifyOtpMutation.isPending ? 'Verifying...' : 'Verify OTP'}
              </button>

              <div className="text-center space-y-1">
                <p className="text-sm font-semibold text-on-surface-variant">
                  {timeLeft > 0 ? `${formatTime(timeLeft)} remaining` : 'OTP expired'}
                </p>
                <button
                  type="button"
                  onClick={handleResendOtp}
                  disabled={timeLeft > 0 || sendOtpMutation.isPending}
                  className={`text-sm font-bold ${timeLeft > 0 ? 'text-outline' : 'text-primary hover:underline'}`}
                >
                  {sendOtpMutation.isPending ? 'Resending...' : 'Resend OTP'}
                </button>
              </div>
            </div>
          )}

          {step === 'password' && (
            <form onSubmit={handleChangePassword} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-on-surface mb-1">New Password</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="w-full px-4 py-2 pr-12 border border-outline-variant/30 rounded-lg bg-surface-container-low text-on-surface focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    className="absolute right-2 top-1/2 -translate-y-1/2 h-8 w-8 rounded-md hover:bg-surface-container text-on-surface-variant"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    <span className="material-symbols-outlined text-[20px]">{showPassword ? 'visibility_off' : 'visibility'}</span>
                  </button>
                </div>
              </div>

              <div className="rounded-xl border border-outline-variant/20 bg-surface-container-low p-4 space-y-2">
                {constraints.map((constraint) => (
                  <p
                    key={constraint.label}
                    className={`flex items-center text-sm font-medium ${constraint.met ? 'text-emerald-600' : 'text-on-surface-variant'}`}
                  >
                    <span className="material-symbols-outlined text-[16px] mr-2">
                      {constraint.met ? 'check_circle' : 'radio_button_unchecked'}
                    </span>
                    {constraint.label}
                  </p>
                ))}
              </div>

              <button
                type="submit"
                disabled={changePasswordMutation.isPending || !allConstraintsMet}
                className="w-full bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700 transition disabled:opacity-50"
              >
                {changePasswordMutation.isPending ? 'Updating...' : 'Save New Password'}
              </button>
            </form>
          )}
        </div>
      </div>
    </PageLayout>
  );
};

export default SettingsPage;

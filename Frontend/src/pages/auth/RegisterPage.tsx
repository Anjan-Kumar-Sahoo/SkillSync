import { useState, type KeyboardEvent } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { useGoogleLogin } from '@react-oauth/google';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';
import { setCredentials } from '../../store/slices/authSlice';
import logo from '../../assets/skillsync-logo.png';
import type { AuthResponse, OAuthResponse } from '../../types';

type RegisterFormValues = {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
};

type RegistrationInitResponse = {
  exists?: boolean;
  message?: string;
};

const initialOtp = () => Array(6).fill('') as string[];

const RegisterPage = () => {
  const [step, setStep] = useState<'verify' | 'profile'>('verify');
  const [registeredEmail, setRegisteredEmail] = useState('');
  const [otp, setOtp] = useState<string[]>(initialOtp());
  const [otpSent, setOtpSent] = useState(false);
  const [isEmailVerified, setIsEmailVerified] = useState(false);
  const [userExists, setUserExists] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const { register, handleSubmit, watch, getValues, trigger, formState: { errors } } = useForm<RegisterFormValues>();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { showToast } = useToast();

  const initiateMutation = useMutation({
    mutationFn: async (data: { email: string }) => {
      const response = await api.post('/api/auth/initiate-registration', data);
      return response.data as RegistrationInitResponse;
    },
    onSuccess: (data, variables) => {
      const normalizedEmail = variables.email.trim();
      setRegisteredEmail(normalizedEmail);
      if (data.exists) {
        setUserExists(true);
        setOtpSent(false);
        setIsEmailVerified(false);
        setOtp(initialOtp());
        showToast({ message: 'Email already registered', type: 'info' });
      } else {
        setUserExists(false);
        setOtpSent(true);
        setIsEmailVerified(false);
        setOtp(initialOtp());
        showToast({ message: data.message || 'OTP sent to your email.' });
      }
    },
    onError: () => showToast({ message: 'Failed to initiate registration.', type: 'error' })
  });

  const verifyMutation = useMutation({
    mutationFn: async (data: { email: string; otp: string }) => {
      const response = await api.post('/api/auth/verify-otp', data);
      return response.data;
    },
    onSuccess: () => {
      setIsEmailVerified(true);
      showToast({ message: 'Email verified successfully!', type: 'success' });
      setStep('profile');
    },
    onError: () => showToast({ message: 'Invalid OTP.', type: 'error' })
  });

  const completeMutation = useMutation({
    mutationFn: async (data: Pick<RegisterFormValues, 'firstName' | 'lastName' | 'password'>) => {
      const response = await api.post('/api/auth/complete-registration', { ...data, email: registeredEmail });
      return response.data as AuthResponse;
    },
    onSuccess: (data) => {
      if (data?.user) {
        dispatch(setCredentials({
          user: data.user,
          accessToken: data.accessToken || '',
          refreshToken: data.refreshToken || '',
        }));
        showToast({ message: 'Registration complete! Welcome to SkillSync.', type: 'success' });
        navigate('/dashboard', { replace: true });
        return;
      }

      showToast({ message: 'Registration complete! Please sign in to continue.', type: 'success' });
      navigate('/login', { replace: true });
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      const message = error.response?.data?.message || 'Failed to complete registration.';
      showToast({ message, type: 'error' });
    }
  });

  const oauthMutation = useMutation({
    mutationFn: async (profile: {
      provider: string;
      providerId: string;
      email: string;
      firstName: string;
      lastName: string;
    }) => {
      const response = await api.post('/api/auth/oauth-login', profile);
      return response.data as OAuthResponse;
    },
    onSuccess: (data, variables) => {
      if (data.passwordSetupRequired) {
        navigate('/setup-password', { state: { email: variables.email } });
      } else {
        dispatch(setCredentials({
          user: data.user,
          accessToken: data.accessToken || '',
          refreshToken: data.refreshToken || '',
        }));
        navigate('/dashboard', { replace: true });
      }
    },
    onError: () => showToast({ message: 'OAuth login failed.', type: 'error' })
  });

  const handleGoogleLogin = useGoogleLogin({
    onSuccess: async (tokenResponse) => {
      try {
        const userInfo = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
          headers: { Authorization: `Bearer ${tokenResponse.access_token}` },
        }).then(res => res.json());

        oauthMutation.mutate({
          provider: 'google', providerId: `google-${userInfo.sub}`,
          email: userInfo.email, firstName: userInfo.given_name, lastName: userInfo.family_name
        });
      } catch (err) {
        showToast({ message: 'Failed to fetch Google profile.', type: 'error' });
      }
    }
  });

  const currentPassword = watch('password', '');
  const constraints = [
    { label: 'At least 8 characters', met: currentPassword.length >= 8 },
    { label: 'One uppercase letter', met: /[A-Z]/.test(currentPassword) },
    { label: 'One lowercase letter', met: /[a-z]/.test(currentPassword) },
    { label: 'One number', met: /\d/.test(currentPassword) },
    { label: 'One special character', met: /[@$!%*?&#]/.test(currentPassword) }
  ];

  const handleConstraintClass = (met: boolean) =>
    `flex items-center text-xs font-semibold transition-colors ${met ? 'text-primary' : 'text-on-surface-variant'}`;

  const handleOtpChange = (index: number, value: string) => {
    if (value.length > 1) value = value.slice(-1);
    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);
    if (value && index < 5) {
      document.getElementById(`otp-${index + 1}`)?.focus();
    }
  };

  const handleOtpKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      document.getElementById(`otp-${index - 1}`)?.focus();
    }
  };

  const submitOtp = () => {
    const otpCode = otp.join('');
    if (otpCode.length === 6) {
      verifyMutation.mutate({ email: registeredEmail || getValues('email'), otp: otpCode });
    }
    else showToast({ message: 'Please enter all 6 digits', type: 'error' });
  };

  const sendOtp = async () => {
    const isValidEmail = await trigger('email');
    if (!isValidEmail) return;

    const email = getValues('email').trim();
    if (!email) {
      showToast({ message: 'Please enter your email address.', type: 'error' });
      return;
    }

    initiateMutation.mutate({ email });
  };

  const onSubmitProfile = (data: RegisterFormValues) => {
    if (!isEmailVerified) {
      showToast({ message: 'Please verify your email first.', type: 'error' });
      setStep('verify');
      return;
    }

    if (!constraints.every(c => c.met)) {
      return showToast({ message: 'Please meet all password constraints', type: 'error' });
    }

    completeMutation.mutate({
      firstName: data.firstName,
      lastName: data.lastName,
      password: data.password,
    });
  };

  const resetVerificationFlow = () => {
    setStep('verify');
    setOtpSent(false);
    setIsEmailVerified(false);
    setUserExists(false);
    setRegisteredEmail('');
    setOtp(initialOtp());
  };

  return (
    <div className="flex flex-col items-center">
      <div className="flex items-center gap-3 mb-6 group transition-all">
        <img src={logo} alt="SkillSync Logo" className="w-12 h-12 object-contain hover:scale-110" />
        <h1 className="text-4xl font-black tracking-tighter text-on-surface text-center">SkillSync</h1>
      </div>

      <div className="w-full bg-surface-container-lowest p-8 md:p-10 rounded-xl shadow-sm border border-outline-variant/15 transition-all">
        {step === 'verify' && (
          <>
            <h2 className="text-xl font-bold text-on-surface mb-2">Register an account</h2>
            <p className="text-sm text-on-surface-variant mb-6">Verify your email first. OTP will be sent on this page itself.</p>

            {!userExists && (
              <div className="space-y-4">
                <div>
                  <label className="text-sm font-semibold text-on-surface-variant block mb-1">Email</label>
                  <input
                    type="email"
                    {...register('email', { required: 'Email is required' })}
                    disabled={otpSent || isEmailVerified}
                    className="w-full h-12 px-4 bg-surface-container-low border rounded-xl outline-none focus:ring-1 focus:ring-primary disabled:opacity-60"
                  />
                  {errors.email && <p className="text-xs text-error mt-1">{errors.email.message}</p>}
                </div>

                <button
                  type="button"
                  onClick={sendOtp}
                  disabled={initiateMutation.isPending || otpSent || isEmailVerified}
                  className="mt-4 w-full h-12 gradient-btn text-white font-bold rounded-xl shadow-md hover:shadow-xl transition-all disabled:opacity-70">
                  {initiateMutation.isPending ? 'Sending OTP...' : otpSent ? 'OTP Sent' : 'Verify Email'}
                </button>

                {otpSent && !isEmailVerified && (
                  <div className="rounded-xl border border-primary/20 bg-primary/5 p-4">
                    <p className="text-sm font-semibold text-on-surface mb-3">OTP sent on your email: {registeredEmail}</p>
                    <div className="flex justify-center gap-2 mb-4">
                      {otp.map((digit, i) => (
                        <input
                          key={i}
                          id={`otp-${i}`}
                          type="text"
                          value={digit}
                          onChange={(e) => handleOtpChange(i, e.target.value)}
                          onKeyDown={(e) => handleOtpKeyDown(i, e)}
                          maxLength={1}
                          className="w-11 h-12 text-center text-lg font-bold bg-surface-container-high border rounded-lg focus:border-primary focus:ring-1 outline-none"
                        />
                      ))}
                    </div>

                    <button
                      type="button"
                      onClick={submitOtp}
                      disabled={verifyMutation.isPending}
                      className="w-full h-11 gradient-btn text-white font-bold rounded-xl shadow-md hover:shadow-xl transition-all disabled:opacity-70"
                    >
                      {verifyMutation.isPending ? 'Verifying...' : 'Verify OTP'}
                    </button>

                    <div className="mt-3 flex items-center justify-between gap-3 text-xs font-semibold">
                      <button
                        type="button"
                        onClick={sendOtp}
                        disabled={initiateMutation.isPending}
                        className="text-primary hover:underline disabled:opacity-50"
                      >
                        Resend OTP
                      </button>
                      <button
                        type="button"
                        onClick={resetVerificationFlow}
                        className="text-on-surface-variant hover:text-on-surface"
                      >
                        Change email
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}

            {userExists && (
              <div className="space-y-4">
                <div className="p-4 bg-primary/10 rounded-xl border border-primary/20 text-center">
                  <p className="font-semibold text-on-surface">This email is already registered.</p>
                  <p className="text-sm text-on-surface-variant mt-1">Please log in to continue.</p>
                </div>
                <button onClick={() => navigate('/login', { replace: true })} className="w-full h-12 bg-primary text-white font-bold rounded-xl">
                  Go to Login
                </button>
              </div>
            )}

            <div className="mt-6 flex items-center justify-center space-x-4">
              <div className="flex-1 h-px bg-outline-variant/30"></div>
              <span className="text-xs font-semibold text-on-surface-variant tracking-wider uppercase">OR</span>
              <div className="flex-1 h-px bg-outline-variant/30"></div>
            </div>
            <button onClick={() => handleGoogleLogin()} className="mt-6 flex items-center justify-center w-full h-12 bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-bold rounded-xl border">
              <svg className="w-5 h-5 mr-3" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
              </svg>
              Continue with Google
            </button>
          </>
        )}

        {step === 'profile' && (
          <>
            <h2 className="text-xl font-bold text-on-surface mb-2">Complete Profile</h2>
            <p className="text-sm text-on-surface-variant mb-6">Email verified: <span className="font-semibold text-on-surface">{registeredEmail}</span></p>

            <form onSubmit={handleSubmit(onSubmitProfile)} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-semibold text-on-surface-variant block mb-1">First Name</label>
                  <input {...register('firstName', { required: 'Required' })}
                    className="w-full h-12 px-4 bg-surface-container-low border rounded-xl outline-none focus:ring-1 focus:ring-primary" />
                  {errors.firstName && <p className="text-xs text-error mt-1">{errors.firstName.message}</p>}
                </div>
                <div>
                  <label className="text-sm font-semibold text-on-surface-variant block mb-1">Last Name</label>
                  <input {...register('lastName', { required: 'Required' })}
                    className="w-full h-12 px-4 bg-surface-container-low border rounded-xl outline-none focus:ring-1 focus:ring-primary" />
                  {errors.lastName && <p className="text-xs text-error mt-1">{errors.lastName.message}</p>}
                </div>
              </div>

              <div>
                <label className="text-sm font-semibold text-on-surface-variant block mb-1">Create Password</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    {...register('password', { required: 'Required' })}
                    className="w-full h-12 pl-4 pr-12 bg-surface-container-low border rounded-xl outline-none focus:ring-1 focus:ring-primary"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-on-surface"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    <span className="material-symbols-outlined text-[20px]">
                      {showPassword ? 'visibility_off' : 'visibility'}
                    </span>
                  </button>
                </div>
                {errors.password && <p className="text-xs text-error mt-1">{errors.password.message}</p>}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-2 bg-surface-container p-3 rounded-xl">
                {constraints.map((c, i) => (
                  <div key={i} className={handleConstraintClass(c.met)}>
                    <svg className="w-3.5 h-3.5 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      {c.met ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" /> : <circle cx="12" cy="12" r="8" strokeWidth={2} />}
                    </svg>
                    {c.label}
                  </div>
                ))}
              </div>

              <button type="submit" disabled={completeMutation.isPending} 
                className="mt-6 w-full h-12 gradient-btn text-white font-bold rounded-xl shadow-md hover:shadow-xl transition-all disabled:opacity-70">
                {completeMutation.isPending ? 'Creating Account...' : 'Finish Registration'}
              </button>

              <button
                type="button"
                onClick={resetVerificationFlow}
                className="w-full text-sm font-semibold text-on-surface-variant hover:text-on-surface"
              >
                Change verified email
              </button>
            </form>
          </>
        )}

        <p className="mt-6 text-center text-sm font-semibold text-on-surface-variant">
          Already have an account? <Link to="/login" className="text-primary hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  );
};

export default RegisterPage;

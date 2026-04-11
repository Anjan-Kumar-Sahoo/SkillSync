import { useState, useRef, useEffect } from 'react';
import type { ClipboardEvent, KeyboardEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';

const OTP_LENGTH = 6;

const VerifyOtpPage = () => {
  const [otp, setOtp] = useState<string[]>(Array(OTP_LENGTH).fill(''));
  const [timeLeft, setTimeLeft] = useState(300); // 5 mins in seconds
  const [attempts, setAttempts] = useState(0);
  
  const inputRefs = useRef<(HTMLInputElement | null)[]>(Array(OTP_LENGTH).fill(null));
  
  const location = useLocation();
  const navigate = useNavigate();
  const { showToast } = useToast();
  
  const email = location.state?.email || '';

  const hasValidEmail = /^\S+@\S+\.\S+$/.test(email);

  useEffect(() => {
    if (hasValidEmail) return;
    showToast({ message: 'Session expired. Please restart the flow.', type: 'error' });
    navigate('/register', { replace: true });
  }, [hasValidEmail, navigate, showToast]);

  // Countdown timer logic
  useEffect(() => {
    if (timeLeft <= 0) return;
    const timer = setInterval(() => {
      setTimeLeft(prev => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [timeLeft]);

  const verifyMutation = useMutation({
    mutationFn: async (otpPayload: { email: string; otp: string }) => {
      const response = await api.post('/api/auth/verify-otp', otpPayload);
      return response.data;
    },
    onSuccess: () => {
      showToast({ message: 'Email verified successfully!', type: 'success' });
      navigate('/login');
    },
    onError: () => {
      const newAttempts = attempts + 1;
      setAttempts(newAttempts);
      if (newAttempts >= 5) {
        showToast({ message: 'Too many attempts. Please register again.', type: 'error' });
        setTimeout(() => navigate('/register'), 3000);
      } else {
        showToast({ message: `Invalid OTP. Attempts left: ${5 - newAttempts}`, type: 'error' });
      }
    }
  });

  const resendMutation = useMutation({
    mutationFn: async (emailPayload: { email: string }) => {
      const response = await api.post('/api/auth/resend-otp', emailPayload, { _skipErrorRedirect: true } as any);
      return response.data;
    },
    onSuccess: () => {
      setTimeLeft(300);
      setAttempts(0);
      setOtp(Array(OTP_LENGTH).fill(''));
      showToast({ message: 'OTP resent successfully.', type: 'success' });
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to resend OTP.';
      showToast({ message, type: 'error' });
    }
  });

  const applyDigitsFromIndex = (index: number, rawValue: string) => {
    const digits = rawValue.replace(/\D/g, '');
    if (!digits) {
      return;
    }

    const newOtp = [...otp];
    let cursor = index;

    for (const digit of digits) {
      if (cursor >= OTP_LENGTH) {
        break;
      }
      newOtp[cursor] = digit;
      cursor += 1;
    }

    setOtp(newOtp);

    const firstEmptyFromCursor = newOtp.findIndex((digit, idx) => idx >= index && digit === '');
    const firstEmpty = firstEmptyFromCursor === -1 ? newOtp.findIndex((digit) => digit === '') : firstEmptyFromCursor;
    if (firstEmpty === -1) {
      inputRefs.current[OTP_LENGTH - 1]?.focus();
      return;
    }

    inputRefs.current[firstEmpty]?.focus();
  };

  const handleChange = (index: number, value: string) => {
    if (value === '') {
      const newOtp = [...otp];
      newOtp[index] = '';
      setOtp(newOtp);
      return;
    }

    const digits = value.replace(/\D/g, '');
    if (!digits) return;

    if (digits.length > 1) {
      applyDigitsFromIndex(index, digits);
      return;
    }

    const newOtp = [...otp];
    newOtp[index] = digits;
    setOtp(newOtp);

    if (index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handlePaste = (index: number, event: ClipboardEvent<HTMLInputElement>) => {
    event.preventDefault();
    const pastedValue = event.clipboardData.getData('text');
    applyDigitsFromIndex(index, pastedValue);
  };

  const handleKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      if (otp[index] !== '') {
        const newOtp = [...otp];
        newOtp[index] = '';
        setOtp(newOtp);
        return;
      }

      if (index > 0) {
        const newOtp = [...otp];
        newOtp[index - 1] = '';
        setOtp(newOtp);
        inputRefs.current[index - 1]?.focus();
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      e.preventDefault();
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === 'ArrowRight' && index < OTP_LENGTH - 1) {
      e.preventDefault();
      inputRefs.current[index + 1]?.focus();
    } else if (e.key === 'Enter') {
      handleSubmit();
    }
  };

  const handleSubmit = () => {
    const otpCode = otp.join('');
    if (otpCode.length !== OTP_LENGTH) {
      showToast({ message: 'Please enter the 6-digit OTP.', type: 'error' });
      return;
    }

    verifyMutation.mutate({ email, otp: otpCode });
  };

  const handleResend = () => {
    if (timeLeft > 0 || !hasValidEmail) return;
    resendMutation.mutate({ email });
  };

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const isComplete = otp.every(digit => digit !== '');

  return (
    <div className="flex flex-col items-center">
      <div className="w-full bg-surface-container-lowest p-8 md:p-10 rounded-xl shadow-sm border border-outline-variant/15 transition-all">
        <h2 className="text-xl font-bold text-on-surface mb-2">Verify your email</h2>
        <p className="text-sm text-on-surface-variant font-medium mb-8">
          We sent a 6-digit code to <span className="font-bold text-on-surface">{email || 'your email'}</span>.
        </p>
        
        <div className="flex justify-between mb-8 space-x-2">
          {otp.map((digit, idx) => (
            <input
              key={idx}
              ref={el => { inputRefs.current[idx] = el; }}
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={1}
              value={digit}
              onChange={e => handleChange(idx, e.target.value)}
              onPaste={e => handlePaste(idx, e)}
              onKeyDown={e => handleKeyDown(idx, e)}
              onFocus={e => e.currentTarget.select()}
              className="w-12 h-14 md:w-14 md:h-16 text-center text-xl font-bold bg-surface-container-low border border-outline-variant/30 rounded-xl focus:ring-2 focus:ring-primary focus:border-primary outline-none transition-all duration-200"
            />
          ))}
        </div>

        <button 
          onClick={handleSubmit}
          disabled={!isComplete || verifyMutation.isPending || timeLeft === 0 || attempts >= 5} 
          className="mt-6 flex items-center justify-center w-full h-12 gradient-btn text-white font-bold rounded-xl shadow-md hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all duration-500 disabled:opacity-70 disabled:scale-100 disabled:shadow-none"
        >
          {verifyMutation.isPending ? (
            <span className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></span>
          ) : 'Verify'}
        </button>

        <div className="mt-8 flex flex-col items-center space-y-2">
          <p className="text-sm font-semibold text-on-surface-variant">
            {timeLeft > 0 ? `${formatTime(timeLeft)} remaining` : 'Code expired'}
          </p>
          <button
            onClick={handleResend}
            disabled={timeLeft > 0 || resendMutation.isPending}
            className={`text-sm font-bold ${timeLeft > 0 ? 'text-outline hover:no-underline' : 'text-primary hover:underline'}`}
          >
            {resendMutation.isPending ? 'Resending...' : 'Resend OTP'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default VerifyOtpPage;

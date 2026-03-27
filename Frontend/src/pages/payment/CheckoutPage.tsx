import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';

declare global {
  interface Window {
    Razorpay: any;
  }
}

const CheckoutPage = () => {
  const { state } = useLocation();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [paymentMethod, setPaymentMethod] = useState<'card' | 'paypal'>('card');
  const [loadingStep, setLoadingStep] = useState<'' | 'session' | 'order' | 'verify'>('');

  useEffect(() => {
    if (!state || !state.mentorId) navigate('/mentors', { replace: true });
    
    // Dynamically load Razorpay SDK
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    document.body.appendChild(script);

    return () => {
      document.body.removeChild(script);
    };
  }, [state, navigate]);

  if (!state) return null;

  const { mentorId, mentorName, slotId, startTime, hourlyRate } = state;
  const platformFee = hourlyRate * 0.05;
  const totalAmount = hourlyRate + platformFee;

  const formatTime = (iso: string) => {
    const d = new Date(iso);
    return `${d.toLocaleDateString('default', { month: 'short', day: 'numeric', year: 'numeric' })} • ${d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })} (60m)`;
  };

  const verifyPaymentMutation = useMutation({
    mutationFn: async (paymentDetails: any) => {
      return api.post('/api/payments/verify', paymentDetails);
    },
    onSuccess: () => {
      showToast({ message: 'Booking confirmed! 🎉', type: 'success' });
      navigate('/sessions', { replace: true });
    },
    onError: () => {
      showToast({ message: 'Payment received but verification failed. Contact support.', type: 'error' });
      setLoadingStep('');
    }
  });

  const handleConfirm = async () => {
    try {
      setLoadingStep('session');
      const sessionRes = await api.post('/api/sessions', {
        mentorId,
        slotId,
        topic: 'Mentoring Session'
      });
      const sessionId = sessionRes.data.id;

      setLoadingStep('order');
      const orderRes = await api.post('/api/payments/order', {
        sessionId,
        amount: totalAmount,
        currency: 'USD'
      });
      const { orderId, amount, currency, keyId } = orderRes.data;

      if (!window.Razorpay) {
        showToast({ message: 'Payment gateway failed to load.', type: 'error' });
        setLoadingStep('');
        return;
      }

      const options = {
        key: keyId,
        amount: amount * 100, // Razorpay takes smallest currency unit
        currency,
        name: 'SkillSync',
        description: `Mentoring Session with ${mentorName}`,
        order_id: orderId,
        handler: async function (response: any) {
          setLoadingStep('verify');
          verifyPaymentMutation.mutate({
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          });
        },
        modal: {
          ondismiss: () => {
            setLoadingStep('');
            showToast({ message: 'Payment cancelled.', type: 'success' });
          }
        },
        theme: {
          color: '#3b82f6'
        }
      };

      const rzp = new window.Razorpay(options);
      rzp.open();

    } catch (e: any) {
      console.error(e);
      showToast({ message: e.response?.data?.message || 'Failed to initialize checkout.', type: 'error' });
      setLoadingStep('');
    }
  };

  const isProcessing = loadingStep !== '';

  return (
    <div className="min-h-screen bg-surface py-8 px-4 flex items-center justify-center font-sans">
      <div className="bg-surface-container-lowest w-full max-w-[560px] rounded-2xl shadow-2xl p-6 md:p-8 border border-outline-variant/10 animate-in slide-in-from-bottom-4 duration-300">
        
        {/* Header */}
        <div className="flex justify-between items-center mb-8 border-b border-outline-variant/10 pb-4">
          <h1 className="text-2xl font-extrabold text-on-surface tracking-tight">Complete Your Booking</h1>
          <button 
            onClick={() => navigate(-1)} 
            disabled={isProcessing}
            className="w-10 h-10 rounded-full bg-surface-container hover:bg-surface-container-high transition-colors text-on-surface flex items-center justify-center disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <span className="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        {/* Session Summary */}
        <div className="bg-surface-container-low/50 rounded-xl p-5 mb-6 border border-outline-variant/10 shadow-sm relative overflow-hidden">
          <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl -mr-10 -mt-10"></div>
          <p className="text-[10px] font-black text-primary uppercase tracking-widest mb-1">Confirmed Session</p>
          <p className="font-extrabold text-on-surface text-lg mb-1 relative z-10">{mentorName}</p>
          <div className="flex items-center text-sm font-semibold text-on-surface-variant relative z-10 gap-1.5">
            <span className="material-symbols-outlined text-[18px]">calendar_month</span> {formatTime(startTime)}
          </div>
        </div>

        {/* Order Summary */}
        <div className="mb-8">
          <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-4">Order Summary</p>
          <div className="flex justify-between items-center text-sm font-semibold text-on-surface-variant mb-2">
            <span>Session Fee (60 mins)</span>
            <span>${hourlyRate.toFixed(2)}</span>
          </div>
          <div className="flex justify-between items-center text-sm font-semibold text-on-surface-variant mb-4">
            <span>Platform Service Fee</span>
            <span>${platformFee.toFixed(2)}</span>
          </div>
          <div className="border-t border-outline-variant/20 pt-4 mt-2 flex justify-between items-center border-dashed">
            <span className="text-base font-extrabold text-on-surface uppercase tracking-wider">Total Amount</span>
            <span className="text-3xl font-black text-primary">${totalAmount.toFixed(2)}</span>
          </div>
        </div>

        {/* Payment Method */}
        <div className="mb-6">
          <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-3">Payment Method</p>
          <div className="space-y-3">
            <div 
              onClick={() => !isProcessing && setPaymentMethod('card')}
              className={`cursor-pointer rounded-xl p-4 flex items-center gap-4 transition-all border ${
                paymentMethod === 'card' 
                  ? 'border-primary bg-primary/5 shadow-sm' 
                  : 'border-outline-variant/20 hover:border-outline-variant/40 bg-surface-container-lowest'
              }`}
            >
              <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${paymentMethod === 'card' ? 'bg-primary text-white shadow-md' : 'bg-surface-container text-on-surface-variant'}`}>
                <span className="material-symbols-outlined text-[20px]">credit_card</span>
              </div>
              <div className="flex-1">
                <p className="text-sm font-bold text-on-surface">Credit or Debit Card</p>
                <p className="text-[10px] font-semibold text-on-surface-variant uppercase tracking-widest">Visa, Mastercard, Amex</p>
              </div>
              <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${paymentMethod === 'card' ? 'border-primary' : 'border-outline-variant/40'}`}>
                {paymentMethod === 'card' && <div className="w-2.5 h-2.5 bg-primary rounded-full"></div>}
              </div>
            </div>

            <div 
              onClick={() => !isProcessing && setPaymentMethod('paypal')}
              className={`cursor-pointer rounded-xl p-4 flex items-center gap-4 transition-all border ${
                paymentMethod === 'paypal' 
                  ? 'border-primary bg-primary/5 shadow-sm' 
                  : 'border-outline-variant/20 hover:border-outline-variant/40 bg-surface-container-lowest'
              }`}
            >
              <div className={`w-10 h-10 rounded-lg flex items-center justify-center font-black italic text-[18px] ${paymentMethod === 'paypal' ? 'bg-[#003087] text-white shadow-md' : 'bg-surface-container text-on-surface-variant'}`}>
                <span className="transform -skew-x-12">P</span>
              </div>
              <div className="flex-1">
                <p className="text-sm font-bold text-on-surface">PayPal</p>
                <p className="text-[10px] font-semibold text-on-surface-variant uppercase tracking-widest">Fast and secure checkout</p>
              </div>
              <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${paymentMethod === 'paypal' ? 'border-primary' : 'border-outline-variant/40'}`}>
                {paymentMethod === 'paypal' && <div className="w-2.5 h-2.5 bg-primary rounded-full"></div>}
              </div>
            </div>
          </div>
        </div>

        {/* Card Form Mock */}
        {paymentMethod === 'card' && (
          <div className="mb-8 space-y-4 animate-in slide-in-from-top-2 duration-300">
            <div>
              <label className="text-[10px] font-extrabold text-on-surface-variant uppercase tracking-widest block mb-2 pl-1">Cardholder Name</label>
              <input type="text" placeholder="John Doe" disabled={isProcessing} className="w-full h-12 bg-surface-container-low border border-outline-variant/20 rounded-xl px-4 text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-all disabled:opacity-50" />
            </div>
            <div>
              <label className="text-[10px] font-extrabold text-on-surface-variant uppercase tracking-widest block mb-2 pl-1">Card Number</label>
              <div className="relative">
                <input type="text" placeholder="**** **** **** 4452" disabled={isProcessing} className="w-full h-12 bg-surface-container-low border border-outline-variant/20 rounded-xl pl-4 pr-10 text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-all disabled:opacity-50 placeholder:text-on-surface-variant/60" />
                <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant/70 text-[20px]">lock</span>
              </div>
            </div>
            <div className="flex gap-4">
              <div className="flex-1">
                <label className="text-[10px] font-extrabold text-on-surface-variant uppercase tracking-widest block mb-2 pl-1">Expiry (MM/YY)</label>
                <input type="text" placeholder="12/26" disabled={isProcessing} className="w-full h-12 bg-surface-container-low border border-outline-variant/20 rounded-xl px-4 text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-all text-center disabled:opacity-50" />
              </div>
              <div className="flex-1">
                <label className="text-[10px] font-extrabold text-on-surface-variant uppercase tracking-widest block mb-2 pl-1">CVV</label>
                <input type="password" placeholder="123" disabled={isProcessing} className="w-full h-12 bg-surface-container-low border border-outline-variant/20 rounded-xl px-4 text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-all text-center placeholder:text-2xl pt-2 disabled:opacity-50" />
              </div>
            </div>
          </div>
        )}

        {/* Action */}
        <button 
          onClick={handleConfirm}
          disabled={isProcessing}
          className="w-full h-14 gradient-btn text-white font-extrabold text-lg rounded-xl shadow-lg hover:shadow-xl hover:-translate-y-0.5 disabled:opacity-70 disabled:scale-100 disabled:translate-y-0 active:scale-[0.98] transition-all flex items-center justify-center gap-3 overflow-hidden relative group"
        >
          {isProcessing && <span className="absolute inset-0 bg-white/20 animate-pulse"></span>}
          {isProcessing ? (
            <>
              <span className="material-symbols-outlined animate-spin text-[24px]">autorenew</span>
              <span className="text-base font-bold">
                {loadingStep === 'session' && 'Reserving slot...'}
                {loadingStep === 'order' && 'Connecting to bank...'}
                {loadingStep === 'verify' && 'Verifying transaction...'}
              </span>
            </>
          ) : (
            <>
              Confirm Payment <span className="text-white/40 px-1">•</span> ${totalAmount.toFixed(2)} 
              <span className="material-symbols-outlined text-[20px] group-hover:translate-x-1 transition-transform">arrow_forward</span>
            </>
          )}
        </button>

        {/* Footer */}
        <div className="mt-8 text-center text-xs font-semibold text-on-surface-variant max-w-[400px] mx-auto opacity-70 leading-relaxed">
          By confirming, you agree to SkillSync's Terms and Cancellation Policy. Payment data flows exclusively via Secure 256-bit encryption.
        </div>
      </div>
    </div>
  );
};

export default CheckoutPage;

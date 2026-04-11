import { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import mentorService from '../../services/mentorService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import api from '../../services/axios';
import { formatClockTime } from '../../utils/dateTime';

declare global {
  interface Window {
    Razorpay: any;
  }
}

const weekdayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
const bookingDurationOptions = [30, 60, 90, 120];

const MentorDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { showToast } = useToast();

  const [selectedSlot, setSelectedSlot] = useState<any>(null);
  const [selectedDurationMinutes, setSelectedDurationMinutes] = useState<number>(60);
  const [loadingStep, setLoadingStep] = useState<'' | 'session' | 'order' | 'verify'>('');
  const pendingSessionIdRef = useRef<number | null>(null);

  const tzOffset = new Date().getTimezoneOffset() * 60000;
  const localIsoDate = new Date(Date.now() - tzOffset).toISOString().split('T')[0];
  const [selectedDateStr, setSelectedDateStr] = useState<string>(localIsoDate);

  // Load Razorpay SDK with proper onload tracking
  const [razorpayReady, setRazorpayReady] = useState(false);
  useEffect(() => {
    // If already loaded (e.g. from a previous page visit), skip
    if (window.Razorpay) {
      setRazorpayReady(true);
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    script.onload = () => setRazorpayReady(true);
    script.onerror = () => console.error('Failed to load Razorpay SDK');
    document.body.appendChild(script);
    return () => {
      try { document.body.removeChild(script); } catch (_) { /* already removed */ }
    };
  }, []);

  // Fetch mentor profile
  const { data: mentor, isLoading } = useQuery({
    queryKey: ['mentor', 'detail', id],
    queryFn: () => mentorService.getMentorById(Number(id)),
  });

  // Fetch reviews for this mentor
  const { data: reviewsData } = useQuery({
    queryKey: ['reviews', 'mentor', id],
    queryFn: async () => {
      try {
        const res = await api.get(`/api/reviews/mentor/${id}?page=0&size=50`, { _skipErrorRedirect: true } as any);
        return res.data;
      } catch { return null; }
    },
    enabled: !!id,
  });

  // Fetch booked sessions to filter out unavailable slots
  const { data: bookedSessionsData } = useQuery({
    queryKey: ['sessions', 'booked', id],
    queryFn: async () => {
      try {
        const res = await api.get(`/api/sessions/public/mentor/${id}/booked`, { _skipErrorRedirect: true } as any);
        return res.data;
      } catch { return []; }
    },
    enabled: !!id,
    refetchInterval: 10000, // Refresh every 10 seconds
  });
  const bookedSessions = bookedSessionsData || [];

  const { data: learnerActiveSessionsData } = useQuery({
    queryKey: ['sessions', 'learner', 'active-bookings', id],
    queryFn: async () => {
      try {
        const params = new URLSearchParams();
        params.set('page', '0');
        params.set('size', '200');
        params.append('status', 'REQUESTED');
        params.append('status', 'ACCEPTED');
        const res = await api.get(`/api/sessions/learner?${params.toString()}`, { _skipErrorRedirect: true } as any);
        return Array.isArray(res.data?.content) ? res.data.content : [];
      } catch {
        return [];
      }
    },
    enabled: !!id,
  });
  const learnerActiveSessions = Array.isArray(learnerActiveSessionsData) ? learnerActiveSessionsData : [];

  const getSlotDurationMinutes = (slot: any): number => {
    if (!slot) return 0;
    const [startHours, startMinutes] = String(slot.startTime).split(':').map(Number);
    const [endHours, endMinutes] = String(slot.endTime).split(':').map(Number);

    const startTotalMinutes = (Number.isFinite(startHours) ? startHours : 0) * 60 + (Number.isFinite(startMinutes) ? startMinutes : 0);
    const endTotalMinutes = (Number.isFinite(endHours) ? endHours : 0) * 60 + (Number.isFinite(endMinutes) ? endMinutes : 0);
    const duration = endTotalMinutes - startTotalMinutes;
    return duration > 0 ? duration : 0;
  };

  const hasDuplicateBookingForSelection = (mentorUserId: number, slotDurationMinutes: number) => {
    if (!selectedSlot || slotDurationMinutes <= 0) return false;

    const [hours, minutes] = String(selectedSlot.startTime).split(':');
    const selectedStart = new Date(selectedDateStr);
    selectedStart.setHours(Number(hours), Number(minutes), 0, 0);
    const selectedStartEpoch = selectedStart.getTime();

    return learnerActiveSessions.some((session: any) => {
      const sessionMentorId = Number(session.mentorId);
      if (sessionMentorId !== mentorUserId) return false;

      const sessionStartRaw = session.startTime || session.sessionDate;
      if (!sessionStartRaw) return false;

      const sessionStartEpoch = new Date(sessionStartRaw).getTime();
      const sessionDuration = Number(session.durationMinutes ?? 60);
      const isActiveStatus = session.status === 'REQUESTED' || session.status === 'ACCEPTED';

      return isActiveStatus && sessionStartEpoch === selectedStartEpoch && sessionDuration === slotDurationMinutes;
    });
  };

  const verifyPaymentMutation = useMutation({
    mutationFn: async (paymentDetails: any) => api.post('/api/payments/verify', paymentDetails),
    onSuccess: () => {
      pendingSessionIdRef.current = null;
      showToast({ message: 'Booking confirmed! 🎉', type: 'success' });
      navigate('/sessions', { replace: true });
    },
    onError: () => {
      void rollbackPendingSession();
      showToast({ message: 'Payment received but verification failed. Contact support.', type: 'error' });
      setLoadingStep('');
    }
  });

  const rollbackPendingSession = async () => {
    const sessionId = pendingSessionIdRef.current;
    if (!sessionId) return;

    try {
      await api.put(`/api/sessions/${sessionId}/cancel`, undefined, { _skipErrorRedirect: true } as any);
    } catch {
      // Best-effort rollback to avoid stale REQUESTED sessions after payment interruption.
    } finally {
      pendingSessionIdRef.current = null;
    }
  };

  const handlePayNow = async () => {
    if (!selectedSlot || !mentor) {
      showToast({ message: 'Please select an available session first', type: 'error' });
      return;
    }

    const m = mentor as any;
    const mentorUserId = Number(m.userId);
    if (!Number.isFinite(mentorUserId)) {
      showToast({ message: 'Invalid mentor information. Please refresh and try again.', type: 'error' });
      return;
    }

    if (!bookingDurationOptions.includes(selectedDurationMinutes)) {
      showToast({ message: 'Please select a valid duration between 30 and 120 minutes.', type: 'error' });
      return;
    }

    if (hasDuplicateBookingForSelection(mentorUserId, selectedDurationMinutes)) {
      showToast({ message: 'Session already booked for this slot', type: 'error' });
      return;
    }

    const mentorName = `${m.firstName || ''} ${m.lastName || ''}`.trim() || 'Mentor';

    // Build a session date from the selectedDateStr + slot time
    const sessionDate = new Date(selectedDateStr);
    const [hours, minutes] = String(selectedSlot.startTime).split(':');
    sessionDate.setHours(Number(hours), Number(minutes), 0, 0);

    // Format as LocalDateTime (no Z suffix) — backend expects java.time.LocalDateTime
    const pad = (n: number) => n.toString().padStart(2, '0');
    const localDateTimeStr = `${sessionDate.getFullYear()}-${pad(sessionDate.getMonth() + 1)}-${pad(sessionDate.getDate())}T${pad(sessionDate.getHours())}:${pad(sessionDate.getMinutes())}:00`;

    try {
      setLoadingStep('session');
      const sessionRes = await api.post('/api/sessions', {
        mentorId: mentorUserId,
        topic: 'Mentoring Session',
        description: `Session with ${mentorName} on ${weekdayNames[selectedSlot.dayOfWeek]}`,
        sessionDate: localDateTimeStr,
        durationMinutes: selectedDurationMinutes,
      }, { _skipErrorRedirect: true } as any);
      const sessionId = sessionRes.data.id;
      pendingSessionIdRef.current = sessionId;

      setLoadingStep('order');
      const orderRes = await api.post('/api/payments/create-order', {
        type: 'SESSION_BOOKING',
        referenceId: sessionId,
        referenceType: 'SESSION_BOOKING',
        amount: estimatedCost
      }, { _skipErrorRedirect: true } as any);
      const { orderId, amount, currency, keyId } = orderRes.data;

      if (!window.Razorpay || !razorpayReady) {
        showToast({ message: 'Payment gateway is still loading. Please wait a moment and try again.', type: 'error' });
        await rollbackPendingSession();
        setLoadingStep('');
        return;
      }

      const rzp = new window.Razorpay({
        key: keyId,
        amount,
        currency,
        name: 'SkillSync',
        description: `Session with ${mentorName}`,
        order_id: orderId,
        handler: async (response: any) => {
          setLoadingStep('verify');
          verifyPaymentMutation.mutate({
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          });
        },
        modal: {
          ondismiss: async () => {
            await rollbackPendingSession();
            setLoadingStep('');
            showToast({ message: 'Payment cancelled.', type: 'success' });
          }
        },
        theme: { color: '#3b82f6' }
      });
      rzp.open();

    } catch (e: any) {
      await rollbackPendingSession();
      showToast({ message: e.response?.data?.message || 'Failed to initiate payment.', type: 'error' });
      setLoadingStep('');
    }
  };

  if (isLoading) {
    return (
      <PageLayout>
        <div className="flex items-center justify-center h-screen">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
        </div>
      </PageLayout>
    );
  }

  if (!mentor) {
    return (
      <PageLayout>
        <div className="text-center py-12">
          <span className="material-symbols-outlined text-5xl text-outline-variant mb-3 block">person_off</span>
          <p className="text-lg text-on-surface-variant mb-4">Mentor not found</p>
          <button onClick={() => navigate('/mentors')} className="gradient-btn text-white px-6 py-2 rounded-lg font-bold">
            Back to Mentors
          </button>
        </div>
      </PageLayout>
    );
  }

  const m = mentor as any;
  const mentorDisplayName = `${m.firstName || ''} ${m.lastName || ''}`.trim() || 'Mentor';
  const mentorReviews = Number(m.reviewCount ?? m.totalReviews ?? 0);
  const mentorSessions = Number(m.totalSessions ?? 0);
  const mentorExperience = Number(m.experience ?? m.experienceYears ?? 0);
  const rawRating = Number(m.rating ?? m.avgRating ?? 0);
  const mentorRating = rawRating;
  const isNewMentor = mentorSessions === 0;
  const slots = (m.availability || []).filter((s: any) => s.isActive !== false);
  const hourlyRate = Number(m.hourlyRate || 0);
  const estimatedCost = (hourlyRate * selectedDurationMinutes) / 60;
  const isProcessing = loadingStep !== '';
  const mentorUserIdForSelection = Number(m.userId);
  const hasDuplicateLearnerBooking = Number.isFinite(mentorUserIdForSelection)
    ? hasDuplicateBookingForSelection(mentorUserIdForSelection, selectedDurationMinutes)
    : false;

  const reviews = reviewsData?.content || [];

  const getInitials = (first?: string, last?: string) => {
    return `${first?.[0] || ''}${last?.[0] || ''}`.toUpperCase() || 'M';
  };

  const formatSlotRange = (startTime: string, endTime: string) => {
    return `${formatClockTime(startTime)} - ${formatClockTime(endTime)} IST`;
  };

  return (
    <PageLayout>
      <div className="w-full space-y-6">
        {/* Back */}
        <button
          onClick={() => navigate('/mentors')}
          className="text-sm font-bold text-primary hover:underline flex items-center gap-1"
        >
          <span className="material-symbols-outlined text-[16px]">arrow_back</span>
          Back to Mentors
        </button>

        {/* Profile Card */}
        <div className="bg-surface-container-lowest rounded-2xl shadow-sm border border-outline-variant/10 overflow-hidden">
          <div className="h-32 bg-gradient-to-r from-primary/20 via-primary/10 to-primary/5 relative">
            <div className="absolute -bottom-12 left-8">
              <div className="w-24 h-24 rounded-2xl bg-gradient-to-tr from-primary to-primary/70 text-white flex items-center justify-center text-3xl font-black shadow-lg ring-4 ring-surface-container-lowest">
                {getInitials(m.firstName, m.lastName)}
              </div>
            </div>
          </div>
          
          <div className="pt-16 px-8 pb-8">
            <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4 mb-6">
              <div>
                <h1 className="text-3xl font-extrabold text-on-surface tracking-tight">{mentorDisplayName}</h1>
                <p className="text-on-surface-variant mt-1">{mentorExperience} years of experience</p>
              </div>
              <div className="flex items-center gap-4">
                <div className="text-right">
                  {!isNewMentor ? (
                    <>
                      <div className="flex items-center gap-1">
                        <span className="text-amber-500 text-2xl">★</span>
                        <span className="text-2xl font-black text-on-surface">{mentorRating.toFixed(1)}</span>
                      </div>
                      <p className="text-xs font-semibold text-on-surface-variant">{mentorReviews} review{mentorReviews !== 1 ? 's' : ''}</p>
                    </>
                  ) : (
                    <span className="bg-primary/10 text-primary text-xs font-bold px-3 py-1.5 rounded-full">NEW MENTOR</span>
                  )}
                </div>
              </div>
            </div>

            <p className="text-on-surface-variant mb-6 leading-relaxed">{m.bio}</p>

            <div className="grid grid-cols-2 gap-4">
              <div className="bg-primary/5 rounded-xl p-4 border border-primary/10">
                <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-0.5">Hourly Rate</p>
                <p className="text-2xl font-black text-primary">₹{hourlyRate}<span className="text-xs font-semibold text-on-surface-variant">/hr</span></p>
              </div>
              <div className="bg-emerald-50 rounded-xl p-4 border border-emerald-100">
                <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-0.5">Status</p>
                <p className="text-2xl font-black text-emerald-600">
                  {m.status === 'APPROVED' ? 'Verified' : m.status || 'Pending'}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Skills */}
        <div className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10">
          <h2 className="text-lg font-extrabold text-on-surface mb-4">Skills & Expertise</h2>
          <div className="flex flex-wrap gap-2">
            {(m.skills || []).map((skill: any, i: number) => (
              <span
                key={i}
                className="bg-primary/10 text-primary px-4 py-2 rounded-full text-sm font-bold border border-primary/20"
              >
                {typeof skill === 'string' ? skill : skill.name || `Skill #${skill.skillId || skill.id}`}
              </span>
            ))}
            {(!m.skills || m.skills.length === 0) && (
              <p className="text-sm text-on-surface-variant">No skills listed yet</p>
            )}
          </div>
        </div>

        {/* Available Sessions (was "Weekly Availability") */}
        <div className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
            <div>
              <h2 className="text-lg font-extrabold text-on-surface">Available Sessions</h2>
              <p className="text-sm text-on-surface-variant">Select a date and time to book</p>
            </div>
            <div>
               <input 
                 type="date" 
                 value={selectedDateStr}
                 min={localIsoDate}
                 onChange={(e) => {
                    setSelectedDateStr(e.target.value);
                    setSelectedSlot(null);
                    setSelectedDurationMinutes(60);
                 }}
                 className="h-10 bg-surface-container border border-outline-variant/30 rounded-lg px-3 text-sm font-bold text-on-surface focus:ring-2 focus:ring-primary outline-none"
               />
            </div>
          </div>

          {slots.filter((s: any) => s.dayOfWeek === new Date(selectedDateStr).getDay()).length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
              {[...slots]
                .filter((s: any) => s.dayOfWeek === new Date(selectedDateStr).getDay())
                .sort((a: any, b: any) => String(a.startTime).localeCompare(String(b.startTime)))
                .map((slot: any) => {
                  const isSelected = selectedSlot?.id === slot.id;
                  const pad = (n: number) => n.toString().padStart(2, '0');
                  const [h, m] = String(slot.startTime).split(':');
                  const slotDurationMinutes = getSlotDurationMinutes(slot);
                  const slotDateTimeStr = `${selectedDateStr}T${pad(Number(h))}:${pad(Number(m))}:00`;
                  const slotStartEpoch = new Date(slotDateTimeStr).getTime();
                  const slotEndEpoch = slotStartEpoch + slotDurationMinutes * 60000;
                  
                  // Check against booked sessions
                  const isBooked = bookedSessions.some((bs: any) => {
                      const bsStart = new Date(bs.sessionDate).getTime();
                      const bsEnd = bsStart + Number(bs.durationMinutes) * 60000;
                      // Conflict occurs if the slots overlap in time
                      return (slotStartEpoch < bsEnd && slotEndEpoch > bsStart);
                  });

                  // Ensure we don't show slots in the past for today
                  const isPast = slotStartEpoch < Date.now();
                  const isUnavailable = isBooked || isPast;
                  return (
                    <button
                      key={slot.id}
                      onClick={() => {
                        if (isSelected) {
                          setSelectedSlot(null);
                          setSelectedDurationMinutes(60);
                          return;
                        }

                        setSelectedSlot(slot);
                        setSelectedDurationMinutes(60);
                      }}
                      disabled={isUnavailable}
                      className={`flex items-center justify-between rounded-xl border p-4 transition-all text-left w-full ${
                        isUnavailable
                          ? 'bg-red-50 border-red-200 opacity-60 cursor-not-allowed'
                          : isSelected
                            ? 'bg-primary/10 border-primary ring-2 ring-primary/30 shadow-md'
                            : 'bg-surface-container-low border-outline-variant/10 hover:border-primary/30 hover:bg-primary/5 cursor-pointer'
                      }`}
                    >
                      <div>
                        <p className={`font-bold ${isSelected ? 'text-primary' : 'text-on-surface'}`}>
                          {weekdayNames[slot.dayOfWeek] || 'Unknown'}
                        </p>
                        <p className="text-sm text-on-surface-variant mt-0.5">
                          {formatSlotRange(String(slot.startTime), String(slot.endTime))}
                        </p>
                      </div>
                      <div className="flex items-center gap-2">
                        {isUnavailable ? (
                          <span className="text-[10px] font-black uppercase tracking-widest px-2 py-1 rounded-md bg-red-100 text-red-700 border border-red-200">
                              {isBooked ? 'Booked' : 'Passed'}
                          </span>
                        ) : isSelected ? (
                          <span className="material-symbols-outlined text-primary text-[22px]">check_circle</span>
                        ) : (
                          <span className="text-[10px] font-black uppercase tracking-widest px-2 py-1 rounded-md bg-emerald-100 text-emerald-700 border border-emerald-200">Available</span>
                        )}
                      </div>
                    </button>
                  );
                })}
            </div>
          ) : (
            <div className="rounded-xl border border-dashed border-outline-variant/30 p-8 text-center text-on-surface-variant flex flex-col items-center">
              <span className="material-symbols-outlined text-4xl text-outline-variant/50 mb-3 block">calendar_month</span>
              <p className="font-bold text-on-surface">No slots on this date</p>
              <p className="text-sm mt-1">Try selecting another date to see the mentor's availability.</p>
            </div>
          )}
        </div>

        {/* Booking & Payment Panel - only shown when slot selected */}
        {selectedSlot && (
          <div className="bg-surface-container-lowest rounded-2xl p-6 shadow-lg border-2 border-primary/20 animate-in slide-in-from-bottom-4 duration-300">
            <h2 className="text-lg font-extrabold text-on-surface mb-4 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary">event_available</span>
              Book This Session
            </h2>

            <div className="bg-primary/5 rounded-xl p-4 border border-primary/10 mb-4">
              <div className="flex justify-between items-center">
                <div>
                  <p className="font-bold text-on-surface">{weekdayNames[selectedSlot.dayOfWeek]}</p>
                  <p className="text-sm text-on-surface-variant">{formatSlotRange(String(selectedSlot.startTime), String(selectedSlot.endTime))}</p>
                </div>
                <div className="text-right">
                  <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Mentor</p>
                  <p className="font-bold text-on-surface">{mentorDisplayName}</p>
                </div>
              </div>
            </div>

            <div className="mb-4 rounded-xl border border-outline-variant/20 bg-surface-container p-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
                <div>
                  <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Session Duration</p>
                  <p className="text-sm font-bold text-on-surface mt-1">Choose 30 to 120 minutes</p>
                  <p className="text-xs text-on-surface-variant mt-1">Pick 30, 60, 90, or 120 minutes.</p>
                </div>

                <div className="w-full md:w-64">
                  <label className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest block mb-1">Duration</label>
                  <select
                    value={selectedDurationMinutes}
                    onChange={(e) => setSelectedDurationMinutes(Number(e.target.value))}
                    className="w-full h-11 px-3 bg-surface-container-low border border-outline-variant/30 rounded-lg text-sm font-bold text-on-surface outline-none focus:ring-2 focus:ring-primary"
                  >
                    {bookingDurationOptions.map((duration) => (
                      <option key={duration} value={duration}>{duration} minutes</option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            <div className="flex justify-between items-center bg-surface-container rounded-xl p-4 mb-4 border border-outline-variant/10">
              <span className="text-sm font-bold text-on-surface-variant">Estimated Cost</span>
              <span className="text-2xl font-black text-primary">₹{estimatedCost.toFixed(0)}</span>
            </div>

            <button
              onClick={handlePayNow}
              disabled={isProcessing || hasDuplicateLearnerBooking}
              className="w-full h-14 gradient-btn text-white font-extrabold text-lg rounded-xl shadow-lg hover:shadow-xl hover:-translate-y-0.5 disabled:opacity-70 active:scale-[0.98] transition-all flex items-center justify-center gap-3 relative group"
            >
              {isProcessing && <span className="absolute inset-0 bg-white/20 animate-pulse"></span>}
              {isProcessing ? (
                <>
                  <span className="material-symbols-outlined animate-spin text-[24px]">autorenew</span>
                  <span className="text-base font-bold">
                    {loadingStep === 'session' && 'Reserving slot...'}
                    {loadingStep === 'order' && 'Connecting to Razorpay...'}
                    {loadingStep === 'verify' && 'Verifying payment...'}
                  </span>
                </>
              ) : (
                hasDuplicateLearnerBooking ? 'Session already booked for this slot' : (
                  <>
                    <span className="material-symbols-outlined text-[22px]">lock</span>
                    Pay Now - ₹{estimatedCost.toFixed(0)}
                    <span className="material-symbols-outlined text-[20px] group-hover:translate-x-1 transition-transform">arrow_forward</span>
                  </>
                )
              )}
            </button>

            <p className="text-center text-[10px] text-on-surface-variant mt-3 opacity-70">
              Powered by Razorpay • Secure 256-bit encryption
            </p>
          </div>
        )}

        {/* Reviews Section */}
        {reviews.length > 0 && (
          <div className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10">
            <h2 className="text-lg font-extrabold text-on-surface mb-4">Reviews ({reviews.length})</h2>
            <div className="space-y-4">
              {reviews.map((review: any) => (
                <div key={review.id} className="pb-4 border-b border-outline-variant/10 last:border-0 last:pb-0">
                  <div className="flex justify-between items-center mb-1">
                    <span className="font-bold text-sm text-on-surface">{review.learnerName || 'Learner'}</span>
                    <span className="text-xs font-semibold text-on-surface-variant">{new Date(review.createdAt).toLocaleDateString()}</span>
                  </div>
                  <div className="flex text-amber-500 text-[14px] mb-1 gap-0.5">
                    {Array(5).fill(0).map((_, i) => (
                      <span key={i} className={`material-symbols-outlined ${i < review.rating ? '' : 'text-outline-variant/30'}`}>star</span>
                    ))}
                  </div>
                  {review.comment && <p className="text-sm text-on-surface-variant italic">"{review.comment}"</p>}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default MentorDetailPage;

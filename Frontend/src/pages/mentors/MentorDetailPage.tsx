import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import mentorService from '../../services/mentorService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';

const weekdayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

const MentorDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { showToast } = useToast();

  const [showBookingForm, setShowBookingForm] = useState(false);
  const [bookingData, setBookingData] = useState({
    sessionDate: '',
    sessionDuration: 60,
  });

  // Fetch mentor profile
  const { data: mentor, isLoading } = useQuery({
    queryKey: ['mentor', 'detail', id],
    queryFn: () => mentorService.getMentorById(Number(id)),
  });

  // Book session mutation
  const bookSessionMutation = useMutation({
    mutationFn: async () => {
      const { default: sessionService } = await import('../../services/sessionService');
      return sessionService.createSession({
        mentorId: Number(id),
        topic: 'Mentoring Session',
        description: `Booked from mentor profile`,
        sessionDate: new Date(bookingData.sessionDate).toISOString(),
        durationMinutes: bookingData.sessionDuration,
      });
    },
    onSuccess: () => {
      showToast({ message: 'Session request sent!', type: 'success' });
      setShowBookingForm(false);
      navigate('/sessions');
    },
    onError: (error: any) => {
      showToast({ message: error.response?.data?.message || 'Failed to book session', type: 'error' });
    },
  });

  const handleBookSession = (e: React.FormEvent) => {
    e.preventDefault();
    if (!bookingData.sessionDate) {
      showToast({ message: 'Please select a session date', type: 'error' });
      return;
    }
    bookSessionMutation.mutate();
  };

  const handleBookWithCheckout = () => {
    if (!mentor) return;
    const m = mentor as any;
    navigate('/checkout', {
      state: {
        mentorId: m.id,
        mentorName: `${m.firstName || ''} ${m.lastName || ''}`.trim() || `Mentor #${m.id}`,
        startTime: new Date().toISOString(),
        hourlyRate: Number(m.hourlyRate || 0),
      },
    });
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
  const mentorDisplayName = `${m.firstName || ''} ${m.lastName || ''}`.trim() || `Mentor #${m.id}`;
  const mentorRating = Number(m.rating ?? m.avgRating ?? 0);
  const mentorReviews = Number(m.reviewCount ?? m.totalReviews ?? 0);
  const mentorExperience = Number(m.experience ?? m.experienceYears ?? 0);
  const slots = m.availability || [];
  const getInitials = (first?: string, last?: string) => {
    return `${first?.[0] || ''}${last?.[0] || ''}`.toUpperCase() || 'M';
  };

  return (
    <PageLayout>
      <div className="max-w-4xl mx-auto space-y-6">
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
          {/* Header gradient */}
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
                  <div className="flex items-center gap-1">
                    <span className="text-amber-500 text-2xl">★</span>
                    <span className="text-2xl font-black text-on-surface">{mentorRating.toFixed(1)}</span>
                  </div>
                  <p className="text-xs font-semibold text-on-surface-variant">{mentorReviews} reviews</p>
                </div>
              </div>
            </div>

            <p className="text-on-surface-variant mb-6 leading-relaxed">{m.bio}</p>

            <div className="grid grid-cols-2 gap-4 mb-6">
              <div className="bg-primary/5 rounded-xl p-4 border border-primary/10">
                <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-0.5">Hourly Rate</p>
                <p className="text-2xl font-black text-primary">₹{m.hourlyRate}<span className="text-xs font-semibold text-on-surface-variant">/hr</span></p>
              </div>
              <div className="bg-emerald-50 rounded-xl p-4 border border-emerald-100">
                <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-0.5">Status</p>
                <p className="text-2xl font-black text-emerald-600">
                  {m.status === 'APPROVED' ? 'Verified' : m.status || 'Pending'}
                </p>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setShowBookingForm(true)}
                className="flex-1 h-12 gradient-btn text-white font-extrabold rounded-xl shadow-md hover:shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2"
              >
                <span className="material-symbols-outlined text-[20px]">event</span>
                Book a Session
              </button>
              <button
                onClick={handleBookWithCheckout}
                className="h-12 px-6 bg-surface-container-high hover:bg-surface-container text-on-surface font-bold rounded-xl transition-all active:scale-95 flex items-center justify-center gap-2 border border-outline-variant/20"
              >
                <span className="material-symbols-outlined text-[20px]">credit_card</span>
                Quick Pay
              </button>
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

        {/* Availability Slots */}
        <div className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10">
          <h2 className="text-lg font-extrabold text-on-surface mb-4">Weekly Availability</h2>
          {slots.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {[...slots]
                .sort((a: any, b: any) => {
                  if (a.dayOfWeek !== b.dayOfWeek) return a.dayOfWeek - b.dayOfWeek;
                  return String(a.startTime).localeCompare(String(b.startTime));
                })
                .filter((slot: any) => slot.isActive !== false)
                .map((slot: any) => (
                  <div key={slot.id} className="flex items-center justify-between rounded-xl border border-outline-variant/10 bg-surface-container-low p-4 hover:border-primary/20 transition-colors">
                    <div>
                      <p className="font-bold text-on-surface">{weekdayNames[slot.dayOfWeek] || 'Unknown'}</p>
                      <p className="text-sm text-on-surface-variant mt-0.5">
                        {String(slot.startTime).slice(0, 5)} – {String(slot.endTime).slice(0, 5)}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className={`text-[10px] font-black uppercase tracking-widest px-2 py-1 rounded-md ${
                        slot.isBooked
                          ? 'bg-red-100 text-red-700 border border-red-200'
                          : 'bg-emerald-100 text-emerald-700 border border-emerald-200'
                      }`}>
                        {slot.isBooked ? 'Booked' : 'Available'}
                      </span>
                    </div>
                  </div>
                ))}
            </div>
          ) : (
            <div className="rounded-xl border border-dashed border-outline-variant/30 p-8 text-center text-on-surface-variant">
              <span className="material-symbols-outlined text-3xl text-outline-variant mb-2 block">calendar_month</span>
              <p className="font-semibold">No availability slots set yet</p>
              <p className="text-sm mt-1">This mentor hasn't configured their availability schedule.</p>
            </div>
          )}
        </div>

        {/* Booking Modal */}
        {showBookingForm && (
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-50">
            <div className="bg-surface-container-lowest rounded-2xl p-8 max-w-md w-full shadow-2xl border border-outline-variant/10 animate-in slide-in-from-bottom-4 duration-300">
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-extrabold text-on-surface">Book a Session</h2>
                <button onClick={() => setShowBookingForm(false)} className="w-9 h-9 rounded-full bg-surface-container hover:bg-surface-container-high flex items-center justify-center transition-colors">
                  <span className="material-symbols-outlined text-[18px]">close</span>
                </button>
              </div>

              <form onSubmit={handleBookSession} className="space-y-4">
                <div>
                  <label className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">Session Date & Time</label>
                  <input
                    type="datetime-local"
                    value={bookingData.sessionDate}
                    onChange={(e) => setBookingData({ ...bookingData, sessionDate: e.target.value })}
                    className="w-full h-12 bg-surface-container border border-outline-variant/20 rounded-xl px-4 text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary transition-all"
                    required
                  />
                </div>

                <div>
                  <label className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">Duration</label>
                  <select
                    value={bookingData.sessionDuration}
                    onChange={(e) => setBookingData({ ...bookingData, sessionDuration: parseInt(e.target.value) })}
                    className="w-full h-12 bg-surface-container border border-outline-variant/20 rounded-xl px-4 text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary transition-all"
                  >
                    <option value="30">30 minutes</option>
                    <option value="60">1 hour</option>
                    <option value="90">1.5 hours</option>
                    <option value="120">2 hours</option>
                  </select>
                </div>

                <div className="bg-primary/5 rounded-xl p-4 border border-primary/10">
                  <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-0.5">Estimated Cost</p>
                  <p className="text-2xl font-black text-primary">
                    ₹{((Number(m.hourlyRate || 0) * bookingData.sessionDuration) / 60).toFixed(0)}
                  </p>
                </div>

                <div className="flex gap-3 pt-2">
                  <button
                    type="submit"
                    disabled={bookSessionMutation.isPending}
                    className="flex-1 h-12 gradient-btn text-white font-bold rounded-xl shadow-md transition-all active:scale-95 disabled:opacity-50"
                  >
                    {bookSessionMutation.isPending ? 'Booking...' : 'Request Session'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowBookingForm(false)}
                    className="flex-1 h-12 bg-surface-container-high text-on-surface font-bold rounded-xl transition-all active:scale-95"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default MentorDetailPage;

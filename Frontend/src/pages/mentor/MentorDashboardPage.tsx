import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import PageLayout from '../../components/layout/PageLayout';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';

const MentorDashboardPage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const navigate = useNavigate();

  // For Inline Reject Confirm
  const [rejectingId, setRejectingId] = useState<number | null>(null);

  // For Availability form
  const [availDate, setAvailDate] = useState('');
  const [availStart, setAvailStart] = useState('09:00');
  const [availEnd, setAvailEnd] = useState('10:00');

  // Fetch Mentor Profile to get mentorId and rating
  const { data: mentorData } = useQuery({
    queryKey: ['mentor', 'my'],
    queryFn: async () => {
      try {
        const res = await api.get('/api/mentors/me', { _skipErrorRedirect: true } as any);
        return res.data;
      } catch (e) {
        return null;
      }
    }
  });

  const mentorId = mentorData?.id;

  // Stats Queries
  const { data: totalSessionsObj } = useQuery({
    queryKey: ['sessions', 'total'],
    queryFn: async () => {
      const res = await api.get('/api/sessions/mentor?page=0&size=1', { _skipErrorRedirect: true } as any);
      return res.data;
    }
  });

  const { data: pendingReqsObj } = useQuery({
    queryKey: ['sessions', 'requested'],
    queryFn: async () => {
      const res = await api.get('/api/sessions/mentor?page=0&size=50', { _skipErrorRedirect: true } as any);
      const allSessions = res.data?.content || [];
      const requested = allSessions.filter((s: any) => s.status === 'REQUESTED');
      return { ...res.data, content: requested.slice(0, 5), totalElements: requested.length };
    }
  });

  const { data: upcomingObj } = useQuery({
    queryKey: ['sessions', 'accepted'],
    queryFn: async () => {
      const res = await api.get('/api/sessions/mentor?page=0&size=50', { _skipErrorRedirect: true } as any);
      const allSessions = res.data?.content || [];
      const accepted = allSessions.filter((s: any) => s.status === 'ACCEPTED');
      return { ...res.data, content: accepted.slice(0, 5), totalElements: accepted.length };
    }
  });

  // Reviews Query
  const { data: recentReviewsObj } = useQuery({
    queryKey: ['reviews', mentorId],
    queryFn: async () => {
      const res = await api.get(`/api/reviews/mentor/${mentorId}?page=0&size=3`, { _skipErrorRedirect: true } as any);
      return res.data;
    },
    enabled: !!mentorId
  });

  // Get Availability query (if not included in mentorData)
  const availableSlots = mentorData?.availableSlots || [];

  // Mutations
  const acceptMutation = useMutation({
    mutationFn: async (id: number) => api.put(`/api/sessions/${id}/accept`, undefined, { _skipErrorRedirect: true } as any),
    onSuccess: () => {
      showToast({ message: 'Session accepted!', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['sessions'] });
    }
  });

  const rejectMutation = useMutation({
    mutationFn: async (id: number) => api.put(`/api/sessions/${id}/reject`, undefined, { _skipErrorRedirect: true } as any),
    onSuccess: () => {
      showToast({ message: 'Session rejected.', type: 'success' });
      setRejectingId(null);
      queryClient.invalidateQueries({ queryKey: ['sessions'] });
    }
  });

  const completeMutation = useMutation({
    mutationFn: async (id: number) => api.put(`/api/sessions/${id}/complete`, undefined, { _skipErrorRedirect: true } as any),
    onSuccess: () => {
      showToast({ message: 'Session marked complete!', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['sessions'] });
    }
  });

  const addSlotMutation = useMutation({
    mutationFn: async () => {
      // Must be ISO strings
      // e.g., 2026-03-28T09:00:00
      const startDateTime = new Date(`${availDate}T${availStart}:00`).toISOString();
      const endDateTime = new Date(`${availDate}T${availEnd}:00`).toISOString();
      return api.post('/api/mentors/me/availability', { startTime: startDateTime, endTime: endDateTime }, { _skipErrorRedirect: true } as any);
    },
    onSuccess: () => {
      showToast({ message: 'Slot added!', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['mentor', 'my'] });
    },
    onError: () => {
      showToast({ message: 'Failed to add slot. Check your times.', type: 'error' });
    }
  });

  const deleteSlotMutation = useMutation({
    mutationFn: async (slotId: number) => api.delete(`/api/mentors/me/availability/${slotId}`, { _skipErrorRedirect: true } as any),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mentor', 'my'] });
      showToast({ message: 'Slot removed.', type: 'success' });
    }
  });

  const getInitials = (name?: string) => {
    if (!name) return 'U';
    const p = name.split(' ');
    return p.length > 1 ? `${p[0][0]}${p[1][0]}`.toUpperCase() : p[0][0].toUpperCase();
  };

  const pendingRequests = pendingReqsObj?.content || [];
  const upcomingSessions = upcomingObj?.content || [];
  const recentReviews = recentReviewsObj?.content || [];
  const mentorRating = Number(mentorData?.avgRating || 0);

  const getSessionDisplayName = (session: any) => {
    if (session.learnerName) return session.learnerName;
    if (session.learnerId) return `Learner #${session.learnerId}`;
    return 'Learner';
  };

  const getSessionDate = (session: any) => {
    const raw = session.startTime || session.sessionDate;
    return raw ? new Date(raw) : null;
  };

  const rightPanel = (
    <>
      {/* Recent Reviews */}
      <div className="bg-surface-container-lowest p-6 rounded-2xl shadow-sm border border-outline-variant/15">
        <h3 className="font-bold text-lg text-on-surface mb-4">Recent Reviews</h3>
        
        {recentReviews.length > 0 ? (
          <div className="space-y-4">
            {recentReviews.map((review: any) => (
              <div key={review.id} className="pb-4 border-b border-outline-variant/10 last:border-0 last:pb-0">
                <div className="flex justify-between items-center mb-1">
                  <span className="font-bold text-sm text-on-surface">{review.learnerName || `Learner #${review.reviewerId}`}</span>
                  <span className="text-xs font-semibold text-on-surface-variant">{new Date(review.createdAt).toLocaleDateString()}</span>
                </div>
                <div className="flex text-amber-500 text-[12px] mb-1">
                  {Array(5).fill(0).map((_, i) => (
                    <span key={i} className={i < review.rating ? 'material-symbols-outlined' : 'material-symbols-outlined text-outline-variant/30'}>
                      star
                    </span>
                  ))}
                </div>
                <p className="text-xs text-on-surface-variant italic line-clamp-2">"{review.comment}"</p>
              </div>
            ))}
            {mentorId && (
              <button 
                onClick={() => navigate(`/mentors/${mentorId}`)}
                className="w-full text-center text-sm font-bold text-primary hover:underline block pt-2"
              >
                View All Profile Reviews
              </button>
            )}
          </div>
        ) : (
          <p className="text-sm font-medium text-on-surface-variant italic text-center py-4">No reviews received yet.</p>
        )}
      </div>

      {/* Mark Sessions Complete Helper */}
      {upcomingSessions.length > 0 && (
        <div className="bg-primary/5 p-6 rounded-2xl shadow-sm border border-primary/20">
          <h3 className="font-bold text-lg text-primary mb-2 flex items-center gap-2">
            <span className="material-symbols-outlined">task_alt</span> Post-Session
          </h3>
          <p className="text-xs text-on-surface-variant font-medium mb-4 leading-relaxed">
            Did you just finish a session? Remember to mark it as completed so the learner can leave you a review.
          </p>
        </div>
      )}
    </>
  );

  return (
    <PageLayout rightPanel={rightPanel}>
      <div className="mb-2 w-full flex justify-between items-end">
        <div>
          <h1 className="text-4xl font-extrabold text-on-surface tracking-tight mb-2">Mentor Dashboard</h1>
          <p className="text-on-surface-variant text-lg">Manage requests, view your schedule, and set availability.</p>
        </div>
        {mentorId && (
          <button onClick={() => navigate(`/mentors/${mentorId}`)} className="hidden md:flex items-center gap-2 bg-surface-container hover:bg-surface-container-high px-4 py-2 rounded-xl text-sm font-bold shadow-sm transition-colors border border-outline-variant/10 text-on-surface">
            View Public Profile <span className="material-symbols-outlined text-[18px]">open_in_new</span>
          </button>
        )}
      </div>

      {/* Stats Row */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-2">
        <div className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10 flex flex-col items-center justify-center text-center">
          <span className="text-4xl font-black text-on-surface mb-1">{totalSessionsObj?.totalElements || 0}</span>
          <span className="text-xs font-bold text-on-surface-variant uppercase tracking-widest">Total Sessions</span>
        </div>
        <div className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10 flex flex-col items-center justify-center text-center">
          <span className="text-4xl font-black text-primary mb-1">{mentorRating.toFixed(1)} <span className="text-amber-500 text-3xl">★</span></span>
          <span className="text-xs font-bold text-on-surface-variant uppercase tracking-widest">Average Rating</span>
        </div>
        <div className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10 flex flex-col items-center justify-center text-center">
          <span className={`text-4xl font-black mb-1 ${pendingReqsObj?.totalElements > 0 ? 'text-amber-500' : 'text-emerald-500'}`}>
            {pendingReqsObj?.totalElements || 0}
          </span>
          <span className="text-xs font-bold text-on-surface-variant uppercase tracking-widest">Pending Requests</span>
        </div>
      </section>

      {/* Pending Requests */}
      <section className="mb-4">
        <div className="flex items-center gap-3 mb-4">
          <h2 className="text-xl font-bold text-on-surface">Action Required</h2>
          {pendingRequests.length > 0 && (
            <span className="bg-error text-white text-xs font-bold px-2 py-0.5 rounded-full">{pendingRequests.length} Pending</span>
          )}
        </div>

        <div className="space-y-4">
          {pendingRequests.length > 0 ? (
            pendingRequests.map((req: any) => (
              <div key={req.id} className="bg-surface-container-lowest rounded-xl p-5 shadow-sm border border-amber-500/20 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-full bg-gradient-to-br from-amber-400 to-orange-500 text-white flex items-center justify-center font-bold text-lg shadow-sm shrink-0">
                    {getInitials(getSessionDisplayName(req))}
                  </div>
                  <div>
                    <h4 className="font-bold text-on-surface leading-tight text-lg">{getSessionDisplayName(req)}</h4>
                    <p className="text-xs font-semibold text-on-surface-variant mt-0.5 flex items-center gap-1">
                      <span className="material-symbols-outlined text-[14px]">calendar_today</span>
                      {getSessionDate(req)?.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })} • {getSessionDate(req)?.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })} ({req.durationMinutes || 60} min)
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-end md:self-auto">
                  {rejectingId === req.id ? (
                    <div className="flex items-center gap-2 bg-error/10 p-2 rounded-lg py-1 px-3">
                      <span className="text-xs font-bold text-error mr-2">Confirm?</span>
                      <button onClick={() => rejectMutation.mutate(req.id)} disabled={rejectMutation.isPending} className="text-xs font-bold bg-error text-white px-3 py-1.5 rounded-md hover:bg-error/90 transition-colors shadow-sm">Yes</button>
                      <button onClick={() => setRejectingId(null)} className="text-xs font-bold text-on-surface-variant hover:text-on-surface px-2 py-1.5">No</button>
                    </div>
                  ) : (
                    <>
                      <button 
                        onClick={() => setRejectingId(req.id)}
                        className="bg-surface-container hover:bg-surface-container-high text-on-surface px-5 py-2 rounded-lg text-sm font-bold shadow-sm transition-colors border border-outline-variant/10"
                      >
                        Reject
                      </button>
                      <button 
                        onClick={() => acceptMutation.mutate(req.id)}
                        disabled={acceptMutation.isPending}
                        className="gradient-btn text-white px-5 py-2 rounded-lg text-sm font-bold shadow-sm hover:shadow-md transition-all active:scale-95 disabled:opacity-50"
                      >
                        Accept Request
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))
          ) : (
            <div className="bg-emerald-50 border border-emerald-200/50 rounded-xl p-8 text-center flex flex-col items-center">
              <span className="material-symbols-outlined text-4xl text-emerald-500 mb-3">check_circle</span>
              <p className="font-bold text-emerald-800">You're all caught up!</p>
              <p className="text-sm text-emerald-600/80 font-medium">No pending requests require your attention right now.</p>
            </div>
          )}
        </div>
      </section>

      {/* Upcoming Sessions */}
      <section className="mb-4">
        <h2 className="text-xl font-bold text-on-surface mb-4">Upcoming Sessions</h2>
        <div className="space-y-4">
          {upcomingSessions.length > 0 ? (
            upcomingSessions.map((session: any) => (
              <div key={session.id} className="bg-surface-container-lowest rounded-xl p-5 shadow-sm border border-outline-variant/10 flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-full bg-surface-container-highest text-on-surface flex items-center justify-center font-bold text-lg shadow-sm shrink-0">
                    {getInitials(getSessionDisplayName(session))}
                  </div>
                  <div>
                    <h4 className="font-bold text-on-surface leading-tight text-lg">{getSessionDisplayName(session)}</h4>
                    <p className="text-xs font-semibold text-on-surface-variant mt-0.5 flex items-center gap-1">
                      <span className="material-symbols-outlined text-[14px]">calendar_today</span>
                      {getSessionDate(session)?.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })} • {getSessionDate(session)?.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-end md:self-auto">
                  <button className="bg-surface-container hover:bg-surface-container-high text-on-surface px-5 py-2.5 rounded-lg text-sm font-bold shadow-sm transition-colors border border-outline-variant/10 flex items-center gap-2">
                    <span className="material-symbols-outlined text-[18px]">videocam</span> Join Call
                  </button>
                  <button 
                    onClick={() => completeMutation.mutate(session.id)}
                    disabled={completeMutation.isPending}
                    className="text-emerald-600 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 px-4 py-2.5 rounded-lg text-sm font-bold transition-colors disabled:opacity-50 whitespace-nowrap"
                  >
                    Mark Comp
                  </button>
                </div>
              </div>
            ))
          ) : (
            <p className="text-sm font-medium text-on-surface-variant px-2">No upcoming confirmed sessions.</p>
          )}
        </div>
      </section>

      {/* Availability Manager */}
      <section id="availability" className="pt-4 scroll-mt-24">
        <div className="bg-surface-container-lowest rounded-2xl p-6 md:p-8 shadow-sm border border-outline-variant/15">
          <h3 className="text-2xl font-extrabold text-on-surface mb-6">Manage Availability</h3>
          
          <div className="flex flex-col md:flex-row gap-4 items-end mb-8 bg-surface-container-low/50 p-5 rounded-xl border border-outline-variant/10">
            <div className="flex-1 w-full">
              <label className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">Date</label>
              <input 
                type="date" 
                value={availDate}
                min={new Date().toISOString().split('T')[0]}
                onChange={(e) => setAvailDate(e.target.value)}
                className="w-full h-10 px-3 bg-surface-container rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
              />
            </div>
            <div className="flex-1 w-full">
              <label className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">Start Time</label>
              <select 
                value={availStart}
                onChange={(e) => setAvailStart(e.target.value)}
                className="w-full h-10 px-3 bg-surface-container rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
              >
                {Array(30).fill(0).map((_, i) => {
                  const hour = Math.floor(i / 2) + 7; // 7 AM to 21:00 (9 PM)
                  const minute = i % 2 === 0 ? '00' : '30';
                  const label = `${hour > 12 ? hour - 12 : hour}:${minute} ${hour >= 12 ? 'PM' : 'AM'}`;
                  const value = `${hour.toString().padStart(2, '0')}:${minute}`;
                  return <option key={value} value={value}>{label}</option>;
                })}
              </select>
            </div>
            <div className="flex-1 w-full">
              <label className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">End Time</label>
              <select 
                value={availEnd}
                onChange={(e) => setAvailEnd(e.target.value)}
                className="w-full h-10 px-3 bg-surface-container rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
              >
                {Array(30).fill(0).map((_, i) => {
                  const hour = Math.floor(i / 2) + 7; // 7 AM to 21:00 (9 PM)
                  const minute = i % 2 === 0 ? '00' : '30';
                  const label = `${hour > 12 ? hour - 12 : hour}:${minute} ${hour >= 12 ? 'PM' : 'AM'}`;
                  const value = `${hour.toString().padStart(2, '0')}:${minute}`;
                  return <option key={value} value={value}>{label}</option>;
                })}
              </select>
            </div>
            <button 
              onClick={() => addSlotMutation.mutate()}
              disabled={!availDate || addSlotMutation.isPending}
              className="w-full md:w-auto h-10 px-6 gradient-btn text-white font-bold rounded-lg shadow-sm hover:shadow-md transition-all active:scale-95 disabled:opacity-50 shrink-0"
            >
              Add Slot
            </button>
          </div>

          <div>
            <h4 className="font-bold text-on-surface mb-3 flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px]">list_alt</span> Current Open Slots
            </h4>
            
            {availableSlots.length > 0 ? (
              <div className="max-h-64 overflow-y-auto pr-2 rounded-xl border border-outline-variant/10">
                {availableSlots
                  .sort((a: any, b: any) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
                  .map((slot: any) => {
                  const sTime = new Date(slot.startTime);
                  const eTime = new Date(slot.endTime);
                  const isPast = sTime < new Date();
                  
                  return (
                    <div key={slot.id} className={`flex justify-between items-center py-3 px-4 border-b border-outline-variant/10 last:border-0 hover:bg-surface-container-lowest transition-colors ${isPast ? 'opacity-50' : ''}`}>
                      <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-4">
                        <span className="font-bold text-sm text-on-surface w-24">{sTime.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })}</span>
                        <span className="text-sm font-semibold text-on-surface-variant flex items-center gap-1">
                          <span className="material-symbols-outlined text-[14px]">schedule</span>
                          {sTime.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit'})} – {eTime.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit'})}
                        </span>
                      </div>
                      <div className="flex items-center gap-3">
                        {slot.isBooked && (
                          <span className="bg-primary/10 text-primary border border-primary/20 text-[10px] font-black uppercase tracking-widest px-2 py-0.5 rounded shadow-sm">
                            Booked
                          </span>
                        )}
                        {!slot.isBooked ? (
                          <button 
                            onClick={() => deleteSlotMutation.mutate(slot.id)}
                            disabled={deleteSlotMutation.isPending}
                            className="p-1.5 rounded-lg text-on-surface-variant hover:bg-error/10 hover:text-error transition-colors"
                            title="Remove Slot"
                          >
                            <span className="material-symbols-outlined text-[18px]">delete</span>
                          </button>
                        ) : (
                          <div className="w-8"></div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="text-sm font-medium text-on-surface-variant italic border border-dashed border-outline-variant/30 rounded-xl p-8 text-center bg-surface-container-lowest">
                You haven't added any available time slots yet.
              </p>
            )}
          </div>
        </div>
      </section>

    </PageLayout>
  );
};

export default MentorDashboardPage;

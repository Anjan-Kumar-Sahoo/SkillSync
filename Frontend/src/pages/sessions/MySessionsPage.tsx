import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import PageLayout from '../../components/layout/PageLayout';
import ReviewModal from '../../components/modals/ReviewModal';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';
import type { RootState } from '../../store';


type Tab = 'Upcoming' | 'Pending' | 'Completed' | 'Cancelled';

const statusMap: Record<Tab, string> = {
  Upcoming: 'ACCEPTED',
  Pending: 'REQUESTED',
  Completed: 'COMPLETED',
  Cancelled: 'CANCELLED',
};

const MySessionsPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const role = useSelector((state: RootState) => state.auth.role);
  const isMentor = role === 'ROLE_MENTOR';

  const tabs: Tab[] = ['Upcoming', 'Pending', 'Completed', 'Cancelled'];
  const [activeTab, setActiveTab] = useState<Tab>('Upcoming');
  const [page, setPage] = useState(0);

  const [reviewModalData, setReviewModalData] = useState<{ isOpen: boolean; sessionId: number; mentorId: number }>({
    isOpen: false,
    sessionId: 0,
    mentorId: 0,
  });

  const { data, isLoading, isError } = useQuery({
    queryKey: ['sessions', role || 'unknown', activeTab, page],
    queryFn: async () => {
      const status = statusMap[activeTab];
      const endpoint = isMentor ? '/api/sessions/mentor' : '/api/sessions/learner';
      const res = await api.get(`${endpoint}?page=${page}&size=50`);
      const allSessions = res.data?.content || [];
      const filtered = allSessions.filter((s: any) => s.status === status);
      return {
        ...res.data,
        content: filtered.slice(0, 10),
        totalElements: filtered.length,
      };
    },
    refetchInterval: activeTab === 'Pending' ? 30000 : false,
  });

  const cancelMutation = useMutation({
    mutationFn: async (id: number) => {
      const res = await api.put(`/api/sessions/${id}/cancel`);
      return res.data;
    },
    onSuccess: () => {
      showToast({ message: 'Session cancelled successfully.', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['sessions'] });
    },
    onError: () => showToast({ message: 'Failed to cancel session.', type: 'error' }),
  });

  const getInitials = (name?: string) => {
    if (!name) return 'U';
    const p = name.split(' ');
    return p.length > 1 ? `${p[0][0]}${p[1][0]}`.toUpperCase() : p[0][0].toUpperCase();
  };

  const getAvatarColor = (name?: string) => {
    const colors = ['bg-blue-500', 'bg-emerald-500', 'bg-violet-500', 'bg-amber-500', 'bg-rose-500'];
    return colors[name ? name.charCodeAt(0) % colors.length : 0];
  };

  const getSessionLabel = (session: any) => {
    if (isMentor) {
      return session.learnerName || (session.learnerId ? `Learner #${session.learnerId}` : 'Learner');
    }

    return session.mentorName || (session.mentorId ? `Mentor #${session.mentorId}` : 'Mentor');
  };

  const handleMentorSessionAction = async (id: number, action: 'accept' | 'reject' | 'complete') => {
    try {
      await api.put(`/api/sessions/${id}/${action}`);
      const actionLabel = action === 'accept' ? 'accepted' : action === 'reject' ? 'rejected' : 'completed';
      showToast({ message: `Session ${actionLabel} successfully.`, type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['sessions'] });
    } catch (error) {
      showToast({ message: `Failed to ${action} session.`, type: 'error' });
    }
  };

  const sessions = data?.content || [];

  return (
    <PageLayout>
      <div className="mb-8">
        <h1 className="text-4xl font-extrabold text-on-surface tracking-tight mb-2">My Sessions</h1>
        <p className="text-on-surface-variant text-lg">
          {isMentor
            ? 'Review learner bookings, accept or reject requests, and manage your completed sessions.'
            : 'Manage your upcoming and past mentoring sessions.'}
        </p>
      </div>

      <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-xl p-1.5 inline-flex gap-1 mb-6 shadow-sm overflow-x-auto max-w-full print:hidden scrollbar-hide">
        {tabs.map((tab) => (
          <button
            key={tab}
            onClick={() => { setActiveTab(tab); setPage(0); }}
            className={`whitespace-nowrap px-5 py-2.5 rounded-lg text-sm font-bold transition-all duration-300 ${
              activeTab === tab
                ? 'bg-primary text-white shadow-md scale-[1.02]'
                : 'text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      <div className="space-y-4">
        {isLoading ? (
          Array(3).fill(0).map((_, i) => (
            <div key={i} className="h-24 bg-surface-container-lowest rounded-xl border border-outline-variant/10 animate-pulse"></div>
          ))
        ) : isError ? (
          <div className="text-center py-10 text-error font-medium">Failed to load sessions.</div>
        ) : sessions.length === 0 ? (
          <div className="bg-surface-container-lowest rounded-xl p-12 text-center border border-outline-variant/10 shadow-sm flex flex-col items-center">
            <span className="material-symbols-outlined text-5xl text-outline-variant mb-4">event_busy</span>
            <p className="font-semibold text-lg text-on-surface mb-2">
              {activeTab === 'Pending' && 'No pending requests'}
              {activeTab === 'Upcoming' && 'No upcoming sessions'}
              {activeTab === 'Completed' && 'No completed sessions yet'}
              {activeTab === 'Cancelled' && 'No cancelled sessions'}
            </p>

          </div>
        ) : (
          sessions.map((session: any) => {
            const displayName = getSessionLabel(session);
            const sessionDateTime = session.startTime || session.sessionDate;
            
            let statusClasses = 'bg-surface-container text-on-surface-variant';
            if (session.status === 'REQUESTED') statusClasses = 'bg-amber-100 text-amber-700';
            if (session.status === 'ACCEPTED') statusClasses = 'bg-blue-100 text-blue-700';
            if (session.status === 'COMPLETED') statusClasses = 'bg-green-100 text-green-800';
            if (session.status === 'CANCELLED') statusClasses = 'bg-red-100 text-red-700';

            return (
              <div key={session.id} className="bg-surface-container-lowest rounded-xl p-5 md:p-6 shadow-sm border border-outline-variant/10 hover:shadow-md hover:border-outline-variant/30 transition-all flex flex-col md:flex-row items-start md:items-center justify-between gap-4 group">
                
                {/* LEFT */}
                <div className="flex items-center gap-4 min-w-[240px]">
                  <div className={`w-12 h-12 rounded-full text-white flex items-center justify-center font-bold text-lg shadow-sm shrink-0 ${getAvatarColor(displayName)}`}>
                    {getInitials(displayName)}
                  </div>
                  <div>
                    <h3 className="font-bold text-on-surface text-lg leading-tight group-hover:text-primary transition-colors">{displayName}</h3>
                    <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-2 text-sm text-on-surface-variant font-medium mt-0.5">
                      <span className="flex items-center"><span className="material-symbols-outlined text-[16px] mr-1">calendar_month</span> {new Date(sessionDateTime).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })}</span>
                      <span className="hidden sm:inline-block w-1 h-1 bg-outline-variant/50 rounded-full"></span>
                      <span className="flex items-center"><span className="material-symbols-outlined text-[16px] mr-1">schedule</span> {new Date(sessionDateTime).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}</span>
                    </div>
                  </div>
                </div>

                {/* CENTER */}
                <div className="flex-1 md:px-6 w-full md:w-auto">
                  <p className="text-sm font-semibold text-on-surface-variant bg-surface-container-low px-3 py-1.5 rounded-lg inline-block border border-outline-variant/5 truncate max-w-full">
                    {session.topic || 'Mentoring Session'}
                  </p>
                </div>

                {/* RIGHT */}
                <div className="flex items-center justify-between md:justify-end gap-4 w-full md:w-auto mt-2 md:mt-0">
                  <span className={`px-3 py-1 rounded-md text-[10px] font-extrabold uppercase tracking-widest ${statusClasses}`}>
                    {session.status}
                  </span>

                  <div className="flex items-center gap-2">
                    {isMentor && session.status === 'REQUESTED' && (
                      <>
                        <button 
                          onClick={() => handleMentorSessionAction(session.id, 'reject')}
                          className="text-error bg-error/10 hover:bg-error/20 px-4 py-2 rounded-lg text-sm font-bold transition-colors border border-transparent hover:border-error/20 shrink-0"
                        >
                          Reject
                        </button>
                        <button 
                          onClick={() => handleMentorSessionAction(session.id, 'accept')}
                          className="bg-primary text-white px-4 py-2 rounded-lg text-sm font-bold transition-colors shrink-0"
                        >
                          Accept Request
                        </button>
                      </>
                    )}

                    {!isMentor && session.status === 'REQUESTED' && (
                      <button 
                        onClick={() => cancelMutation.mutate(session.id)}
                        disabled={cancelMutation.isPending}
                        className="text-error bg-error/10 hover:bg-error/20 px-4 py-2 rounded-lg text-sm font-bold transition-colors border border-transparent hover:border-error/20 shrink-0 disabled:opacity-50"
                      >
                        Cancel request
                      </button>
                    )}

                    {!isMentor && session.status === 'ACCEPTED' && (
                      <>
                        <button className="bg-surface-container-high hover:bg-surface-container-highest text-on-surface px-4 py-2 rounded-lg text-sm font-bold shadow-sm transition-colors border border-outline-variant/10 shrink-0">
                          Join Call
                        </button>
                        <button 
                          onClick={() => cancelMutation.mutate(session.id)}
                          disabled={cancelMutation.isPending}
                          className="text-on-surface-variant hover:text-error hover:bg-error/10 p-2 rounded-lg transition-colors border border-transparent"
                          title="Cancel Session"
                        >
                          <span className="material-symbols-outlined text-[20px]">cancel</span>
                        </button>
                      </>
                    )}

                    {isMentor && session.status === 'ACCEPTED' && (
                      <button 
                        onClick={() => handleMentorSessionAction(session.id, 'complete')}
                        className="bg-emerald-600 text-white px-4 py-2 rounded-lg text-sm font-bold transition-colors shrink-0"
                      >
                        Mark Complete
                      </button>
                    )}

                    {!isMentor && session.status === 'COMPLETED' && (
                      <button 
                        onClick={() => setReviewModalData({ isOpen: true, sessionId: session.id, mentorId: session.mentorId })}
                        className="bg-primary/10 hover:bg-primary/20 text-primary px-4 py-2 rounded-lg text-sm font-bold transition-colors border border-primary/20 shrink-0"
                      >
                        Leave Review
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      <ReviewModal 
        isOpen={reviewModalData.isOpen} 
        onClose={() => setReviewModalData(prev => ({ ...prev, isOpen: false }))} 
        sessionId={reviewModalData.sessionId} 
        mentorId={reviewModalData.mentorId} 
        onSuccess={() => queryClient.invalidateQueries({ queryKey: ['sessions', 'Completed'] })}
      />

    </PageLayout>
  );
};

export default MySessionsPage;

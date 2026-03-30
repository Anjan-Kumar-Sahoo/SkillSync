import { useQuery } from '@tanstack/react-query';
import { useSelector } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../services/axios';
import type { RootState } from '../../store';
import PageLayout from '../../components/layout/PageLayout';

const LearnerDashboardPage = () => {
  const user = useSelector((state: RootState) => state.auth.user);
  const navigate = useNavigate();

  // Queries
  const { data: upSessions, isLoading: loadingUp } = useQuery({
    queryKey: ['sessions', 'upcoming'],
    queryFn: async () => {
      const res = await api.get('/api/sessions?status=ACCEPTED&page=0&size=3');
      return res.data;
    }
  });

  const { data: compSessions } = useQuery({
    queryKey: ['sessions', 'completed'],
    queryFn: async () => {
      const res = await api.get('/api/sessions?status=COMPLETED&page=0&size=1');
      return res.data;
    }
  });

  const { data: mentors, isLoading: loadingMentors } = useQuery({
    queryKey: ['mentors', 'recommended'],
    queryFn: async () => {
      const res = await api.get('/api/mentors?page=0&size=4&sort=rating,desc');
      return res.data;
    }
  });

  const { data: groupsData } = useQuery({
    queryKey: ['groups', 'my'],
    queryFn: async () => {
      try {
        const res = await api.get('/api/groups/my');
        return res.data;
      } catch (e: any) {
        if (e.response?.status === 404) return [];
        return [];
      }
    }
  });

  const groups = Array.isArray(groupsData) ? groupsData : groupsData?.content || [];

  const getInitials = (name?: string) => {
    if (!name) return 'U';
    const parts = name.split(' ');
    return parts.length > 1 ? `${parts[0][0]}${parts[1][0]}`.toUpperCase() : parts[0][0].toUpperCase();
  };

  const getAvatarColor = (name?: string) => {
    const colors = ['bg-blue-500', 'bg-emerald-500', 'bg-violet-500', 'bg-amber-500', 'bg-rose-500'];
    const idx = name ? name.charCodeAt(0) % colors.length : 0;
    return colors[idx];
  };

  const formatDateTime = (iso?: string) => {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) + ' • ' + d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
  };

  const rightPanel = (
    <>
      <div className="bg-primary-container text-white p-6 rounded-2xl relative overflow-hidden shadow-sm">
        <h3 className="font-bold text-lg mb-1 relative z-10">Current Path Progress</h3>
        <p className="text-sm opacity-80 mb-6 relative z-10">Senior UI Architect Track</p>
        <div className="w-full bg-black/20 rounded-full h-2 mb-2 relative z-10">
          <div className="bg-white h-2 rounded-full" style={{ width: '64%' }}></div>
        </div>
        <p className="text-xs font-semibold mb-6 relative z-10">64% Completed</p>
        <button className="w-full bg-white text-primary-container hover:bg-white/90 font-bold py-2.5 rounded-xl transition-all shadow-sm relative z-10">
          Continue Learning
        </button>
        <div className="absolute -top-12 -right-12 w-40 h-40 bg-white/10 rounded-full pointer-events-none"></div>
      </div>

      <div className="bg-surface-container-lowest p-6 rounded-2xl shadow-sm border border-outline-variant/15">
        <h3 className="font-bold text-lg text-on-surface mb-4">My Groups</h3>
        {groups.length === 0 ? (
          <div className="flex flex-col items-center py-6 text-center">
            <span className="material-symbols-outlined text-4xl text-outline-variant mb-2">group_add</span>
            <p className="text-sm font-semibold text-on-surface-variant mb-4">No active groups yet</p>
            <Link to="/groups" className="text-primary border border-primary hover:bg-primary/5 font-bold px-6 py-2 rounded-xl transition-all text-sm">
              Find a Group
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {groups.map((g: any, i: number) => (
              <div key={i} className="flex justify-between items-center text-sm font-semibold p-2 rounded-lg hover:bg-surface-container-low transition-colors">
                <span>{g.name}</span>
                <span className="text-on-surface-variant text-xs">{g.memberCount || 1} members</span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="bg-surface-container-lowest p-6 rounded-2xl shadow-sm border border-outline-variant/15">
        <h3 className="font-bold text-lg text-on-surface mb-4">Quick Stats</h3>
        <div className="mb-6 space-y-2 text-sm font-bold text-on-surface-variant">
          <p>● Sessions Completed — <span className="text-on-surface">{compSessions?.totalElements || 0}</span></p>
          <p>● Skills Tagged — <span className="text-on-surface">{user?.skills?.length || 0}</span></p>
        </div>
        
        <div className="bg-surface-container-low rounded-xl p-4">
          <p className="text-xs font-semibold text-on-surface-variant mb-4 text-center">Activity (Last 7 Days)</p>
          <div className="flex items-end justify-between h-20 gap-1.5">
            <div className="flex-1 bg-primary/20 rounded-t-sm" style={{ height: '30%' }}></div>
            <div className="flex-1 bg-primary/30 rounded-t-sm" style={{ height: '50%' }}></div>
            <div className="flex-1 bg-primary/20 rounded-t-sm" style={{ height: '20%' }}></div>
            <div className="flex-1 bg-primary/40 rounded-t-sm" style={{ height: '70%' }}></div>
            <div className="flex-1 bg-primary/20 rounded-t-sm" style={{ height: '40%' }}></div>
            <div className="flex-1 bg-primary/30 rounded-t-sm" style={{ height: '60%' }}></div>
            <div className="flex-1 bg-primary rounded-t-sm shadow-sm" style={{ height: '90%' }}></div>
          </div>
        </div>
      </div>
    </>
  );

  return (
    <PageLayout rightPanel={rightPanel}>
      {/* Header Section */}
      <section className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
        <div>
          <h1 className="text-3xl font-extrabold text-on-surface tracking-tight">Welcome back, {user?.firstName}!</h1>
          <p className="text-on-surface-variant font-medium mt-1">You're making great progress. Keep it up.</p>
        </div>
        <div className="flex gap-4 w-full md:w-auto overflow-x-auto pb-2 md:pb-0 scrollbar-hide">
          <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-xl px-5 py-3 shadow-sm text-center shrink-0 min-w-[140px]">
            <p className="text-[10px] text-on-surface-variant uppercase tracking-widest font-bold mb-1">Upcoming Sessions</p>
            <p className="text-2xl font-black text-on-surface">{upSessions?.totalElements || 0}</p>
          </div>
          <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-xl px-5 py-3 shadow-sm text-center shrink-0 min-w-[140px]">
            <p className="text-[10px] text-on-surface-variant uppercase tracking-widest font-bold mb-1">Joined Groups</p>
            <p className="text-2xl font-black text-on-surface">{groups.length}</p>
          </div>
        </div>
      </section>

      {/* Upcoming Sessions Section */}
      <section>
        <div className="flex justify-between items-end mb-4">
          <h2 className="text-xl font-bold text-on-surface">Upcoming Sessions</h2>
          {upSessions?.content?.length > 0 && <Link to="/sessions" className="text-sm font-bold text-primary hover:underline">View Schedule</Link>}
        </div>
        
        <div className="space-y-3">
          {loadingUp ? (
            Array(3).fill(0).map((_, i) => (
              <div key={i} className="h-20 rounded-xl bg-surface-container-low animate-pulse"></div>
            ))
          ) : upSessions?.content?.length > 0 ? (
            upSessions.content.map((session: any) => (
              <div key={session.id} className="bg-surface-container-lowest rounded-xl p-4 flex flex-col md:flex-row md:items-center gap-4 shadow-sm border border-outline-variant/10 hover:shadow-md transition-shadow">
                <div className="flex items-center gap-4 flex-1">
                  <div className={`w-10 h-10 rounded-full text-white flex items-center justify-center font-bold shadow-sm shrink-0 ${getAvatarColor(session.mentorName)}`}>
                    {getInitials(session.mentorName)}
                  </div>
                  <div>
                    <h4 className="font-bold text-on-surface">{session.mentorName}</h4>
                    <p className="text-xs font-semibold text-on-surface-variant">{session.topic || 'Mentorship Session'}</p>
                  </div>
                </div>
                <div className="flex items-center justify-between md:justify-end gap-6 w-full md:w-auto">
                  <p className="text-sm font-semibold text-on-surface-variant text-right">{formatDateTime(session.startTime)}</p>
                  <span className="bg-primary-container/20 text-primary-container px-3 py-1 rounded-md text-xs font-bold uppercase tracking-wider">
                    {session.status}
                  </span>
                </div>
              </div>
            ))
          ) : (
            <div className="bg-surface-container-lowest rounded-xl p-8 flex flex-col items-center text-center shadow-sm border border-outline-variant/10">
              <span className="material-symbols-outlined text-4xl text-outline-variant mb-2">calendar_today</span>
              <p className="text-sm font-semibold text-on-surface-variant mb-4">No upcoming sessions</p>
              <button onClick={() => navigate('/mentors')} className="gradient-btn text-white px-6 py-2.5 rounded-xl font-bold hover:shadow-lg transition-all text-sm">
                Find a Mentor
              </button>
            </div>
          )}
        </div>
      </section>

      {/* Recommended Mentors Section */}
      <section>
        <div className="flex justify-between items-end mb-4">
          <h2 className="text-xl font-bold text-on-surface">Recommended Mentors</h2>
          <div className="flex gap-2 text-on-surface-variant">
            <button className="hover:text-primary transition-colors"><span className="material-symbols-outlined">arrow_back</span></button>
            <button className="hover:text-primary transition-colors"><span className="material-symbols-outlined">arrow_forward</span></button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {loadingMentors ? (
            Array(2).fill(0).map((_, i) => (
              <div key={i} className="h-48 rounded-xl bg-surface-container-low animate-pulse"></div>
            ))
          ) : mentors?.content?.map((mnt: any) => (
            <div key={mnt.id} className="bg-surface-container-lowest p-6 rounded-xl shadow-sm border border-transparent hover:border-primary/20 hover:-translate-y-1 transition-all duration-300 flex flex-col">
              <div className="flex items-start gap-4 mb-4">
                <div className="relative">
                  <div className={`w-14 h-14 rounded-xl text-white flex items-center justify-center font-bold text-lg shadow-sm ${getAvatarColor(mnt.firstName)}`}>
                    {getInitials(`${mnt.firstName} ${mnt.lastName}`)}
                  </div>
                  <div className="absolute -bottom-1 -right-1 w-3.5 h-3.5 bg-green-500 rounded-full border-2 border-white"></div>
                </div>
                <div className="flex-1">
                  <div className="flex justify-between items-start">
                    <h3 className="font-bold text-on-surface leading-tight">{mnt.firstName} {mnt.lastName}</h3>
                    <div className="flex items-center gap-1 bg-secondary-container/30 px-2 py-0.5 rounded text-xs font-bold text-on-secondary-container">
                      <span className="material-symbols-outlined text-[14px]">star</span>
                      {mnt.rating?.toFixed(1) || 'NEW'}
                    </div>
                  </div>
                  <p className="text-xs font-medium text-on-surface-variant mt-1 line-clamp-2">{mnt.headline}</p>
                </div>
              </div>
              
              <div className="flex flex-wrap gap-1.5 mb-6">
                {(mnt.skills || []).slice(0, 3).map((skill: string, i: number) => (
                  <span key={i} className="bg-surface-container-low text-on-surface-variant text-[10px] font-bold px-2 py-1 rounded uppercase tracking-wider">
                    {skill}
                  </span>
                ))}
                {(mnt.skills?.length > 3) && (
                  <span className="bg-surface-container-low text-on-surface-variant text-[10px] font-bold px-2 py-1 rounded">+{mnt.skills.length - 3}</span>
                )}
              </div>

              <div className="flex justify-between items-end mt-auto pt-4 border-t border-outline-variant/10">
                <div>
                  <p className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mb-0.5">Starting at</p>
                  <p className="text-lg font-black text-primary">${mnt.hourlyRate}/<span className="text-sm font-semibold text-on-surface-variant">hr</span></p>
                </div>
                <button 
                  onClick={() => navigate(`/mentors/${mnt.id}`)}
                  className="bg-surface-container-high hover:bg-primary hover:text-white px-5 py-2 rounded-lg text-sm font-bold transition-all duration-300"
                >
                  Book Session
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Mobile FAB */}
      <button 
        onClick={() => navigate('/mentors')}
        className="lg:hidden fixed bottom-6 right-6 w-14 h-14 bg-primary text-white rounded-full shadow-2xl flex items-center justify-center hover:scale-105 active:scale-95 transition-transform z-50"
      >
        <span className="material-symbols-outlined text-2xl">search</span>
      </button>

    </PageLayout>
  );
};

export default LearnerDashboardPage;

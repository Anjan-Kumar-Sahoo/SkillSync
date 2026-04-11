import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSelector } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../services/axios';
import type { RootState } from '../../store';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import { formatDateTimeIST } from '../../utils/dateTime';

const LearnerDashboardPage = () => {
  const user = useSelector((state: RootState) => state.auth.user);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const [showApplyForm, setShowApplyForm] = useState(false);
  const [applyData, setApplyData] = useState({
    bio: '',
    experienceYears: 1,
    hourlyRate: 25,
    skillIds: [] as number[],
  });

  // Queries
  const { data: upSessions, isLoading: loadingUp } = useQuery({
    queryKey: ['sessions', 'upcoming'],
    queryFn: async () => {
      const res = await api.get('/api/sessions/learner?page=0&size=50');
      const allSessions = res.data?.content || [];
      const accepted = allSessions.filter((s: any) => s.status === 'ACCEPTED');
      return { ...res.data, content: accepted.slice(0, 3), totalElements: accepted.length };
    }
  });


  const { data: mentors, isLoading: loadingMentors } = useQuery({
    queryKey: ['mentors', 'recommended'],
    queryFn: async () => {
      const res = await api.get('/api/mentors/search?page=0&size=4&sort=avgRating,desc');
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

  const { data: myMentorProfile } = useQuery({
    queryKey: ['mentor', 'my', 'learner-dashboard'],
    queryFn: async () => {
      try {
        const res = await api.get('/api/mentors/me', { _skipErrorRedirect: true } as any);
        return res.data;
      } catch {
        return null;
      }
    },
  });

  const { data: allSkills } = useQuery({
    queryKey: ['skills', 'all', 'apply'],
    queryFn: async () => {
      const size = 200;
      const pagesToFetch = 10;
      const collected: any[] = [];

      for (let page = 0; page < pagesToFetch; page += 1) {
        const res = await api.get(`/api/skills?page=${page}&size=${size}`, { _skipErrorRedirect: true } as any);
        const content = Array.isArray(res.data?.content) ? res.data.content : [];
        if (content.length === 0) break;
        collected.push(...content);
        if (res.data?.last !== false) break;
      }

      const uniqueById = new Map(collected.map((skill: any) => [skill.id, skill]));
      return Array.from(uniqueById.values());
    },
  });

  const applyMutation = useMutation({
    mutationFn: async () => {
      return api.post('/api/mentors/apply', {
        bio: applyData.bio,
        experienceYears: applyData.experienceYears,
        hourlyRate: applyData.hourlyRate,
        skillIds: applyData.skillIds,
      });
    },
    onSuccess: () => {
      showToast({ message: 'Mentor application submitted successfully.', type: 'success' });
      setShowApplyForm(false);
      queryClient.invalidateQueries({ queryKey: ['mentor', 'my', 'learner-dashboard'] });
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to submit mentor application.';
      showToast({ message, type: 'error' });
    },
  });

  const groups = Array.isArray(groupsData) ? groupsData : groupsData?.content || [];
  const mentorStatus = myMentorProfile?.status || null;
  const canReapply = mentorStatus === 'REJECTED' || mentorStatus === 'SUSPENDED';
  const mentorApplied = Boolean(myMentorProfile) && !canReapply;
  const skills = Array.isArray(allSkills) ? allSkills : [];

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
    return formatDateTimeIST(iso);
  };

  const getSessionMentorName = (session: any) => session.mentorName || 'Mentor';
  const getSessionDateTime = (session: any) => session.startTime || session.sessionDate;

  const toggleSkill = (skillId: number) => {
    setApplyData((prev) => ({
      ...prev,
      skillIds: prev.skillIds.includes(skillId)
        ? prev.skillIds.filter((id) => id !== skillId)
        : [...prev.skillIds, skillId].slice(0, 10),
    }));
  };

  const submitMentorApplication = () => {
    if (applyData.bio.trim().length < 50) {
      showToast({ message: 'Bio must be at least 50 characters for mentor application.', type: 'error' });
      return;
    }

    if (applyData.skillIds.length === 0) {
      showToast({ message: 'Select at least one skill to apply as mentor.', type: 'error' });
      return;
    }

    applyMutation.mutate();
  };

  const rightPanel = (
    <>
      <div className="bg-surface-container-lowest p-6 rounded-2xl shadow-sm border border-outline-variant/15">
        <h3 className="font-bold text-lg text-on-surface mb-2">Apply As Mentor</h3>
        {mentorApplied ? (
          <div className={`rounded-xl border p-4 ${
            mentorStatus === 'APPROVED' ? 'border-green-300 bg-green-50' :
            mentorStatus === 'REJECTED' ? 'border-red-300 bg-red-50' :
            'border-amber-300 bg-amber-50'
          }`}>
            <p className="text-sm font-semibold text-on-surface">
              Application Status: <span className={`font-bold ${
                mentorStatus === 'APPROVED' ? 'text-green-600' :
                mentorStatus === 'REJECTED' ? 'text-red-600' :
                'text-amber-600'
              }`}>{mentorStatus || 'PENDING'}</span>
            </p>
            <p className="text-xs text-on-surface-variant mt-2">
              {mentorStatus === 'APPROVED' ? 'Congratulations! You are now a mentor. Log out and back in to access your mentor dashboard.' :
               mentorStatus === 'REJECTED' ? 'Your application was not approved. You may contact support for more details.' :
               'Your application is under review by an admin. You will be notified once it is approved.'}
            </p>
          </div>
        ) : (
          <>
            <p className="text-sm text-on-surface-variant mb-4">
              {canReapply
                ? 'Your mentor role was demoted/rejected. You can submit a fresh mentor application now.'
                : 'Share your expertise and start mentoring learners.'}
            </p>
            <button
              onClick={() => setShowApplyForm(true)}
              className="w-full gradient-btn text-white py-2.5 rounded-xl font-bold"
            >
              {canReapply ? 'Re-apply as Mentor' : 'Start Mentor Application'}
            </button>
          </>
        )}
      </div>

      <div className="bg-surface-container-lowest p-6 rounded-2xl shadow-sm border border-outline-variant/15">
        <h3 className="font-bold text-lg text-on-surface mb-4">Practice Links</h3>
        <div className="space-y-2 text-sm font-semibold">
          <a href="https://leetcode.com" target="_blank" rel="noreferrer" className="block text-primary hover:underline">LeetCode</a>
          <a href="https://www.hackerrank.com" target="_blank" rel="noreferrer" className="block text-primary hover:underline">HackerRank</a>
          <a href="https://www.geeksforgeeks.org" target="_blank" rel="noreferrer" className="block text-primary hover:underline">GeeksforGeeks</a>
          <a href="https://www.codechef.com" target="_blank" rel="noreferrer" className="block text-primary hover:underline">CodeChef</a>
        </div>
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


    </>
  );

  return (
    <PageLayout rightPanel={rightPanel}>
      {showApplyForm && !mentorApplied && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl bg-surface-container-lowest border border-outline-variant/20 shadow-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-2xl font-extrabold text-on-surface">Mentor Application</h2>
              <button onClick={() => setShowApplyForm(false)} className="text-on-surface-variant hover:text-on-surface">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-bold text-on-surface mb-1">Bio (minimum 50 characters)</label>
                <textarea
                  value={applyData.bio}
                  onChange={(e) => setApplyData((prev) => ({ ...prev, bio: e.target.value }))}
                  rows={5}
                  className="w-full rounded-xl border border-outline-variant/30 bg-surface px-4 py-3 text-sm text-on-surface outline-none focus:ring-2 focus:ring-primary/40"
                />
                <p className={`mt-1 text-xs font-semibold ${applyData.bio.trim().length >= 50 ? 'text-emerald-600' : 'text-error'}`}>
                  {applyData.bio.trim().length}/50 characters minimum
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-bold text-on-surface mb-1">Experience (years)</label>
                  <input
                    type="number"
                    min={0}
                    max={50}
                    value={applyData.experienceYears}
                    onChange={(e) => setApplyData((prev) => ({ ...prev, experienceYears: Number(e.target.value) }))}
                    className="w-full rounded-xl border border-outline-variant/30 bg-surface px-4 py-3 text-sm text-on-surface outline-none focus:ring-2 focus:ring-primary/40"
                  />
                </div>
                <div>
                  <label className="block text-sm font-bold text-on-surface mb-1">Hourly Rate (₹ per hour)</label>
                  <input
                    type="number"
                    min={5}
                    max={500}
                    value={applyData.hourlyRate}
                    onChange={(e) => setApplyData((prev) => ({ ...prev, hourlyRate: Number(e.target.value) }))}
                    className="w-full rounded-xl border border-outline-variant/30 bg-surface px-4 py-3 text-sm text-on-surface outline-none focus:ring-2 focus:ring-primary/40"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-bold text-on-surface mb-2">Select Skills (max 10)</label>
                <div className="max-h-56 overflow-y-auto rounded-xl border border-outline-variant/30 p-3">
                  <div className="flex flex-wrap gap-2">
                    {skills.map((skill: any) => {
                      const selected = applyData.skillIds.includes(skill.id);
                      return (
                        <button
                          key={skill.id}
                          type="button"
                          onClick={() => toggleSkill(skill.id)}
                          className={`px-3 py-1.5 rounded-full text-xs font-bold border transition-colors ${
                            selected
                              ? 'bg-primary text-white border-primary'
                              : 'bg-surface-container-low text-on-surface-variant border-outline-variant/30 hover:border-primary/40'
                          }`}
                        >
                          {skill.name}
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  onClick={submitMentorApplication}
                  disabled={applyMutation.isPending}
                  className="flex-1 gradient-btn text-white py-2.5 rounded-xl font-bold disabled:opacity-50"
                >
                  {applyMutation.isPending ? 'Submitting...' : 'Submit Application'}
                </button>
                <button
                  onClick={() => setShowApplyForm(false)}
                  className="flex-1 bg-surface-container text-on-surface py-2.5 rounded-xl font-bold"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Header Section */}
      <section className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
        <div>
          <h1 className="text-3xl font-extrabold text-on-surface tracking-tight">Welcome back, {user?.firstName}!</h1>
          <p className="text-on-surface-variant font-medium mt-1">You're making great progress. Keep it up.</p>
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
                  <div className={`w-10 h-10 rounded-full text-white flex items-center justify-center font-bold shadow-sm shrink-0 ${getAvatarColor(getSessionMentorName(session))}`}>
                    {getInitials(getSessionMentorName(session))}
                  </div>
                  <div>
                    <h4 className="font-bold text-on-surface">{getSessionMentorName(session)}</h4>
                    <p className="text-xs font-semibold text-on-surface-variant">{session.topic || 'Mentorship Session'}</p>
                  </div>
                </div>
                <div className="flex items-center justify-between md:justify-end gap-6 w-full md:w-auto">
                  <p className="text-sm font-semibold text-on-surface-variant text-right">{formatDateTime(getSessionDateTime(session))}</p>
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
          ) : mentors?.content?.map((mnt: any) => {
            const avgRating = Number(mnt.avgRating ?? mnt.rating ?? 0);
            const sessionsHeld = Number(mnt.totalSessions ?? 0);
            const isNewMentor = sessionsHeld === 0;

            return (
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
                      {isNewMentor ? 'NEW' : avgRating.toFixed(1)}
                    </div>
                  </div>
                  <p className="text-xs font-medium text-on-surface-variant mt-1 line-clamp-2">{mnt.headline}</p>
                </div>
              </div>
              
              <div className="flex flex-wrap gap-1.5 mb-6">
                {(mnt.skills || []).slice(0, 3).map((skill: any, i: number) => (
                  <span key={i} className="bg-surface-container-low text-on-surface-variant text-[10px] font-bold px-2 py-1 rounded uppercase tracking-wider">
                    {typeof skill === 'string' ? skill : (skill.name || `Skill #${skill.id}`)}
                  </span>
                ))}
                {(mnt.skills?.length > 3) && (
                  <span className="bg-surface-container-low text-on-surface-variant text-[10px] font-bold px-2 py-1 rounded">+{mnt.skills.length - 3}</span>
                )}
              </div>

              <div className="flex justify-between items-end mt-auto pt-4 border-t border-outline-variant/10">
                <div>
                  <p className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest mb-0.5">Starting at</p>
                  <p className="text-lg font-black text-primary">₹{mnt.hourlyRate}/<span className="text-sm font-semibold text-on-surface-variant">hr</span></p>
                </div>
                <button 
                  onClick={() => navigate(`/mentors/${mnt.id}`)}
                  className="bg-surface-container-high hover:bg-primary hover:text-white px-5 py-2 rounded-lg text-sm font-bold transition-all duration-300"
                >
                  Book Session
                </button>
              </div>
            </div>
            );
          })}
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

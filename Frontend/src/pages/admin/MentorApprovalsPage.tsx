import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import PageLayout from '../../components/layout/PageLayout';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';

const PAGE_SIZE = 6;

const MentorApprovalsPage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const [page, setPage] = useState(0);

  const { data: mentorsData, isLoading } = useQuery({
    queryKey: ['admin', 'mentors', 'pending', page],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.append('page', String(page));
      params.append('size', String(PAGE_SIZE));
      params.append('sort', 'id,asc');

      const { data } = await api.get(`/api/admin/mentors/pending?${params.toString()}`);
      return data;
    },
  });

  const approveMutation = useMutation({
    mutationFn: async (id: number) => {
      await api.post(`/api/admin/mentors/${id}/approve`);
    },
    onSuccess: () => {
      showToast({ message: 'Mentor approved successfully', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['admin', 'mentors', 'pending'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
    },
    onError: () => showToast({ message: 'Failed to approve mentor', type: 'error' }),
  });

  const rejectMutation = useMutation({
    mutationFn: async (id: number) => {
      await api.post(`/api/admin/mentors/${id}/reject?reason=Rejected+by+admin`);
    },
    onSuccess: () => {
      showToast({ message: 'Mentor rejected', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['admin', 'mentors', 'pending'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
    },
    onError: () => showToast({ message: 'Failed to reject mentor', type: 'error' }),
  });

  const pendingMentors = mentorsData?.content || mentorsData || [];
  const mentorsList = Array.isArray(pendingMentors)
    ? [...pendingMentors].sort((a: any, b: any) => Number(a?.id || 0) - Number(b?.id || 0))
    : [];
  const totalElements = Array.isArray(mentorsData?.content)
    ? Number(mentorsData?.totalElements || mentorsList.length || 0)
    : mentorsList.length;
  const totalPages = Array.isArray(mentorsData?.content)
    ? Math.max(1, Number(mentorsData?.totalPages || 1))
    : 1;
  const currentPage = Array.isArray(mentorsData?.content)
    ? Number(mentorsData?.number ?? page)
    : 0;

  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-2xl p-6 shadow-sm">
          <h1 className="text-3xl font-extrabold text-on-surface tracking-tight">Mentor Approvals</h1>
          <p className="text-on-surface-variant mt-2">Review and manage pending mentor applications</p>
          {!isLoading && (
            <p className="mt-3 text-sm font-bold text-primary bg-primary/10 inline-block px-3 py-1 rounded-full">
              {totalElements} pending application{totalElements !== 1 ? 's' : ''}
            </p>
          )}
        </div>

        {isLoading ? (
          <div className="text-center py-10">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-3"></div>
            <p className="text-on-surface-variant">Loading pending mentors...</p>
          </div>
        ) : mentorsList.length === 0 ? (
          <div className="text-center py-16 bg-surface-container-lowest rounded-2xl border border-outline-variant/10 shadow-sm">
            <span className="material-symbols-outlined text-5xl text-outline-variant mb-3 block">how_to_reg</span>
            <h3 className="text-lg font-bold text-on-surface mb-1">All caught up!</h3>
            <p className="text-sm text-on-surface-variant">No pending mentor applications to review.</p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {mentorsList.map((mentor: any) => (
                <div key={mentor.id} className="bg-surface-container-lowest border border-outline-variant/10 rounded-2xl p-6 shadow-sm flex flex-col justify-between hover:shadow-md transition-shadow">
                <div>
                  <div className="flex items-start justify-between mb-4">
                    <div>
                      <h3 className="text-lg font-extrabold text-on-surface">
                        {mentor.firstName && mentor.lastName
                          ? `${mentor.firstName} ${mentor.lastName}`
                          : 'Mentor'}
                      </h3>
                      <p className="text-xs font-semibold text-on-surface-variant">
                        #{mentor.id} •
                        {' '}
                        {mentor.email || `User ID: ${mentor.userId}`}
                      </p>
                    </div>
                    <span className="text-[10px] font-black uppercase tracking-widest px-2 py-1 rounded-md bg-amber-100 text-amber-700 border border-amber-200">
                      Pending
                    </span>
                  </div>

                  <div className="space-y-3 mb-6">
                    <div className="grid grid-cols-2 gap-3">
                      <div className="bg-surface-container rounded-lg p-3">
                        <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-0.5">Experience</p>
                        <p className="text-sm font-bold text-on-surface">{mentor.experienceYears || 0} years</p>
                      </div>
                      <div className="bg-surface-container rounded-lg p-3">
                        <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-0.5">Hourly Rate</p>
                        <p className="text-sm font-bold text-primary">₹{mentor.hourlyRate || 'N/A'}/hr</p>
                      </div>
                    </div>

                    <div>
                      <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-1">Bio</p>
                      <p className="text-sm text-on-surface-variant line-clamp-3">{mentor.bio || 'No bio provided'}</p>
                    </div>

                    <div>
                      <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-1">Skills</p>
                      <div className="flex flex-wrap gap-1.5">
                        {(mentor.skills || []).map((skill: any, index: number) => (
                          <span key={index} className="bg-surface-container text-on-surface-variant text-[10px] font-bold px-2 py-1 rounded-md uppercase tracking-wider border border-outline-variant/10">
                            {typeof skill === 'string' ? skill : skill.name || `Skill #${skill.skillId || skill.id}`}
                          </span>
                        ))}
                        {(!mentor.skills || mentor.skills.length === 0) && (
                          <span className="text-xs text-on-surface-variant/50">None listed</span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>

                <div className="flex gap-3 pt-4 border-t border-outline-variant/10 mt-auto">
                  <button
                    onClick={() => approveMutation.mutate(mentor.id)}
                    disabled={approveMutation.isPending}
                    className="flex-1 h-10 bg-green-600 hover:bg-green-700 text-white font-bold rounded-xl transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    <span className="material-symbols-outlined text-[18px]">check_circle</span>
                    Approve
                  </button>
                  <button
                    onClick={() => rejectMutation.mutate(mentor.id)}
                    disabled={rejectMutation.isPending}
                    className="flex-1 h-10 bg-red-100 hover:bg-red-200 text-red-700 font-bold rounded-xl transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    <span className="material-symbols-outlined text-[18px]">cancel</span>
                    Reject
                  </button>
                </div>
                </div>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="px-5 py-3 border border-outline-variant/10 rounded-xl bg-surface-container-lowest flex items-center justify-between gap-3">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={currentPage <= 0}
                  className="px-3 py-1.5 rounded-md text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <p className="text-xs font-semibold text-on-surface-variant">
                  Page {currentPage + 1} of {totalPages}
                </p>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={currentPage >= totalPages - 1}
                  className="px-3 py-1.5 rounded-md text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </PageLayout>
  );
};

export default MentorApprovalsPage;

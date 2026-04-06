import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import PageLayout from '../../components/layout/PageLayout';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';

const MentorApprovalsPage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  const { data: mentorsData, isLoading } = useQuery({
    queryKey: ['admin', 'mentors', 'pending'],
    queryFn: async () => {
      const { data } = await api.get('/api/admin/mentors/pending');
      return data;
    },
  });

  const approveMutation = useMutation({
    mutationFn: async (id: number) => {
      await api.post(`/api/admin/mentors/${id}/approve`);
    },
    onSuccess: () => {
      showToast({ message: 'Mentor approved', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['admin', 'mentors', 'pending'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
    },
    onError: () => showToast({ message: 'Failed to approve mentor', type: 'error' }),
  });

  const rejectMutation = useMutation({
    mutationFn: async (id: number) => {
      await api.post(`/api/admin/mentors/${id}/reject`);
    },
    onSuccess: () => {
      showToast({ message: 'Mentor rejected', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['admin', 'mentors', 'pending'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
    },
    onError: () => showToast({ message: 'Failed to reject mentor', type: 'error' }),
  });

  const pendingMentors = mentorsData?.content || mentorsData || [];
  const mentorsList = Array.isArray(pendingMentors) ? pendingMentors : [];

  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
          <h1 className="text-2xl font-bold text-gray-900">Approve Mentors</h1>
          <p className="text-gray-600 mt-2">Review and manage pending mentor applications</p>
        </div>

        {isLoading ? (
          <div className="text-center text-gray-500 py-10">Loading pending mentors...</div>
        ) : mentorsList.length === 0 ? (
          <div className="text-center text-gray-500 py-10 bg-white rounded-xl border border-gray-200">
            No pending mentor applications.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {mentorsList.map((mentor: any) => (
              <div key={mentor.id} className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm flex flex-col justify-between">
                <div>
                  <h3 className="text-lg font-bold text-gray-900">{mentor.name || `${mentor.firstName} ${mentor.lastName}`}</h3>
                  <p className="text-sm text-gray-500 mb-4">{mentor.email}</p>
                  
                  <div className="space-y-3 mb-6">
                    <div>
                      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Experience</p>
                      <p className="text-sm text-gray-800 font-medium">{mentor.experienceYears || mentor.experience} years</p>
                    </div>
                    <div>
                      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">Skills</p>
                      <div className="flex flex-wrap gap-2">
                        {(mentor.skills || []).map((skill: string, index: number) => (
                          <span key={index} className="bg-gray-100 text-gray-700 text-xs px-2 py-1 rounded font-medium">
                            {skill}
                          </span>
                        ))}
                        {(!mentor.skills || mentor.skills.length === 0) && (
                          <span className="text-sm text-gray-400">None listed</span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
                
                <div className="flex gap-3 pt-4 border-t border-gray-100 mt-auto">
                  <button
                    onClick={() => approveMutation.mutate(mentor.id)}
                    className="flex-1 bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded-lg transition"
                  >
                    Approve
                  </button>
                  <button
                    onClick={() => rejectMutation.mutate(mentor.id)}
                    className="flex-1 bg-red-100 hover:bg-red-200 text-red-700 font-bold py-2 px-4 rounded-lg transition"
                  >
                    Reject
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default MentorApprovalsPage;

import { useQuery } from '@tanstack/react-query';
import api from '../../services/axios';
import PageLayout from '../../components/layout/PageLayout';

interface AdminStats {
  users?: number;
  mentors?: number;
  sessions?: number;
  pendingApprovals?: number;
  totalUsers?: number;
  totalMentors?: number;
  totalSessions?: number;
  totalRevenue?: number;
  pendingMentorApprovals?: number;
}

const AdminDashboardPage = () => {

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['admin', 'stats'],
    queryFn: async () => {
      try {
        const res = await api.get('/api/admin/stats');
        return res.data as AdminStats;
      } catch {
        return {
          totalUsers: 0,
          totalMentors: 0,
          totalSessions: 0,
          totalRevenue: 0,
          pendingApprovals: 0,
        } as AdminStats;
      }
    },
  });

  if (statsLoading) {
    return (
      <PageLayout>
        <div className="flex items-center justify-center h-screen">
          <div className="text-lg text-gray-500">Loading admin dashboard...</div>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="bg-gradient-to-r from-purple-600 to-indigo-600 rounded-lg p-8 text-white">
          <h1 className="text-3xl font-bold mb-2">Admin Dashboard</h1>
          <p className="text-purple-100">System overview and management tools</p>
        </div>

        {/* Stats Cards */}
        {stats && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-2">Total Users</div>
              <div className="text-3xl font-bold text-gray-900">{stats.users ?? stats.totalUsers ?? 0}</div>
            </div>
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-2">Mentors</div>
              <div className="text-3xl font-bold text-blue-600">{stats.mentors ?? stats.totalMentors ?? 0}</div>
            </div>
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-2">Total Sessions</div>
              <div className="text-3xl font-bold text-green-600">{stats.sessions ?? stats.totalSessions ?? 0}</div>
            </div>
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-2">Pending Approvals</div>
              <div className="text-3xl font-bold text-orange-600">{stats.pendingApprovals ?? stats.pendingMentorApprovals ?? 0}</div>
            </div>
          </div>
        )}

      </div>
    </PageLayout>
  );
};

export default AdminDashboardPage;

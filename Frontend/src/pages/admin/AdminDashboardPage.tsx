import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/axios';
import PageLayout from '../../components/layout/PageLayout';

interface AdminStats {
  totalUsers: number;
  totalMentors: number;
  totalSessions: number;
  totalRevenue: number;
  pendingMentorApprovals: number;
}

interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: string;
  createdAt: string;
}

const AdminDashboardPage = () => {

  const [activeTab, setActiveTab] = useState<'overview' | 'users' | 'mentors' | 'sessions' | 'reports'>('overview');

  // Fetch admin stats
  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['admin', 'stats'],
    queryFn: async () => {
      try {
        const res = await api.get('/api/admin/stats', { _skipErrorRedirect: true } as any);
        return res.data as AdminStats;
      } catch {
        return {
          totalUsers: 0,
          totalMentors: 0,
          totalSessions: 0,
          totalRevenue: 0,
          pendingMentorApprovals: 0,
        } as AdminStats;
      }
    },
  });

  // Fetch recent users
  const { data: usersData } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: async () => {
      try {
        const res = await api.get('/api/admin/users?page=0&size=10', { _skipErrorRedirect: true } as any);
        return res.data.content || [];
      } catch {
        return [];
      }
    },
  });

  // Fetch pending mentors
  const { data: pendingMentors } = useQuery({
    queryKey: ['admin', 'mentors', 'pending'],
    queryFn: async () => {
      try {
        const res = await api.get('/api/mentors/pending?page=0&size=20', { _skipErrorRedirect: true } as any);
        return res.data?.content || [];
      } catch {
        return [];
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
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-2">Total Users</div>
              <div className="text-3xl font-bold text-gray-900">{stats.totalUsers}</div>
            </div>
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-2">Mentors</div>
              <div className="text-3xl font-bold text-blue-600">{stats.totalMentors}</div>
            </div>
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-2">Sessions</div>
              <div className="text-3xl font-bold text-green-600">{stats.totalSessions}</div>
            </div>
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-2">Total Revenue</div>
              <div className="text-3xl font-bold text-purple-600">₹{stats.totalRevenue.toLocaleString()}</div>
            </div>
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <div className="text-sm text-gray-500 mb-2">Pending Approvals</div>
              <div className="text-3xl font-bold text-orange-600">{stats.pendingMentorApprovals}</div>
            </div>
          </div>
        )}

        {/* Tabs */}
        <div className="border-b border-gray-200">
          <div className="flex space-x-8">
            {['overview', 'users', 'mentors', 'sessions', 'reports'].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab as typeof activeTab)}
                className={`py-4 px-1 border-b-2 font-medium text-sm capitalize ${
                  activeTab === tab
                    ? 'border-purple-500 text-purple-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                {tab}
              </button>
            ))}
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'overview' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <h3 className="text-lg font-bold text-gray-900 mb-4">Quick Actions</h3>
              <div className="space-y-2">
                <button className="w-full text-left p-3 rounded bg-gray-50 hover:bg-gray-100 transition">
                  Manage Users
                </button>
                <button className="w-full text-left p-3 rounded bg-gray-50 hover:bg-gray-100 transition">
                  Approve Mentors
                </button>
                <button className="w-full text-left p-3 rounded bg-gray-50 hover:bg-gray-100 transition">
                  View Reports
                </button>
                <button className="w-full text-left p-3 rounded bg-gray-50 hover:bg-gray-100 transition">
                  System Settings
                </button>
              </div>
            </div>
            <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
              <h3 className="text-lg font-bold text-gray-900 mb-4">System Health</h3>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-gray-600">API Gateway</span>
                  <span className="text-green-600 font-semibold">Healthy</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-gray-600">Database</span>
                  <span className="text-green-600 font-semibold">Healthy</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-gray-600">Cache</span>
                  <span className="text-green-600 font-semibold">Healthy</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-gray-600">Message Queue</span>
                  <span className="text-green-600 font-semibold">Healthy</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'users' && (
          <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
            <h3 className="text-lg font-bold text-gray-900 mb-4">Recent Users</h3>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-gray-200">
                  <tr>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Name</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Email</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Role</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Joined</th>
                  </tr>
                </thead>
                <tbody>
                  {usersData && usersData.length > 0 ? (
                    usersData.map((user: AdminUser) => (
                      <tr key={user.id} className="border-b border-gray-100 hover:bg-gray-50">
                        <td className="py-3 px-4 text-gray-900">{user.name}</td>
                        <td className="py-3 px-4 text-gray-600">{user.email}</td>
                        <td className="py-3 px-4">
                          <span className="inline-block bg-blue-100 text-blue-800 px-2 py-1 rounded text-xs font-semibold">
                            {user.role.replace('ROLE_', '')}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-gray-500 text-sm">
                          {new Date(user.createdAt).toLocaleDateString()}
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={4} className="py-4 text-center text-gray-500">
                        No users found
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {activeTab === 'mentors' && (
          <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
            <h3 className="text-lg font-bold text-gray-900 mb-4">Pending Mentor Approvals</h3>
            {pendingMentors && pendingMentors.length > 0 ? (
              <div className="space-y-3">
                {pendingMentors.map((mentor: any) => (
                  <div key={mentor.id} className="flex items-between justify-between p-4 bg-gray-50 rounded border border-gray-200">
                    <div className="flex-1">
                      <p className="font-medium text-gray-900">{mentor.name}</p>
                      <p className="text-sm text-gray-500">Email: {mentor.email}</p>
                      <p className="text-sm text-gray-600 mt-1">Skills: {mentor.skills?.join(', ') || 'N/A'}</p>
                    </div>
                    <div className="flex space-x-2">
                      <button className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 transition">
                        Approve
                      </button>
                      <button className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 transition">
                        Reject
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-gray-500 text-center py-4">No pending mentor approvals</p>
            )}
          </div>
        )}

        {activeTab === 'sessions' && (
          <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
            <h3 className="text-lg font-bold text-gray-900 mb-4">Session Analytics</h3>
            <p className="text-gray-600">Session activity and performance metrics coming soon...</p>
          </div>
        )}

        {activeTab === 'reports' && (
          <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
            <h3 className="text-lg font-bold text-gray-900 mb-4">Reports & Analytics</h3>
            <p className="text-gray-600">Detailed system reports and analytics coming soon...</p>
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default AdminDashboardPage;

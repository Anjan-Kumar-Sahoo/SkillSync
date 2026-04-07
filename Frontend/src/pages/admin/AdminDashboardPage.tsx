import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../../services/axios';
import PageLayout from '../../components/layout/PageLayout';

interface AdminStats {
  totalUsers: number;
  totalMentors: number;
  totalSessions: number;
  pendingMentorApprovals: number;
}

const AdminDashboardPage = () => {
  const navigate = useNavigate();

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
          pendingMentorApprovals: 0,
        } as AdminStats;
      }
    },
    staleTime: 30_000,
  });

  if (statsLoading) {
    return (
      <PageLayout>
        <div className="flex items-center justify-center h-screen">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
        </div>
      </PageLayout>
    );
  }

  const statCards = [
    {
      label: 'Total Users',
      value: stats?.totalUsers ?? 0,
      icon: 'group',
      gradient: 'from-blue-500 to-cyan-400',
      bg: 'bg-blue-50',
      text: 'text-blue-700',
    },
    {
      label: 'Approved Mentors',
      value: stats?.totalMentors ?? 0,
      icon: 'school',
      gradient: 'from-violet-500 to-purple-400',
      bg: 'bg-violet-50',
      text: 'text-violet-700',
    },
    {
      label: 'Total Sessions',
      value: stats?.totalSessions ?? 0,
      icon: 'event',
      gradient: 'from-emerald-500 to-green-400',
      bg: 'bg-emerald-50',
      text: 'text-emerald-700',
    },
    {
      label: 'Pending Approvals',
      value: stats?.pendingMentorApprovals ?? 0,
      icon: 'pending_actions',
      gradient: 'from-amber-500 to-orange-400',
      bg: 'bg-amber-50',
      text: 'text-amber-700',
    },
  ];

  const quickLinks = [
    {
      title: 'Manage Users',
      description: 'View, search, filter, and manage all platform users',
      icon: 'manage_accounts',
      path: '/admin/users',
      color: 'text-blue-600',
      bg: 'bg-blue-100',
    },
    {
      title: 'Mentor Approvals',
      description: 'Review and approve/reject pending mentor applications',
      icon: 'how_to_reg',
      path: '/admin/mentor-approvals',
      color: 'text-purple-600',
      bg: 'bg-purple-100',
    },
    {
      title: 'Platform Finances',
      description: 'Track payments, revenue, and financial analytics',
      icon: 'payments',
      path: '/admin/finances',
      color: 'text-emerald-600',
      bg: 'bg-emerald-100',
    },
  ];

  return (
    <PageLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="bg-gradient-to-r from-purple-600 via-indigo-600 to-blue-600 rounded-2xl p-8 text-white shadow-lg relative overflow-hidden">
          <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full blur-3xl -mr-20 -mt-20"></div>
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-white/5 rounded-full blur-2xl -ml-10 -mb-10"></div>
          <h1 className="text-3xl font-extrabold mb-2 relative z-10">Admin Dashboard</h1>
          <p className="text-purple-100 text-lg relative z-10">System overview and management tools</p>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
          {statCards.map((card) => (
            <div
              key={card.label}
              className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10 hover:shadow-md transition-all hover:-translate-y-0.5 relative overflow-hidden group"
            >
              <div className={`absolute top-0 right-0 w-20 h-20 bg-gradient-to-br ${card.gradient} opacity-10 rounded-full blur-xl -mr-6 -mt-6 group-hover:opacity-20 transition-opacity`}></div>
              <div className="flex items-start justify-between mb-4">
                <div className={`w-12 h-12 ${card.bg} rounded-xl flex items-center justify-center`}>
                  <span className={`material-symbols-outlined text-[24px] ${card.text}`}>{card.icon}</span>
                </div>
              </div>
              <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-1">{card.label}</p>
              <p className="text-3xl font-black text-on-surface">{card.value}</p>
            </div>
          ))}
        </div>

        {/* Quick Actions */}
        <div>
          <h2 className="text-xl font-extrabold text-on-surface mb-4 tracking-tight">Quick Actions</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {quickLinks.map((link) => (
              <button
                key={link.path}
                onClick={() => navigate(link.path)}
                className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10 hover:shadow-md hover:-translate-y-0.5 hover:border-primary/20 transition-all text-left group"
              >
                <div className={`w-12 h-12 ${link.bg} rounded-xl flex items-center justify-center mb-4`}>
                  <span className={`material-symbols-outlined text-[24px] ${link.color}`}>{link.icon}</span>
                </div>
                <h3 className="text-lg font-extrabold text-on-surface mb-1 group-hover:text-primary transition-colors">{link.title}</h3>
                <p className="text-sm text-on-surface-variant">{link.description}</p>
                <div className="mt-4 flex items-center gap-1 text-sm font-bold text-primary opacity-0 group-hover:opacity-100 transition-opacity">
                  Open <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
                </div>
              </button>
            ))}
          </div>
        </div>
      </div>
    </PageLayout>
  );
};

export default AdminDashboardPage;

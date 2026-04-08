import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import PageLayout from '../../components/layout/PageLayout';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';
import { useActionConfirm } from '../../components/ui/ActionConfirm';

const PAGE_SIZE = 20;

const UsersCenterPage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const { requestConfirmation } = useActionConfirm();

  const [page, setPage] = useState(0);
  const [roleFilter, setRoleFilter] = useState('');
  const [searchText, setSearchText] = useState('');
  const [searchInput, setSearchInput] = useState('');

  const { data: usersData, isLoading } = useQuery({
    queryKey: ['admin', 'users', page, roleFilter, searchText],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.append('page', String(page));
      params.append('size', String(PAGE_SIZE));
      if (roleFilter) params.append('role', roleFilter);
      if (searchText) params.append('search', searchText);
      const { data } = await api.get(`/api/admin/users?${params.toString()}`);
      return data;
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/api/admin/users/${id}`);
    },
    onSuccess: () => {
      showToast({ message: 'User deleted successfully', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
    },
    onError: () => showToast({ message: 'Failed to delete user', type: 'error' }),
  });

  const roleMutation = useMutation({
    mutationFn: async ({ id, role }: { id: number; role: string }) => {
      await api.put(`/api/admin/users/${id}/role`, { role });
    },
    onSuccess: () => {
      showToast({ message: 'User role updated', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
    },
    onError: () => showToast({ message: 'Failed to update role', type: 'error' }),
  });

  const users = [...(usersData?.content || [])].sort((a: any, b: any) => {
    const aId = Number(a?.id || 0);
    const bId = Number(b?.id || 0);
    return aId - bId;
  });
  const totalElements = Number(usersData?.totalElements || users.length || 0);
  const totalPages = Math.max(1, Number(usersData?.totalPages || 1));
  const currentPage = Number(usersData?.number ?? page);

  const handleSearch = () => {
    setSearchText(searchInput.trim());
    setPage(0);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch();
  };

  const handleDeleteUser = async (id: number, email: string) => {
    const confirmed = await requestConfirmation({
      title: 'Delete User?',
      message: `Are you sure you want to delete user ${email}? This action cannot be undone.`,
      confirmLabel: 'Yes, delete user',
      requiredText: 'YES',
    });

    if (!confirmed) {
      return;
    }

    deleteMutation.mutate(id);
  };

  const getRoleBadgeStyle = (role: string) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return 'bg-red-100 text-red-700 border-red-200';
      case 'ROLE_MENTOR':
        return 'bg-purple-100 text-purple-700 border-purple-200';
      default:
        return 'bg-blue-100 text-blue-700 border-blue-200';
    }
  };

  const getRoleLabel = (role: string) => {
    return role?.replace('ROLE_', '') || 'LEARNER';
  };

  return (
    <PageLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-2xl p-6 shadow-sm">
          <h1 className="text-3xl font-extrabold text-on-surface tracking-tight">Manage Users</h1>
          <p className="text-on-surface-variant mt-2">View, filter, search, and manage all platform users</p>
        </div>

        {/* Controls */}
        <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-2xl p-4 shadow-sm flex flex-col md:flex-row items-end gap-4">
          {/* Left: Role filter */}
          <div className="w-full md:w-48">
            <label className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">Role Filter</label>
            <select
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value);
                setPage(0);
              }}
              className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
            >
              <option value="">All Roles</option>
              <option value="ROLE_LEARNER">Learner</option>
              <option value="ROLE_MENTOR">Mentor</option>
              <option value="ROLE_ADMIN">Admin</option>
            </select>
          </div>

          {/* Right: Search */}
          <div className="flex-1 w-full flex gap-2">
            <div className="flex-1">
              <label className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">Search by Email</label>
              <input
                type="text"
                placeholder="Type email to search..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onKeyDown={handleKeyDown}
                className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent placeholder:text-on-surface-variant/50"
              />
            </div>
            <button
              onClick={handleSearch}
              className="h-10 px-5 gradient-btn text-white font-bold rounded-lg shadow-sm hover:shadow-md transition-all active:scale-95 self-end"
            >
              Search
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-2xl shadow-sm overflow-hidden">
          {isLoading ? (
            <div className="p-8 text-center text-on-surface-variant">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-3"></div>
              Loading users...
            </div>
          ) : users.length === 0 ? (
            <div className="p-8 text-center text-on-surface-variant">
              <span className="material-symbols-outlined text-4xl text-outline-variant mb-2 block">search_off</span>
              No users found matching your criteria.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-surface-container border-b border-outline-variant/10">
                  <tr>
                    <th className="text-left py-3 px-5 text-[10px] font-black text-on-surface-variant uppercase tracking-widest">ID</th>
                    <th className="text-left py-3 px-5 text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Email</th>
                    <th className="text-left py-3 px-5 text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Name</th>
                    <th className="text-left py-3 px-5 text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Role</th>
                    <th className="text-right py-3 px-5 text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user: any) => (
                    <tr key={user.id} className="border-b border-outline-variant/5 hover:bg-surface-container-low/50 transition-colors">
                      <td className="py-3 px-5 text-sm font-bold text-on-surface">#{user.id}</td>
                      <td className="py-3 px-5 text-sm font-semibold text-on-surface">{user.email}</td>
                      <td className="py-3 px-5 text-sm text-on-surface-variant">{user.firstName} {user.lastName}</td>
                      <td className="py-3 px-5">
                        <span className={`inline-block px-2.5 py-1 rounded-md text-[10px] font-black uppercase tracking-widest border ${getRoleBadgeStyle(user.role)}`}>
                          {getRoleLabel(user.role)}
                        </span>
                      </td>
                      <td className="py-3 px-5 text-right">
                        <div className="flex gap-2 justify-end">
                          {user.role === 'ROLE_LEARNER' && (
                            <button
                              onClick={() => roleMutation.mutate({ id: user.id, role: 'ROLE_MENTOR' })}
                              disabled={roleMutation.isPending}
                              className="text-[10px] font-bold bg-purple-100 text-purple-700 hover:bg-purple-200 px-3 py-1.5 rounded-lg transition disabled:opacity-50"
                            >
                              Promote
                            </button>
                          )}
                          {user.role === 'ROLE_MENTOR' && (
                            <button
                              onClick={() => roleMutation.mutate({ id: user.id, role: 'ROLE_LEARNER' })}
                              disabled={roleMutation.isPending}
                              className="text-[10px] font-bold bg-orange-100 text-orange-700 hover:bg-orange-200 px-3 py-1.5 rounded-lg transition disabled:opacity-50"
                            >
                              Demote
                            </button>
                          )}
                          {user.role !== 'ROLE_ADMIN' && (
                            <button
                              onClick={() => void handleDeleteUser(user.id, user.email)}
                              disabled={deleteMutation.isPending}
                              className="text-[10px] font-bold bg-red-100 text-red-700 hover:bg-red-200 px-3 py-1.5 rounded-lg transition disabled:opacity-50"
                            >
                              Delete
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {!isLoading && users.length > 0 && (
            <>
              <div className="px-5 py-3 border-t border-outline-variant/10 bg-surface-container-low/30 text-xs font-semibold text-on-surface-variant">
                Showing {users.length} of {totalElements} user{totalElements !== 1 ? 's' : ''}
                {roleFilter && ` • Role: ${getRoleLabel(roleFilter)}`}
                {searchText && ` • Search: "${searchText}"`}
                {' • Sorted by ID'}
              </div>

              <div className="px-5 py-3 border-t border-outline-variant/10 bg-surface-container-lowest flex items-center justify-between gap-3">
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
            </>
          )}
        </div>
      </div>
    </PageLayout>
  );
};

export default UsersCenterPage;

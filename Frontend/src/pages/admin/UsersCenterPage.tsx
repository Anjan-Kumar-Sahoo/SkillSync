import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import PageLayout from '../../components/layout/PageLayout';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';

const UsersCenterPage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  const { data: usersData, isLoading } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: async () => {
      const { data } = await api.get('/api/admin/users');
      return data;
    },
  });

  const roleMutation = useMutation({
    mutationFn: async ({ id, role }: { id: number; role: string }) => {
      await api.put(`/api/admin/users/${id}/role`, { role });
    },
    onSuccess: () => {
      showToast({ message: 'User role updated', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: () => showToast({ message: 'Failed to update role', type: 'error' }),
  });

  const statusMutation = useMutation({
    mutationFn: async ({ id, status }: { id: number; status: string }) => {
      await api.put(`/api/admin/users/${id}/status`, { status });
    },
    onSuccess: () => {
      showToast({ message: 'User status updated', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: () => showToast({ message: 'Failed to update status', type: 'error' }),
  });

  const users = usersData?.content || [];

  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
          <h1 className="text-2xl font-bold text-gray-900">Manage Users</h1>
          <p className="text-gray-600 mt-2">View and manage system users</p>
        </div>

        <div className="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden">
          {isLoading ? (
            <div className="p-8 text-center text-gray-500">Loading users...</div>
          ) : users.length === 0 ? (
            <div className="p-8 text-center text-gray-500">No users found.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="text-left py-4 px-6 font-semibold text-gray-700">Name</th>
                    <th className="text-left py-4 px-6 font-semibold text-gray-700">Email</th>
                    <th className="text-left py-4 px-6 font-semibold text-gray-700">Role</th>
                    <th className="text-left py-4 px-6 font-semibold text-gray-700">Status</th>
                    <th className="text-left py-4 px-6 font-semibold text-gray-700">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user: any) => (
                    <tr key={user.id} className="border-b border-gray-100 hover:bg-gray-50">
                      <td className="py-4 px-6 text-gray-900">{user.firstName} {user.lastName}</td>
                      <td className="py-4 px-6 text-gray-600">{user.email}</td>
                      <td className="py-4 px-6">
                        <span className="inline-block bg-blue-100 text-blue-800 px-2 py-1 rounded text-xs font-bold">
                          {user.role}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        <span className={`inline-block px-2 py-1 rounded text-xs font-bold ${user.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                          {user.status || 'ACTIVE'}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        <div className="flex gap-2">
                          {user.role === 'ROLE_LEARNER' && (
                            <button
                              onClick={() => roleMutation.mutate({ id: user.id, role: 'ROLE_MENTOR' })}
                              className="text-xs bg-purple-100 text-purple-700 hover:bg-purple-200 px-3 py-1.5 rounded font-bold transition"
                            >
                              Promote to Mentor
                            </button>
                          )}
                          {user.status !== 'BANNED' && user.status !== 'INACTIVE' ? (
                            <button
                              onClick={() => statusMutation.mutate({ id: user.id, status: 'BANNED' })}
                              className="text-xs bg-red-100 text-red-700 hover:bg-red-200 px-3 py-1.5 rounded font-bold transition"
                            >
                              Ban User
                            </button>
                          ) : (
                            <button
                              onClick={() => statusMutation.mutate({ id: user.id, status: 'ACTIVE' })}
                              className="text-xs bg-green-100 text-green-700 hover:bg-green-200 px-3 py-1.5 rounded font-bold transition"
                            >
                              Activate User
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
        </div>
      </div>
    </PageLayout>
  );
};

export default UsersCenterPage;

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import groupService from '../../services/groupService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import { useActionConfirm } from '../../components/ui/ActionConfirm';
import type { RootState } from '../../store';

const GroupsPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const { requestConfirmation } = useActionConfirm();
  const role = useSelector((state: RootState) => state.auth.role);

  const [activeTab, setActiveTab] = useState<'explore' | 'mygroups'>('explore');
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const { data: exploreData, isLoading: exploreLoading } = useQuery({
    queryKey: ['groups', 'explore', page, search],
    queryFn: () => groupService.getGroups(search, undefined, page, 10),
  });

  const { data: myGroupsData, isLoading: myGroupsLoading } = useQuery({
    queryKey: ['groups', 'my', page],
    queryFn: () => groupService.getMyGroups(page, 10),
  });

  const joinGroupMutation = useMutation({
    mutationFn: (groupId: number) => groupService.joinGroup(groupId),
    onSuccess: () => {
      showToast({ message: 'Joined group successfully', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: () => {
      showToast({ message: 'Failed to join group', type: 'error' });
    },
  });

  const leaveGroupMutation = useMutation({
    mutationFn: (groupId: number) => groupService.leaveGroup(groupId),
    onSuccess: () => {
      showToast({ message: 'Left group successfully', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: () => {
      showToast({ message: 'Failed to leave group', type: 'error' });
    },
  });

  const handleLeaveGroup = async (groupId: number, groupName: string) => {
    const confirmed = await requestConfirmation({
      title: 'Leave Group?',
      message: `Are you sure you want to leave "${groupName}"?`,
      confirmLabel: 'Yes, leave group',
    });

    if (!confirmed) {
      return;
    }

    leaveGroupMutation.mutate(groupId);
  };

  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-gradient-to-r from-green-600 to-teal-600 rounded-lg p-8 text-white">
          <h1 className="text-3xl font-bold mb-2">Learning Groups</h1>
          <p className="text-green-100">Join communities and communicate with members in group messages.</p>
        </div>

        <div className="border-b border-gray-200">
          <div className="flex space-x-8">
            {['explore', 'mygroups'].map((tab) => (
              <button
                key={tab}
                onClick={() => {
                  setActiveTab(tab as typeof activeTab);
                  setPage(0);
                }}
                className={`py-4 px-1 border-b-2 font-medium text-sm capitalize ${
                  activeTab === tab
                    ? 'border-green-500 text-green-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                {tab === 'explore' ? 'Explore Groups' : 'Joined Groups'}
              </button>
            ))}
          </div>
        </div>

        {activeTab === 'explore' && (
          <div className="space-y-4">
            <div className="flex gap-4">
              <input
                type="text"
                placeholder="Search groups..."
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
              />
              {role === 'ROLE_ADMIN' && (
                <button
                  onClick={() => navigate('/admin/groups')}
                  className="bg-green-600 text-white px-6 py-2 rounded-lg hover:bg-green-700 transition"
                >
                  Manage Groups
                </button>
              )}
            </div>

            {exploreLoading ? (
              <p className="text-center text-gray-500 py-8">Loading groups...</p>
            ) : exploreData?.content && exploreData.content.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {exploreData.content.map((group: any) => {
                  const isJoining = joinGroupMutation.isPending && joinGroupMutation.variables === group.id;

                  return (
                    <div
                      key={group.id}
                      className="bg-white rounded-lg p-6 shadow-sm border border-gray-200 hover:shadow-md transition"
                    >
                      <h3 className="font-bold text-gray-900 mb-2">{group.name}</h3>
                      <p className="text-sm text-gray-600 mb-4 line-clamp-2">{group.description}</p>
                      <div className="flex items-center justify-between mb-4">
                        <span className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">
                          {group.category}
                        </span>
                        <span className="text-xs text-gray-500">
                          {group.memberCount}/{group.maxMembers || '?'} members
                        </span>
                      </div>

                      {group.isJoined ? (
                        <button
                          onClick={() => navigate(`/groups/${group.id}`)}
                          className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition"
                        >
                          Open Group
                        </button>
                      ) : (
                        <button
                          onClick={() => joinGroupMutation.mutate(group.id)}
                          disabled={isJoining}
                          className="w-full bg-green-600 text-white py-2 rounded hover:bg-green-700 transition disabled:opacity-50"
                        >
                          {isJoining ? 'Joining...' : 'Join Group'}
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="text-center text-gray-500 py-8">No groups found</p>
            )}

            {exploreData && exploreData.totalElements > 10 && (
              <div className="flex justify-center gap-2 pt-4">
                <button
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                  className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50"
                >
                  Previous
                </button>
                <span className="px-4 py-2">
                  Page {page + 1} of {Math.ceil(exploreData.totalElements / 10)}
                </span>
                <button
                  onClick={() => setPage(page + 1)}
                  disabled={page >= Math.ceil(exploreData.totalElements / 10) - 1}
                  className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            )}
          </div>
        )}

        {activeTab === 'mygroups' && (
          <div className="space-y-4">
            {myGroupsLoading ? (
              <p className="text-center text-gray-500 py-8">Loading your groups...</p>
            ) : myGroupsData?.content && myGroupsData.content.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {myGroupsData.content.map((group: any) => (
                  <div
                    key={group.id}
                    className="bg-white rounded-lg p-6 shadow-sm border border-gray-200 hover:shadow-md transition"
                  >
                    <h3 className="font-bold text-gray-900 mb-2">{group.name}</h3>
                    <p className="text-sm text-gray-600 mb-4 line-clamp-2">{group.description}</p>
                    <div className="flex items-center justify-between mb-4">
                      <span className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">
                        {group.category}
                      </span>
                      <span className="text-xs text-gray-500">{group.memberCount} members</span>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => navigate(`/groups/${group.id}`)}
                        className="flex-1 bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition text-sm"
                      >
                        View
                      </button>
                      <button
                        onClick={() => void handleLeaveGroup(group.id, group.name)}
                        disabled={leaveGroupMutation.isPending}
                        className="flex-1 bg-red-600 text-white py-2 rounded hover:bg-red-700 transition text-sm disabled:opacity-50"
                      >
                        Leave
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-center text-gray-500 py-8">You haven't joined any groups yet</p>
            )}
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default GroupsPage;

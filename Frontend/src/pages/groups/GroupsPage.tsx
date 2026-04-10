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
        <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-2xl p-6 shadow-sm">
          <h1 className="text-3xl font-extrabold text-on-surface tracking-tight">Learning Groups</h1>
          <p className="text-on-surface-variant mt-2">
            Explore communities, join discussions, and collaborate with peers in real-time.
          </p>
        </div>

        <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-xl p-1.5 inline-flex gap-1 shadow-sm overflow-x-auto max-w-full scrollbar-hide">
          <div className="flex gap-1">
            {['explore', 'mygroups'].map((tab) => (
              <button
                key={tab}
                onClick={() => {
                  setActiveTab(tab as typeof activeTab);
                  setPage(0);
                }}
                className={`whitespace-nowrap px-5 py-2.5 rounded-lg text-sm font-bold transition-all duration-300 capitalize ${
                  activeTab === tab
                    ? 'bg-primary text-white shadow-md scale-[1.02]'
                    : 'text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
                }`}
              >
                {tab === 'explore' ? 'Explore Groups' : 'Joined Groups'}
              </button>
            ))}
          </div>
        </div>

        {activeTab === 'explore' && (
          <div className="space-y-4">
            <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-2xl p-4 shadow-sm flex flex-col lg:flex-row items-stretch gap-3">
              <input
                type="text"
                placeholder="Search groups..."
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                className="flex-1 h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
              />
              {role === 'ROLE_ADMIN' && (
                <button
                  onClick={() => navigate('/admin/groups')}
                  className="h-10 px-5 bg-primary text-on-primary rounded-lg font-bold hover:bg-primary-dark transition-colors"
                >
                  Manage Groups
                </button>
              )}
            </div>

            {exploreLoading ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {Array.from({ length: 3 }).map((_, index) => (
                  <div key={index} className="h-44 bg-surface-container-lowest rounded-2xl border border-outline-variant/10 animate-pulse" />
                ))}
              </div>
            ) : exploreData?.content && exploreData.content.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {exploreData.content.map((group: any) => {
                  const isJoining = joinGroupMutation.isPending && joinGroupMutation.variables === group.id;

                  return (
                    <div
                      key={group.id}
                      className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10 hover:shadow-md hover:border-outline-variant/25 transition"
                    >
                      <h3 className="font-bold text-on-surface mb-2 text-lg">{group.name}</h3>
                      <p className="text-sm text-on-surface-variant mb-4 line-clamp-2">{group.description}</p>
                      <div className="flex items-center justify-between mb-4">
                        <span className="text-xs bg-surface-container text-on-surface-variant px-2 py-1 rounded font-semibold">
                          {group.category}
                        </span>
                        <span className="text-xs text-on-surface-variant font-semibold">
                          {group.memberCount}/{group.maxMembers || '?'} members
                        </span>
                      </div>

                      {group.isJoined ? (
                        <button
                          onClick={() => navigate(`/groups/${group.id}`)}
                          className="w-full h-10 bg-primary text-on-primary rounded-lg font-bold hover:bg-primary-dark transition-colors"
                        >
                          Open Group
                        </button>
                      ) : (
                        <button
                          onClick={() => joinGroupMutation.mutate(group.id)}
                          disabled={isJoining}
                          className="w-full h-10 bg-surface-container-high text-on-surface rounded-lg font-bold hover:bg-surface-container-highest transition-colors disabled:opacity-50"
                        >
                          {isJoining ? 'Joining...' : 'Join Group'}
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="bg-surface-container-lowest rounded-2xl p-10 text-center border border-outline-variant/10 shadow-sm">
                <p className="text-on-surface-variant font-semibold">No groups found</p>
              </div>
            )}

            {exploreData && exploreData.totalElements > 10 && (
              <div className="flex justify-center gap-2 pt-4">
                <button
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                  className="px-4 py-2 rounded-lg text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <span className="px-4 py-2 text-sm font-semibold text-on-surface-variant">
                  Page {page + 1} of {Math.ceil(exploreData.totalElements / 10)}
                </span>
                <button
                  onClick={() => setPage(page + 1)}
                  disabled={page >= Math.ceil(exploreData.totalElements / 10) - 1}
                  className="px-4 py-2 rounded-lg text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
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
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {Array.from({ length: 3 }).map((_, index) => (
                  <div key={index} className="h-44 bg-surface-container-lowest rounded-2xl border border-outline-variant/10 animate-pulse" />
                ))}
              </div>
            ) : myGroupsData?.content && myGroupsData.content.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {myGroupsData.content.map((group: any) => (
                  <div
                    key={group.id}
                    className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10 hover:shadow-md hover:border-outline-variant/25 transition"
                  >
                    <h3 className="font-bold text-on-surface mb-2 text-lg">{group.name}</h3>
                    <p className="text-sm text-on-surface-variant mb-4 line-clamp-2">{group.description}</p>
                    <div className="flex items-center justify-between mb-4">
                      <span className="text-xs bg-surface-container text-on-surface-variant px-2 py-1 rounded font-semibold">
                        {group.category}
                      </span>
                      <span className="text-xs text-on-surface-variant font-semibold">{group.memberCount} members</span>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => navigate(`/groups/${group.id}`)}
                        className="flex-1 h-10 bg-primary text-on-primary rounded-lg font-bold hover:bg-primary-dark transition-colors text-sm"
                      >
                        View
                      </button>
                      <button
                        onClick={() => void handleLeaveGroup(group.id, group.name)}
                        disabled={leaveGroupMutation.isPending}
                        className="flex-1 h-10 bg-error text-white rounded-lg font-bold hover:opacity-90 transition text-sm disabled:opacity-50"
                      >
                        Leave
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="bg-surface-container-lowest rounded-2xl p-10 text-center border border-outline-variant/10 shadow-sm">
                <p className="text-on-surface-variant font-semibold">You have not joined any groups yet</p>
              </div>
            )}
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default GroupsPage;

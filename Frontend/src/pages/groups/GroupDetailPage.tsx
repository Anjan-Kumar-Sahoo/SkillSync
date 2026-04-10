import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { useSelector } from 'react-redux';
import groupService from '../../services/groupService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import { useActionConfirm } from '../../components/ui/ActionConfirm';
import type { RootState } from '../../store';
import type { DiscussionPayload, GroupMemberPayload } from '../../services/groupService';

const GroupDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const { requestConfirmation } = useActionConfirm();

  const currentUserId = useSelector((state: RootState) => state.auth.user?.id);
  const currentRole = useSelector((state: RootState) => state.auth.role);
  const groupId = Number(id);

  const [activeTab, setActiveTab] = useState<'discussion' | 'members'>('discussion');
  const [newDiscussionTitle, setNewDiscussionTitle] = useState('');
  const [newDiscussionContent, setNewDiscussionContent] = useState('');
  const [showDiscussionForm, setShowDiscussionForm] = useState(false);

  const { data: group, isLoading: groupLoading } = useQuery({
    queryKey: ['group', id],
    queryFn: () => groupService.getGroupById(groupId),
    enabled: Number.isFinite(groupId),
  });

  const isJoined = Boolean(group?.isJoined);
  const canLeaveGroup = isJoined && currentRole !== 'ROLE_ADMIN';
  const canViewMessages = currentRole === 'ROLE_ADMIN' || isJoined;

  const { data: discussions, isLoading: discussionsLoading } = useQuery({
    queryKey: ['group', id, 'discussions'],
    queryFn: () => groupService.getGroupDiscussions(groupId),
    enabled: Number.isFinite(groupId) && canViewMessages,
    refetchInterval: canViewMessages ? 4000 : false,
  });

  const { data: members, isLoading: membersLoading } = useQuery({
    queryKey: ['group', id, 'members'],
    queryFn: () => groupService.getGroupMembers(groupId),
    enabled: Number.isFinite(groupId),
  });

  const joinGroupMutation = useMutation({
    mutationFn: () => groupService.joinGroup(groupId),
    onSuccess: () => {
      showToast({ message: 'Joined group successfully', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['group', id] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: () => {
      showToast({ message: 'Failed to join group', type: 'error' });
    },
  });

  const leaveGroupMutation = useMutation({
    mutationFn: () => groupService.leaveGroup(groupId),
    onSuccess: () => {
      showToast({ message: 'Left group successfully', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
      navigate('/groups');
    },
    onError: () => {
      showToast({ message: 'Failed to leave group', type: 'error' });
    },
  });

  const postDiscussionMutation = useMutation({
    mutationFn: () => groupService.postDiscussion(groupId, newDiscussionTitle, newDiscussionContent),
    onSuccess: () => {
      showToast({ message: 'Message posted successfully', type: 'success' });
      setNewDiscussionTitle('');
      setNewDiscussionContent('');
      setShowDiscussionForm(false);
      queryClient.invalidateQueries({ queryKey: ['group', id, 'discussions'] });
    },
    onError: () => {
      showToast({ message: 'Failed to post message', type: 'error' });
    },
  });

  const deleteDiscussionMutation = useMutation({
    mutationFn: (discussionId: number) => groupService.deleteDiscussion(groupId, discussionId),
    onSuccess: () => {
      showToast({ message: 'Message deleted', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['group', id, 'discussions'] });
    },
    onError: () => {
      showToast({ message: 'Failed to delete message', type: 'error' });
    },
  });

  const removeMemberMutation = useMutation({
    mutationFn: (memberUserId: number) => groupService.removeGroupMember(groupId, memberUserId),
    onSuccess: () => {
      showToast({ message: 'Member removed successfully', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['group', id] });
      queryClient.invalidateQueries({ queryKey: ['group', id, 'members'] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: () => {
      showToast({ message: 'Failed to remove member', type: 'error' });
    },
  });

  const canDeleteDiscussion = (discussion: DiscussionPayload) => {
    if (currentRole === 'ROLE_ADMIN') return true;
    if (!currentUserId) return false;
    if (discussion.authorId === currentUserId) return true;
    return currentRole === 'ROLE_MENTOR' && discussion.authorRole === 'ROLE_LEARNER';
  };

  const handlePostDiscussion = (event: React.FormEvent) => {
    event.preventDefault();
    if (!newDiscussionTitle.trim() || !newDiscussionContent.trim()) {
      showToast({ message: 'Please provide both title and message', type: 'error' });
      return;
    }
    postDiscussionMutation.mutate();
  };

  const handleLeaveGroup = async () => {
    if (!canLeaveGroup) return;

    const groupName = group?.name || 'this group';
    const confirmed = await requestConfirmation({
      title: 'Leave Group?',
      message: `Are you sure you want to leave "${groupName}"?`,
      confirmLabel: 'Yes, leave group',
    });

    if (!confirmed) return;
    leaveGroupMutation.mutate();
  };

  const handleDeleteDiscussion = async (discussion: DiscussionPayload) => {
    const confirmed = await requestConfirmation({
      title: 'Delete Message?',
      message: 'This message will be permanently removed from the group conversation.',
      confirmLabel: 'Yes, delete message',
    });

    if (!confirmed) return;
    deleteDiscussionMutation.mutate(discussion.id);
  };

  const handleRemoveMember = async (member: GroupMemberPayload) => {
    const confirmed = await requestConfirmation({
      title: 'Remove Member?',
      message: `Remove ${member.name} from this group?`,
      confirmLabel: 'Yes, remove member',
    });

    if (!confirmed) return;

    removeMemberMutation.mutate(member.userId);

    if (member.userId === currentUserId) {
      navigate('/groups');
    }
  };

  if (groupLoading) {
    return (
      <PageLayout>
        <div className="bg-surface-container-lowest rounded-2xl p-12 border border-outline-variant/10 shadow-sm text-center">
          <div className="text-lg font-semibold text-on-surface-variant">Loading group...</div>
        </div>
      </PageLayout>
    );
  }

  if (!group) {
    return (
      <PageLayout>
        <div className="bg-surface-container-lowest rounded-2xl p-12 border border-outline-variant/10 shadow-sm text-center">
          <p className="text-lg text-on-surface-variant mb-4 font-semibold">Group not found</p>
          <button
            onClick={() => navigate('/groups')}
            className="h-10 px-6 bg-primary text-on-primary rounded-lg font-bold hover:bg-primary-dark transition-colors"
          >
            Back to Groups
          </button>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <div className="space-y-6">
        <button
          onClick={() => navigate('/groups')}
          className="inline-flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-bold text-on-surface-variant hover:bg-surface-container hover:text-on-surface transition-colors"
        >
          ← Back to Groups
        </button>

        <div className="bg-surface-container-lowest rounded-2xl p-6 border border-outline-variant/10 shadow-sm">
          <div className="flex justify-between items-start gap-4">
            <div>
              <h1 className="text-3xl font-extrabold text-on-surface tracking-tight">{group.name}</h1>
              <p className="text-on-surface-variant mt-2">{group.description}</p>
              <div className="flex flex-wrap gap-3 mt-4 text-sm">
                <span className="px-2.5 py-1 rounded-md bg-surface-container text-on-surface-variant font-semibold">
                  {group.memberCount}/{group.maxMembers || '?'} members
                </span>
                <span className="px-2.5 py-1 rounded-md bg-surface-container text-on-surface-variant font-semibold">
                  {group.category}
                </span>
              </div>
            </div>

            {!isJoined ? (
              <button
                onClick={() => joinGroupMutation.mutate()}
                disabled={joinGroupMutation.isPending}
                className="h-10 px-5 bg-primary text-on-primary rounded-lg font-bold hover:bg-primary-dark transition-colors disabled:opacity-50"
              >
                {joinGroupMutation.isPending ? 'Joining...' : 'Join Group'}
              </button>
            ) : canLeaveGroup ? (
              <button
                onClick={() => void handleLeaveGroup()}
                disabled={leaveGroupMutation.isPending}
                className="h-10 px-5 bg-error text-white rounded-lg font-bold hover:opacity-90 transition disabled:opacity-50"
              >
                Leave Group
              </button>
            ) : (
              <div className="h-10 px-4 inline-flex items-center rounded-lg bg-surface-container text-on-surface font-semibold">
                Admin Member
              </div>
            )}
          </div>
        </div>

        <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-xl p-1.5 inline-flex gap-1 shadow-sm overflow-x-auto max-w-full scrollbar-hide">
          <div className="flex gap-1">
            {['discussion', 'members'].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab as typeof activeTab)}
                className={`whitespace-nowrap px-5 py-2.5 rounded-lg text-sm font-bold transition-all duration-300 capitalize ${
                  activeTab === tab
                    ? 'bg-primary text-white shadow-md scale-[1.02]'
                    : 'text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
                }`}
              >
                {tab}
              </button>
            ))}
          </div>
        </div>

        {activeTab === 'discussion' && (
          <div className="space-y-6">
            {!canViewMessages ? (
              <div className="text-center py-10 bg-surface-container-lowest rounded-2xl border border-outline-variant/10 shadow-sm">
                <p className="text-on-surface-variant mb-3 font-semibold">Join this group to access member-only messages.</p>
                <button
                  onClick={() => joinGroupMutation.mutate()}
                  disabled={joinGroupMutation.isPending}
                  className="h-10 px-5 bg-primary text-on-primary rounded-lg font-bold hover:bg-primary-dark transition-colors disabled:opacity-50"
                >
                  {joinGroupMutation.isPending ? 'Joining...' : 'Join to Start Messaging'}
                </button>
              </div>
            ) : (
              <>
                <button
                  onClick={() => setShowDiscussionForm((value) => !value)}
                  className="h-10 px-6 bg-primary text-on-primary rounded-lg font-bold hover:bg-primary-dark transition-colors"
                >
                  {showDiscussionForm ? 'Close Composer' : '+ New Message'}
                </button>

                {showDiscussionForm && (
                  <div className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10">
                    <form onSubmit={handlePostDiscussion} className="space-y-4">
                      <div>
                        <label className="block text-sm font-bold text-on-surface-variant mb-1">Title</label>
                        <input
                          type="text"
                          value={newDiscussionTitle}
                          onChange={(event) => setNewDiscussionTitle(event.target.value)}
                          className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
                          placeholder="Message title"
                          required
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-bold text-on-surface-variant mb-1">Message</label>
                        <textarea
                          value={newDiscussionContent}
                          onChange={(event) => setNewDiscussionContent(event.target.value)}
                          rows={4}
                          className="w-full bg-surface-container px-3 py-2 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
                          placeholder="Write your message"
                          required
                        />
                      </div>

                      <div className="flex gap-2">
                        <button
                          type="submit"
                          disabled={postDiscussionMutation.isPending}
                          className="h-10 px-6 bg-primary text-on-primary rounded-lg font-bold hover:bg-primary-dark transition-colors disabled:opacity-50"
                        >
                          {postDiscussionMutation.isPending ? 'Posting...' : 'Post Message'}
                        </button>
                        <button
                          type="button"
                          onClick={() => setShowDiscussionForm(false)}
                          className="h-10 px-6 rounded-lg text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface"
                        >
                          Cancel
                        </button>
                      </div>
                    </form>
                  </div>
                )}

                {discussionsLoading ? (
                  <div className="space-y-3">
                    {Array.from({ length: 3 }).map((_, index) => (
                      <div key={index} className="h-24 bg-surface-container-lowest rounded-2xl border border-outline-variant/10 animate-pulse" />
                    ))}
                  </div>
                ) : discussions?.content && discussions.content.length > 0 ? (
                  <div className="space-y-4">
                    {discussions.content.map((discussion: DiscussionPayload) => {
                      const canDelete = canDeleteDiscussion(discussion);
                      const isDeleting =
                        deleteDiscussionMutation.isPending &&
                        deleteDiscussionMutation.variables === discussion.id;

                      return (
                        <div key={discussion.id} className="bg-surface-container-lowest rounded-2xl p-6 shadow-sm border border-outline-variant/10">
                          <div className="flex justify-between items-start gap-4 mb-3">
                            <div>
                              <h3 className="font-bold text-on-surface">{discussion.title}</h3>
                              <p className="text-sm text-on-surface-variant">
                                by {discussion.authorName} ({discussion.authorRole.replace('ROLE_', '')})
                                {' • '}
                                {new Date(discussion.createdAt).toLocaleString()}
                              </p>
                            </div>
                            {canDelete && (
                              <button
                                type="button"
                                onClick={() => void handleDeleteDiscussion(discussion)}
                                disabled={isDeleting}
                                className="text-[11px] font-bold bg-red-100 text-red-700 hover:bg-red-200 px-3 py-1.5 rounded-lg transition disabled:opacity-50"
                              >
                                {isDeleting ? 'Deleting...' : 'Delete'}
                              </button>
                            )}
                          </div>
                          <p className="text-on-surface whitespace-pre-wrap">{discussion.content}</p>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="text-center py-8 bg-surface-container-lowest rounded-2xl border border-outline-variant/10 shadow-sm">
                    <p className="text-on-surface-variant font-semibold">No messages yet. Start the conversation.</p>
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {activeTab === 'members' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {membersLoading ? (
              <div className="col-span-2 grid grid-cols-1 md:grid-cols-2 gap-4">
                {Array.from({ length: 4 }).map((_, index) => (
                  <div key={index} className="h-28 bg-surface-container-lowest rounded-2xl border border-outline-variant/10 animate-pulse" />
                ))}
              </div>
            ) : members?.content && members.content.length > 0 ? (
              members.content.map((member: GroupMemberPayload) => {
                const canRemove = currentRole === 'ROLE_ADMIN' && member.role !== 'OWNER';
                const isRemoving = removeMemberMutation.isPending && removeMemberMutation.variables === member.userId;

                return (
                  <div key={member.id} className="bg-surface-container-lowest rounded-2xl p-4 shadow-sm border border-outline-variant/10">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="font-medium text-on-surface">{member.name}</p>
                        <p className="text-sm text-on-surface-variant">{member.email}</p>
                        <p className="text-xs text-on-surface-variant mt-1">
                          Joined {new Date(member.joinedAt).toLocaleDateString()}
                        </p>
                        <p className="text-[10px] font-black text-on-surface-variant mt-1">{member.role}</p>
                      </div>

                      {canRemove && (
                        <button
                          type="button"
                          onClick={() => void handleRemoveMember(member)}
                          disabled={isRemoving}
                          className="text-[11px] font-bold bg-red-100 text-red-700 hover:bg-red-200 px-3 py-1.5 rounded-lg transition disabled:opacity-50"
                        >
                          {isRemoving ? 'Removing...' : 'Remove'}
                        </button>
                      )}
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="text-center col-span-2 py-8 bg-surface-container-lowest rounded-2xl border border-outline-variant/10 shadow-sm">
                <p className="text-on-surface-variant font-semibold">No members</p>
              </div>
            )}
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default GroupDetailPage;

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
  const canViewMessages = currentRole === 'ROLE_ADMIN' || isJoined;

  const { data: discussions, isLoading: discussionsLoading } = useQuery({
    queryKey: ['group', id, 'discussions'],
    queryFn: () => groupService.getGroupDiscussions(groupId),
    enabled: Number.isFinite(groupId) && canViewMessages,
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
        <div className="flex items-center justify-center h-screen">
          <div className="text-lg text-gray-500">Loading group...</div>
        </div>
      </PageLayout>
    );
  }

  if (!group) {
    return (
      <PageLayout>
        <div className="text-center py-12">
          <p className="text-lg text-gray-500 mb-4">Group not found</p>
          <button
            onClick={() => navigate('/groups')}
            className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700"
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
          className="text-blue-600 hover:text-blue-700 flex items-center gap-2"
        >
          ← Back to Groups
        </button>

        <div className="bg-gradient-to-r from-teal-600 to-green-600 rounded-lg p-8 text-white">
          <div className="flex justify-between items-start gap-4">
            <div>
              <h1 className="text-3xl font-bold">{group.name}</h1>
              <p className="text-teal-100 mt-2">{group.description}</p>
              <div className="flex gap-4 mt-4 text-sm">
                <span>{group.memberCount}/{group.maxMembers || '?'} members</span>
                <span>{group.category}</span>
              </div>
            </div>

            {!isJoined ? (
              <button
                onClick={() => joinGroupMutation.mutate()}
                disabled={joinGroupMutation.isPending}
                className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition disabled:opacity-50"
              >
                {joinGroupMutation.isPending ? 'Joining...' : 'Join Group'}
              </button>
            ) : (
              <button
                onClick={() => void handleLeaveGroup()}
                disabled={leaveGroupMutation.isPending}
                className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 transition disabled:opacity-50"
              >
                Leave Group
              </button>
            )}
          </div>
        </div>

        <div className="border-b border-gray-200">
          <div className="flex space-x-8">
            {['discussion', 'members'].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab as typeof activeTab)}
                className={`py-4 px-1 border-b-2 font-medium text-sm capitalize ${
                  activeTab === tab
                    ? 'border-teal-500 text-teal-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
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
              <div className="text-center py-10 bg-gray-50 rounded-lg border border-gray-200">
                <p className="text-gray-600 mb-3">Join this group to access member-only messages.</p>
                <button
                  onClick={() => joinGroupMutation.mutate()}
                  disabled={joinGroupMutation.isPending}
                  className="bg-blue-600 text-white px-5 py-2 rounded hover:bg-blue-700 transition disabled:opacity-50"
                >
                  {joinGroupMutation.isPending ? 'Joining...' : 'Join to Start Messaging'}
                </button>
              </div>
            ) : (
              <>
                <button
                  onClick={() => setShowDiscussionForm((value) => !value)}
                  className="bg-teal-600 text-white px-6 py-2 rounded hover:bg-teal-700 transition"
                >
                  + New Message
                </button>

                {showDiscussionForm && (
                  <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
                    <form onSubmit={handlePostDiscussion} className="space-y-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
                        <input
                          type="text"
                          value={newDiscussionTitle}
                          onChange={(event) => setNewDiscussionTitle(event.target.value)}
                          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                          placeholder="Message title"
                          required
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Message</label>
                        <textarea
                          value={newDiscussionContent}
                          onChange={(event) => setNewDiscussionContent(event.target.value)}
                          rows={4}
                          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                          placeholder="Write your message"
                          required
                        />
                      </div>

                      <div className="flex gap-2">
                        <button
                          type="submit"
                          disabled={postDiscussionMutation.isPending}
                          className="bg-teal-600 text-white px-6 py-2 rounded hover:bg-teal-700 transition disabled:opacity-50"
                        >
                          {postDiscussionMutation.isPending ? 'Posting...' : 'Post Message'}
                        </button>
                        <button
                          type="button"
                          onClick={() => setShowDiscussionForm(false)}
                          className="bg-gray-300 text-gray-900 px-6 py-2 rounded hover:bg-gray-400 transition"
                        >
                          Cancel
                        </button>
                      </div>
                    </form>
                  </div>
                )}

                {discussionsLoading ? (
                  <p className="text-center text-gray-500 py-8">Loading messages...</p>
                ) : discussions?.content && discussions.content.length > 0 ? (
                  <div className="space-y-4">
                    {discussions.content.map((discussion: DiscussionPayload) => {
                      const canDelete = canDeleteDiscussion(discussion);
                      const isDeleting =
                        deleteDiscussionMutation.isPending &&
                        deleteDiscussionMutation.variables === discussion.id;

                      return (
                        <div key={discussion.id} className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
                          <div className="flex justify-between items-start gap-4 mb-3">
                            <div>
                              <h3 className="font-bold text-gray-900">{discussion.title}</h3>
                              <p className="text-sm text-gray-500">
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
                          <p className="text-gray-700 whitespace-pre-wrap">{discussion.content}</p>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="text-center py-8 bg-gray-50 rounded-lg border border-gray-200">
                    <p className="text-gray-500">No messages yet. Start the conversation.</p>
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {activeTab === 'members' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {membersLoading ? (
              <p className="text-center col-span-2 text-gray-500 py-8">Loading members...</p>
            ) : members?.content && members.content.length > 0 ? (
              members.content.map((member: GroupMemberPayload) => {
                const canRemove = currentRole === 'ROLE_ADMIN' && member.role !== 'OWNER';
                const isRemoving = removeMemberMutation.isPending && removeMemberMutation.variables === member.userId;

                return (
                  <div key={member.id} className="bg-white rounded-lg p-4 shadow-sm border border-gray-200">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="font-medium text-gray-900">{member.name}</p>
                        <p className="text-sm text-gray-500">{member.email}</p>
                        <p className="text-xs text-gray-400 mt-1">
                          Joined {new Date(member.joinedAt).toLocaleDateString()}
                        </p>
                        <p className="text-[10px] font-black text-gray-500 mt-1">{member.role}</p>
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
              <p className="text-center col-span-2 text-gray-500 py-8">No members</p>
            )}
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default GroupDetailPage;

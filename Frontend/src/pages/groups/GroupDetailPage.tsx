import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import groupService from '../../services/groupService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import { useActionConfirm } from '../../components/ui/ActionConfirm';

const GroupDetailPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const { requestConfirmation } = useActionConfirm();

  const [activeTab, setActiveTab] = useState<'discussion' | 'members'>('discussion');
  const [newDiscussionTitle, setNewDiscussionTitle] = useState('');
  const [newDiscussionContent, setNewDiscussionContent] = useState('');
  const [showDiscussionForm, setShowDiscussionForm] = useState(false);

  // Fetch group
  const { data: group, isLoading: groupLoading } = useQuery({
    queryKey: ['group', id],
    queryFn: () => groupService.getGroupById(Number(id)),
  });

  // Fetch discussions
  const { data: discussions, isLoading: discussionsLoading } = useQuery({
    queryKey: ['group', id, 'discussions'],
    queryFn: () => groupService.getGroupDiscussions(Number(id)),
  });

  // Fetch members
  const { data: members } = useQuery({
    queryKey: ['group', id, 'members'],
    queryFn: () => groupService.getGroupMembers(Number(id)),
  });

  // Post discussion mutation
  const postDiscussionMutation = useMutation({
    mutationFn: () =>
      groupService.postDiscussion(Number(id), newDiscussionTitle, newDiscussionContent),
    onSuccess: () => {
      showToast({ message: 'Discussion posted successfully', type: 'success' });
      setNewDiscussionTitle('');
      setNewDiscussionContent('');
      setShowDiscussionForm(false);
      queryClient.invalidateQueries({ queryKey: ['group', id, 'discussions'] });
    },
    onError: () => {
      showToast({ message: 'Failed to post discussion', type: 'error' });
    },
  });

  // Leave group mutation
  const leaveGroupMutation = useMutation({
    mutationFn: () => groupService.leaveGroup(Number(id)),
    onSuccess: () => {
      showToast({ message: 'Left group successfully', type: 'success' });
      navigate('/groups');
    },
    onError: () => {
      showToast({ message: 'Failed to leave group', type: 'error' });
    },
  });

  const handlePostDiscussion = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newDiscussionTitle || !newDiscussionContent) {
      showToast({ message: 'Please fill in all fields', type: 'error' });
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
      requiredText: 'YES',
    });

    if (!confirmed) {
      return;
    }

    leaveGroupMutation.mutate();
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
        {/* Back Button */}
        <button
          onClick={() => navigate('/groups')}
          className="text-blue-600 hover:text-blue-700 flex items-center gap-2"
        >
          ← Back to Groups
        </button>

        {/* Group Header */}
        <div className="bg-gradient-to-r from-teal-600 to-green-600 rounded-lg p-8 text-white">
          <div className="flex justify-between items-start">
            <div>
              <h1 className="text-3xl font-bold">{group.name}</h1>
              <p className="text-teal-100 mt-2">{group.description}</p>
              <div className="flex gap-4 mt-4 text-sm">
                <span>📊 {group.memberCount} members</span>
                <span>📂 {group.category}</span>
              </div>
            </div>
            <button
              onClick={() => void handleLeaveGroup()}
              disabled={leaveGroupMutation.isPending}
              className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 transition disabled:opacity-50"
            >
              Leave Group
            </button>
          </div>
        </div>

        {/* Tabs */}
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

        {/* Discussion Tab */}
        {activeTab === 'discussion' && (
          <div className="space-y-6">
            {/* New Discussion Button */}
            <button
              onClick={() => setShowDiscussionForm(!showDiscussionForm)}
              className="bg-teal-600 text-white px-6 py-2 rounded hover:bg-teal-700 transition"
            >
              + Start Discussion
            </button>

            {/* Discussion Form */}
            {showDiscussionForm && (
              <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
                <form onSubmit={handlePostDiscussion} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
                    <input
                      type="text"
                      value={newDiscussionTitle}
                      onChange={(e) => setNewDiscussionTitle(e.target.value)}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                      placeholder="What's on your mind?"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Message</label>
                    <textarea
                      value={newDiscussionContent}
                      onChange={(e) => setNewDiscussionContent(e.target.value)}
                      rows={4}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                      placeholder="Share your thoughts..."
                      required
                    />
                  </div>

                  <div className="flex gap-2">
                    <button
                      type="submit"
                      disabled={postDiscussionMutation.isPending}
                      className="bg-teal-600 text-white px-6 py-2 rounded hover:bg-teal-700 transition disabled:opacity-50"
                    >
                      {postDiscussionMutation.isPending ? 'Posting...' : 'Post'}
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

            {/* Discussions List */}
            {discussionsLoading ? (
              <p className="text-center text-gray-500 py-8">Loading discussions...</p>
            ) : discussions?.content && discussions.content.length > 0 ? (
              <div className="space-y-4">
                {discussions.content.map((discussion: any) => (
                  <div key={discussion.id} className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
                    <div className="flex justify-between items-start mb-3">
                      <div>
                        <h3 className="font-bold text-gray-900">{discussion.title}</h3>
                        <p className="text-sm text-gray-500">
                          by {discussion.authorName} • {new Date(discussion.createdAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                    <p className="text-gray-700">{discussion.content}</p>
                    {discussion.replies && (
                      <div className="mt-4 text-sm text-gray-500">
                        {discussion.replies} replies
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8 bg-gray-50 rounded-lg border border-gray-200">
                <p className="text-gray-500">No discussions yet. Start one!</p>
              </div>
            )}
          </div>
        )}

        {/* Members Tab */}
        {activeTab === 'members' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {members?.content && members.content.length > 0 ? (
              members.content.map((member: any) => (
                <div key={member.id} className="bg-white rounded-lg p-4 shadow-sm border border-gray-200">
                  <div className="flex items-center gap-4">
                    <img
                      src={member.profileImage || 'https://via.placeholder.com/80'}
                      alt={member.name}
                      className="w-16 h-16 rounded-full object-cover"
                    />
                    <div className="flex-1">
                      <p className="font-medium text-gray-900">{member.name}</p>
                      <p className="text-sm text-gray-500">{member.email}</p>
                      <p className="text-xs text-gray-400 mt-1">
                        Joined {new Date(member.joinedAt).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                </div>
              ))
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

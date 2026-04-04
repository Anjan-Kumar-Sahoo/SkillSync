import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import groupService from '../../services/groupService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import type { CreateGroupPayload } from '../../services/groupService';

const GroupsPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  const [activeTab, setActiveTab] = useState<'explore' | 'mygroups'>('explore');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    category: '',
  });

  // Fetch explore groups
  const { data: exploreData, isLoading: exploreLoading } = useQuery({
    queryKey: ['groups', 'explore', page, search],
    queryFn: () => groupService.getGroups(search, undefined, page, 10),
  });

  // Fetch my groups
  const { data: myGroupsData, isLoading: myGroupsLoading } = useQuery({
    queryKey: ['groups', 'my', page],
    queryFn: () => groupService.getMyGroups(page, 10),
  });

  // Create group mutation
  const createGroupMutation = useMutation({
    mutationFn: (payload: CreateGroupPayload) => groupService.createGroup(payload),
    onSuccess: () => {
      showToast({ message: 'Group created successfully', type: 'success' });
      setShowCreateModal(false);
      setFormData({ name: '', description: '', category: '' });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: () => {
      showToast({ message: 'Failed to create group', type: 'error' });
    },
  });

  // Join group mutation
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

  // Leave group mutation
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

  const handleCreateGroup = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name || !formData.description) {
      showToast({ message: 'Please fill in all fields', type: 'error' });
      return;
    }
    createGroupMutation.mutate({
      name: formData.name,
      description: formData.description,
      category: formData.category || 'General',
    });
  };

  return (
    <PageLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="bg-gradient-to-r from-green-600 to-teal-600 rounded-lg p-8 text-white">
          <h1 className="text-3xl font-bold mb-2">Learning Groups</h1>
          <p className="text-green-100">Join communities and learn together with peers</p>
        </div>

        {/* Tabs */}
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
                {tab === 'explore' ? 'Explore Groups' : 'My Groups'}
              </button>
            ))}
          </div>
        </div>

        {/* Explore Groups Tab */}
        {activeTab === 'explore' && (
          <div className="space-y-4">
            {/* Search and Create */}
            <div className="flex gap-4">
              <input
                type="text"
                placeholder="Search groups..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
              />
              <button
                onClick={() => setShowCreateModal(true)}
                className="bg-green-600 text-white px-6 py-2 rounded-lg hover:bg-green-700 transition"
              >
                + Create Group
              </button>
            </div>

            {/* Groups Grid */}
            {exploreLoading ? (
              <p className="text-center text-gray-500 py-8">Loading groups...</p>
            ) : exploreData?.content && exploreData.content.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {exploreData.content.map((group: any) => (
                  <div
                    key={group.id}
                    className="bg-white rounded-lg p-6 shadow-sm border border-gray-200 hover:shadow-md transition cursor-pointer"
                  >
                    <h3 className="font-bold text-gray-900 mb-2">{group.name}</h3>
                    <p className="text-sm text-gray-600 mb-4 line-clamp-2">{group.description}</p>
                    <div className="flex items-center justify-between mb-4">
                      <span className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">
                        {group.category}
                      </span>
                      <span className="text-xs text-gray-500">{group.memberCount} members</span>
                    </div>
                    <button
                      onClick={() => joinGroupMutation.mutate(group.id)}
                      disabled={joinGroupMutation.isPending}
                      className="w-full bg-green-600 text-white py-2 rounded hover:bg-green-700 transition disabled:opacity-50"
                    >
                      {group.isJoined ? 'Joined' : 'Join Group'}
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-center text-gray-500 py-8">No groups found</p>
            )}

            {/* Pagination */}
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

        {/* My Groups Tab */}
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
                        onClick={() => leaveGroupMutation.mutate(group.id)}
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

        {/* Create Group Modal */}
        {showCreateModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-lg p-8 max-w-md w-full">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">Create New Group</h2>
              <form onSubmit={handleCreateGroup} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Group Name</label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    rows={4}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                  <select
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
                  >
                    <option value="">Select Category</option>
                    <option value="Programming">Programming</option>
                    <option value="Design">Design</option>
                    <option value="Business">Business</option>
                    <option value="General">General</option>
                  </select>
                </div>

                <div className="flex gap-3 pt-4">
                  <button
                    type="submit"
                    disabled={createGroupMutation.isPending}
                    className="flex-1 bg-green-600 text-white py-2 rounded-lg hover:bg-green-700 transition disabled:opacity-50"
                  >
                    {createGroupMutation.isPending ? 'Creating...' : 'Create Group'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowCreateModal(false)}
                    className="flex-1 bg-gray-300 text-gray-900 py-2 rounded-lg hover:bg-gray-400 transition"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default GroupsPage;

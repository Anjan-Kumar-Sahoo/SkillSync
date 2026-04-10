import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import PageLayout from '../../components/layout/PageLayout';
import groupService from '../../services/groupService';
import { useToast } from '../../components/ui/Toast';
import { useActionConfirm } from '../../components/ui/ActionConfirm';
import type { CreateGroupPayload, GroupMemberPayload, UpdateGroupPayload } from '../../services/groupService';
import type { GroupData } from '../../store/slices/groupsSlice';

const PAGE_SIZE = 10;
const MEMBERS_PAGE_SIZE = 20;

const initialForm = {
  name: '',
  description: '',
  category: 'General',
  maxMembers: 50,
};

const AdminGroupsPage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const { requestConfirmation } = useActionConfirm();

  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [searchText, setSearchText] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createForm, setCreateForm] = useState(initialForm);

  const [editingGroup, setEditingGroup] = useState<GroupData | null>(null);
  const [editForm, setEditForm] = useState(initialForm);

  const [membersGroup, setMembersGroup] = useState<GroupData | null>(null);
  const [membersPage, setMembersPage] = useState(0);

  const { data: groupsData, isLoading } = useQuery({
    queryKey: ['admin', 'groups', page, searchText, categoryFilter],
    queryFn: () => groupService.getGroups(searchText, categoryFilter || undefined, page, PAGE_SIZE),
    refetchInterval: 5000,
  });

  const { data: membersData, isLoading: membersLoading } = useQuery({
    queryKey: ['admin', 'groups', membersGroup?.id, 'members', membersPage],
    queryFn: () => groupService.getGroupMembers(membersGroup!.id, membersPage, MEMBERS_PAGE_SIZE),
    enabled: !!membersGroup,
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreateGroupPayload) => groupService.createGroup(payload),
    onSuccess: () => {
      showToast({ message: 'Group created successfully', type: 'success' });
      setShowCreateModal(false);
      setCreateForm(initialForm);
      setSearchInput('');
      setSearchText('');
      setCategoryFilter('');
      setPage(0);
      queryClient.invalidateQueries({ queryKey: ['admin', 'groups'] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
      void queryClient.refetchQueries({ queryKey: ['admin', 'groups'] });
      void queryClient.refetchQueries({ queryKey: ['groups'] });
    },
    onError: () => showToast({ message: 'Failed to create group', type: 'error' }),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateGroupPayload }) =>
      groupService.updateGroup(id, payload),
    onSuccess: () => {
      showToast({ message: 'Group updated successfully', type: 'success' });
      setEditingGroup(null);
      queryClient.invalidateQueries({ queryKey: ['admin', 'groups'] });
      queryClient.invalidateQueries({ queryKey: ['group'] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: () => showToast({ message: 'Failed to update group', type: 'error' }),
  });

  const deleteMutation = useMutation({
    mutationFn: (groupId: number) => groupService.deleteGroup(groupId),
    onSuccess: () => {
      showToast({ message: 'Group deleted successfully', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['admin', 'groups'] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: () => showToast({ message: 'Failed to delete group', type: 'error' }),
  });

  const removeMemberMutation = useMutation({
    mutationFn: ({ groupId, memberUserId }: { groupId: number; memberUserId: number }) =>
      groupService.removeGroupMember(groupId, memberUserId),
    onSuccess: () => {
      showToast({ message: 'Member removed successfully', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['admin', 'groups'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'groups', membersGroup?.id, 'members'] });
      queryClient.invalidateQueries({ queryKey: ['group', membersGroup?.id, 'members'] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: () => showToast({ message: 'Failed to remove member', type: 'error' }),
  });

  const groups = groupsData?.content || [];
  const totalElements = Number(groupsData?.totalElements || groups.length || 0);
  const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE));

  const memberRows = useMemo<GroupMemberPayload[]>(() => {
    return membersData?.content || [];
  }, [membersData]);

  const membersTotalPages = Math.max(
    1,
    Math.ceil(Number(membersData?.totalElements || memberRows.length || 0) / MEMBERS_PAGE_SIZE)
  );

  const resetEditForm = (group: GroupData) => {
    setEditForm({
      name: group.name,
      description: group.description || '',
      category: group.category || 'General',
      maxMembers: Math.max(2, Number(group.maxMembers || group.memberCount || 2)),
    });
  };

  const handleSearch = () => {
    setSearchText(searchInput.trim());
    setPage(0);
  };

  const handleCreate = (event: React.FormEvent) => {
    event.preventDefault();
    if (!createForm.name.trim()) {
      showToast({ message: 'Group name is required', type: 'error' });
      return;
    }

    createMutation.mutate({
      name: createForm.name.trim(),
    });
  };

  const handleUpdate = (event: React.FormEvent) => {
    event.preventDefault();
    if (!editingGroup) return;

    updateMutation.mutate({
      id: editingGroup.id,
      payload: {
        name: editForm.name.trim(),
        description: editForm.description.trim(),
        category: editForm.category.trim(),
        maxMembers: Number(editForm.maxMembers),
      },
    });
  };

  const handleDeleteGroup = async (group: GroupData) => {
    const confirmed = await requestConfirmation({
      title: 'Delete Group?',
      message: `Delete group "${group.name}"? All group messages and membership links will be removed.`,
      confirmLabel: 'Yes, delete group',
    });

    if (!confirmed) return;
    deleteMutation.mutate(group.id);
  };

  const handleRemoveMember = async (member: GroupMemberPayload) => {
    if (!membersGroup) return;

    const confirmed = await requestConfirmation({
      title: 'Remove Member?',
      message: `Remove ${member.name} from "${membersGroup.name}"?`,
      confirmLabel: 'Yes, remove member',
    });

    if (!confirmed) return;

    removeMemberMutation.mutate({
      groupId: membersGroup.id,
      memberUserId: member.userId,
    });
  };

  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-2xl p-6 shadow-sm">
          <h1 className="text-3xl font-extrabold text-on-surface tracking-tight">Manage Groups</h1>
          <p className="text-on-surface-variant mt-2">
            Admin can create groups, edit group details, and remove members.
          </p>
        </div>

        <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-2xl p-4 shadow-sm flex flex-col lg:flex-row items-end gap-4">
          <div className="flex-1 w-full">
            <label className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">
              Search
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="Search by name or description"
                className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
              />
              <button
                type="button"
                onClick={handleSearch}
                className="h-10 px-5 gradient-btn text-white font-bold rounded-lg shadow-sm hover:shadow-md transition-all"
              >
                Search
              </button>
            </div>
          </div>

          <div className="w-full lg:w-48">
            <label className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">
              Category
            </label>
            <select
              value={categoryFilter}
              onChange={(event) => {
                setCategoryFilter(event.target.value);
                setPage(0);
              }}
              className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
            >
              <option value="">All</option>
              <option value="Programming">Programming</option>
              <option value="Design">Design</option>
              <option value="Business">Business</option>
              <option value="General">General</option>
            </select>
          </div>

          <button
            type="button"
            onClick={() => setShowCreateModal(true)}
            className="h-10 px-5 bg-primary text-on-primary rounded-lg font-bold hover:bg-primary-dark transition-colors"
          >
            + Create Group
          </button>
        </div>

        <div className="bg-surface-container-lowest border border-outline-variant/10 rounded-2xl shadow-sm overflow-hidden">
          {isLoading ? (
            <div className="p-8 text-center text-on-surface-variant">Loading groups...</div>
          ) : groups.length === 0 ? (
            <div className="p-8 text-center text-on-surface-variant">No groups found.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-surface-container border-b border-outline-variant/10">
                  <tr>
                    <th className="text-left py-3 px-5 text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Name</th>
                    <th className="text-left py-3 px-5 text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Category</th>
                    <th className="text-left py-3 px-5 text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Members</th>
                    <th className="text-left py-3 px-5 text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Max</th>
                    <th className="text-right py-3 px-5 text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {groups.map((group) => (
                    <tr key={group.id} className="border-b border-outline-variant/5 hover:bg-surface-container-low/50 transition-colors">
                      <td className="py-3 px-5">
                        <p className="text-sm font-bold text-on-surface">{group.name}</p>
                        <p className="text-xs text-on-surface-variant line-clamp-2">{group.description}</p>
                      </td>
                      <td className="py-3 px-5 text-sm text-on-surface-variant">{group.category}</td>
                      <td className="py-3 px-5 text-sm text-on-surface">{group.memberCount}</td>
                      <td className="py-3 px-5 text-sm text-on-surface">{group.maxMembers || '-'}</td>
                      <td className="py-3 px-5 text-right">
                        <div className="flex justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => {
                              setMembersGroup(group);
                              setMembersPage(0);
                            }}
                            className="text-[10px] font-bold bg-blue-100 text-blue-700 hover:bg-blue-200 px-3 py-1.5 rounded-lg transition"
                          >
                            Members
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setEditingGroup(group);
                              resetEditForm(group);
                            }}
                            className="text-[10px] font-bold bg-amber-100 text-amber-700 hover:bg-amber-200 px-3 py-1.5 rounded-lg transition"
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            onClick={() => void handleDeleteGroup(group)}
                            disabled={deleteMutation.isPending}
                            className="text-[10px] font-bold bg-red-100 text-red-700 hover:bg-red-200 px-3 py-1.5 rounded-lg transition disabled:opacity-50"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {!isLoading && groups.length > 0 && totalPages > 1 && (
            <div className="px-5 py-3 border-t border-outline-variant/10 bg-surface-container-lowest flex items-center justify-between gap-3">
              <button
                onClick={() => setPage((value) => Math.max(0, value - 1))}
                disabled={page <= 0}
                className="px-3 py-1.5 rounded-md text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <p className="text-xs font-semibold text-on-surface-variant">
                Page {page + 1} of {totalPages}
              </p>
              <button
                onClick={() => setPage((value) => value + 1)}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 rounded-md text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          )}
        </div>

        {showCreateModal && (
          <div className="fixed inset-0 z-[120] bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="w-full max-w-xl rounded-2xl border border-outline-variant/20 bg-surface-container-lowest shadow-2xl overflow-hidden">
              <div className="px-6 py-5 border-b border-outline-variant/10 bg-surface-container-low">
                <h2 className="text-xl font-extrabold text-on-surface">Create Group</h2>
              </div>
              <form onSubmit={handleCreate} className="px-6 py-5 space-y-4">
                <input
                  type="text"
                  value={createForm.name}
                  onChange={(event) => setCreateForm((prev) => ({ ...prev, name: event.target.value }))}
                  placeholder="Group name"
                  className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
                  required
                />
                <p className="text-xs font-semibold text-on-surface-variant">
                  Group will be created immediately with default settings. You can edit description/category/capacity later.
                </p>
                <div className="flex justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setShowCreateModal(false)}
                    className="px-4 py-2 rounded-lg text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={createMutation.isPending}
                    className="px-4 py-2 rounded-lg text-sm font-bold bg-primary text-on-primary hover:bg-primary-dark disabled:opacity-50"
                  >
                    {createMutation.isPending ? 'Creating...' : 'Create'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {editingGroup && (
          <div className="fixed inset-0 z-[120] bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="w-full max-w-xl rounded-2xl border border-outline-variant/20 bg-surface-container-lowest shadow-2xl overflow-hidden">
              <div className="px-6 py-5 border-b border-outline-variant/10 bg-surface-container-low">
                <h2 className="text-xl font-extrabold text-on-surface">Edit Group</h2>
              </div>
              <form onSubmit={handleUpdate} className="px-6 py-5 space-y-4">
                <input
                  type="text"
                  value={editForm.name}
                  onChange={(event) => setEditForm((prev) => ({ ...prev, name: event.target.value }))}
                  placeholder="Group name"
                  className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
                  required
                />
                <textarea
                  value={editForm.description}
                  onChange={(event) => setEditForm((prev) => ({ ...prev, description: event.target.value }))}
                  placeholder="Description"
                  rows={4}
                  className="w-full bg-surface-container px-3 py-2 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
                  required
                />
                <div className="grid grid-cols-2 gap-3">
                  <select
                    value={editForm.category}
                    onChange={(event) => setEditForm((prev) => ({ ...prev, category: event.target.value }))}
                    className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
                  >
                    <option value="Programming">Programming</option>
                    <option value="Design">Design</option>
                    <option value="Business">Business</option>
                    <option value="General">General</option>
                  </select>
                  <input
                    type="number"
                    min={2}
                    max={200}
                    value={editForm.maxMembers}
                    onChange={(event) =>
                      setEditForm((prev) => ({ ...prev, maxMembers: Number(event.target.value) || 2 }))
                    }
                    className="w-full h-10 bg-surface-container px-3 rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
                  />
                </div>
                <div className="flex justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setEditingGroup(null)}
                    className="px-4 py-2 rounded-lg text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={updateMutation.isPending}
                    className="px-4 py-2 rounded-lg text-sm font-bold bg-primary text-on-primary hover:bg-primary-dark disabled:opacity-50"
                  >
                    {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {membersGroup && (
          <div className="fixed inset-0 z-[120] bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="w-full max-w-2xl rounded-2xl border border-outline-variant/20 bg-surface-container-lowest shadow-2xl overflow-hidden">
              <div className="px-6 py-5 border-b border-outline-variant/10 bg-surface-container-low flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-extrabold text-on-surface">Group Members</h2>
                  <p className="text-sm text-on-surface-variant">{membersGroup.name}</p>
                </div>
                <button
                  type="button"
                  onClick={() => setMembersGroup(null)}
                  className="px-3 py-1.5 rounded-lg text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface"
                >
                  Close
                </button>
              </div>

              <div className="px-6 py-5 space-y-3 max-h-[65vh] overflow-y-auto">
                {membersLoading ? (
                  <p className="text-sm text-on-surface-variant">Loading members...</p>
                ) : memberRows.length === 0 ? (
                  <p className="text-sm text-on-surface-variant">No members found.</p>
                ) : (
                  memberRows.map((member) => {
                    const isOwner = member.role === 'OWNER';
                    return (
                      <div
                        key={member.id}
                        className="p-3 rounded-xl border border-outline-variant/20 bg-surface-container-low flex items-center justify-between gap-3"
                      >
                        <div>
                          <p className="text-sm font-bold text-on-surface">{member.name}</p>
                          <p className="text-xs text-on-surface-variant">{member.email}</p>
                          <p className="text-[10px] font-black uppercase tracking-widest text-on-surface-variant mt-1">
                            {member.role}
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => void handleRemoveMember(member)}
                          disabled={isOwner || removeMemberMutation.isPending}
                          className="text-[10px] font-bold bg-red-100 text-red-700 hover:bg-red-200 px-3 py-1.5 rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          Remove
                        </button>
                      </div>
                    );
                  })
                )}
              </div>

              {memberRows.length > 0 && membersTotalPages > 1 && (
                <div className="px-6 py-4 border-t border-outline-variant/10 bg-surface-container-low/60 flex items-center justify-between gap-3">
                  <button
                    onClick={() => setMembersPage((value) => Math.max(0, value - 1))}
                    disabled={membersPage <= 0}
                    className="px-3 py-1.5 rounded-md text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Previous
                  </button>
                  <p className="text-xs font-semibold text-on-surface-variant">
                    Page {membersPage + 1} of {membersTotalPages}
                  </p>
                  <button
                    onClick={() => setMembersPage((value) => value + 1)}
                    disabled={membersPage >= membersTotalPages - 1}
                    className="px-3 py-1.5 rounded-md text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Next
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default AdminGroupsPage;

import api from './axios';
import type { GroupData } from '../store/slices/groupsSlice';
import { store } from '../store';

export interface CreateGroupPayload {
  name: string;
  description: string;
  category: string;
}

export interface UpdateGroupPayload {
  name?: string;
  description?: string;
  category?: string;
}

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

const JOINED_GROUPS_KEY = 'skillsync.joinedGroups';

const getJoinedGroupIds = (): number[] => {
  try {
    const raw = window.localStorage.getItem(JOINED_GROUPS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
};

const setJoinedGroupIds = (ids: number[]) => {
  window.localStorage.setItem(JOINED_GROUPS_KEY, JSON.stringify([...new Set(ids)]));
};

const mapGroup = (group: any): GroupData => {
  const userId = store.getState().auth.user?.id;
  const joinedIds = getJoinedGroupIds();
  const joined = joinedIds.includes(group.id) || (userId ? group.createdBy === userId : false);

  return {
    id: group.id,
    name: group.name,
    description: group.description,
    category: group.category || 'General',
    createdBy: group.createdBy,
    createdByName: group.createdByName || `User #${group.createdBy}`,
    memberCount: group.memberCount || 0,
    members: group.members || [],
    isJoined: joined,
    createdAt: group.createdAt,
    updatedAt: group.updatedAt || group.createdAt,
  };
};

class GroupService {
  async getGroups(
    search?: string,
    category?: string,
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedResponse<GroupData>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/groups?${params.toString()}`);
    const serverContent = (res.data?.content || []).map(mapGroup);
    const filtered = serverContent.filter((g: GroupData) => {
      const matchesSearch = !search || g.name.toLowerCase().includes(search.toLowerCase()) || g.description.toLowerCase().includes(search.toLowerCase());
      const matchesCategory = !category || category === 'All' || g.category?.toLowerCase() === category.toLowerCase();
      return matchesSearch && matchesCategory;
    });

    return {
      content: filtered,
      totalElements: filtered.length,
      page,
      size,
    };
  }

  async getMyGroups(
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedResponse<GroupData>> {
    const allGroups = await this.getGroups(undefined, undefined, page, size * 3);
    const userId = store.getState().auth.user?.id;
    const joinedIds = getJoinedGroupIds();
    const mine = allGroups.content.filter((g) => joinedIds.includes(g.id) || (userId ? g.createdBy === userId : false));
    return {
      content: mine,
      totalElements: mine.length,
      page,
      size,
    };
  }

  async getGroupById(id: number): Promise<GroupData> {
    const res = await api.get(`/api/groups/${id}`);
    return mapGroup(res.data);
  }

  async createGroup(payload: CreateGroupPayload): Promise<GroupData> {
    const res = await api.post('/api/groups', payload);
    const created = mapGroup(res.data);
    setJoinedGroupIds([...getJoinedGroupIds(), created.id]);
    return created;
  }

  async updateGroup(
    id: number,
    payload: UpdateGroupPayload
  ): Promise<GroupData> {
    const res = await api.put(`/api/groups/${id}`, payload);
    return res.data;
  }

  async deleteGroup(id: number): Promise<void> {
    await api.delete(`/api/groups/${id}`);
  }

  async joinGroup(id: number): Promise<GroupData> {
    await api.post(`/api/groups/${id}/join`, {});
    setJoinedGroupIds([...getJoinedGroupIds(), id]);
    return this.getGroupById(id);
  }

  async leaveGroup(id: number): Promise<void> {
    await api.post(`/api/groups/${id}/leave`, {});
    setJoinedGroupIds(getJoinedGroupIds().filter((groupId) => groupId !== id));
  }

  async getGroupMembers(
    _groupId: number,
    page: number = 0,
    size: number = 20
  ): Promise<any> {
    return {
      content: [],
      totalElements: 0,
      page,
      size,
    };
  }

  async getGroupDiscussions(
    groupId: number,
    page: number = 0,
    size: number = 20
  ): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/groups/${groupId}/discussions?${params.toString()}`);
    return res.data;
  }

  async postDiscussion(
    groupId: number,
    title: string,
    content: string
  ): Promise<any> {
    const res = await api.post(`/api/groups/${groupId}/discussions`, {
      title,
      content,
    });
    return res.data;
  }
}

export default new GroupService();

import api from './axios';
import type { GroupData } from '../store/slices/groupsSlice';

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

class GroupService {
  async getGroups(
    search?: string,
    category?: string,
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedResponse<GroupData>> {
    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (category) params.append('category', category);
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/groups?${params.toString()}`);
    return res.data;
  }

  async getMyGroups(
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedResponse<GroupData>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/groups/my?${params.toString()}`);
    return res.data;
  }

  async getGroupById(id: number): Promise<GroupData> {
    const res = await api.get(`/api/groups/${id}`);
    return res.data;
  }

  async createGroup(payload: CreateGroupPayload): Promise<GroupData> {
    const res = await api.post('/api/groups', payload);
    return res.data;
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
    const res = await api.post(`/api/groups/${id}/join`, {});
    return res.data;
  }

  async leaveGroup(id: number): Promise<void> {
    await api.post(`/api/groups/${id}/leave`, {});
  }

  async getGroupMembers(
    groupId: number,
    page: number = 0,
    size: number = 20
  ): Promise<any> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/groups/${groupId}/members?${params.toString()}`);
    return res.data;
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

import api from './axios';
import type { GroupData } from '../store/slices/groupsSlice';

export interface CreateGroupPayload {
  name: string;
  description?: string;
  category?: string;
  maxMembers?: number;
}

export interface UpdateGroupPayload {
  name?: string;
  description?: string;
  category?: string;
  maxMembers?: number;
}

export interface GroupMemberPayload {
  id: number;
  userId: number;
  name: string;
  email: string;
  role: string;
  joinedAt: string;
}

export interface DiscussionPayload {
  id: number;
  groupId: number;
  authorId: number;
  authorName: string;
  authorRole: string;
  title: string;
  content: string;
  parentId: number | null;
  replies: number;
  createdAt: string;
  isAdmin?: boolean;
}

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

const mapGroup = (group: any): GroupData => {
  const joined = Boolean(group.joined);

  return {
    id: group.id,
    name: group.name,
    description: group.description || 'No description provided.',
    category: group.category || 'General',
    maxMembers: group.maxMembers,
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
    params.append('sort', 'createdAt,desc');
    if (search?.trim()) params.append('search', search.trim());
    if (category && category !== 'All') params.append('category', category);

    const res = await api.get(`/api/groups?${params.toString()}`);
    const serverContent = (res.data?.content || []).map(mapGroup);

    return {
      content: serverContent,
      totalElements: res.data?.totalElements ?? serverContent.length,
      page: res.data?.number ?? page,
      size: res.data?.size ?? size,
    };
  }

  async getMyGroups(
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedResponse<GroupData>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sort', 'createdAt,desc');

    const res = await api.get(`/api/groups/my?${params.toString()}`);
    const mine = (res.data?.content || []).map(mapGroup);
    return {
      content: mine,
      totalElements: res.data?.totalElements ?? mine.length,
      page: res.data?.number ?? page,
      size: res.data?.size ?? size,
    };
  }

  async getGroupById(id: number): Promise<GroupData> {
    const res = await api.get(`/api/groups/${id}`);
    return mapGroup(res.data);
  }

  async createGroup(payload: CreateGroupPayload): Promise<GroupData> {
    const res = await api.post('/api/groups', payload);
    return mapGroup(res.data);
  }

  async updateGroup(
    id: number,
    payload: UpdateGroupPayload
  ): Promise<GroupData> {
    const res = await api.put(`/api/groups/${id}`, payload);
    return mapGroup(res.data);
  }

  async deleteGroup(id: number): Promise<void> {
    await api.delete(`/api/groups/${id}`);
  }

  async joinGroup(id: number): Promise<GroupData> {
    await api.post(`/api/groups/${id}/join`, {});
    return this.getGroupById(id);
  }

  async leaveGroup(id: number): Promise<void> {
    await api.post(`/api/groups/${id}/leave`, {});
  }

  async getGroupMembers(
    groupId: number,
    page: number = 0,
    size: number = 20
  ): Promise<PaginatedResponse<GroupMemberPayload>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sort', 'joinedAt,desc');
    const res = await api.get(`/api/groups/${groupId}/members?${params.toString()}`);
    return {
      content: (res.data?.content || []).map((member: any) => ({
        id: member.id,
        userId: member.userId,
        name: member.name,
        email: member.email,
        role: member.role,
        joinedAt: member.joinedAt,
      })),
      totalElements: res.data?.totalElements ?? 0,
      page: res.data?.number ?? page,
      size: res.data?.size ?? size,
    };
  }

  async getGroupDiscussions(
    groupId: number,
    page: number = 0,
    size: number = 20
  ): Promise<PaginatedResponse<DiscussionPayload>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    params.append('sort', 'createdAt,desc');
    const res = await api.get(`/api/groups/${groupId}/messages?${params.toString()}`);
    return {
      content: (res.data?.content || []).map((message: DiscussionPayload) => ({
        ...message,
        isAdmin: Boolean(message.isAdmin),
      })),
      totalElements: res.data?.totalElements ?? 0,
      page: res.data?.number ?? page,
      size: res.data?.size ?? size,
    };
  }

  async postDiscussion(
    groupId: number,
    title: string,
    content: string
  ): Promise<DiscussionPayload> {
    const res = await api.post(`/api/groups/${groupId}/message`, {
      title,
      content,
    });
    return res.data;
  }

  async deleteDiscussion(_groupId: number, discussionId: number): Promise<void> {
    await api.delete(`/api/groups/message/${discussionId}`);
  }

  async addGroupMember(groupId: number, email: string): Promise<GroupMemberPayload> {
    const res = await api.post(`/api/groups/${groupId}/members`, { email });
    return res.data;
  }

  async removeGroupMember(groupId: number, memberUserId: number): Promise<void> {
    await api.delete(`/api/groups/${groupId}/members/${memberUserId}`);
  }
}

export default new GroupService();

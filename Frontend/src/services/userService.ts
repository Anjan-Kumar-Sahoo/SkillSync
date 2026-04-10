import api from './axios';

export interface UserProfile {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  avatarUrl?: string;
  bio?: string;
  phone?: string;
  location?: string;
  profileCompletePct?: number;
  skills?: Array<{ id: number; name: string; categoryName?: string }>;
  createdAt: string;
}

export interface UpdateProfilePayload {
  firstName?: string;
  lastName?: string;
  bio?: string;
  phone?: string;
  location?: string;
  avatarUrl?: string;
}

class UserService {
  async getMyProfile(): Promise<UserProfile> {
    const res = await api.get('/api/users/me');
    return res.data;
  }

  async getUserById(id: number): Promise<UserProfile> {
    const res = await api.get(`/api/users/${id}`);
    return res.data;
  }

  async updateProfile(payload: UpdateProfilePayload): Promise<UserProfile> {
    const res = await api.put('/api/users/me', payload);
    return res.data;
  }

  async deactivateAccount(): Promise<void> {
    await api.post('/api/users/me/deactivate', {});
  }
}

export default new UserService();

import api from './axios';

export interface UserProfile {
  id: number;
  name: string;
  email: string;
  profileImage?: string;
  bio?: string;
  phoneNumber?: string;
  location?: string;
  role: 'ROLE_LEARNER' | 'ROLE_MENTOR' | 'ROLE_ADMIN';
  isEmailVerified: boolean;
  isMentor: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfilePayload {
  name?: string;
  bio?: string;
  phoneNumber?: string;
  location?: string;
  profileImage?: string;
}

export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface UserPreferences {
  theme: 'light' | 'dark';
  emailNotifications: boolean;
  pushNotifications: boolean;
  sessionReminders: boolean;
  newsletter: boolean;
  [key: string]: any;
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

  async uploadProfileImage(file: File): Promise<{ imageUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post('/api/users/me/upload-image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  }

  async changePassword(payload: ChangePasswordPayload): Promise<void> {
    await api.put('/api/users/me/change-password', payload);
  }

  async getPreferences(): Promise<UserPreferences> {
    const res = await api.get('/api/users/me/preferences');
    return res.data;
  }

  async updatePreferences(preferences: Partial<UserPreferences>): Promise<UserPreferences> {
    const res = await api.put('/api/users/me/preferences', preferences);
    return res.data;
  }

  async deactivateAccount(): Promise<void> {
    await api.post('/api/users/me/deactivate', {});
  }
}

export default new UserService();

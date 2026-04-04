import api from './axios';
import type { MentorData } from '../store/slices/mentorsSlice';

export interface MentorUpdatePayload {
  bio?: string;
  experience?: number;
  hourlyRate?: number;
  skills?: number[];
  availability?: string[];
}

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

class MentorService {
  async getMentors(
    filters?: {
      skill?: string;
      rating?: number;
      minPrice?: number;
      maxPrice?: number;
      search?: string;
    },
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedResponse<MentorData>> {
    const params = new URLSearchParams();
    if (filters?.skill) params.append('skill', filters.skill);
    if (filters?.rating) params.append('rating', filters.rating.toString());
    if (filters?.minPrice) params.append('minPrice', filters.minPrice.toString());
    if (filters?.maxPrice) params.append('maxPrice', filters.maxPrice.toString());
    if (filters?.search) params.append('search', filters.search);
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/mentors?${params.toString()}`);
    return res.data;
  }

  async getMentorById(id: number): Promise<MentorData> {
    const res = await api.get(`/api/mentors/${id}`);
    return res.data;
  }

  async getMyMentorProfile(): Promise<MentorData> {
    const res = await api.get('/api/mentors/me');
    return res.data;
  }

  async updateMentorProfile(
    id: number,
    payload: MentorUpdatePayload
  ): Promise<MentorData> {
    const res = await api.put(`/api/mentors/${id}`, payload);
    return res.data;
  }

  async applyAsMentor(payload: MentorUpdatePayload): Promise<MentorData> {
    const res = await api.post('/api/mentors', payload);
    return res.data;
  }

  async getMentorAvailability(mentorId: number): Promise<string[]> {
    const res = await api.get(`/api/mentors/${mentorId}/availability`);
    return res.data;
  }

  async updateMentorAvailability(
    mentorId: number,
    availability: string[]
  ): Promise<void> {
    await api.put(`/api/mentors/${mentorId}/availability`, { availability });
  }

  async getTopMentors(limit: number = 5): Promise<MentorData[]> {
    const res = await api.get(`/api/mentors?sort=rating,desc&size=${limit}`);
    return res.data.content || [];
  }
}

export default new MentorService();

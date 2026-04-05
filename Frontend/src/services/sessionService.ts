import api from './axios';
import type { SessionData } from '../store/slices/sessionsSlice';

export interface CreateSessionPayload {
  mentorId: number;
  topic: string;
  description?: string;
  sessionDate: string;
  durationMinutes: number;
}

export interface UpdateSessionPayload {
  status: 'ACCEPTED' | 'REJECTED' | 'CANCELLED';
}

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

class SessionService {
  async getSessions(
    status?: string,
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedResponse<SessionData>> {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/sessions/learner?${params.toString()}`);
    return res.data;
  }

  async getMyMentorSessions(
    status?: string,
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedResponse<SessionData>> {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/sessions/mentor?${params.toString()}`);
    return res.data;
  }

  async getSessionById(id: number): Promise<SessionData> {
    const res = await api.get(`/api/sessions/${id}`);
    return res.data;
  }

  async createSession(payload: CreateSessionPayload): Promise<SessionData> {
    const res = await api.post('/api/sessions', payload);
    return res.data;
  }

  async updateSession(
    id: number,
    payload: UpdateSessionPayload
  ): Promise<SessionData> {
    const action =
      payload.status === 'CANCELLED'
        ? 'cancel'
        : payload.status === 'ACCEPTED'
          ? 'accept'
          : 'reject';
    const res = await api.put(`/api/sessions/${id}/${action}`);
    return res.data;
  }

  async cancelSession(id: number): Promise<void> {
    await this.updateSession(id, { status: 'CANCELLED' });
  }

  async acceptSession(id: number): Promise<SessionData> {
    return this.updateSession(id, { status: 'ACCEPTED' });
  }

  async rejectSession(id: number): Promise<SessionData> {
    return this.updateSession(id, { status: 'REJECTED' });
  }
}

export default new SessionService();

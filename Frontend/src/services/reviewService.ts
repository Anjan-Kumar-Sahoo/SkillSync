import api from './axios';
import type { ReviewData } from '../store/slices/reviewsSlice';

export interface CreateReviewPayload {
  mentorId: number;
  rating: number;
  comment: string;
}

export interface UpdateReviewPayload {
  rating?: number;
  comment?: string;
  isAnonymous?: boolean;
}

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

class ReviewService {
  async getReviews(
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedResponse<ReviewData>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/reviews?${params.toString()}`);
    return res.data;
  }

  async getMentorReviews(
    mentorId: number,
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedResponse<ReviewData>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/reviews/mentor/${mentorId}?${params.toString()}`);
    return res.data;
  }

  async getMyReviews(
    page: number = 0,
    size: number = 10
  ): Promise<PaginatedResponse<ReviewData>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/reviews/me?${params.toString()}`);
    return res.data;
  }

  async getReviewById(id: number): Promise<ReviewData> {
    const res = await api.get(`/api/reviews/${id}`);
    return res.data;
  }

  async createReview(payload: CreateReviewPayload): Promise<ReviewData> {
    const res = await api.post('/api/reviews', payload);
    return res.data;
  }

  async updateReview(
    id: number,
    payload: UpdateReviewPayload
  ): Promise<ReviewData> {
    const res = await api.put(`/api/reviews/${id}`, payload);
    return res.data;
  }

  async deleteReview(id: number): Promise<void> {
    await api.delete(`/api/reviews/${id}`);
  }

  async getMentorAverageRating(mentorId: number): Promise<{
    averageRating: number;
    totalReviews: number;
  }> {
    const res = await api.get(`/api/reviews/mentor/${mentorId}/summary`);
    return res.data;
  }
}

export default new ReviewService();

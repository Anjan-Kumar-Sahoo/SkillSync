import api from './axios';
import type { NotificationData } from '../store/slices/notificationsSlice';

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

class NotificationService {
  async getNotifications(
    page: number = 0,
    size: number = 20
  ): Promise<PaginatedResponse<NotificationData>> {
    const params = new URLSearchParams();
    params.append('page', page.toString());
    params.append('size', size.toString());
    const res = await api.get(`/api/notifications?${params.toString()}`);
    return res.data;
  }

  async getUnreadNotifications(): Promise<NotificationData[]> {
    const res = await api.get('/api/notifications/unread');
    return res.data;
  }

  async getUnreadCount(): Promise<number> {
    const res = await api.get('/api/notifications/unread/count');
    return res.data.count || 0;
  }

  async markAsRead(id: number): Promise<NotificationData> {
    const res = await api.put(`/api/notifications/${id}/read`, {});
    return res.data;
  }

  async markAllAsRead(): Promise<void> {
    await api.put('/api/notifications/read-all', {});
  }

  async deleteNotification(id: number): Promise<void> {
    await api.delete(`/api/notifications/${id}`);
  }

  async clearAllNotifications(): Promise<void> {
    await api.delete('/api/notifications');
  }

  async subscribeToNotifications(): Promise<void> {
    // WebSocket subscription handled by setupNotificationListener
    // This is a placeholder for future WebSocket handling
  }
}

export default new NotificationService();

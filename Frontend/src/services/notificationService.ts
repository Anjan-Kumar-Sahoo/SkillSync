import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import api from './axios';
import { API_BASE_URL } from './axios';
import type { NotificationData } from '../store/slices/notificationsSlice';

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

class NotificationService {
  private stompClient: Client | null = null;
  private subscription: StompSubscription | null = null;
  private listeners = new Set<(notification: NotificationData) => void>();

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
    const res = await this.getNotifications();
    return res.content.filter((notification) => !notification.isRead);
  }

  async getUnreadCount(): Promise<number> {
    const res = await api.get('/api/notifications/unread/count');
    return res.data.count || 0;
  }

  async markAsRead(id: number): Promise<void> {
    try {
      await api.post(`/api/notifications/read/${id}`);
    } catch {
      await api.put(`/api/notifications/${id}/read`, {});
    }
  }

  async markAllAsRead(): Promise<void> {
    await api.put('/api/notifications/read-all', {});
  }

  async deleteNotification(id: number): Promise<void> {
    await api.delete(`/api/notifications/${id}`);
  }

  async clearAllNotifications(): Promise<void> {
    try {
      await api.delete('/api/notifications/all');
    } catch {
      // Backward-compatible fallback for older backend versions.
      const allNotifications = await this.getNotifications(0, 200);
      await Promise.all(allNotifications.content.map((notification) => this.deleteNotification(notification.id)));
    }
  }

  subscribeToNotifications(listener: (notification: NotificationData) => void): () => void {
    this.listeners.add(listener);
    this.ensureWebSocketConnection();

    return () => {
      this.listeners.delete(listener);
      if (this.listeners.size === 0) {
        this.disconnectWebSocket();
      }
    };
  }

  private ensureWebSocketConnection(): void {
    if (this.stompClient?.active) {
      return;
    }

    const socketUrl = this.getWebSocketUrl();
    const client = new Client({
      brokerURL: socketUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.onConnect = () => {
      this.subscription?.unsubscribe();
      this.subscription = client.subscribe('/user/queue/notifications', (message: IMessage) => {
        this.handleIncomingNotification(message);
      });
    };

    client.onStompError = (frame) => {
      console.warn('Notification WebSocket STOMP error', frame.headers['message']);
    };

    client.onWebSocketError = () => {
      console.warn('Notification WebSocket connection error');
    };

    this.stompClient = client;
    client.activate();
  }

  private disconnectWebSocket(): void {
    this.subscription?.unsubscribe();
    this.subscription = null;

    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }

  private handleIncomingNotification(message: IMessage): void {
    try {
      const payload = JSON.parse(message.body) as NotificationData;
      this.listeners.forEach((listener) => listener(payload));
    } catch (error) {
      console.warn('Failed to parse incoming notification payload', error);
    }
  }

  private getWebSocketUrl(): string {
    if (API_BASE_URL.startsWith('https://')) {
      return `${API_BASE_URL.replace('https://', 'wss://')}/ws/notifications`;
    }
    if (API_BASE_URL.startsWith('http://')) {
      return `${API_BASE_URL.replace('http://', 'ws://')}/ws/notifications`;
    }
    return `${API_BASE_URL}/ws/notifications`;
  }
}

export default new NotificationService();

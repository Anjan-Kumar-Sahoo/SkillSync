import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSelector } from 'react-redux';
import notificationService from '../../services/notificationService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import type { RootState } from '../../store';

const NotificationsPage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const userId = useSelector((state: RootState) => state.auth.user?.id);

  // Fetch notifications
  const { data: notificationsData, isLoading } = useQuery({
    queryKey: ['notifications', userId || 'unknown'],
    queryFn: () => notificationService.getNotifications(0, 50),
    enabled: !!userId,
    refetchInterval: 30000,
  });

  // Mark as read mutation
  const markAsReadMutation = useMutation({
    mutationFn: (id: number) => notificationService.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // Mark all as read mutation
  const markAllAsReadMutation = useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: () => {
      showToast({ message: 'All notifications marked as read', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // Delete notification mutation
  const deleteNotificationMutation = useMutation({
    mutationFn: (id: number) => notificationService.deleteNotification(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const notifications = notificationsData?.content || [];
  const unreadCount = notifications.filter(n => !n.isRead).length;

  const getNotificationIcon = (type: string) => {
    const icons: Record<string, string> = {
      SESSION_REQUEST: '📅',
      SESSION_ACCEPTED: '✅',
      SESSION_REJECTED: '❌',
      MENTOR_APPROVED: '⭐',
      REVIEW_RECEIVED: '⭐',
      SYSTEM: 'ℹ️',
      GROUP_INVITE: '👥',
    };
    return icons[type] || 'ℹ️';
  };

  const getNotificationColor = (type: string) => {
    const colors: Record<string, string> = {
      SESSION_REQUEST: 'bg-blue-50 border-blue-200',
      SESSION_ACCEPTED: 'bg-green-50 border-green-200',
      SESSION_REJECTED: 'bg-red-50 border-red-200',
      MENTOR_APPROVED: 'bg-yellow-50 border-yellow-200',
      REVIEW_RECEIVED: 'bg-purple-50 border-purple-200',
      SYSTEM: 'bg-gray-50 border-gray-200',
      GROUP_INVITE: 'bg-teal-50 border-teal-200',
    };
    return colors[type] || 'bg-gray-50 border-gray-200';
  };

  return (
    <PageLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="bg-gradient-to-r from-purple-600 to-pink-600 rounded-lg p-8 text-white">
          <h1 className="text-3xl font-bold mb-2">Notifications</h1>
          <p className="text-purple-100">Stay updated with your mentoring activities</p>
        </div>

        {/* Controls */}
        <div className="flex justify-between items-center">
          <div>
            <p className="text-gray-600">
              {unreadCount > 0 ? (
                <>
                  You have <span className="font-bold text-purple-600">{unreadCount}</span> unread
                  notification{unreadCount > 1 ? 's' : ''}
                </>
              ) : (
                'All notifications read'
              )}
            </p>
          </div>
          {unreadCount > 0 && (
            <button
              onClick={() => markAllAsReadMutation.mutate()}
              disabled={markAllAsReadMutation.isPending}
              className="text-blue-600 hover:text-blue-700 font-medium text-sm disabled:opacity-50"
            >
              Mark all as read
            </button>
          )}
        </div>

        {/* Notifications List */}
        {isLoading ? (
          <p className="text-center text-gray-500 py-8">Loading notifications...</p>
        ) : notifications.length > 0 ? (
          <div className="space-y-3">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                className={`rounded-lg p-4 border-2 transition ${
                  notification.isRead ? 'opacity-75' : 'opacity-100'
                } ${getNotificationColor(notification.type)}`}
              >
                <div className="flex items-start gap-4">
                  <span className="text-2xl mt-1">{getNotificationIcon(notification.type)}</span>
                  <div className="flex-1">
                    <h3 className="font-semibold text-gray-900">{notification.title}</h3>
                    <p className="text-gray-700 text-sm mt-1">{notification.message}</p>
                    <p className="text-xs text-gray-500 mt-2">
                      {new Date(notification.createdAt).toLocaleDateString()} at{' '}
                      {new Date(notification.createdAt).toLocaleTimeString([], {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    {!notification.isRead && (
                      <button
                        onClick={() => markAsReadMutation.mutate(notification.id)}
                        disabled={markAsReadMutation.isPending}
                        className="text-blue-600 hover:text-blue-700 text-xs font-medium disabled:opacity-50"
                      >
                        Mark read
                      </button>
                    )}
                    <button
                      onClick={() => deleteNotificationMutation.mutate(notification.id)}
                      disabled={deleteNotificationMutation.isPending}
                      className="text-red-600 hover:text-red-700 text-xs font-medium disabled:opacity-50"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12 bg-white rounded-lg border border-gray-200">
            <p className="text-lg text-gray-500 mb-4">No notifications yet</p>
            <p className="text-sm text-gray-400">You'll see updates here when you get session requests, approvals, and more</p>
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default NotificationsPage;

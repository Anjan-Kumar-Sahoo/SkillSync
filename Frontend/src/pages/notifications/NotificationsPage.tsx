import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSelector } from 'react-redux';
import notificationService from '../../services/notificationService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import { useActionConfirm } from '../../components/ui/ActionConfirm';
import type { RootState } from '../../store';
import { formatDateTimeIST } from '../../utils/dateTime';

const NotificationsPage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const { requestConfirmation } = useActionConfirm();
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
      queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
    },
  });

  // Mark all as read mutation
  const markAllAsReadMutation = useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: () => {
      showToast({ message: 'All notifications marked as read', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
    },
  });

  // Delete notification mutation
  const deleteNotificationMutation = useMutation({
    mutationFn: (id: number) => notificationService.deleteNotification(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
    },
  });

  const clearAllNotificationsMutation = useMutation({
    mutationFn: () => notificationService.clearAllNotifications(),
    onSuccess: () => {
      showToast({ message: 'All notifications deleted', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
    },
  });

  const notifications = notificationsData?.content || [];
  const unreadCount = notifications.filter(n => !n.isRead).length;

  const handleDeleteNotification = async (notificationId: number) => {
    const confirmed = await requestConfirmation({
      title: 'Delete Notification?',
      message: 'Are you sure you want to delete this notification?',
      confirmLabel: 'Yes, delete notification',
    });

    if (!confirmed) {
      return;
    }

    deleteNotificationMutation.mutate(notificationId);
  };

  const handleDeleteAllNotifications = async () => {
    const confirmed = await requestConfirmation({
      title: 'Delete All Notifications?',
      message: 'Are you sure you want to delete all notifications? This cannot be undone.',
      confirmLabel: 'Yes, delete all',
    });

    if (!confirmed) {
      return;
    }

    clearAllNotificationsMutation.mutate();
  };

  const getNotificationIcon = (type: string) => {
    const icons: Record<string, string> = {
      SESSION_REQUEST: '📅',
      SESSION_REQUESTED: '📅',
      SESSION_REQUESTED_CONFIRMATION: '📨',
      SESSION_ACCEPTED: '✅',
      SESSION_APPROVED: '✅',
      SESSION_REJECTED: '❌',
      SESSION_CANCELLED: '🚫',
      SESSION_COMPLETED: '🏁',
      MENTOR_APPROVED: '⭐',
      REVIEW_RECEIVED: '⭐',
      SYSTEM: 'ℹ️',
      GROUP_INVITE: '👥',
    };
    return icons[type] || 'ℹ️';
  };

  const getNotificationColor = (type: string) => {
    const colors: Record<string, string> = {
      SESSION_REQUEST: 'border-l-blue-500',
      SESSION_REQUESTED: 'border-l-blue-500',
      SESSION_REQUESTED_CONFIRMATION: 'border-l-indigo-500',
      SESSION_ACCEPTED: 'border-l-green-500',
      SESSION_APPROVED: 'border-l-green-500',
      SESSION_REJECTED: 'border-l-red-500',
      SESSION_CANCELLED: 'border-l-red-500',
      SESSION_COMPLETED: 'border-l-emerald-500',
      MENTOR_APPROVED: 'border-l-amber-500',
      REVIEW_RECEIVED: 'border-l-fuchsia-500',
      SYSTEM: 'border-l-slate-500',
      GROUP_INVITE: 'border-l-teal-500',
    };
    return colors[type] || 'border-l-slate-500';
  };

  return (
    <PageLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="bg-surface-container-lowest rounded-lg p-8 border border-outline-variant/20 shadow-sm">
          <h1 className="text-3xl font-bold mb-2 text-on-surface">Notifications</h1>
          <p className="text-on-surface-variant">Stay updated with your mentoring activities</p>
        </div>

        {/* Controls */}
        <div className="flex flex-wrap justify-between items-center gap-3">
          <div>
            <p className="text-on-surface-variant">
              {unreadCount > 0 ? (
                <>
                  You have <span className="font-bold text-primary">{unreadCount}</span> unread
                  notification{unreadCount > 1 ? 's' : ''}
                </>
              ) : (
                'All notifications read'
              )}
            </p>
          </div>
          <div className="flex items-center gap-3">
            {unreadCount > 0 && (
              <button
                onClick={() => markAllAsReadMutation.mutate()}
                disabled={markAllAsReadMutation.isPending}
                className="text-primary hover:opacity-80 font-medium text-sm disabled:opacity-50"
              >
                Mark all as read
              </button>
            )}

            {notifications.length > 0 && (
              <button
                onClick={() => void handleDeleteAllNotifications()}
                disabled={clearAllNotificationsMutation.isPending}
                className="text-error hover:opacity-80 font-medium text-sm disabled:opacity-50"
              >
                Delete all
              </button>
            )}
          </div>
        </div>

        {/* Notifications List */}
        {isLoading ? (
          <p className="text-center text-on-surface-variant py-8">Loading notifications...</p>
        ) : notifications.length > 0 ? (
          <div className="space-y-3">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                className={`rounded-lg p-4 border-2 transition ${
                  notification.isRead ? 'opacity-80 bg-surface-container-low border-outline-variant/20' : 'opacity-100 bg-surface-container-lowest border-outline-variant/30 ring-1 ring-primary/15'
                } border-l-4 ${getNotificationColor(notification.type)}`}
              >
                <div className="flex items-start gap-4">
                  <span className="text-2xl mt-1">{getNotificationIcon(notification.type)}</span>
                  <div className="flex-1">
                    <h3 className="font-semibold text-on-surface">{notification.title}</h3>
                    <p className="text-on-surface-variant text-sm mt-1">{notification.message}</p>
                    <p className="text-xs text-on-surface-variant mt-2">{formatDateTimeIST(notification.createdAt)}</p>
                  </div>
                  <div className="flex gap-2">
                    {!notification.isRead && (
                      <button
                        onClick={() => markAsReadMutation.mutate(notification.id)}
                        disabled={markAsReadMutation.isPending}
                        className="text-primary hover:opacity-80 text-xs font-medium disabled:opacity-50"
                      >
                        Mark read
                      </button>
                    )}
                    <button
                      onClick={() => void handleDeleteNotification(notification.id)}
                      disabled={deleteNotificationMutation.isPending}
                      className="text-error hover:text-error text-xs font-medium disabled:opacity-50"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12 bg-surface-container-lowest rounded-lg border border-outline-variant/20">
            <p className="text-lg text-on-surface-variant mb-4">No notifications yet</p>
            <p className="text-sm text-on-surface-variant">You'll see updates here when you get session requests, approvals, and more</p>
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default NotificationsPage;

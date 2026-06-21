import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../../services/axios';
import notificationService from '../../services/notificationService';
import type { RootState } from '../../store';
import ThemeToggleButton from '../ui/ThemeToggleButton';

const Navbar = () => {
  const user = useSelector((state: RootState) => state.auth.user);
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!user?.id) {
      return;
    }

    const unsubscribe = notificationService.subscribeToNotifications(() => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
    });

    return unsubscribe;
  }, [queryClient, user?.id]);

  const { data: notificationData } = useQuery({
    queryKey: ['unread-notifications', user?.id || 'unknown'],
    queryFn: async () => {
      try {
        const response = await api.get('/api/notifications/unread/count', { _skipErrorRedirect: true } as any);
        return response.data;
      } catch {
        return { count: 0 };
      }
    },
    enabled: !!user?.id,
    refetchInterval: 30000,
  });

  const unreadCount = notificationData?.count || 0;
  
  const initial1 = user?.firstName?.[0]?.toUpperCase() || 'U';
  const initial2 = user?.lastName?.[0]?.toUpperCase() || '';
  const initials = `${initial1}${initial2}`;
  
  const colors = ['bg-blue-500', 'bg-emerald-500', 'bg-violet-500', 'bg-amber-500', 'bg-rose-500'];
  const colorIndex = (initial1.charCodeAt(0) % colors.length);
  const avatarClass = colors[colorIndex] || 'bg-primary';

  return (
    <header className="h-16 w-full glass-panel-lowest flex items-center justify-between px-4 lg:px-8 z-30 sticky top-0 transition-all shadow-sm">
      <div className="flex-1 flex items-center">
      </div>

      <div className="flex items-center space-x-4">
        <ThemeToggleButton className="px-2.5 py-1.5 hover:bg-surface-container rounded-lg" showLabel={false} />

        <Link to="/notifications" className="relative p-2 rounded-full flex hover:bg-surface-container text-on-surface-variant hover:text-primary transition-all duration-200 hover:scale-105 active:scale-95">
          <span className="material-symbols-outlined text-[26px]">notifications</span>
          {unreadCount > 0 && (
            <span className="absolute top-1.5 right-1.5 min-w-[16px] h-[16px] bg-error text-white text-[9px] font-extrabold rounded-full flex items-center justify-center px-1 shadow-sm pulse-glow-badge">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </Link>

        <Link to="/profile" className="flex items-center pl-4 border-l border-outline-variant/30 hover:opacity-90 transition-all duration-250 group">
          <div className="hidden md:flex flex-col items-end mr-3">
            <span className="text-sm font-bold text-on-surface leading-tight group-hover:text-primary transition-colors">{user?.firstName} {user?.lastName}</span>
            <span className="text-[10px] text-on-surface-variant font-bold uppercase tracking-wider">Hello there</span>
          </div>
          <div className={`w-9 h-9 rounded-full ${avatarClass} text-white flex items-center justify-center font-bold shadow-md shrink-0 ring-2 ring-primary/10 group-hover:ring-primary/45 transition-all`}>
            {initials}
          </div>
        </Link>
      </div>
    </header>
  );
};

export default Navbar;

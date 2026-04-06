import { useQuery } from '@tanstack/react-query';
import api from '../services/axios';

export const useAdminStats = () => {
  return useQuery({
    queryKey: ['admin', 'stats'],
    queryFn: async () => {
      const { data } = await api.get('/api/admin/stats');
      return data;
    },
  });
};

export const useUsers = () => {
  return useQuery({
    queryKey: ['admin', 'users'],
    queryFn: async () => {
      const { data } = await api.get('/api/admin/users');
      return data;
    },
  });
};

export const usePendingMentors = () => {
  return useQuery({
    queryKey: ['admin', 'mentors', 'pending'],
    queryFn: async () => {
      const { data } = await api.get('/api/admin/mentors/pending');
      return data;
    },
  });
};

export const useMentors = () => {
  return useQuery({
    queryKey: ['mentors', 'ROLE_MENTOR'],
    queryFn: async () => {
      const { data } = await api.get('/api/mentors/search');
      return data;
    },
  });
};

export const useAvailability = () => {
  return useQuery({
    queryKey: ['mentor', 'availability'],
    queryFn: async () => {
      const { data } = await api.get('/api/mentors/me/availability');
      return data;
    },
  });
};

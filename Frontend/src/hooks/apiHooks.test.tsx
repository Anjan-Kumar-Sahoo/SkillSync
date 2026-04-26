import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import api from '../services/axios';
import { useAdminStats, useAvailability, useMentors, usePendingMentors, useUsers } from './apiHooks';

jest.mock('../services/axios', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
  },
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('apiHooks', () => {
  const mockedApi = api as jest.Mocked<typeof api>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches admin stats', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { users: 5 } } as any);

    const { result } = renderHook(() => useAdminStats(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(mockedApi.get).toHaveBeenCalledWith('/api/admin/stats');
    expect(result.current.data).toEqual({ users: 5 });
  });

  it('fetches users and pending mentors', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: [{ id: 1 }] } as any);
    mockedApi.get.mockResolvedValueOnce({ data: [{ id: 2 }] } as any);

    const usersHook = renderHook(() => useUsers(), { wrapper: createWrapper() });
    const mentorsHook = renderHook(() => usePendingMentors(), { wrapper: createWrapper() });

    await waitFor(() => expect(usersHook.result.current.isSuccess).toBe(true));
    await waitFor(() => expect(mentorsHook.result.current.isSuccess).toBe(true));

    expect(mockedApi.get).toHaveBeenNthCalledWith(1, '/api/admin/users');
    expect(mockedApi.get).toHaveBeenNthCalledWith(2, '/api/admin/mentors/pending');
  });

  it('fetches mentors and availability', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { content: [] } } as any);
    mockedApi.get.mockResolvedValueOnce({ data: [] } as any);

    const mentorsHook = renderHook(() => useMentors(), { wrapper: createWrapper() });
    const availabilityHook = renderHook(() => useAvailability(), { wrapper: createWrapper() });

    await waitFor(() => expect(mentorsHook.result.current.isSuccess).toBe(true));
    await waitFor(() => expect(availabilityHook.result.current.isSuccess).toBe(true));

    expect(mockedApi.get).toHaveBeenNthCalledWith(1, '/api/mentors/search');
    expect(mockedApi.get).toHaveBeenNthCalledWith(2, '/api/mentors/me/availability');
  });

  it('surfaces errors when endpoint fails', async () => {
    mockedApi.get.mockRejectedValueOnce(new Error('failed'));

    const { result } = renderHook(() => useAdminStats(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

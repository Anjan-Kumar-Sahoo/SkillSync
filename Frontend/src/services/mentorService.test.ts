import mentorService from './mentorService';
import api from './axios';

jest.mock('./axios', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    put: jest.fn(),
    post: jest.fn(),
    delete: jest.fn(),
  },
}));

describe('mentorService', () => {
  const mockedApi = api as jest.Mocked<typeof api>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('gets mentors with optional filters and pagination', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { content: [] } } as any);

    await mentorService.getMentors(
      { skill: 'React', rating: 4, minPrice: 100, maxPrice: 500, search: 'john' },
      1,
      20,
    );

    expect(mockedApi.get).toHaveBeenCalledWith(
      '/api/mentors/search?skill=React&rating=4&minPrice=100&maxPrice=500&search=john&page=1&size=20',
    );
  });

  it('uses default paging and skips empty optional filters', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { content: [] } } as any);
    mockedApi.get.mockResolvedValueOnce({ data: { content: [] } } as any);

    await mentorService.getMentors();
    await mentorService.getMentors({ search: 'mentor-name' });

    expect(mockedApi.get).toHaveBeenNthCalledWith(1, '/api/mentors/search?page=0&size=10');
    expect(mockedApi.get).toHaveBeenNthCalledWith(2, '/api/mentors/search?search=mentor-name&page=0&size=10');
  });

  it('gets mentor profile variants', async () => {
    mockedApi.get.mockResolvedValue({ data: { id: 3 } } as any);

    await mentorService.getMentorById(3);
    await mentorService.getMyMentorProfile();

    expect(mockedApi.get).toHaveBeenNthCalledWith(1, '/api/mentors/3');
    expect(mockedApi.get).toHaveBeenNthCalledWith(2, '/api/mentors/me');
  });

  it('updates mentor profile and applies as mentor', async () => {
    mockedApi.put.mockResolvedValueOnce({ data: { id: 3, bio: 'Updated' } } as any);
    mockedApi.post.mockResolvedValueOnce({ data: { id: 3 } } as any);

    await mentorService.updateMentorProfile(3, { bio: 'Updated' });
    await mentorService.applyAsMentor({ bio: 'B', experienceYears: 2, hourlyRate: 1000, skillIds: [1, 2] });

    expect(mockedApi.put).toHaveBeenCalledWith('/api/mentors/3', { bio: 'Updated' });
    expect(mockedApi.post).toHaveBeenCalledWith('/api/mentors/apply', {
      bio: 'B',
      experienceYears: 2,
      hourlyRate: 1000,
      skillIds: [1, 2],
    });
  });

  it('manages mentor availability endpoints', async () => {
    mockedApi.get.mockResolvedValue({ data: [] } as any);
    mockedApi.post.mockResolvedValue({ data: { id: 1 } } as any);
    mockedApi.delete.mockResolvedValue({ data: {} } as any);

    await mentorService.getMyAvailability();
    await mentorService.addMyAvailability({ dayOfWeek: 2, startTime: '10:00', endTime: '11:00' });
    await mentorService.removeMyAvailability(55);
    await mentorService.getMentorAvailability(6);

    expect(mockedApi.get).toHaveBeenNthCalledWith(1, '/api/mentors/me/availability');
    expect(mockedApi.post).toHaveBeenCalledWith('/api/mentors/me/availability', {
      dayOfWeek: 2,
      startTime: '10:00',
      endTime: '11:00',
    });
    expect(mockedApi.delete).toHaveBeenCalledWith('/api/mentors/me/availability/55');
    expect(mockedApi.get).toHaveBeenNthCalledWith(2, '/api/mentors/6/availability');
  });

  it('returns top mentors from search content or empty fallback', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { content: [{ id: 10 }] } } as any);
    mockedApi.get.mockResolvedValueOnce({ data: null } as any);

    const top = await mentorService.getTopMentors(4);
    const fallback = await mentorService.getTopMentors();

    expect(mockedApi.get).toHaveBeenNthCalledWith(1, '/api/mentors/search?sort=avgRating,desc&size=4');
    expect(top).toEqual([{ id: 10 }]);
    expect(fallback).toEqual([]);
  });
});

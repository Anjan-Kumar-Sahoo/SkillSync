import sessionService from './sessionService';
import api from './axios';

jest.mock('./axios', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
  },
}));

describe('sessionService', () => {
  const mockedApi = api as jest.Mocked<typeof api>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('gets learner sessions with status filter and pagination', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { content: [] } } as any);

    await sessionService.getSessions('REQUESTED', 2, 15);

    expect(mockedApi.get).toHaveBeenCalledWith('/api/sessions/learner?status=REQUESTED&page=2&size=15');
  });

  it('uses default learner paging and omits status when not provided', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { content: [] } } as any);

    await sessionService.getSessions();

    expect(mockedApi.get).toHaveBeenCalledWith('/api/sessions/learner?page=0&size=10');
  });

  it('gets mentor sessions with pagination', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { content: [] } } as any);

    await sessionService.getMyMentorSessions(undefined, 1, 5);

    expect(mockedApi.get).toHaveBeenCalledWith('/api/sessions/mentor?page=1&size=5');
  });

  it('appends mentor status when provided and uses default page size', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { content: [] } } as any);

    await sessionService.getMyMentorSessions('ACCEPTED');

    expect(mockedApi.get).toHaveBeenCalledWith('/api/sessions/mentor?status=ACCEPTED&page=0&size=10');
  });

  it('gets session by id', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { id: 11 } } as any);

    const result = await sessionService.getSessionById(11);

    expect(mockedApi.get).toHaveBeenCalledWith('/api/sessions/11');
    expect(result.id).toBe(11);
  });

  it('creates a session', async () => {
    mockedApi.post.mockResolvedValueOnce({ data: { id: 99 } } as any);

    const payload = {
      mentorId: 1,
      topic: 'React',
      description: 'Deep dive',
      sessionDate: '2026-05-01T10:00:00Z',
      durationMinutes: 60,
    };

    const result = await sessionService.createSession(payload);

    expect(mockedApi.post).toHaveBeenCalledWith('/api/sessions', payload);
    expect(result.id).toBe(99);
  });

  it('maps update action endpoint based on status', async () => {
    mockedApi.put.mockResolvedValue({ data: { id: 1 } } as any);

    await sessionService.updateSession(1, { status: 'CANCELLED' });
    await sessionService.updateSession(2, { status: 'ACCEPTED' });
    await sessionService.updateSession(3, { status: 'REJECTED' });

    expect(mockedApi.put).toHaveBeenNthCalledWith(1, '/api/sessions/1/cancel');
    expect(mockedApi.put).toHaveBeenNthCalledWith(2, '/api/sessions/2/accept');
    expect(mockedApi.put).toHaveBeenNthCalledWith(3, '/api/sessions/3/reject');
  });

  it('uses convenience wrappers for cancel, accept and reject', async () => {
    mockedApi.put.mockResolvedValue({ data: { id: 1 } } as any);

    await sessionService.cancelSession(10);
    await sessionService.acceptSession(11);
    await sessionService.rejectSession(12);

    expect(mockedApi.put).toHaveBeenCalledWith('/api/sessions/10/cancel');
    expect(mockedApi.put).toHaveBeenCalledWith('/api/sessions/11/accept');
    expect(mockedApi.put).toHaveBeenCalledWith('/api/sessions/12/reject');
  });
});

import groupService from './groupService';
import api from './axios';

jest.mock('./axios', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

describe('groupService', () => {
  const mockedApi = api as jest.Mocked<typeof api>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  const groupPayload = {
    id: 1,
    name: 'Frontend Guild',
    description: '',
    category: '',
    createdBy: 7,
    createdByName: '',
    memberCount: 0,
    joined: true,
    createdAt: '2026-04-16T10:00:00Z',
  };

  it('maps group data and applies query parameters', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { content: [groupPayload], totalElements: 1, number: 2, size: 5 } } as any);

    const result = await groupService.getGroups(' react ', 'Programming', 2, 5);

    expect(mockedApi.get).toHaveBeenCalledWith(
      '/api/groups?page=2&size=5&sort=createdAt%2Cdesc&search=react&category=Programming',
    );
    expect(result.content[0].description).toBe('No description provided.');
    expect(result.content[0].category).toBe('General');
    expect(result.content[0].createdByName).toBe('User #7');
    expect(result.content[0].isJoined).toBe(true);
  });

  it('uses default query params and fallback pagination values', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: {} } as any);

    const result = await groupService.getGroups('   ', 'All');

    expect(mockedApi.get).toHaveBeenCalledWith('/api/groups?page=0&size=10&sort=createdAt%2Cdesc');
    expect(result).toEqual({
      content: [],
      totalElements: 0,
      page: 0,
      size: 10,
    });
  });

  it('gets my groups and group by id', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { content: [groupPayload], totalElements: 1, number: 0, size: 10 } } as any);
    mockedApi.get.mockResolvedValueOnce({ data: groupPayload } as any);

    const my = await groupService.getMyGroups();
    const one = await groupService.getGroupById(1);

    expect(mockedApi.get).toHaveBeenNthCalledWith(1, '/api/groups/my?page=0&size=10&sort=createdAt%2Cdesc');
    expect(mockedApi.get).toHaveBeenNthCalledWith(2, '/api/groups/1');
    expect(my.totalElements).toBe(1);
    expect(one.id).toBe(1);
  });

  it('falls back for my-groups pagination values when backend omits metadata', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: {} } as any);

    const result = await groupService.getMyGroups();

    expect(mockedApi.get).toHaveBeenCalledWith('/api/groups/my?page=0&size=10&sort=createdAt%2Cdesc');
    expect(result).toEqual({
      content: [],
      totalElements: 0,
      page: 0,
      size: 10,
    });
  });

  it('creates, updates, deletes, joins and leaves groups', async () => {
    mockedApi.post.mockResolvedValueOnce({ data: groupPayload } as any);
    mockedApi.put.mockResolvedValueOnce({ data: { ...groupPayload, name: 'Updated' } } as any);
    mockedApi.delete.mockResolvedValueOnce({ data: {} } as any);
    mockedApi.post.mockResolvedValueOnce({ data: {} } as any);
    mockedApi.get.mockResolvedValueOnce({ data: groupPayload } as any);
    mockedApi.post.mockResolvedValueOnce({ data: {} } as any);

    const created = await groupService.createGroup({ name: 'Frontend Guild' });
    const updated = await groupService.updateGroup(1, { name: 'Updated' });
    await groupService.deleteGroup(1);
    const joined = await groupService.joinGroup(1);
    await groupService.leaveGroup(1);

    expect(created.id).toBe(1);
    expect(updated.name).toBe('Updated');
    expect(joined.id).toBe(1);
    expect(mockedApi.post).toHaveBeenCalledWith('/api/groups/1/join', {});
    expect(mockedApi.post).toHaveBeenCalledWith('/api/groups/1/leave', {});
  });

  it('handles members and discussions endpoints', async () => {
    mockedApi.get.mockResolvedValueOnce({
      data: { content: [{ id: 1, userId: 2, name: 'A', email: 'a@b.dev', role: 'MEMBER', joinedAt: '2026-04-16' }], totalElements: 1 },
    } as any);

    mockedApi.get.mockResolvedValueOnce({
      data: { content: [{ id: 4, isAdmin: 0, title: 'T', content: 'C' }], totalElements: 1 },
    } as any);

    mockedApi.post.mockResolvedValueOnce({ data: { id: 7 } } as any);
    mockedApi.delete.mockResolvedValue({ data: {} } as any);
    mockedApi.post.mockResolvedValueOnce({ data: { id: 9 } } as any);

    const members = await groupService.getGroupMembers(1, 0, 20);
    const discussions = await groupService.getGroupDiscussions(1, 0, 20);
    const posted = await groupService.postDiscussion(1, 'Hello', 'World');
    await groupService.deleteDiscussion(1, 4);
    await groupService.addGroupMember(1, 'member@skillsync.dev');
    await groupService.removeGroupMember(1, 2);

    expect(members.content[0].email).toBe('a@b.dev');
    expect(discussions.content[0].isAdmin).toBe(false);
    expect(posted.id).toBe(7);
    expect(mockedApi.delete).toHaveBeenCalledWith('/api/groups/message/4');
    expect(mockedApi.post).toHaveBeenCalledWith('/api/groups/1/members', { email: 'member@skillsync.dev' });
    expect(mockedApi.delete).toHaveBeenCalledWith('/api/groups/1/members/2');
  });

  it('uses endpoint defaults for members and discussions when server returns empty payload', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: {} } as any);
    mockedApi.get.mockResolvedValueOnce({ data: {} } as any);

    const members = await groupService.getGroupMembers(10);
    const discussions = await groupService.getGroupDiscussions(10);

    expect(mockedApi.get).toHaveBeenNthCalledWith(1, '/api/groups/10/members?page=0&size=20&sort=joinedAt%2Cdesc');
    expect(mockedApi.get).toHaveBeenNthCalledWith(2, '/api/groups/10/messages?page=0&size=20&sort=createdAt%2Cdesc');
    expect(members).toEqual({
      content: [],
      totalElements: 0,
      page: 0,
      size: 20,
    });
    expect(discussions).toEqual({
      content: [],
      totalElements: 0,
      page: 0,
      size: 20,
    });
  });
});

import userService from './userService';
import api from './axios';

jest.mock('./axios', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    put: jest.fn(),
    post: jest.fn(),
  },
}));

describe('userService', () => {
  const mockedApi = api as jest.Mocked<typeof api>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('gets current user profile', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { id: 1 } } as any);

    const result = await userService.getMyProfile();

    expect(mockedApi.get).toHaveBeenCalledWith('/api/users/me');
    expect(result).toEqual({ id: 1 });
  });

  it('gets user by id', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { id: 7 } } as any);

    const result = await userService.getUserById(7);

    expect(mockedApi.get).toHaveBeenCalledWith('/api/users/7');
    expect(result.id).toBe(7);
  });

  it('updates profile', async () => {
    mockedApi.put.mockResolvedValueOnce({ data: { firstName: 'Updated' } } as any);

    const result = await userService.updateProfile({ firstName: 'Updated' });

    expect(mockedApi.put).toHaveBeenCalledWith('/api/users/me', { firstName: 'Updated' });
    expect(result.firstName).toBe('Updated');
  });

  it('deactivates account', async () => {
    mockedApi.post.mockResolvedValueOnce({ data: {} } as any);

    await userService.deactivateAccount();

    expect(mockedApi.post).toHaveBeenCalledWith('/api/users/me/deactivate', {});
  });
});

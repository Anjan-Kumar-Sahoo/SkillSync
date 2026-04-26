import reviewService from './reviewService';
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

describe('reviewService', () => {
  const mockedApi = api as jest.Mocked<typeof api>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('gets paginated review lists from each endpoint', async () => {
    mockedApi.get.mockResolvedValue({ data: { content: [] } } as any);

    await reviewService.getReviews(1, 5);
    await reviewService.getMentorReviews(7, 0, 10);
    await reviewService.getMyReviews(2, 20);

    expect(mockedApi.get).toHaveBeenNthCalledWith(1, '/api/reviews?page=1&size=5');
    expect(mockedApi.get).toHaveBeenNthCalledWith(2, '/api/reviews/mentor/7?page=0&size=10');
    expect(mockedApi.get).toHaveBeenNthCalledWith(3, '/api/reviews/me?page=2&size=20');
  });

  it('uses default page and size values for list endpoints', async () => {
    mockedApi.get.mockResolvedValue({ data: { content: [] } } as any);

    await reviewService.getReviews();
    await reviewService.getMentorReviews(5);
    await reviewService.getMyReviews();

    expect(mockedApi.get).toHaveBeenNthCalledWith(1, '/api/reviews?page=0&size=10');
    expect(mockedApi.get).toHaveBeenNthCalledWith(2, '/api/reviews/mentor/5?page=0&size=10');
    expect(mockedApi.get).toHaveBeenNthCalledWith(3, '/api/reviews/me?page=0&size=10');
  });

  it('gets single review by id', async () => {
    mockedApi.get.mockResolvedValueOnce({ data: { id: 9 } } as any);

    const result = await reviewService.getReviewById(9);

    expect(mockedApi.get).toHaveBeenCalledWith('/api/reviews/9');
    expect(result.id).toBe(9);
  });

  it('creates and updates reviews', async () => {
    mockedApi.post.mockResolvedValueOnce({ data: { id: 1 } } as any);
    mockedApi.put.mockResolvedValueOnce({ data: { id: 1, comment: 'Updated' } } as any);

    await reviewService.createReview({ sessionId: 1, mentorId: 2, rating: 5, comment: 'Great' });
    const updated = await reviewService.updateReview(1, { comment: 'Updated' });

    expect(mockedApi.post).toHaveBeenCalledWith('/api/reviews', {
      sessionId: 1,
      mentorId: 2,
      rating: 5,
      comment: 'Great',
    });
    expect(mockedApi.put).toHaveBeenCalledWith('/api/reviews/1', { comment: 'Updated' });
    expect(updated.comment).toBe('Updated');
  });

  it('deletes review and gets mentor summary', async () => {
    mockedApi.delete.mockResolvedValueOnce({ data: {} } as any);
    mockedApi.get.mockResolvedValueOnce({ data: { averageRating: 4.7, totalReviews: 20 } } as any);

    await reviewService.deleteReview(4);
    const summary = await reviewService.getMentorAverageRating(2);

    expect(mockedApi.delete).toHaveBeenCalledWith('/api/reviews/4');
    expect(mockedApi.get).toHaveBeenCalledWith('/api/reviews/mentor/2/summary');
    expect(summary.totalReviews).toBe(20);
  });
});

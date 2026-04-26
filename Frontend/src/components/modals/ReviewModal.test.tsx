import { waitFor, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ReviewModal from './ReviewModal';
import { renderWithProviders } from '../../../tests/utils/renderWithProviders';
import api from '../../services/axios';

const showToast = jest.fn();

jest.mock('../../services/axios', () => ({
  __esModule: true,
  default: {
    post: jest.fn(),
  },
}));

jest.mock('../ui/Toast', () => ({
  ...jest.requireActual('../ui/Toast'),
  useToast: () => ({ showToast }),
}));

describe('ReviewModal', () => {
  const mockedApi = api as jest.Mocked<typeof api>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns null when closed', () => {
    const { container } = renderWithProviders(
      <ReviewModal isOpen={false} onClose={jest.fn()} onSuccess={jest.fn()} mentorId={10} sessionId={20} />,
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('submits review successfully and triggers callbacks', async () => {
    const user = userEvent.setup();
    const onClose = jest.fn();
    const onSuccess = jest.fn();
    mockedApi.post.mockResolvedValueOnce({ data: { id: 101 } } as any);

    renderWithProviders(
      <ReviewModal isOpen={true} onClose={onClose} onSuccess={onSuccess} mentorId={10} sessionId={20} />,
    );

    const stars = screen.getAllByText('star');
    await user.click(stars[3]);
    await user.type(screen.getByPlaceholderText(/share your experience/i), 'Great session!');
    await user.click(screen.getByRole('button', { name: 'Submit Review' }));

    await waitFor(() => {
      expect(mockedApi.post).toHaveBeenCalledWith('/api/reviews', {
        sessionId: 20,
        mentorId: 10,
        rating: 4,
        comment: 'Great session!',
      });
      expect(showToast).toHaveBeenCalledWith({ message: 'Review submitted successfully!', type: 'success' });
      expect(onSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('shows error toast when submission fails', async () => {
    const user = userEvent.setup();
    mockedApi.post.mockRejectedValueOnce({ response: { data: { message: 'Cannot review yet' } } } as any);

    renderWithProviders(
      <ReviewModal isOpen={true} onClose={jest.fn()} onSuccess={jest.fn()} mentorId={10} sessionId={20} />,
    );

    await user.click(screen.getAllByText('star')[4]);
    await user.click(screen.getByRole('button', { name: 'Submit Review' }));

    await waitFor(() => {
      expect(showToast).toHaveBeenCalledWith({ message: 'Cannot review yet', type: 'error' });
    });
  });

  it('shows loading spinner when submitting review', async () => {
    const user = userEvent.setup();
    let resolvePromise: any;
    mockedApi.post.mockImplementationOnce(() => new Promise((resolve) => { resolvePromise = resolve; }));
    renderWithProviders(
      <ReviewModal isOpen={true} onClose={jest.fn()} onSuccess={jest.fn()} mentorId={10} sessionId={20} />,
    );
    await user.click(screen.getAllByText('star')[4]);
    await user.click(screen.getByRole('button', { name: 'Submit Review' }));
    // Spinner should be visible
    expect(screen.getByText('autorenew')).toBeInTheDocument();
    // Finish the request
    resolvePromise({ data: { id: 123 } });
  });

  it('shows fallback error toast if error response is missing', async () => {
    const user = userEvent.setup();
    mockedApi.post.mockRejectedValueOnce({});
    renderWithProviders(
      <ReviewModal isOpen={true} onClose={jest.fn()} onSuccess={jest.fn()} mentorId={10} sessionId={20} />,
    );
    await user.click(screen.getAllByText('star')[4]);
    await user.click(screen.getByRole('button', { name: 'Submit Review' }));
    await waitFor(() => {
      expect(showToast).toHaveBeenCalledWith({ message: 'Failed to submit review. Please try again.', type: 'error' });
    });
  });
});

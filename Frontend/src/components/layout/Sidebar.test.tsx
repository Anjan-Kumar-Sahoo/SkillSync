import { screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Sidebar from './Sidebar';
import { renderWithProviders } from '../../../tests/utils/renderWithProviders';
import api from '../../services/axios';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => {
  const actual = jest.requireActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => ({ pathname: '/learner' }),
  };
});

jest.mock('../../services/axios', () => ({
  __esModule: true,
  default: {
    post: jest.fn(),
  },
}));

describe('Sidebar', () => {
  const mockedApi = api as jest.Mocked<typeof api>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders learner navigation and find mentor CTA', () => {
    renderWithProviders(<Sidebar role="ROLE_LEARNER" />);

    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Mentor Search')).toBeInTheDocument();
    expect(screen.getByText('Find a Mentor')).toBeInTheDocument();
  });

  it('renders mentor-specific links', () => {
    renderWithProviders(<Sidebar role="ROLE_MENTOR" />);

    expect(screen.getByText('My Availability')).toBeInTheDocument();
    expect(screen.getByText('Earnings')).toBeInTheDocument();
    expect(screen.queryByText('Find a Mentor')).not.toBeInTheDocument();
  });

  it('calls logout API and navigates to login on logout click', async () => {
    const user = userEvent.setup();
    const clearSpy = jest.spyOn(window.localStorage.__proto__, 'clear').mockImplementation(() => undefined);
    mockedApi.post.mockResolvedValueOnce({ data: { ok: true } } as any);

    renderWithProviders(<Sidebar role="ROLE_ADMIN" />);
    await user.click(screen.getByText('Logout'));

    expect(mockedApi.post).toHaveBeenCalledWith('/api/auth/logout');
    expect(clearSpy).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/login');

    clearSpy.mockRestore();
  });

  it('still clears session and navigates even if logout API fails', async () => {
    const user = userEvent.setup();
    mockedApi.post.mockRejectedValueOnce(new Error('network failed'));

    renderWithProviders(<Sidebar role="ROLE_LEARNER" />);
    await user.click(screen.getByText('Logout'));

    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('sets placeholder image on logo error', () => {
    renderWithProviders(<Sidebar role="ROLE_LEARNER" />);
    const logoImg = screen.getByAltText('SkillSync logo') as HTMLImageElement;
    fireEvent.error(logoImg);
    expect(logoImg.src).toContain('via.placeholder.com');
  });

  it('navigates to mentors page when Find a Mentor is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Sidebar role="ROLE_LEARNER" />);
    await user.click(screen.getByText('Find a Mentor'));
    expect(mockNavigate).toHaveBeenCalledWith('/mentors');
  });
});

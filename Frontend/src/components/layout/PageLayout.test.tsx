import { renderWithProviders } from '../../../tests/utils/renderWithProviders';
import PageLayout from './PageLayout';

jest.mock('./Sidebar', () => ({
  __esModule: true,
  default: ({ role }: { role: string }) => <div>Sidebar {role}</div>,
}));

jest.mock('./Navbar', () => ({
  __esModule: true,
  default: () => <div>Navbar</div>,
}));

describe('PageLayout', () => {
  it('uses learner role fallback and renders content without right panel', () => {
    const { getByText } = renderWithProviders(
      <PageLayout>
        <div>Main content</div>
      </PageLayout>,
      {
        preloadedState: {
          auth: { user: null, accessToken: null, refreshToken: null, isAuthenticated: false, role: null },
        },
      },
    );

    expect(getByText('Sidebar ROLE_LEARNER')).toBeInTheDocument();
    expect(getByText('Navbar')).toBeInTheDocument();
    expect(getByText('Main content')).toBeInTheDocument();
  });

  it('renders right panel when provided', () => {
    const { getByText } = renderWithProviders(
      <PageLayout rightPanel={<div>Right panel</div>}>
        <div>Main content</div>
      </PageLayout>,
      {
        preloadedState: {
          auth: {
            user: { id: 5, firstName: 'A', lastName: 'B', email: 'a@b.dev', role: 'ROLE_MENTOR' },
            accessToken: 'token',
            refreshToken: 'refresh',
            isAuthenticated: true,
            role: 'ROLE_MENTOR',
          },
        },
      },
    );

    expect(getByText('Sidebar ROLE_MENTOR')).toBeInTheDocument();
    expect(getByText('Right panel')).toBeInTheDocument();
  });
});

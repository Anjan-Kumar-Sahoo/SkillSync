import authReducer, { logout, setCredentials, updateUserName } from './authSlice';
import uiReducer, {
  clearError,
  setError,
  setLoading,
  setNotificationsOpen,
  setSidebarOpen,
  setTheme,
  toggleNotifications,
  toggleSidebar,
} from './uiSlice';
import sessionsReducer, {
  addSession,
  deleteSession,
  setCancelledSessions,
  setCompletedSessions,
  setPendingSessions,
  setSelectedSession,
  setSessions,
  setSessionsError,
  setSessionsLoading,
  setSessionsPage,
  setSessionsTotalElements,
  setUpcomingSessions,
  updateSession,
} from './sessionsSlice';
import mentorsReducer, {
  addMentor,
  clearMentorsFilters,
  setMentors,
  setMentorsError,
  setMentorsFilters,
  setMentorsLoading,
  setMentorsPage,
  setMentorsTotalElements,
  setMyMentorProfile,
  setSelectedMentor,
  updateMentor,
} from './mentorsSlice';
import groupsReducer, {
  addGroup,
  joinGroup,
  leaveGroup,
  removeGroup,
  setGroups,
  setGroupsError,
  setGroupsLoading,
  setGroupsPage,
  setGroupsSearchQuery,
  setGroupsTotalElements,
  setMyGroups,
  setSelectedGroup,
  updateGroup,
} from './groupsSlice';
import notificationsReducer, {
  addNotification,
  clearNotifications,
  markAllAsRead,
  markAsRead,
  removeNotification,
  setNotifications,
  setNotificationsError,
  setNotificationsLoading,
  setNotificationsTotalElements,
} from './notificationsSlice';
import reviewsReducer, {
  addReview,
  removeReview,
  setMentorReviews,
  setMyReviews,
  setReviews,
  setReviewsError,
  setReviewsLoading,
  setReviewsPage,
  setReviewsTotalElements,
  setSelectedReview,
  updateReview,
} from './reviewsSlice';

describe('redux slices', () => {
  const user = {
    id: 1,
    firstName: 'Test',
    lastName: 'User',
    email: 'test@skillsync.dev',
    role: 'ROLE_LEARNER' as const,
  };

  const session = {
    id: 1,
    mentorId: 2,
    learnerId: 1,
    mentorName: 'Mentor',
    learnerName: 'Learner',
    sessionDate: '2026-04-16T10:00:00Z',
    sessionDuration: 60,
    sessionFees: 1000,
    status: 'REQUESTED' as const,
    createdAt: '2026-04-16T09:00:00Z',
    updatedAt: '2026-04-16T09:00:00Z',
  };

  const mentor = {
    id: 1,
    userId: 1,
    name: 'Mentor One',
    email: 'mentor@skillsync.dev',
    bio: 'Bio',
    experience: 5,
    hourlyRate: 1200,
    rating: 4.5,
    reviewCount: 10,
    isApproved: true,
    skills: [],
    createdAt: '2026-04-16T00:00:00Z',
    updatedAt: '2026-04-16T00:00:00Z',
  };

  const group = {
    id: 1,
    name: 'Group 1',
    description: 'desc',
    category: 'General',
    createdBy: 1,
    createdByName: 'Admin',
    memberCount: 1,
    isJoined: false,
    createdAt: '2026-04-16T00:00:00Z',
    updatedAt: '2026-04-16T00:00:00Z',
  };

  const notification = {
    id: 1,
    userId: 1,
    type: 'SYSTEM' as const,
    title: 'System',
    message: 'Hello',
    isRead: false,
    createdAt: '2026-04-16T00:00:00Z',
    updatedAt: '2026-04-16T00:00:00Z',
  };

  const review = {
    id: 1,
    mentorId: 1,
    mentorName: 'Mentor',
    learnerId: 2,
    learnerName: 'Learner',
    sessionId: 10,
    rating: 5,
    comment: 'Great',
    isAnonymous: false,
    createdAt: '2026-04-16T00:00:00Z',
    updatedAt: '2026-04-16T00:00:00Z',
  };

  it('authSlice handles setCredentials, token preservation, updateUserName, and logout', () => {
    let state = authReducer(undefined, { type: 'init' });

    state = authReducer(state, setCredentials({ user, accessToken: 'access', refreshToken: 'refresh' }));
    expect(state.isAuthenticated).toBe(true);
    expect(state.accessToken).toBe('access');
    expect(state.refreshToken).toBe('refresh');

    state = authReducer(state, setCredentials({ user: { ...user, firstName: 'Updated' }, accessToken: '', refreshToken: null }));
    expect(state.accessToken).toBe('access');
    expect(state.refreshToken).toBe('refresh');

    state = authReducer(state, updateUserName({ firstName: 'Final', lastName: 'Name' }));
    expect(state.user?.firstName).toBe('Final');

    state = authReducer(state, logout());
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);

    state = authReducer(state, updateUserName({ firstName: 'Noop', lastName: 'Noop' }));
    expect(state.user).toBeNull();
  });

  it('uiSlice toggles and sets UI state values', () => {
    let state = uiReducer(undefined, { type: 'init' });

    state = uiReducer(state, toggleSidebar());
    expect(state.sidebarOpen).toBe(false);
    state = uiReducer(state, setSidebarOpen(true));
    expect(state.sidebarOpen).toBe(true);

    state = uiReducer(state, toggleNotifications());
    expect(state.notificationsOpen).toBe(true);
    state = uiReducer(state, setNotificationsOpen(false));
    expect(state.notificationsOpen).toBe(false);

    state = uiReducer(state, setTheme('dark'));
    expect(state.theme).toBe('dark');

    state = uiReducer(state, setLoading(true));
    expect(state.loading).toBe(true);

    state = uiReducer(state, setError('Oops'));
    expect(state.error).toBe('Oops');
    state = uiReducer(state, clearError());
    expect(state.error).toBeNull();
  });

  it('sessionsSlice updates list, status buckets, and selection', () => {
    let state = sessionsReducer(undefined, { type: 'init' });

    state = sessionsReducer(state, setSessions([session]));
    state = sessionsReducer(state, setUpcomingSessions([session]));
    state = sessionsReducer(state, setCompletedSessions([session]));
    state = sessionsReducer(state, setPendingSessions([session]));
    state = sessionsReducer(state, setCancelledSessions([session]));

    state = sessionsReducer(state, addSession({ ...session, id: 2 }));
    expect(state.sessions[0].id).toBe(2);

    state = sessionsReducer(state, updateSession({ ...session, id: 2, status: 'ACCEPTED' }));
    expect(state.sessions[0].status).toBe('ACCEPTED');

    state = sessionsReducer(state, updateSession({ ...session, id: 999 }));
    expect(state.sessions.some((s) => s.id === 999)).toBe(false);

    state = sessionsReducer(state, deleteSession(2));
    expect(state.sessions.find((s) => s.id === 2)).toBeUndefined();

    state = sessionsReducer(state, setSelectedSession(session));
    state = sessionsReducer(state, setSessionsLoading(true));
    state = sessionsReducer(state, setSessionsError('error'));
    state = sessionsReducer(state, setSessionsTotalElements(42));
    state = sessionsReducer(state, setSessionsPage(3));

    expect(state.selectedSession?.id).toBe(1);
    expect(state.isLoading).toBe(true);
    expect(state.error).toBe('error');
    expect(state.totalElements).toBe(42);
    expect(state.currentPage).toBe(3);
  });

  it('mentorsSlice handles CRUD-like updates and filter lifecycle', () => {
    let state = mentorsReducer(undefined, { type: 'init' });

    state = mentorsReducer(state, setMentors([mentor]));
    state = mentorsReducer(state, addMentor({ ...mentor, id: 2 }));
    state = mentorsReducer(state, updateMentor({ ...mentor, id: 2, rating: 4.9 }));
    state = mentorsReducer(state, updateMentor({ ...mentor, id: 999 }));

    state = mentorsReducer(state, setSelectedMentor(mentor));
    state = mentorsReducer(state, setMyMentorProfile(mentor));
    state = mentorsReducer(state, setMentorsLoading(true));
    state = mentorsReducer(state, setMentorsError('err'));
    state = mentorsReducer(state, setMentorsTotalElements(99));
    state = mentorsReducer(state, setMentorsPage(4));
    state = mentorsReducer(state, setMentorsFilters({ skill: 'React', minRating: 4 }));

    expect(state.mentors.find((m) => m.id === 2)?.rating).toBe(4.9);
    expect(state.filters.skill).toBe('React');
    expect(state.filters.minRating).toBe(4);

    state = mentorsReducer(state, clearMentorsFilters());
    expect(state.filters.skill).toBe('');
    expect(state.filters.minRating).toBe(0);
  });

  it('groupsSlice handles group joins, leaves, and metadata updates', () => {
    let state = groupsReducer(undefined, { type: 'init' });

    state = groupsReducer(state, setGroups([group]));
    state = groupsReducer(state, setMyGroups([]));
    state = groupsReducer(state, addGroup({ ...group, id: 2 }));

    state = groupsReducer(state, updateGroup({ ...group, id: 2, name: 'Updated Group' }));
    expect(state.groups.find((g) => g.id === 2)?.name).toBe('Updated Group');

    state = groupsReducer(state, joinGroup(1));
    expect(state.groups.find((g) => g.id === 1)?.isJoined).toBe(true);

    state = groupsReducer(state, leaveGroup(1));
    expect(state.groups.find((g) => g.id === 1)?.isJoined).toBe(false);

    state = groupsReducer(state, joinGroup(999));
    state = groupsReducer(state, leaveGroup(999));

    state = groupsReducer(state, setSelectedGroup(group));
    state = groupsReducer(state, setGroupsLoading(true));
    state = groupsReducer(state, setGroupsError('boom'));
    state = groupsReducer(state, setGroupsTotalElements(12));
    state = groupsReducer(state, setGroupsPage(2));
    state = groupsReducer(state, setGroupsSearchQuery('frontend'));

    expect(state.selectedGroup?.id).toBe(1);
    expect(state.currentPage).toBe(2);
    expect(state.searchQuery).toBe('frontend');

    state = groupsReducer(state, removeGroup(2));
    expect(state.groups.find((g) => g.id === 2)).toBeUndefined();
  });

  it('notificationsSlice tracks unread counters across transitions', () => {
    let state = notificationsReducer(undefined, { type: 'init' });

    state = notificationsReducer(state, setNotifications([notification, { ...notification, id: 2, isRead: true }]));
    expect(state.unreadCount).toBe(1);

    state = notificationsReducer(state, addNotification({ ...notification, id: 3, isRead: false }));
    expect(state.unreadCount).toBe(2);

    state = notificationsReducer(state, markAsRead(3));
    expect(state.unreadCount).toBe(1);

    state = notificationsReducer(state, removeNotification(1));
    expect(state.unreadCount).toBe(0);

    state = notificationsReducer(state, setNotificationsLoading(true));
    state = notificationsReducer(state, setNotificationsError('err'));
    state = notificationsReducer(state, setNotificationsTotalElements(88));
    state = notificationsReducer(state, markAllAsRead());
    expect(state.unreadCount).toBe(0);

    state = notificationsReducer(state, clearNotifications());
    expect(state.notifications).toEqual([]);
  });

  it('reviewsSlice updates review collections and pagination state', () => {
    let state = reviewsReducer(undefined, { type: 'init' });

    state = reviewsReducer(state, setReviews([review]));
    state = reviewsReducer(state, setMentorReviews([review]));
    state = reviewsReducer(state, setMyReviews([review]));
    state = reviewsReducer(state, addReview({ ...review, id: 2 }));

    state = reviewsReducer(state, updateReview({ ...review, id: 2, comment: 'Updated' }));
    expect(state.reviews.find((r) => r.id === 2)?.comment).toBe('Updated');

    state = reviewsReducer(state, updateReview({ ...review, id: 999 }));

    state = reviewsReducer(state, setSelectedReview(review));
    state = reviewsReducer(state, setReviewsLoading(true));
    state = reviewsReducer(state, setReviewsError('err'));
    state = reviewsReducer(state, setReviewsTotalElements(7));
    state = reviewsReducer(state, setReviewsPage(2));

    expect(state.selectedReview?.id).toBe(1);
    expect(state.currentPage).toBe(2);

    state = reviewsReducer(state, removeReview(2));
    expect(state.reviews.find((r) => r.id === 2)).toBeUndefined();
  });
});

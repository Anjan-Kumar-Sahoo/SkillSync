import { http, HttpResponse } from 'msw';

const defaultUser = {
  id: 1,
  firstName: 'Test',
  lastName: 'User',
  email: 'test.user@skillsync.dev',
  role: 'ROLE_LEARNER',
};

export const handlers = [
  http.get('*/api/auth/me', () => HttpResponse.json(defaultUser)),
  http.post('*/api/auth/refresh', () => HttpResponse.json({ user: defaultUser, accessToken: 'a', refreshToken: 'r' })),
  http.post('*/api/auth/logout', () => HttpResponse.json({ success: true })),

  http.get('*/api/notifications/unread/count', () => HttpResponse.json({ count: 0 })),
  http.get('*/api/notifications', () => HttpResponse.json({ content: [], totalElements: 0, page: 0, size: 20 })),

  http.get('*/api/admin/stats', () => HttpResponse.json({ users: 0, mentors: 0, sessions: 0 })),
  http.get('*/api/admin/users', () => HttpResponse.json([])),
  http.get('*/api/admin/mentors/pending', () => HttpResponse.json([])),

  http.get('*/api/mentors/search', () => HttpResponse.json({ content: [], totalElements: 0, page: 0, size: 10 })),
  http.get('*/api/mentors/me/availability', () => HttpResponse.json([])),

  http.get('*/api/sessions/learner', () => HttpResponse.json({ content: [], totalElements: 0, page: 0, size: 10 })),
  http.get('*/api/sessions/mentor', () => HttpResponse.json({ content: [], totalElements: 0, page: 0, size: 10 })),

  http.get('*/api/groups', () => HttpResponse.json({ content: [], totalElements: 0, number: 0, size: 10 })),
  http.get('*/api/groups/my', () => HttpResponse.json({ content: [], totalElements: 0, number: 0, size: 10 })),
  http.get('*/api/groups/:id', ({ params }) => HttpResponse.json({
    id: Number(params.id),
    name: 'Group',
    description: 'Desc',
    category: 'General',
    createdBy: 1,
    createdByName: 'Admin',
    memberCount: 1,
    joined: false,
    createdAt: new Date().toISOString(),
  })),
  http.get('*/api/groups/:id/messages', () => HttpResponse.json({ content: [], totalElements: 0, number: 0, size: 20 })),

  http.get('*/api/users/me', () => HttpResponse.json(defaultUser)),
  http.put('*/api/users/me', () => HttpResponse.json(defaultUser)),

  http.get('*/api/reviews', () => HttpResponse.json({ content: [], totalElements: 0, page: 0, size: 10 })),
];

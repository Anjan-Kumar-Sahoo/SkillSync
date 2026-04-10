import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../../services/axios';
import PageLayout from '../../components/layout/PageLayout';

type MentorProfile = {
  hourlyRate?: number | string;
};

type MentorSession = {
  id: number;
  topic?: string;
  sessionDate?: string;
  createdAt?: string;
  durationMinutes?: number;
};

const PAGE_SIZE = 100;
const MAX_PAGES = 20;

const formatInr = (value: number) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number.isFinite(value) ? value : 0);

const getSessionDate = (session: MentorSession) => {
  const rawDate = session.sessionDate || session.createdAt;
  const parsed = rawDate ? new Date(rawDate) : null;
  return parsed && !Number.isNaN(parsed.getTime()) ? parsed : null;
};

const EarningsPage = () => {
  const { data: mentorProfile, isLoading: mentorLoading } = useQuery<MentorProfile | null>({
    queryKey: ['mentor', 'my'],
    queryFn: async () => {
      try {
        const res = await api.get('/api/mentors/me', { _skipErrorRedirect: true } as any);
        return res.data;
      } catch {
        return null;
      }
    },
    refetchInterval: 30000,
  });

  const { data: completedSessions = [], isLoading: sessionsLoading } = useQuery<MentorSession[]>({
    queryKey: ['mentor', 'earnings', 'completed-sessions'],
    queryFn: async () => {
      const sessions: MentorSession[] = [];

      for (let page = 0; page < MAX_PAGES; page += 1) {
        const res = await api.get('/api/sessions/mentor', {
          params: { page, size: PAGE_SIZE, status: 'COMPLETED' },
          _skipErrorRedirect: true,
        } as any);

        const payload = res.data;
        const content = Array.isArray(payload?.content) ? payload.content : [];
        sessions.push(...content);

        if (payload?.last !== false) {
          break;
        }
      }

      return sessions;
    },
    refetchInterval: 30000,
  });

  const hourlyRate = Number(mentorProfile?.hourlyRate ?? 0);

  const summary = useMemo(() => {
    const now = new Date();
    let thisMonth = 0;
    let thisMonthSessions = 0;
    let lifetime = 0;

    for (const session of completedSessions) {
      const durationMinutes = Math.max(Number(session.durationMinutes ?? 0), 0);
      const sessionEarning = (hourlyRate * durationMinutes) / 60;
      lifetime += sessionEarning;

      const sessionDate = getSessionDate(session);
      if (sessionDate && sessionDate.getMonth() === now.getMonth() && sessionDate.getFullYear() === now.getFullYear()) {
        thisMonth += sessionEarning;
        thisMonthSessions += 1;
      }
    }

    return {
      thisMonth,
      pendingPayout: lifetime,
      lifetime,
      completedSessions: completedSessions.length,
      thisMonthSessions,
    };
  }, [completedSessions, hourlyRate]);

  const recentCompleted = useMemo(
    () =>
      [...completedSessions]
        .sort((a, b) => {
          const aTime = getSessionDate(a)?.getTime() ?? 0;
          const bTime = getSessionDate(b)?.getTime() ?? 0;
          return bTime - aTime;
        })
        .slice(0, 5),
    [completedSessions]
  );

  const isLoading = mentorLoading || sessionsLoading;

  return (
    <PageLayout>
      <div className="space-y-6">
        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <h1 className="text-2xl font-bold text-gray-900">Earnings</h1>
          <p className="text-gray-600 mt-2">
            Earnings are now calculated from your completed sessions using your current hourly rate.
          </p>
          <div className="mt-4 rounded-lg bg-amber-50 border border-amber-200 px-4 py-3">
            <p className="text-sm font-semibold text-amber-900">Settlement Note</p>
            <p className="text-sm text-amber-800 mt-1">
              Payout settlement APIs are pending, so Pending Payout currently mirrors total completed-session earnings.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">This Month</p>
            <p className="text-3xl font-bold text-emerald-600 mt-1">
              {isLoading ? 'Loading...' : formatInr(summary.thisMonth)}
            </p>
            <p className="text-xs text-gray-500 mt-1">{summary.thisMonthSessions} completed sessions</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">Pending Payout</p>
            <p className="text-3xl font-bold text-amber-600 mt-1">
              {isLoading ? 'Loading...' : formatInr(summary.pendingPayout)}
            </p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">Lifetime</p>
            <p className="text-3xl font-bold text-blue-600 mt-1">
              {isLoading ? 'Loading...' : formatInr(summary.lifetime)}
            </p>
          </div>
          <div className="bg-white border border-gray-200 rounded-xl p-5">
            <p className="text-sm text-gray-500">Completed Sessions</p>
            <p className="text-3xl font-bold text-gray-900 mt-1">{isLoading ? '...' : summary.completedSessions}</p>
          </div>
        </div>

        <div className="bg-white border border-gray-200 rounded-xl p-6">
          <h2 className="text-lg font-bold text-gray-900 mb-4">Recent Completed Sessions</h2>

          {isLoading ? (
            <p className="text-sm text-gray-600">Loading earnings data...</p>
          ) : recentCompleted.length === 0 ? (
            <p className="text-sm text-gray-600">No completed sessions yet. Earnings will appear here after your first completed session.</p>
          ) : (
            <div className="space-y-3">
              {recentCompleted.map((session) => {
                const duration = Math.max(Number(session.durationMinutes ?? 0), 0);
                const amount = (hourlyRate * duration) / 60;

                return (
                  <div key={session.id} className="flex items-center justify-between border border-gray-100 rounded-lg px-4 py-3">
                    <div>
                      <p className="font-semibold text-gray-900">{session.topic || `Session #${session.id}`}</p>
                      <p className="text-xs text-gray-500">
                        {getSessionDate(session)?.toLocaleString() || 'Completed session'}
                        {' • '}
                        {duration} min
                      </p>
                    </div>
                    <p className="text-sm font-bold text-emerald-600">{formatInr(amount)}</p>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </PageLayout>
  );
};

export default EarningsPage;

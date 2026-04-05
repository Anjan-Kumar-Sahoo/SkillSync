import { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import PageLayout from '../../components/layout/PageLayout';
import api from '../../services/axios';
import { useToast } from '../../components/ui/Toast';

const weekdayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

const MentorAvailabilityPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  const [dayOfWeek, setDayOfWeek] = useState('1');
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('10:00');
  const [localSlots, setLocalSlots] = useState<any[]>([]);

  const { data: mentorData, isLoading } = useQuery({
    queryKey: ['mentor', 'my', 'availability'],
    queryFn: async () => {
      const res = await api.get('/api/mentors/me', { _skipErrorRedirect: true } as any);
      return res.data;
    },
  });

  useEffect(() => {
    setLocalSlots(mentorData?.availability || []);
  }, [mentorData]);

  const addSlotMutation = useMutation({
    mutationFn: async () =>
      api.post(
        '/api/mentors/me/availability',
        {
          dayOfWeek: Number(dayOfWeek),
          startTime: `${startTime}:00`,
          endTime: `${endTime}:00`,
        },
        { _skipErrorRedirect: true } as any
      ),
    onSuccess: (response) => {
      const createdSlot = response?.data;
      if (createdSlot?.id) {
        setLocalSlots((prev) => {
          const exists = prev.some((slot: any) => slot.id === createdSlot.id);
          return exists ? prev : [...prev, createdSlot];
        });
      }
      showToast({ message: 'Availability slot added.', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['mentor', 'my'] });
      queryClient.invalidateQueries({ queryKey: ['mentor', 'my', 'availability'] });
    },
    onError: () => {
      showToast({ message: 'Failed to add availability slot.', type: 'error' });
    },
  });

  const deleteSlotMutation = useMutation({
    mutationFn: async (slotId: number) => api.delete(`/api/mentors/me/availability/${slotId}`, { _skipErrorRedirect: true } as any),
    onSuccess: (_, slotId) => {
      setLocalSlots((prev) => prev.filter((slot: any) => slot.id !== slotId));
      showToast({ message: 'Availability slot removed.', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['mentor', 'my'] });
      queryClient.invalidateQueries({ queryKey: ['mentor', 'my', 'availability'] });
    },
    onError: () => {
      showToast({ message: 'Failed to remove slot.', type: 'error' });
    },
  });

  const slots = localSlots;

  const sortedSlots = [...slots].sort((a: any, b: any) => {
    if (a.dayOfWeek !== b.dayOfWeek) return a.dayOfWeek - b.dayOfWeek;
    return String(a.startTime).localeCompare(String(b.startTime));
  });

  return (
    <PageLayout>
      <div className="max-w-5xl mx-auto space-y-6">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <button
              onClick={() => navigate('/mentor')}
              className="text-sm font-bold text-primary hover:underline mb-3"
            >
              Back to dashboard
            </button>
            <h1 className="text-4xl font-extrabold text-on-surface tracking-tight">Availability</h1>
            <p className="text-on-surface-variant text-lg mt-2">Set the weekly time windows learners can book.</p>
          </div>
        </div>

        <div className="bg-surface-container-lowest rounded-2xl p-6 md:p-8 shadow-sm border border-outline-variant/15">
          <h2 className="text-2xl font-extrabold text-on-surface mb-6">Add Weekly Slot</h2>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
            <div>
              <label className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">Day</label>
              <select
                value={dayOfWeek}
                onChange={(e) => setDayOfWeek(e.target.value)}
                className="w-full h-11 px-3 bg-surface-container rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
              >
                {weekdayNames.map((name, index) => (
                  <option key={name} value={index}>{name}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">Start Time</label>
              <input
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                className="w-full h-11 px-3 bg-surface-container rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
              />
            </div>

            <div>
              <label className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest block mb-1 pl-1">End Time</label>
              <input
                type="time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                className="w-full h-11 px-3 bg-surface-container rounded-lg text-sm font-semibold text-on-surface outline-none focus:ring-1 focus:ring-primary border border-transparent"
              />
            </div>

            <button
              onClick={() => addSlotMutation.mutate()}
              disabled={addSlotMutation.isPending}
              className="w-full h-11 gradient-btn text-white font-bold rounded-lg shadow-sm hover:shadow-md transition-all active:scale-95 disabled:opacity-50"
            >
              Add Slot
            </button>
          </div>
        </div>

        <div className="bg-surface-container-lowest rounded-2xl p-6 md:p-8 shadow-sm border border-outline-variant/15">
          <div className="flex items-center justify-between gap-4 mb-6">
            <h2 className="text-2xl font-extrabold text-on-surface">Current Slots</h2>
            {!isLoading && <span className="text-sm font-semibold text-on-surface-variant">{slots.length} total</span>}
          </div>

          {sortedSlots.length > 0 ? (
            <div className="space-y-3">
              {sortedSlots.map((slot: any) => (
                <div key={slot.id} className="flex flex-col md:flex-row md:items-center md:justify-between gap-3 rounded-xl border border-outline-variant/10 bg-surface-container-low p-4">
                  <div>
                    <p className="font-bold text-on-surface">{weekdayNames[slot.dayOfWeek] || 'Unknown day'}</p>
                    <p className="text-sm text-on-surface-variant mt-1">
                      {String(slot.startTime).slice(0, 5)} - {String(slot.endTime).slice(0, 5)}
                      {slot.isBooked ? ' • Booked' : ''}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {slot.isBooked ? (
                      <span className="text-[10px] font-black uppercase tracking-widest px-2 py-1 rounded-md bg-primary/10 text-primary border border-primary/20">Booked</span>
                    ) : (
                      <button
                        onClick={() => deleteSlotMutation.mutate(slot.id)}
                        disabled={deleteSlotMutation.isPending}
                        className="text-error bg-error/10 hover:bg-error/20 px-3 py-2 rounded-lg text-sm font-bold transition-colors disabled:opacity-50"
                      >
                        Remove
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="rounded-xl border border-dashed border-outline-variant/30 bg-surface-container-lowest p-10 text-center text-on-surface-variant">
              No availability slots yet.
            </div>
          )}
        </div>
      </div>
    </PageLayout>
  );
};

export default MentorAvailabilityPage;
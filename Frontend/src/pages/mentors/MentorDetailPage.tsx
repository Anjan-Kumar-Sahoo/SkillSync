import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import mentorService from '../../services/mentorService';
import sessionService from '../../services/sessionService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import type { CreateSessionPayload } from '../../services/sessionService';

const MentorProfilePage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { showToast } = useToast();

  const [showBookingForm, setShowBookingForm] = useState(false);
  const [bookingData, setBookingData] = useState({
    sessionDate: '',
    sessionDuration: 60,
  });

  // Fetch mentor profile
  const { data: mentor, isLoading } = useQuery({
    queryKey: ['mentor', 'detail', id],
    queryFn: () => mentorService.getMentorById(Number(id)),
  });

  // Fetch mentor details and reviews
  useQuery({
    queryKey: ['mentor', 'reviews', id],
    queryFn: () => mentorService.getMentorById(Number(id)),
  });

  // Book session mutation
  const bookSessionMutation = useMutation({
    mutationFn: (payload: CreateSessionPayload) => sessionService.createSession(payload),
    onSuccess: () => {
      showToast({ message: 'Session request sent successfully', type: 'success' });
      setShowBookingForm(false);
      setBookingData({ sessionDate: '', sessionDuration: 60 });
      navigate('/sessions');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Failed to book session';
      showToast({ message, type: 'error' });
    },
  });

  const handleBookSession = (e: React.FormEvent) => {
    e.preventDefault();
    if (!bookingData.sessionDate) {
      showToast({ message: 'Please select a session date', type: 'error' });
      return;
    }

    bookSessionMutation.mutate({
      mentorId: Number(id),
      topic: 'Mentoring Session',
      description: 'Booked from mentor profile',
      sessionDate: new Date(bookingData.sessionDate).toISOString(),
      durationMinutes: bookingData.sessionDuration,
    });
  };

  if (isLoading) {
    return (
      <PageLayout>
        <div className="flex items-center justify-center h-screen">
          <div className="text-lg text-gray-500">Loading mentor profile...</div>
        </div>
      </PageLayout>
    );
  }

  if (!mentor) {
    return (
      <PageLayout>
        <div className="text-center py-12">
          <p className="text-lg text-gray-500">Mentor not found</p>
          <button
            onClick={() => navigate('/mentors')}
            className="mt-4 bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700"
          >
            Back to Mentors
          </button>
        </div>
      </PageLayout>
    );
  }

  const mentorAny = mentor as any;
  const mentorDisplayName = mentorAny.name || `${mentorAny.firstName || ''} ${mentorAny.lastName || ''}`.trim() || `Mentor #${mentorAny.id}`;
  const mentorRating = Number(mentorAny.rating ?? mentorAny.avgRating ?? 0);
  const mentorReviews = Number(mentorAny.reviewCount ?? mentorAny.totalReviews ?? 0);
  const mentorExperience = Number(mentorAny.experience ?? mentorAny.experienceYears ?? 0);
  const mentorAvatar = mentorAny.profileImage || mentorAny.avatarUrl || 'https://via.placeholder.com/150';

  return (
    <PageLayout>
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Back Button */}
        <button
          onClick={() => navigate('/mentors')}
          className="text-blue-600 hover:text-blue-700 flex items-center gap-2"
        >
          ← Back to Mentors
        </button>

        {/* Mentor Header */}
        <div className="bg-white rounded-lg p-8 shadow-sm border border-gray-200">
          <div className="flex flex-col md:flex-row gap-8">
            {/* Profile Picture */}
            <div className="flex-shrink-0">
              <img
                src={mentorAvatar}
                alt={mentorDisplayName}
                className="w-40 h-40 rounded-full object-cover border-4 border-gray-200"
              />
            </div>

            {/* Mentor Info */}
            <div className="flex-1">
              <div className="flex items-start justify-between mb-4">
                <div>
                  <h1 className="text-3xl font-bold text-gray-900">{mentorDisplayName}</h1>
                  <p className="text-gray-500 mt-1">{mentorExperience} years of experience</p>
                </div>
                <div className="text-right">
                  <div className="text-4xl font-bold text-yellow-500">
                    {mentorRating.toFixed(1)}
                    <span className="text-lg text-gray-400">/ 5</span>
                  </div>
                  <p className="text-sm text-gray-500">({mentorReviews} reviews)</p>
                </div>
              </div>

              <p className="text-gray-600 mb-6">{mentor.bio}</p>

              <div className="grid grid-cols-2 gap-4 mb-6">
                <div className="bg-blue-50 rounded p-4">
                  <p className="text-sm text-gray-600">Hourly Rate</p>
                  <p className="text-2xl font-bold text-blue-600">₹{mentor.hourlyRate}/hr</p>
                </div>
                <div className="bg-green-50 rounded p-4">
                  <p className="text-sm text-gray-600">Status</p>
                  <p className="text-2xl font-bold text-green-600">
                    {mentorAny.isApproved || mentorAny.status === 'APPROVED' ? 'Verified' : mentorAny.status || 'Pending'}
                  </p>
                </div>
              </div>

              <button
                onClick={() => setShowBookingForm(true)}
                className="bg-blue-600 text-white px-8 py-3 rounded-lg hover:bg-blue-700 transition"
              >
                Book a Session
              </button>
            </div>
          </div>
        </div>

        {/* Skills */}
        <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
          <h2 className="text-lg font-bold text-gray-900 mb-4">Skills & Expertise</h2>
          <div className="flex flex-wrap gap-2">
            {mentor.skills?.map((skill) => (
              <span
                key={skill.id ?? skill.name}
                className="bg-blue-100 text-blue-800 px-4 py-2 rounded-full text-sm font-semibold"
              >
                {skill.name}
              </span>
            ))}
          </div>
        </div>

        {/* Booking Modal */}
        {showBookingForm && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-lg p-8 max-w-md w-full">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">Book a Session</h2>
              <form onSubmit={handleBookSession} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Session Date & Time
                  </label>
                  <input
                    type="datetime-local"
                    value={bookingData.sessionDate}
                    onChange={(e) => setBookingData({ ...bookingData, sessionDate: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Session Duration (minutes)
                  </label>
                  <select
                    value={bookingData.sessionDuration}
                    onChange={(e) =>
                      setBookingData({ ...bookingData, sessionDuration: parseInt(e.target.value) })
                    }
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="30">30 minutes</option>
                    <option value="60">1 hour</option>
                    <option value="90">1.5 hours</option>
                    <option value="120">2 hours</option>
                  </select>
                </div>

                <div className="bg-gray-50 rounded p-4">
                  <p className="text-sm text-gray-600">Estimated Cost</p>
                  <p className="text-2xl font-bold text-gray-900">
                    ₹{((mentor.hourlyRate * bookingData.sessionDuration) / 60).toFixed(0)}
                  </p>
                </div>

                <div className="flex gap-3 pt-4">
                  <button
                    type="submit"
                    disabled={bookSessionMutation.isPending}
                    className="flex-1 bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
                  >
                    {bookSessionMutation.isPending ? 'Booking...' : 'Request Session'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowBookingForm(false)}
                    className="flex-1 bg-gray-300 text-gray-900 py-2 rounded-lg hover:bg-gray-400 transition"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default MentorProfilePage;

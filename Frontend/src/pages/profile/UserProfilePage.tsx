import { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import userService from '../../services/userService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import type { RootState } from '../../store';

const UserProfilePage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const role = useSelector((state: RootState) => state.auth.role);

  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    bio: '',
    phone: '',
    location: '',
  });
  const [canSaveEdits, setCanSaveEdits] = useState(false);

  // Fetch user profile
  const { data: profile, isLoading } = useQuery({
    queryKey: ['user', 'profile'],
    queryFn: () => userService.getMyProfile(),
  });

  useEffect(() => {
    if (!profile) return;

    setFormData({
      firstName: profile.firstName || '',
      lastName: profile.lastName || '',
      bio: profile.bio || '',
      phone: profile.phone || '',
      location: profile.location || '',
    });
  }, [profile]);

  useEffect(() => {
    if (!isEditing) {
      setCanSaveEdits(false);
      return;
    }

    // Prevent accidental immediate submit when switching from Edit button to Save button.
    const timer = window.setTimeout(() => setCanSaveEdits(true), 600);
    return () => window.clearTimeout(timer);
  }, [isEditing]);

  // Update profile mutation
  const updateProfileMutation = useMutation({
    mutationFn: () => {
      const cleanedPayload = {
        firstName: formData.firstName.trim().length >= 2 ? formData.firstName.trim() : undefined,
        lastName: formData.lastName.trim().length >= 2 ? formData.lastName.trim() : undefined,
        bio: formData.bio.trim() || undefined,
        phone: formData.phone.trim() || undefined,
        location: formData.location.trim() || undefined,
      };

      return userService.updateProfile(cleanedPayload);
    },
    onSuccess: () => {
      showToast({ message: 'Profile updated successfully', type: 'success' });
      setIsEditing(false);
      queryClient.invalidateQueries({ queryKey: ['user', 'profile'] });
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || 'Failed to update profile';
      showToast({ message: msg, type: 'error' });
    },
  });


  const handleSubmit = () => {
    if (!isEditing || !canSaveEdits) {
      return;
    }

    if (formData.firstName.trim() && formData.firstName.trim().length < 2) {
      showToast({ message: 'First name must be at least 2 characters.', type: 'error' });
      return;
    }

    if (formData.lastName.trim() && formData.lastName.trim().length < 2) {
      showToast({ message: 'Last name must be at least 2 characters.', type: 'error' });
      return;
    }

    updateProfileMutation.mutate();
  };

  if (isLoading) {
    return (
      <PageLayout>
        <div className="flex items-center justify-center h-screen">
          <div className="text-lg text-gray-500">Loading profile...</div>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <div className="max-w-2xl mx-auto space-y-6">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 rounded-lg p-8 text-white">
          <h1 className="text-3xl font-bold">My Profile</h1>
          <p className="text-blue-100 mt-2">Manage your personal information, bio, and profile image</p>
        </div>

        {/* Profile Card */}
        <div className="bg-white rounded-lg p-8 shadow-sm border border-gray-200">
          <div className="flex flex-col md:flex-row gap-8">
            {/* Profile Picture */}
            <div className="flex flex-col items-center">
              <div className="relative">
                <div className="w-32 h-32 rounded-full bg-blue-500 text-white flex items-center justify-center text-4xl font-bold border-4 border-gray-200">
                  {(profile?.firstName?.[0] || '') + (profile?.lastName?.[0] || '') || 'U'}
                </div>
              </div>
              <div className="text-center mt-4">
                <p className="font-semibold text-gray-900">
                  {[profile?.firstName, profile?.lastName].filter(Boolean).join(' ') || 'Your Profile'}
                </p>
                <p className="text-sm text-gray-500">{profile?.email}</p>
                <span className="inline-block mt-2 bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-xs font-semibold">
                  {(role || 'ROLE_LEARNER').replace('ROLE_', '')}
                </span>
              </div>
            </div>

            {/* Profile Form */}
            <div className="flex-1">
              <form className="space-y-4">

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">First Name</label>
                  <input
                    type="text"
                    value={formData.firstName}
                    onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                    disabled={!isEditing}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Last Name</label>
                  <input
                    type="text"
                    value={formData.lastName}
                    onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                    disabled={!isEditing}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Bio</label>
                  <textarea
                    value={formData.bio}
                    onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
                    disabled={!isEditing}
                    rows={3}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
                    placeholder="Tell us about yourself..."
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Phone Number</label>
                  <input
                    type="tel"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    disabled={!isEditing}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Location</label>
                  <input
                    type="text"
                    value={formData.location}
                    onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                    disabled={!isEditing}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
                  />
                </div>

                <div className="flex gap-2 pt-4">
                  {!isEditing ? (
                    <button
                      type="button"
                      onClick={() => setIsEditing(true)}
                      className="flex-1 bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition"
                    >
                      Edit Profile
                    </button>
                  ) : (
                    <>
                      <button
                        type="button"
                        onClick={handleSubmit}
                        disabled={updateProfileMutation.isPending || !canSaveEdits}
                        className="flex-1 bg-green-600 text-white py-2 rounded-lg hover:bg-green-700 transition disabled:opacity-50"
                      >
                        Save Changes
                      </button>
                      <button
                        type="button"
                        onClick={() => setIsEditing(false)}
                        className="flex-1 bg-gray-400 text-white py-2 rounded-lg hover:bg-gray-500 transition"
                      >
                        Cancel
                      </button>
                    </>
                  )}
                </div>
              </form>
            </div>
          </div>
        </div>

        {/* Account Settings */}
        <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
          <h2 className="text-lg font-bold text-gray-900 mb-4">Account Settings</h2>
          {profile?.profileCompletePct !== undefined && (
            <div className="mb-4 rounded-lg border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-900">
              Profile completion: {profile.profileCompletePct}%
            </div>
          )}
          <div className="space-y-3">
            <button
              onClick={() => navigate('/settings/password')}
              className="w-full text-left p-4 rounded bg-gray-50 hover:bg-gray-100 transition border border-gray-200"
            >
              <p className="font-medium text-gray-900">Change Password</p>
              <p className="text-sm text-gray-500">Update your password regularly for security</p>
            </button>
          </div>
        </div>
      </div>
    </PageLayout>
  );
};

export default UserProfilePage;

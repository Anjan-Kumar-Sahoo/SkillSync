import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import userService from '../../services/userService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';

const UserProfilePage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    bio: '',
    phoneNumber: '',
    location: '',
  });
  const [, setProfileImage] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string>('');

  // Fetch user profile
  const { data: profile, isLoading } = useQuery({
    queryKey: ['user', 'profile'],
    queryFn: () => userService.getMyProfile(),
  });

  // Update profile mutation
  const updateProfileMutation = useMutation({
    mutationFn: () => userService.updateProfile(formData),
    onSuccess: () => {
      showToast({ message: 'Profile updated successfully', type: 'success' });
      setIsEditing(false);
      queryClient.invalidateQueries({ queryKey: ['user', 'profile'] });
    },
    onError: () => {
      showToast({ message: 'Failed to update profile', type: 'error' });
    },
  });

  // Upload image mutation
  const uploadImageMutation = useMutation({
    mutationFn: (file: File) => userService.uploadProfileImage(file),
    onSuccess: (data) => {
      setPreviewUrl(data.imageUrl);
      showToast({ message: 'Profile image updated', type: 'success' });
      queryClient.invalidateQueries({ queryKey: ['user', 'profile'] });
    },
    onError: () => {
      showToast({ message: 'Failed to upload image', type: 'error' });
    },
  });

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setProfileImage(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewUrl(reader.result as string);
      };
      reader.readAsDataURL(file);
      uploadImageMutation.mutate(file);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
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
          <p className="text-blue-100 mt-2">Manage your personal information and settings</p>
        </div>

        {/* Profile Card */}
        <div className="bg-white rounded-lg p-8 shadow-sm border border-gray-200">
          <div className="flex flex-col md:flex-row gap-8">
            {/* Profile Picture */}
            <div className="flex flex-col items-center">
              <div className="relative">
                <img
                  src={previewUrl || 'https://via.placeholder.com/150'}
                  alt="Profile"
                  className="w-32 h-32 rounded-full object-cover border-4 border-gray-200"
                />
                {isEditing && (
                  <label className="absolute bottom-0 right-0 bg-blue-600 text-white p-2 rounded-full cursor-pointer hover:bg-blue-700">
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handleImageChange}
                      className="hidden"
                      disabled={uploadImageMutation.isPending}
                    />
                    📷
                  </label>
                )}
              </div>
              <div className="text-center mt-4">
                <p className="font-semibold text-gray-900">{profile?.name}</p>
                <p className="text-sm text-gray-500">{profile?.email}</p>
                <span className="inline-block mt-2 bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-xs font-semibold">
                  {profile?.role?.replace('ROLE_', '')}
                </span>
              </div>
            </div>

            {/* Profile Form */}
            <div className="flex-1">
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
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
                    value={formData.phoneNumber}
                    onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
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
                        type="submit"
                        disabled={updateProfileMutation.isPending}
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
          <div className="space-y-3">
            <button
              onClick={() => navigate('/settings/password')}
              className="w-full text-left p-4 rounded bg-gray-50 hover:bg-gray-100 transition border border-gray-200"
            >
              <p className="font-medium text-gray-900">Change Password</p>
              <p className="text-sm text-gray-500">Update your password regularly for security</p>
            </button>
            <button
              onClick={() => navigate('/settings/preferences')}
              className="w-full text-left p-4 rounded bg-gray-50 hover:bg-gray-100 transition border border-gray-200"
            >
              <p className="font-medium text-gray-900">Notification Preferences</p>
              <p className="text-sm text-gray-500">Manage how you receive notifications</p>
            </button>
            <button
              onClick={() => navigate('/settings/security')}
              className="w-full text-left p-4 rounded bg-gray-50 hover:bg-gray-100 transition border border-gray-200"
            >
              <p className="font-medium text-gray-900">Security & Privacy</p>
              <p className="text-sm text-gray-500">Control your account security</p>
            </button>
          </div>
        </div>
      </div>
    </PageLayout>
  );
};

export default UserProfilePage;

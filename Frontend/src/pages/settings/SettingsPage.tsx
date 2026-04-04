import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import userService from '../../services/userService';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';
import type { UserPreferences } from '../../services/userService';

const SettingsPage = () => {
  const { showToast } = useToast();
  const [activeTab, setActiveTab] = useState<'notifications' | 'security' | 'privacy' | 'password'>('notifications');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [oldPassword, setOldPassword] = useState('');

  // Fetch preferences
  const { data: preferences, isLoading } = useQuery({
    queryKey: ['user', 'preferences'],
    queryFn: () => userService.getPreferences(),
  });

  // Update preferences mutation
  const updatePreferencesMutation = useMutation({
    mutationFn: (prefs: Partial<UserPreferences>) => userService.updatePreferences(prefs),
    onSuccess: () => {
      showToast({ message: 'Preferences updated successfully', type: 'success' });
    },
    onError: () => {
      showToast({ message: 'Failed to update preferences', type: 'error' });
    },
  });

  // Change password mutation
  const changePasswordMutation = useMutation({
    mutationFn: () =>
      userService.changePassword({
        oldPassword,
        newPassword,
        confirmPassword,
      }),
    onSuccess: () => {
      showToast({ message: 'Password changed successfully', type: 'success' });
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Failed to change password';
      showToast({ message, type: 'error' });
    },
  });

  const handlePreferenceChange = (key: keyof UserPreferences, value: any) => {
    updatePreferencesMutation.mutate({ [key]: value } as Partial<UserPreferences>);
  };

  const handleChangePassword = (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      showToast({ message: 'Passwords do not match', type: 'error' });
      return;
    }
    if (newPassword.length < 8) {
      showToast({ message: 'Password must be at least 8 characters', type: 'error' });
      return;
    }
    changePasswordMutation.mutate();
  };

  if (isLoading) {
    return (
      <PageLayout>
        <div className="flex items-center justify-center h-screen">
          <div className="text-lg text-gray-500">Loading settings...</div>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <div className="max-w-2xl mx-auto space-y-6">
        {/* Header */}
        <div className="bg-gradient-to-r from-indigo-600 to-blue-600 rounded-lg p-8 text-white">
          <h1 className="text-3xl font-bold">Settings</h1>
          <p className="text-indigo-100 mt-2">Manage your account settings and preferences</p>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200">
          <div className="flex space-x-8">
            {['notifications', 'password', 'security', 'privacy'].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab as typeof activeTab)}
                className={`py-4 px-1 border-b-2 font-medium text-sm capitalize ${
                  activeTab === tab
                    ? 'border-indigo-500 text-indigo-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                {tab}
              </button>
            ))}
          </div>
        </div>

        {/* Content */}
        {activeTab === 'notifications' && (
          <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200 space-y-6">
            <h2 className="text-lg font-bold text-gray-900">Notification Preferences</h2>

            <div className="flex items-center justify-between p-4 bg-gray-50 rounded border border-gray-200">
              <div>
                <p className="font-medium text-gray-900">Email Notifications</p>
                <p className="text-sm text-gray-500">Receive updates via email</p>
              </div>
              <input
                type="checkbox"
                checked={preferences?.emailNotifications || false}
                onChange={(e) => handlePreferenceChange('emailNotifications', e.target.checked)}
                className="w-5 h-5 rounded"
              />
            </div>

            <div className="flex items-center justify-between p-4 bg-gray-50 rounded border border-gray-200">
              <div>
                <p className="font-medium text-gray-900">Push Notifications</p>
                <p className="text-sm text-gray-500">Receive browser notifications</p>
              </div>
              <input
                type="checkbox"
                checked={preferences?.pushNotifications || false}
                onChange={(e) => handlePreferenceChange('pushNotifications', e.target.checked)}
                className="w-5 h-5 rounded"
              />
            </div>

            <div className="flex items-center justify-between p-4 bg-gray-50 rounded border border-gray-200">
              <div>
                <p className="font-medium text-gray-900">Session Reminders</p>
                <p className="text-sm text-gray-500">Get reminders for upcoming sessions</p>
              </div>
              <input
                type="checkbox"
                checked={preferences?.sessionReminders || false}
                onChange={(e) => handlePreferenceChange('sessionReminders', e.target.checked)}
                className="w-5 h-5 rounded"
              />
            </div>

            <div className="flex items-center justify-between p-4 bg-gray-50 rounded border border-gray-200">
              <div>
                <p className="font-medium text-gray-900">Newsletter</p>
                <p className="text-sm text-gray-500">Subscribe to our weekly newsletter</p>
              </div>
              <input
                type="checkbox"
                checked={preferences?.newsletter || false}
                onChange={(e) => handlePreferenceChange('newsletter', e.target.checked)}
                className="w-5 h-5 rounded"
              />
            </div>
          </div>
        )}

        {activeTab === 'password' && (
          <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200">
            <h2 className="text-lg font-bold text-gray-900 mb-6">Change Password</h2>
            <form onSubmit={handleChangePassword} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Current Password</label>
                <input
                  type="password"
                  value={oldPassword}
                  onChange={(e) => setOldPassword(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">New Password</label>
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  required
                  minLength={8}
                />
                <p className="text-xs text-gray-500 mt-1">At least 8 characters</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Confirm Password</label>
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={changePasswordMutation.isPending}
                className="w-full bg-indigo-600 text-white py-2 rounded-lg hover:bg-indigo-700 transition disabled:opacity-50"
              >
                {changePasswordMutation.isPending ? 'Updating...' : 'Update Password'}
              </button>
            </form>
          </div>
        )}

        {activeTab === 'security' && (
          <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200 space-y-6">
            <h2 className="text-lg font-bold text-gray-900">Security Settings</h2>

            <div className="p-4 bg-green-50 rounded border border-green-200">
              <p className="text-sm font-medium text-green-900 mb-2">✓ Two-Factor Authentication</p>
              <p className="text-sm text-green-700">Your account has 2FA enabled</p>
              <button className="mt-3 text-green-700 hover:text-green-800 text-sm font-medium">
                Manage 2FA
              </button>
            </div>

            <div className="p-4 bg-blue-50 rounded border border-blue-200">
              <p className="text-sm font-medium text-blue-900 mb-2">Active Sessions</p>
              <p className="text-sm text-blue-700 mb-3">You have 1 active session</p>
              <button className="text-blue-700 hover:text-blue-800 text-sm font-medium">
                View all sessions
              </button>
            </div>

            <div className="p-4 bg-red-50 rounded border border-red-200">
              <p className="text-sm font-medium text-red-900 mb-2">Deactivate Account</p>
              <p className="text-sm text-red-700 mb-3">Permanently deactivate your account</p>
              <button className="text-red-700 hover:text-red-800 text-sm font-medium">
                Deactivate account
              </button>
            </div>
          </div>
        )}

        {activeTab === 'privacy' && (
          <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200 space-y-6">
            <h2 className="text-lg font-bold text-gray-900">Privacy Settings</h2>

            <div className="flex items-center justify-between p-4 bg-gray-50 rounded border border-gray-200">
              <div>
                <p className="font-medium text-gray-900">Profile Visibility</p>
                <p className="text-sm text-gray-500">Your profile can be viewed by mentors</p>
              </div>
              <select className="border border-gray-300 rounded px-3 py-1 focus:outline-none focus:ring-2 focus:ring-indigo-500">
                <option>Public</option>
                <option>Private</option>
              </select>
            </div>

            <div className="flex items-center justify-between p-4 bg-gray-50 rounded border border-gray-200">
              <div>
                <p className="font-medium text-gray-900">Show Online Status</p>
                <p className="text-sm text-gray-500">Let others know when you're online</p>
              </div>
              <input type="checkbox" defaultChecked className="w-5 h-5 rounded" />
            </div>

            <div className="flex items-center justify-between p-4 bg-gray-50 rounded border border-gray-200">
              <div>
                <p className="font-medium text-gray-900">Allow Messages</p>
                <p className="text-sm text-gray-500">Allow other users to message you</p>
              </div>
              <input type="checkbox" defaultChecked className="w-5 h-5 rounded" />
            </div>
          </div>
        )}
      </div>
    </PageLayout>
  );
};

export default SettingsPage;

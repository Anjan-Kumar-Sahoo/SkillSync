import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import api from '../../services/axios';
import PageLayout from '../../components/layout/PageLayout';
import { useToast } from '../../components/ui/Toast';

const SettingsPage = () => {
  const { showToast } = useToast();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const changePasswordMutation = useMutation({
    mutationFn: async () => {
      const response = await api.post(
        '/api/auth/reset-password',
        {
          currentPassword,
          newPassword,
          confirmPassword,
        },
        { _skipErrorRedirect: true } as any
      );
      return response.data;
    },
    onSuccess: (data: any) => {
      showToast({
        message: data?.message || 'Password updated successfully',
        type: 'success',
      });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Failed to change password';
      showToast({ message, type: 'error' });
    },
  });

  const handleChangePassword = (e: React.FormEvent) => {
    e.preventDefault();

    if (!currentPassword.trim()) {
      showToast({ message: 'Current password is required', type: 'error' });
      return;
    }

    if (newPassword !== confirmPassword) {
      showToast({ message: 'Passwords do not match', type: 'error' });
      return;
    }

    if (currentPassword === newPassword) {
      showToast({ message: 'New password must be different from current password', type: 'error' });
      return;
    }

    if (newPassword.length < 8) {
      showToast({ message: 'Password must be at least 8 characters', type: 'error' });
      return;
    }

    changePasswordMutation.mutate();
  };

  return (
    <PageLayout>
      <div className="max-w-2xl mx-auto">
        {/* Header */}
        <div className="bg-gradient-to-r from-indigo-600 to-blue-600 rounded-lg p-8 text-white">
          <h1 className="text-3xl font-bold">Change Password</h1>
          <p className="text-indigo-100 mt-2">Secure your account with an updated password.</p>
        </div>

        <div className="bg-white rounded-lg p-6 shadow-sm border border-gray-200 mt-6">
          <form onSubmit={handleChangePassword} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Current Password</label>
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
                minLength={8}
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
              <label className="block text-sm font-medium text-gray-700 mb-1">Confirm New Password</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
                minLength={8}
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
      </div>
    </PageLayout>
  );
};

export default SettingsPage;

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import ProtectedRoute from './components/layout/ProtectedRoute';
import { ToastProvider } from './components/ui/Toast';
import AuthLayout from './components/layout/AuthLayout';
import AuthLoader from './components/layout/AuthLoader';
import type { RootState } from './store';

import LoginPage from './pages/auth/LoginPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import RegisterPage from './pages/auth/RegisterPage';
import VerifyOtpPage from './pages/auth/VerifyOtpPage';
import ResetPasswordPage from './pages/auth/ResetPasswordPage';
import SetupPasswordPage from './pages/auth/SetupPasswordPage';
import UnauthorizedPage from './pages/auth/UnauthorizedPage';
import ServerErrorPage from './pages/error/ServerErrorPage';
import LandingPage from './pages/LandingPage';
import LearnerDashboardPage from './pages/learner/LearnerDashboardPage';
import MentorDashboardPage from './pages/mentor/MentorDashboardPage';
import MentorAvailabilityPage from './pages/mentor/MentorAvailabilityPage';
import EarningsPage from './pages/mentor/EarningsPage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import UsersCenterPage from './pages/admin/UsersCenterPage';
import PlatformFinancesPage from './pages/admin/PlatformFinancesPage';
import DiscoverMentorsPage from './pages/mentors/DiscoverMentorsPage';
import MentorDetailPage from './pages/mentors/MentorDetailPage';
import MySessionsPage from './pages/sessions/MySessionsPage';
import CheckoutPage from './pages/payment/CheckoutPage';
import UserProfilePage from './pages/profile/UserProfilePage';
import GroupsPage from './pages/groups/GroupsPage';
import GroupDetailPage from './pages/groups/GroupDetailPage';
import NotificationsPage from './pages/notifications/NotificationsPage';
import SettingsPage from './pages/settings/SettingsPage';
import HelpCenterPage from './pages/support/HelpCenterPage';

const DashboardRedirect = () => {
  const role = useSelector((state: RootState) => state.auth.role);

  if (role === 'ROLE_ADMIN') return <Navigate to="/admin" replace />;
  if (role === 'ROLE_MENTOR') return <Navigate to="/mentor" replace />;

  return <Navigate to="/learner" replace />;
};

function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <AuthLoader>
          <Routes>
            <Route path="/" element={<LandingPage />} />

            {/* Public Auth Routes */}
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/forgot-password" element={<ForgotPasswordPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/verify-otp" element={<VerifyOtpPage />} />
              <Route path="/reset-password" element={<ResetPasswordPage />} />
              <Route path="/setup-password" element={<SetupPasswordPage />} />
            </Route>

            <Route path="/unauthorized" element={<UnauthorizedPage />} />
            <Route path="/500" element={<ServerErrorPage />} />

            {/* Protected Routes */}
            <Route element={<ProtectedRoute />}>
              <Route path="/dashboard" element={<DashboardRedirect />} />
              <Route path="/learner" element={<LearnerDashboardPage />} />
              <Route path="/learning-path" element={<Navigate to="/groups" replace />} />
              <Route path="/resources" element={<Navigate to="/groups" replace />} />
              <Route path="/mentor" element={<MentorDashboardPage />} />
              <Route path="/mentor/availability" element={<MentorAvailabilityPage />} />
              <Route path="/mentor/earnings" element={<EarningsPage />} />
              <Route path="/admin" element={<AdminDashboardPage />} />
              <Route path="/admin/users" element={<UsersCenterPage />} />
              <Route path="/admin/finances" element={<PlatformFinancesPage />} />
              <Route path="/mentors" element={<DiscoverMentorsPage />} />
              <Route path="/mentors/:id" element={<MentorDetailPage />} />
              <Route path="/sessions" element={<MySessionsPage />} />
              <Route path="/groups" element={<GroupsPage />} />
              <Route path="/groups/:id" element={<GroupDetailPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
              <Route path="/profile" element={<UserProfilePage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/settings/password" element={<SettingsPage />} />
              <Route path="/settings/preferences" element={<SettingsPage />} />
              <Route path="/settings/security" element={<SettingsPage />} />
              <Route path="/help" element={<HelpCenterPage />} />
              <Route path="/checkout" element={<CheckoutPage />} />
            </Route>
          </Routes>
        </AuthLoader>
      </BrowserRouter>
    </ToastProvider>
  );
}

export default App;

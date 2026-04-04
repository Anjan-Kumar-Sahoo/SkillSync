import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './components/layout/ProtectedRoute';
import { ToastProvider } from './components/ui/Toast';
import AuthLayout from './components/layout/AuthLayout';
import AuthLoader from './components/layout/AuthLoader';

import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import VerifyOtpPage from './pages/auth/VerifyOtpPage';
import SetupPasswordPage from './pages/auth/SetupPasswordPage';
import UnauthorizedPage from './pages/auth/UnauthorizedPage';
import ServerErrorPage from './pages/error/ServerErrorPage';
import LandingPage from './pages/LandingPage';
import LearnerDashboardPage from './pages/learner/LearnerDashboardPage';
import MentorDashboardPage from './pages/mentor/MentorDashboardPage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import DiscoverMentorsPage from './pages/mentors/DiscoverMentorsPage';
import MentorProfilePage from './pages/mentors/MentorProfilePage';
import MentorDetailPage from './pages/mentors/MentorDetailPage';
import MySessionsPage from './pages/sessions/MySessionsPage';
import CheckoutPage from './pages/payment/CheckoutPage';
import UserProfilePage from './pages/profile/UserProfilePage';
import GroupsPage from './pages/groups/GroupsPage';
import GroupDetailPage from './pages/groups/GroupDetailPage';
import NotificationsPage from './pages/notifications/NotificationsPage';
import SettingsPage from './pages/settings/SettingsPage';

const DashboardRedirect = () => {
  // Logic to redirect based on user role (e.g. from Redux) goes here. 
  // For now, default to learner dashboard.
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
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/verify-otp" element={<VerifyOtpPage />} />
              <Route path="/setup-password" element={<SetupPasswordPage />} />
            </Route>

            <Route path="/unauthorized" element={<UnauthorizedPage />} />
            <Route path="/500" element={<ServerErrorPage />} />

            {/* Protected Routes */}
            <Route element={<ProtectedRoute />}>
              <Route path="/dashboard" element={<DashboardRedirect />} />
              <Route path="/learner" element={<LearnerDashboardPage />} />
              <Route path="/mentor" element={<MentorDashboardPage />} />
              <Route path="/admin" element={<AdminDashboardPage />} />
              <Route path="/mentors" element={<DiscoverMentorsPage />} />
              <Route path="/mentors/:id" element={<MentorDetailPage />} />
              <Route path="/sessions" element={<MySessionsPage />} />
              <Route path="/groups" element={<GroupsPage />} />
              <Route path="/groups/:id" element={<GroupDetailPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
              <Route path="/profile" element={<UserProfilePage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/checkout" element={<CheckoutPage />} />
            </Route>
          </Routes>
        </AuthLoader>
      </BrowserRouter>
    </ToastProvider>
  );
}

export default App;

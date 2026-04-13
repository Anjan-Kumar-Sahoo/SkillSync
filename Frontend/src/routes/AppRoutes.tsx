import { Navigate, Route, Routes } from 'react-router-dom';

import ProtectedRoute from '../components/layout/ProtectedRoute';
import RoleGuard from '../components/layout/RoleGuard';
import AuthLayout from '../components/layout/AuthLayout';

import LoginPage from '../pages/auth/LoginPage';
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage';
import RegisterPage from '../pages/auth/RegisterPage';
import VerifyOtpPage from '../pages/auth/VerifyOtpPage';
import ResetPasswordPage from '../pages/auth/ResetPasswordPage';
import SetupPasswordPage from '../pages/auth/SetupPasswordPage';
import UnauthorizedPage from '../pages/auth/UnauthorizedPage';
import ServerErrorPage from '../pages/error/ServerErrorPage';

import LandingPage from '../pages/LandingPage.tsx';
import PptLandingPage from '../pages/PptLandingPage.tsx';
import LearnDocsPage from '../pages/docs/LearnDocsPage';

import LearnerDashboardPage from '../pages/learner/LearnerDashboardPage';
import MentorDashboardPage from '../pages/mentor/MentorDashboardPage';
import MentorAvailabilityPage from '../pages/mentor/MentorAvailabilityPage';
import EarningsPage from '../pages/mentor/EarningsPage';

import AdminDashboardPage from '../pages/admin/AdminDashboardPage';
import UsersCenterPage from '../pages/admin/UsersCenterPage';
import MentorApprovalsPage from '../pages/admin/MentorApprovalsPage';
import AdminSkillsPage from '../pages/admin/AdminSkillsPage';
import AdminGroupsPage from '../pages/admin/AdminGroupsPage';

import DiscoverMentorsPage from '../pages/mentors/DiscoverMentorsPage';
import MentorDetailPage from '../pages/mentors/MentorDetailPage';
import MySessionsPage from '../pages/sessions/MySessionsPage';
import CheckoutPage from '../pages/payment/CheckoutPage';
import UserProfilePage from '../pages/profile/UserProfilePage';
import GroupsPage from '../pages/groups/GroupsPage';
import GroupDetailPage from '../pages/groups/GroupDetailPage';
import NotificationsPage from '../pages/notifications/NotificationsPage';
import SettingsPage from '../pages/settings/SettingsPage';
import HelpCenterPage from '../pages/support/HelpCenterPage';

import DashboardRedirect from './DashboardRedirect';

const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/ppt" element={<PptLandingPage />} />
      <Route path="/learn" element={<LearnDocsPage />} />
      <Route path="/feppt" element={<Navigate to="/skillsync_study_guide.html" replace />} />

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

      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<DashboardRedirect />} />
        <Route path="/learner" element={<LearnerDashboardPage />} />
        <Route path="/learning-path" element={<Navigate to="/groups" replace />} />
        <Route path="/resources" element={<Navigate to="/groups" replace />} />
        <Route path="/mentor" element={<MentorDashboardPage />} />
        <Route path="/mentor/availability" element={<MentorAvailabilityPage />} />
        <Route path="/mentor/earnings" element={<EarningsPage />} />
        <Route path="/mentor/sessions" element={<MySessionsPage />} />
        <Route element={<RoleGuard allowedRoles={['ROLE_ADMIN']} />}>
          <Route path="/admin" element={<AdminDashboardPage />} />
          <Route path="/admin/users" element={<UsersCenterPage />} />
          <Route path="/admin/mentor-approvals" element={<MentorApprovalsPage />} />
          <Route path="/admin/skills" element={<AdminSkillsPage />} />
          <Route path="/admin/groups" element={<AdminGroupsPage />} />
        </Route>
        <Route path="/mentors" element={<DiscoverMentorsPage />} />
        <Route path="/mentors/:id" element={<MentorDetailPage />} />
        <Route path="/sessions" element={<MySessionsPage />} />
        <Route path="/groups" element={<GroupsPage />} />
        <Route path="/groups/:id" element={<GroupDetailPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/profile" element={<UserProfilePage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/settings/password" element={<SettingsPage />} />
        <Route path="/help" element={<HelpCenterPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
      </Route>
    </Routes>
  );
};

export default AppRoutes;

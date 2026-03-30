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
import LearnerDashboardPage from './pages/learner/LearnerDashboardPage';
import DiscoverMentorsPage from './pages/mentors/DiscoverMentorsPage';
import MentorProfilePage from './pages/mentors/MentorProfilePage';
import MySessionsPage from './pages/sessions/MySessionsPage';
import CheckoutPage from './pages/payment/CheckoutPage';

const DashboardRedirect = () => {
  // Logic to redirect based on user role (e.g. from Redux) goes here. 
  // For now, default to learner dashboard.
  return <Navigate to="/learner" replace />;
};
const MentorDashboard = () => <div className="p-8">Mentor Dashboard</div>;
const AdminDashboard = () => <div className="p-8">Admin Dashboard</div>;
const Groups = () => <div className="p-8">Groups</div>;
const Notifications = () => <div className="p-8">Notifications</div>;

function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <AuthLoader>
          <Routes>
            {/* Public Auth Routes */}
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/verify-otp" element={<VerifyOtpPage />} />
              <Route path="/setup-password" element={<SetupPasswordPage />} />
            </Route>

            <Route path="/unauthorized" element={<UnauthorizedPage />} />

            {/* Protected Routes */}
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardRedirect />} />
              <Route path="/learner" element={<LearnerDashboardPage />} />
              <Route path="/mentor" element={<MentorDashboard />} />
              <Route path="/admin" element={<AdminDashboard />} />
              <Route path="/mentors" element={<DiscoverMentorsPage />} />
              <Route path="/mentors/:id" element={<MentorProfilePage />} />
              <Route path="/sessions" element={<MySessionsPage />} />
              <Route path="/groups" element={<Groups />} />
              <Route path="/notifications" element={<Notifications />} />
              <Route path="/checkout" element={<CheckoutPage />} />
            </Route>
          </Routes>
        </AuthLoader>
      </BrowserRouter>
    </ToastProvider>
  );
}

export default App;

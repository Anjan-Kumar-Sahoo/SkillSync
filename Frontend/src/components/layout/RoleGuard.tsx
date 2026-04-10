import { Navigate, Outlet } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../../store';

interface RoleGuardProps {
  allowedRoles: Array<'ROLE_LEARNER' | 'ROLE_MENTOR' | 'ROLE_ADMIN'>;
}

const RoleGuard = ({ allowedRoles }: RoleGuardProps) => {
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const role = useSelector((state: RootState) => state.auth.role);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!role || !allowedRoles.includes(role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
};

export default RoleGuard;

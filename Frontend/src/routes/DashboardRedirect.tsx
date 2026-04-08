import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../store';

const DashboardRedirect = () => {
  const role = useSelector((state: RootState) => state.auth.role);

  if (role === 'ROLE_ADMIN') return <Navigate to="/admin" replace />;
  if (role === 'ROLE_MENTOR') return <Navigate to="/mentor" replace />;

  return <Navigate to="/learner" replace />;
};

export default DashboardRedirect;

import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { logout } from '../../store/slices/authSlice';
import api from '../../services/axios';
import logo from '../../assets/skillsync-logo.png';

interface SidebarProps {
  role: 'ROLE_LEARNER' | 'ROLE_MENTOR' | 'ROLE_ADMIN';
}

const Sidebar = ({ role }: SidebarProps) => {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const learnerNav = [
    { name: 'Dashboard', icon: 'grid_view', path: '/learner' }, 
    { name: 'Mentor Search', icon: 'person_search', path: '/mentors' },
    { name: 'My Sessions', icon: 'event_upcoming', path: '/sessions' },
    { name: 'Groups', icon: 'groups', path: '/groups' },
  ];

  const mentorNav = [
    { name: 'Dashboard', icon: 'grid_view', path: '/mentor' },
    { name: 'My Sessions', icon: 'event_upcoming', path: '/sessions' },
    { name: 'Manage Groups', icon: 'groups', path: '/groups' },
    { name: 'My Availability', icon: 'event_available', path: '/mentor/availability' },
    { name: 'My Profile', icon: 'account_circle', path: '/profile' },
    { name: 'Earnings', icon: 'payments', path: '/mentor/earnings' },
  ];

  const adminNav = [
    { name: 'Dashboard', icon: 'grid_view', path: '/admin' },
    { name: 'Manage Users', icon: 'group', path: '/admin/users' },
    { name: 'Approve Mentors', icon: 'how_to_reg', path: '/admin/mentor-approvals' },
    { name: 'Manage Skills', icon: 'psychology', path: '/admin/skills' },
    { name: 'Manage Groups', icon: 'groups', path: '/admin/groups' },
  ];

  const activeNav = role === 'ROLE_MENTOR' ? mentorNav : role === 'ROLE_ADMIN' ? adminNav : learnerNav;

  const handleLogout = async () => {
    try {
      await api.post('/api/auth/logout');
    } catch (e) {
      console.warn("Logout request failed cleanly", e);
    } finally {
      dispatch(logout());
      localStorage.clear();
      navigate('/login');
    }
  };

  return (
    <aside className="fixed left-0 top-0 h-screen w-20 lg:w-64 bg-surface-container-lowest/95 border-r border-outline-variant/20 flex flex-col justify-between z-40 transition-all duration-300 shadow-sm backdrop-blur-xl">
      <div className="flex flex-col flex-1 overflow-y-auto w-full scrollbar-hide">
        {/* LOGO SECTION */}
        <div className="flex items-center justify-center lg:justify-start lg:px-6 h-20 shrink-0 border-b border-outline-variant/10">
          <img src={logo} alt="SkillSync logo" className="w-10 h-10 object-contain hover:scale-105 transition duration-300" onError={(e: any) => { e.target.src = 'https://via.placeholder.com/40'; }} />
          <div className="hidden lg:flex flex-col ml-3">
            <span className="text-lg font-black text-primary tracking-tight leading-tight">SkillSync</span>
            <span className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">{role.replace('ROLE_', '')}</span>
          </div>
        </div>

        {/* NAVIGATION LIKS */}
        <nav className="flex-1 w-full px-2 lg:px-4 py-8 space-y-2">
          {activeNav.map((item) => {
            const isActive = location.pathname === item.path;
            const linkClasses = `flex items-center justify-center lg:justify-start px-2 lg:px-4 py-3 rounded-xl transition-all duration-200 group ${
              isActive 
                ? 'bg-surface-container-high text-primary shadow-sm font-extrabold' 
                : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface font-semibold'
            }`;

            return (
              <Link key={item.name} to={item.path} className={linkClasses}>
                <span className={`material-symbols-outlined text-2xl transition-transform duration-200 ${isActive ? 'scale-110' : 'group-hover:scale-110'}`}>
                  {item.icon}
                </span>
                <span className="hidden lg:inline ml-4 text-sm whitespace-nowrap">{item.name}</span>
              </Link>
            );
          })}
        </nav>
      </div>

      {/* BOTTOM SECTION */}
      <div className="w-full shrink-0 p-2 lg:p-4 border-t border-outline-variant/10 flex flex-col gap-2">
        {role === 'ROLE_LEARNER' && (
          <button 
            onClick={() => navigate('/mentors')}
            className="w-full flex items-center justify-center h-12 gradient-btn text-white rounded-xl shadow-md hover:shadow-lg hover:scale-[1.02] active:scale-[0.98] transition-all duration-300"
          >
            <span className="material-symbols-outlined text-xl">search</span>
            <span className="hidden lg:inline ml-2 text-sm font-bold whitespace-nowrap">Find a Mentor</span>
          </button>
        )}

        <Link to="/help" className="w-full flex items-center justify-center lg:justify-start px-2 lg:px-4 h-12 rounded-xl text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface transition-all duration-200 group">
          <span className="material-symbols-outlined text-2xl group-hover:scale-110 transition-transform">help</span>
          <span className="hidden lg:inline ml-4 text-sm font-semibold whitespace-nowrap">Help Center</span>
        </Link>

        <button 
          onClick={handleLogout}
          className="w-full flex items-center justify-center lg:justify-start px-2 lg:px-4 h-12 rounded-xl text-error hover:bg-error/10 transition-all duration-200 group"
        >
          <span className="material-symbols-outlined text-2xl group-hover:scale-110 transition-transform">logout</span>
          <span className="hidden lg:inline ml-4 text-sm font-semibold whitespace-nowrap">Logout</span>
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;

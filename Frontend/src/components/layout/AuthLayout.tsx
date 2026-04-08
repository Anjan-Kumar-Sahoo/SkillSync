import { Outlet } from 'react-router-dom';
import ThemeToggleButton from '../ui/ThemeToggleButton';

const AuthLayout = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-surface relative overflow-hidden py-12 px-4 sm:px-6 lg:px-8 z-0">
      <div className="fixed top-4 right-4 z-30">
        <ThemeToggleButton showLabel={false} />
      </div>

      {/* Top-left blob */}
      <div className="fixed top-[-10%] left-[-10%] w-[40%] h-[40%] bg-primary/5 rounded-full blur-[120px] -z-10 animate-pulse pointer-events-none" />
      
      {/* Bottom-right blob */}
      <div className="fixed bottom-[-10%] right-[-10%] w-[30%] h-[30%] bg-secondary-container/10 rounded-full blur-[100px] -z-10 animate-pulse pointer-events-none" />
      
      <div className="w-full max-w-[440px]">
        <Outlet />
      </div>
    </div>
  );
};

export default AuthLayout;

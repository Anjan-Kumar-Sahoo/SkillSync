import type { ReactNode } from 'react';
import { useSelector } from 'react-redux';
import type { RootState } from '../../store';
import Sidebar from './Sidebar';
import Navbar from './Navbar';

interface PageLayoutProps {
  children: ReactNode;
  rightPanel?: ReactNode;
}

const PageLayout = ({ children, rightPanel }: PageLayoutProps) => {
  const role = useSelector((state: RootState) => state.auth.role);
  const activeRole = role || 'ROLE_LEARNER';

  return (
    <div className="flex h-screen bg-surface font-sans text-on-surface overflow-hidden">
      <Sidebar role={activeRole} />
      
      <div className="flex-1 flex flex-col min-w-0 ml-20 lg:ml-64 transition-all duration-300">
        <Navbar />
        
        <main className="flex-1 overflow-x-hidden overflow-y-auto w-full p-4 md:p-6 lg:p-8 scroll-smooth">
          {rightPanel ? (
            <div className="max-w-7xl mx-auto flex flex-col xl:flex-row gap-6 lg:gap-8">
              <div className="flex-1 min-w-0 flex flex-col gap-6 lg:gap-8">
                {children}
              </div>
              <aside className="w-full xl:w-80 shrink-0 flex flex-col gap-6">
                {rightPanel}
              </aside>
            </div>
          ) : (
            <div className="max-w-7xl mx-auto flex flex-col gap-6 lg:gap-8">
              {children}
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default PageLayout;

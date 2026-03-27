import { useNavigate } from 'react-router-dom';
import logo from '../../assets/skillsync-logo.png';

const UnauthorizedPage = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-surface p-4">
      <div className="w-full max-w-[440px] bg-surface-container-lowest p-8 md:p-10 rounded-xl shadow-sm border border-outline-variant/15 text-center transition-all">
        <img 
          src={logo} 
          alt="SkillSync" 
          className="w-20 h-20 mx-auto mb-6 hover:scale-105 transition duration-300 opacity-90" 
          onError={(e: any) => { e.target.src = 'https://via.placeholder.com/80?text=Logo'; }} 
        />
        <h1 className="text-3xl font-extrabold text-on-surface mb-2">Access Denied</h1>
        <p className="text-sm font-medium text-on-surface-variant mb-8 px-4">
          You don't have permission to view this page.
        </p>
        <button 
          onClick={() => navigate('/dashboard')}
          className="mt-2 flex items-center justify-center w-full h-12 gradient-btn text-white font-bold rounded-xl shadow-md hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all duration-500"
        >
          Go to Dashboard
        </button>
      </div>
    </div>
  );
};

export default UnauthorizedPage;

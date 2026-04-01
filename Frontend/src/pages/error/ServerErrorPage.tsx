import { Link } from 'react-router-dom';

const ServerErrorPage = () => (
  <div className="min-h-screen flex flex-col items-center justify-center bg-surface p-4">
    <div className="max-w-md w-full text-center space-y-6">
      <h1 className="text-6xl font-black text-error">500</h1>
      <h2 className="text-2xl font-bold text-on-surface">Something went wrong</h2>
      <p className="text-on-surface-variant">We're experiencing some technical difficulties on our end. Please try again later.</p>
      <Link to="/" className="inline-block mt-4 px-6 py-3 gradient-btn text-white font-bold rounded-xl shadow-md hover:shadow-xl hover:scale-[1.02] transition-all duration-300">
        Return Home
      </Link>
    </div>
  </div>
);

export default ServerErrorPage;

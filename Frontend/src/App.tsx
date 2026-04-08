import { BrowserRouter } from 'react-router-dom';
import { ToastProvider } from './components/ui/Toast';
import AuthLoader from './components/layout/AuthLoader';
import AppRoutes from './routes/AppRoutes';

function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <AuthLoader>
          <AppRoutes />
        </AuthLoader>
      </BrowserRouter>
    </ToastProvider>
  );
}

export default App;

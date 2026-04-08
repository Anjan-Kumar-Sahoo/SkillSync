import { BrowserRouter } from 'react-router-dom';
import { ToastProvider } from './components/ui/Toast';
import { ActionConfirmProvider } from './components/ui/ActionConfirm';
import AuthLoader from './components/layout/AuthLoader';
import AppRoutes from './routes/AppRoutes';

function App() {
  return (
    <ToastProvider>
      <ActionConfirmProvider>
        <BrowserRouter>
          <AuthLoader>
            <AppRoutes />
          </AuthLoader>
        </BrowserRouter>
      </ActionConfirmProvider>
    </ToastProvider>
  );
}

export default App;

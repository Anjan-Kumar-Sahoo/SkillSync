import { createContext, useContext, useState, useCallback } from 'react';
import type { ReactNode } from 'react';

type ToastType = 'success' | 'error' | 'info';

interface ToastOptions {
  message: string;
  type?: ToastType;
}

interface ToastContextType {
  showToast: (options: ToastOptions) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const ToastProvider = ({ children }: { children: ReactNode }) => {
  const [toastData, setToastData] = useState<ToastOptions | null>(null);

  const showToast = useCallback(({ message, type = 'success' }: ToastOptions) => {
    setToastData({ message, type });
    setTimeout(() => {
      setToastData(null);
    }, 3000);
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {toastData && (
        <div 
          className={`fixed bottom-6 right-6 px-6 py-3 rounded-xl shadow-lg font-bold text-white z-50 transition-all duration-300 transform translate-y-0 ${
            toastData.type === 'error' ? 'bg-error' : toastData.type === 'info' ? 'bg-secondary' : 'bg-primary'
          }`}
        >
          {toastData.message}
        </div>
      )}
    </ToastContext.Provider>
  );
};

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used within ToastProvider');
  return context;
};

import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';

type ConfirmOptions = {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
};

type ConfirmContextType = {
  requestConfirmation: (options: ConfirmOptions) => Promise<boolean>;
};

type InternalConfirmOptions = {
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel: string;
};

const ConfirmContext = createContext<ConfirmContextType | undefined>(undefined);

const DEFAULT_OPTIONS: Omit<InternalConfirmOptions, 'message'> = {
  title: 'Are you sure?',
  confirmLabel: 'Yes',
  cancelLabel: 'No',
};

export const ActionConfirmProvider = ({ children }: { children: ReactNode }) => {
  const resolverRef = useRef<((confirmed: boolean) => void) | null>(null);

  const [isOpen, setIsOpen] = useState(false);
  const [options, setOptions] = useState<InternalConfirmOptions>({
    ...DEFAULT_OPTIONS,
    message: '',
  });

  const closeModal = useCallback((confirmed: boolean) => {
    setIsOpen(false);

    if (resolverRef.current) {
      resolverRef.current(confirmed);
      resolverRef.current = null;
    }
  }, []);

  const requestConfirmation = useCallback((incoming: ConfirmOptions) => {
    return new Promise<boolean>((resolve) => {
      resolverRef.current = resolve;

      setOptions({
        title: incoming.title || DEFAULT_OPTIONS.title,
        message: incoming.message,
        confirmLabel: incoming.confirmLabel || DEFAULT_OPTIONS.confirmLabel,
        cancelLabel: incoming.cancelLabel || DEFAULT_OPTIONS.cancelLabel,
      });

      setIsOpen(true);
    });
  }, []);

  useEffect(() => {
    if (!isOpen) return;

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        closeModal(false);
      }
    };

    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [isOpen, closeModal]);

  return (
    <ConfirmContext.Provider value={{ requestConfirmation }}>
      {children}

      {isOpen && (
        <div className="fixed inset-0 z-[120] bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-sm rounded-2xl border border-outline-variant/20 bg-surface-container-lowest shadow-2xl overflow-hidden">
            <div className="px-6 py-5 border-b border-outline-variant/10 bg-surface-container-low">
              <h2 className="text-xl font-extrabold text-on-surface">{options.title}</h2>
              <p className="text-sm text-on-surface-variant mt-2">{options.message}</p>
            </div>

            <div className="px-6 py-4 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => closeModal(false)}
                className="px-5 py-2.5 rounded-lg text-sm font-bold bg-surface-container hover:bg-surface-container-high text-on-surface transition-colors"
              >
                {options.cancelLabel}
              </button>
              <button
                type="button"
                onClick={() => closeModal(true)}
                className="px-5 py-2.5 rounded-lg text-sm font-bold bg-error text-white hover:opacity-90 transition-opacity"
              >
                {options.confirmLabel}
              </button>
            </div>
          </div>
        </div>
      )}
    </ConfirmContext.Provider>
  );
};

export const useActionConfirm = () => {
  const context = useContext(ConfirmContext);

  if (!context) {
    throw new Error('useActionConfirm must be used within ActionConfirmProvider');
  }

  return context;
};

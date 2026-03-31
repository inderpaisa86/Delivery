import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
type ToastType = 'success' | 'error' | 'info';
interface Toast { id: number; message: string; type: ToastType; }
const ToastContext = createContext<{ toast: (m: string, t?: ToastType) => void } | null>(null);
let nextId = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const addToast = useCallback((message: string, type: ToastType = 'info') => {
    const id = nextId++;
    setToasts((p) => [...p, { id, message, type }]);
    setTimeout(() => setToasts((p) => p.filter((t) => t.id !== id)), 3500);
  }, []);
  const colors: Record<ToastType, string> = {
    success: 'bg-green-500 border-green-400',
    error: 'bg-red-500 border-red-400',
    info: 'bg-[var(--accent)] border-orange-400',
  };
  return (
    <ToastContext.Provider value={{ toast: addToast }}>
      {children}
      <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2">
        {toasts.map((t) => (
          <div key={t.id} className={`${colors[t.type]} text-white px-5 py-3 rounded-2xl shadow-lg text-sm font-medium border animate-slide-in`}>{t.message}</div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
export function useToast() { const ctx = useContext(ToastContext); if (!ctx) throw new Error('useToast must be inside ToastProvider'); return ctx; }

import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';

interface AuthState {
  token: string;
  role: 'restaurante' | 'domiciliario';
  perfil?: 'admin' | 'operario';
  domiciliarioId?: number;
}

interface AuthContextType {
  auth: AuthState | null;
  login: (token: string, role: AuthState['role'], domiciliarioId?: number, perfil?: AuthState['perfil']) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(() => {
    const saved = localStorage.getItem('auth');
    return saved ? JSON.parse(saved) : null;
  });

  const login = useCallback(
    (token: string, role: AuthState['role'], domiciliarioId?: number, perfil?: AuthState['perfil']) => {
      const state: AuthState = { token, role, domiciliarioId, perfil };
      localStorage.setItem('auth', JSON.stringify(state));
      localStorage.setItem('api_token', token);
      setAuth(state);
    },
    [],
  );

  const logout = useCallback(() => {
    localStorage.removeItem('auth');
    localStorage.removeItem('api_token');
    setAuth(null);
  }, []);

  return (
    <AuthContext.Provider value={{ auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be inside AuthProvider');
  return ctx;
}

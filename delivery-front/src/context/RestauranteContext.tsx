import { createContext, useContext, useState, type ReactNode } from 'react';
import type { RestauranteResponse } from '../types';

interface RestauranteContextType {
  restaurante: RestauranteResponse | null;
  setRestaurante: (r: RestauranteResponse | null) => void;
}

const RestauranteContext = createContext<RestauranteContextType | null>(null);

export function RestauranteProvider({ children }: { children: ReactNode }) {
  const [restaurante, setRestaurante] = useState<RestauranteResponse | null>(() => {
    const saved = localStorage.getItem('restaurante');
    return saved ? JSON.parse(saved) : null;
  });

  const set = (r: RestauranteResponse | null) => {
    if (r) localStorage.setItem('restaurante', JSON.stringify(r));
    else localStorage.removeItem('restaurante');
    setRestaurante(r);
  };

  return (
    <RestauranteContext.Provider value={{ restaurante, setRestaurante: set }}>
      {children}
    </RestauranteContext.Provider>
  );
}

export function useRestaurante() {
  const ctx = useContext(RestauranteContext);
  if (!ctx) throw new Error('useRestaurante must be inside RestauranteProvider');
  return ctx;
}

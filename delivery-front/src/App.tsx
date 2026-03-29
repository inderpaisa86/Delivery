import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { RestauranteProvider } from './context/RestauranteContext';
import { ToastProvider } from './components/Toast';
import { Navbar } from './components/Navbar';
import { LoginPage } from './pages/LoginPage';
import { PedidosPage } from './pages/restaurante/PedidosPage';
import { ProductosPage } from './pages/restaurante/ProductosPage';
import { DomiciliariosPage } from './pages/restaurante/DomiciliariosPage';
import { DomiciliarioPanel } from './pages/domiciliario/DomiciliarioPanel';
import { TrackingPage } from './pages/TrackingPage';
import type { ReactNode } from 'react';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 3000 },
  },
});

function ProtectedRoute({ children, role }: { children: ReactNode; role?: string }) {
  const { auth } = useAuth();
  if (!auth) return <Navigate to="/login" replace />;
  if (role && auth.role !== role) return <Navigate to="/" replace />;
  return <>{children}</>;
}

function HomeRedirect() {
  const { auth } = useAuth();
  if (!auth) return <Navigate to="/login" replace />;
  if (auth.role === 'restaurante') return <Navigate to="/restaurante/pedidos" replace />;
  return <Navigate to="/domiciliario" replace />;
}

function AppRoutes() {
  return (
    <BrowserRouter>
      <Navbar />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<HomeRedirect />} />

        {/* Restaurante */}
        <Route path="/restaurante/pedidos" element={
          <ProtectedRoute role="restaurante"><PedidosPage /></ProtectedRoute>
        } />
        <Route path="/restaurante/productos" element={
          <ProtectedRoute role="restaurante"><ProductosPage /></ProtectedRoute>
        } />
        <Route path="/restaurante/domiciliarios" element={
          <ProtectedRoute role="restaurante"><DomiciliariosPage /></ProtectedRoute>
        } />

        {/* Domiciliario */}
        <Route path="/domiciliario" element={
          <ProtectedRoute role="domiciliario"><DomiciliarioPanel /></ProtectedRoute>
        } />

        {/* Tracking público */}
        <Route path="/track/:token" element={<TrackingPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RestauranteProvider>
          <ToastProvider>
            <AppRoutes />
          </ToastProvider>
        </RestauranteProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

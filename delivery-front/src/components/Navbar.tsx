import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useRestaurante } from '../context/RestauranteContext';

export function Navbar() {
  const { auth, logout } = useAuth();
  const { restaurante } = useRestaurante();
  const location = useLocation();

  const isActive = (path: string) =>
    location.pathname.startsWith(path) ? 'bg-indigo-700' : 'hover:bg-indigo-500/50';

  if (!auth) return null;

  return (
    <nav className="bg-indigo-600 text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4 flex items-center justify-between h-14">
        <div className="flex items-center gap-1">
          <Link to="/" className="font-bold text-lg mr-4">🛵 Delivery</Link>
          {auth.role === 'restaurante' && (
            <>
              <Link to="/restaurante/pedidos" className={`px-3 py-1.5 rounded-lg text-sm ${isActive('/restaurante/pedidos')}`}>
                Pedidos
              </Link>
              <Link to="/restaurante/productos" className={`px-3 py-1.5 rounded-lg text-sm ${isActive('/restaurante/productos')}`}>
                Productos
              </Link>
              <Link to="/restaurante/domiciliarios" className={`px-3 py-1.5 rounded-lg text-sm ${isActive('/restaurante/domiciliarios')}`}>
                Domiciliarios
              </Link>
            </>
          )}
          {auth.role === 'domiciliario' && (
            <Link to="/domiciliario" className={`px-3 py-1.5 rounded-lg text-sm ${isActive('/domiciliario')}`}>
              Mis Pedidos
            </Link>
          )}
        </div>
        <div className="flex items-center gap-3 text-sm">
          {restaurante && (
            <span className="bg-indigo-500/40 px-2 py-1 rounded">📍 {restaurante.nombre}</span>
          )}
          <button onClick={logout} className="hover:bg-indigo-500/50 px-3 py-1.5 rounded-lg">
            Salir
          </button>
        </div>
      </div>
    </nav>
  );
}

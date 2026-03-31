import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useRestaurante } from '../context/RestauranteContext';

const restauranteLinks = [
  { to: '/restaurante/pedidos', icon: '📋', label: 'Pedidos' },
  { to: '/restaurante/productos', icon: '🍔', label: 'Productos' },
  { to: '/restaurante/domiciliarios', icon: '🛵', label: 'Domiciliarios' },
  { to: '/restaurante/usuarios', icon: '👤', label: 'Usuarios' },
];
const domiciliarioLinks = [
  { to: '/domiciliario', icon: '🗺️', label: 'Mis Pedidos' },
];

export function Sidebar() {
  const { auth, logout } = useAuth();
  const { restaurante } = useRestaurante();
  const location = useLocation();
  if (!auth) return null;
  const links = auth.role === 'restaurante' ? restauranteLinks : domiciliarioLinks;

  return (
    <aside className="fixed left-0 top-0 h-screen w-[230px] bg-gradient-to-b from-[var(--bg-sidebar)] to-[#e85d2c] flex flex-col z-40 shadow-xl">
      <div className="p-6 pb-4">
        <div className="flex items-center gap-2.5">
          <div className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center text-xl backdrop-blur-sm">🛵</div>
          <div>
            <span className="font-bold text-lg text-white">Delivery</span>
            {restaurante && <p className="text-xs text-white/60 truncate max-w-[130px]">{restaurante.nombre}</p>}
          </div>
        </div>
      </div>
      <nav className="flex-1 px-3 space-y-1">
        {links.map((link) => {
          const active = location.pathname === link.to;
          return (
            <Link key={link.to} to={link.to}
              className={`flex items-center gap-3 px-4 py-3 rounded-xl text-sm transition-all ${
                active ? 'bg-white/20 text-white font-semibold shadow-sm backdrop-blur-sm' : 'text-white/70 hover:bg-white/10 hover:text-white'
              }`}>
              <span className="text-lg">{link.icon}</span><span>{link.label}</span>
            </Link>
          );
        })}
      </nav>
      <div className="p-3">
        <button onClick={logout} className="flex items-center gap-3 px-4 py-3 rounded-xl text-sm text-white/60 hover:bg-white/10 hover:text-white transition-all w-full">
          <span className="text-lg">🚪</span><span>Cerrar sesión</span>
        </button>
      </div>
    </aside>
  );
}

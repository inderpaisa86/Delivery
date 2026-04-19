import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useRestaurante } from '../context/RestauranteContext';

const allRestauranteLinks = [
  { to: '/restaurante/pedidos', icon: '📋', label: 'Pedidos', perfiles: ['admin', 'operario'] },
  { to: '/restaurante/historial', icon: '📊', label: 'Historial', perfiles: ['admin'] },
  { to: '/restaurante/productos', icon: '🍔', label: 'Productos', perfiles: ['admin'] },
  { to: '/restaurante/domiciliarios', icon: '🛵', label: 'Domiciliarios', perfiles: ['admin'] },
  { to: '/restaurante/usuarios', icon: '👤', label: 'Usuarios', perfiles: ['admin'] },
];
const domiciliarioLinks = [
  { to: '/domiciliario', icon: '🗺️', label: 'Mis Pedidos' },
];

export function Sidebar() {
  const { auth, logout } = useAuth();
  const { restaurante } = useRestaurante();
  const location = useLocation();
  if (!auth) return null;

  const perfil = auth.perfil || 'operario';
  const links = auth.role === 'restaurante'
    ? allRestauranteLinks.filter((l) => l.perfiles.includes(perfil))
    : domiciliarioLinks;

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
        {auth.role === 'restaurante' && (
          <p className="text-xs text-white/40 mt-2 ml-12">{perfil === 'admin' ? '👑 Administrador' : '👤 Operario'}</p>
        )}
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

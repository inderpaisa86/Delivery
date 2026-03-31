import { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useRestaurante } from '../context/RestauranteContext';
import { authService } from '../services/auth';
import { domiciliarioService } from '../services/domiciliarios';

const API_TOKEN = import.meta.env.VITE_API_TOKEN || 'delivery-internal-token';
const inputClass = 'w-full bg-white border border-[var(--border)] text-[var(--text-primary)] rounded-2xl px-5 py-3.5 text-sm focus:ring-2 focus:ring-[var(--accent)] outline-none transition shadow-sm placeholder:text-[var(--text-muted)]';

export function LoginPage() {
  const { auth, login } = useAuth();
  const { setRestaurante } = useRestaurante();
  const navigate = useNavigate();
  const [role, setRole] = useState<'restaurante' | 'domiciliario'>('restaurante');
  const [username, setUsername] = useState(''); const [password, setPassword] = useState(''); const [cedula, setCedula] = useState('');
  const [loginError, setLoginError] = useState(''); const [loading, setLoading] = useState(false);

  if (auth) return <Navigate to="/" replace />;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); setLoginError(''); setLoading(true);
    try {
      if (role === 'restaurante') {
        const res = await authService.login({ username, password });
        if (res.restauranteId && res.restauranteNombre) setRestaurante({ id: res.restauranteId, nombre: res.restauranteNombre, telefono: '', direccion: '', lat: 0, lng: 0, activo: true, whatsappPhoneId: '' });
        login(res.token, 'restaurante'); navigate('/');
      } else {
        localStorage.setItem('api_token', API_TOKEN);
        const dom = await domiciliarioService.buscarPorCedula(cedula);
        login(API_TOKEN, 'domiciliario', dom.id); navigate('/');
      }
    } catch { if (role === 'domiciliario') localStorage.removeItem('api_token'); setLoginError(role === 'restaurante' ? 'Usuario o contraseña incorrectos' : 'Cédula no encontrada'); }
    finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen flex">
      {/* Izquierda — visual naranja */}
      <div className="hidden lg:flex flex-1 relative overflow-hidden bg-gradient-to-br from-[var(--accent)] via-[#ff8255] to-[#ff9a76]">
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="text-center text-white px-10">
            <div className="text-[100px] mb-4">🛵</div>
            <h2 className="text-4xl font-bold mb-3">Tu delivery,<br/>más inteligente</h2>
            <p className="text-white/80 text-lg max-w-md mx-auto">Gestiona pedidos, domiciliarios y entregas en tiempo real desde un solo lugar.</p>
          </div>
        </div>
        <div className="absolute -bottom-20 -left-20 w-64 h-64 rounded-full bg-white/10" />
        <div className="absolute -top-10 -right-10 w-40 h-40 rounded-full bg-white/10" />
        <div className="absolute top-1/3 left-10 w-20 h-20 rounded-full bg-white/5" />
      </div>

      {/* Derecha — formulario */}
      <div className="w-full lg:w-[500px] flex flex-col justify-center px-8 md:px-16 py-12 bg-[var(--bg-primary)]">
        <div className="mb-10">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-12 h-12 bg-[var(--accent)] rounded-2xl flex items-center justify-center text-2xl text-white shadow-lg animate-glow">🛵</div>
            <div><span className="text-xl font-bold">DeliveryApp</span><p className="text-xs text-[var(--text-muted)]">Panel de gestión</p></div>
          </div>
        </div>

        <h1 className="text-3xl font-bold mb-2">¡Hola de nuevo! 👋</h1>
        <p className="text-[var(--text-muted)] mb-8">Ingresa tus credenciales para continuar</p>

        <form onSubmit={handleSubmit} className="space-y-5">
          {loginError && <div className="bg-red-50 border border-red-200 rounded-2xl p-4 text-sm text-red-600 font-medium">{loginError}</div>}

          <div>
            <label className="block text-xs font-bold text-[var(--text-muted)] uppercase tracking-wider mb-2.5">Ingresar como</label>
            <div className="grid grid-cols-2 gap-3">
              {(['restaurante', 'domiciliario'] as const).map((r) => (
                <button key={r} type="button" onClick={() => { setRole(r); setLoginError(''); }}
                  className={`py-3.5 rounded-2xl text-sm font-semibold border-2 transition-all ${
                    role === r ? 'border-[var(--accent)] bg-[var(--accent-light)] text-[var(--accent)] shadow-sm' : 'border-[var(--border)] text-[var(--text-muted)] bg-white hover:border-[var(--border-hover)]'
                  }`}>
                  {r === 'restaurante' ? '🏪 Restaurante' : '🛵 Domiciliario'}
                </button>
              ))}
            </div>
          </div>

          {role === 'restaurante' ? (
            <>
              <div><label className="block text-xs font-bold text-[var(--text-muted)] uppercase tracking-wider mb-2.5">Usuario</label><input type="text" value={username} onChange={(e) => setUsername(e.target.value)} className={inputClass} placeholder="admin" autoComplete="username" required /></div>
              <div><label className="block text-xs font-bold text-[var(--text-muted)] uppercase tracking-wider mb-2.5">Contraseña</label><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className={inputClass} placeholder="••••••••" autoComplete="current-password" required /></div>
            </>
          ) : (
            <div><label className="block text-xs font-bold text-[var(--text-muted)] uppercase tracking-wider mb-2.5">Número de cédula</label><input type="text" value={cedula} onChange={(e) => setCedula(e.target.value.replace(/\D/g, ''))} className={inputClass} placeholder="1234567890" inputMode="numeric" required /></div>
          )}

          <button type="submit" disabled={loading}
            className="w-full bg-[var(--accent)] text-white py-3.5 rounded-2xl font-bold text-sm hover:brightness-110 active:scale-[0.98] transition-all btn-glow disabled:opacity-50">
            {loading ? 'Verificando…' : 'Ingresar →'}
          </button>
        </form>

        <p className="text-xs text-[var(--text-muted)] mt-12 text-center">© 2026 DeliveryApp</p>
      </div>
    </div>
  );
}

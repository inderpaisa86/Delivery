import { useState, useEffect } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { useRestaurante } from '../context/RestauranteContext';
import { restauranteService } from '../services/restaurantes';
import { domiciliarioService } from '../services/domiciliarios';
import type { RestauranteResponse } from '../types';

const API_TOKEN = import.meta.env.VITE_API_TOKEN || 'delivery-internal-token';

const RESTAURANT_IMAGES: Record<number, string> = {};
const DEFAULT_IMAGE =
  'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=1200&q=80';

function getImage(id?: number) {
  return id && RESTAURANT_IMAGES[id] ? RESTAURANT_IMAGES[id] : DEFAULT_IMAGE;
}

export function LoginPage() {
  const { auth, login } = useAuth();
  const { setRestaurante } = useRestaurante();
  const navigate = useNavigate();
  const [role, setRole] = useState<'restaurante' | 'domiciliario'>('restaurante');
  const [cedula, setCedula] = useState('');
  const [loginError, setLoginError] = useState('');
  const [loading, setLoading] = useState(false);
  const [selectedRestaurante, setSelectedRestaurante] = useState<RestauranteResponse | null>(null);

  const { data: restaurantes } = useQuery({
    queryKey: ['restaurantes-login'],
    queryFn: restauranteService.listar,
  });

  useEffect(() => {
    if (restaurantes?.length && !selectedRestaurante) {
      setSelectedRestaurante(restaurantes[0]);
    }
  }, [restaurantes, selectedRestaurante]);

  if (auth) return <Navigate to="/" replace />;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError('');

    if (selectedRestaurante) setRestaurante(selectedRestaurante);

    if (role === 'domiciliario') {
      setLoading(true);
      try {
        // Guardar token antes de la llamada para que el header Authorization funcione
        localStorage.setItem('api_token', API_TOKEN);
        const dom = await domiciliarioService.buscarPorCedula(cedula);
        login(API_TOKEN, role, dom.id);
        navigate('/');
      } catch {
        localStorage.removeItem('api_token');
        setLoginError('No se encontró un domiciliario con esa cédula');
      } finally {
        setLoading(false);
      }
    } else {
      login(API_TOKEN, role);
      navigate('/');
    }
  };

  const bgImage = getImage(selectedRestaurante?.id);

  return (
    <div className="min-h-screen flex">
      {/* ── Lado izquierdo: formulario ── */}
      <div className="w-full lg:w-[480px] flex flex-col justify-center px-8 md:px-14 py-12 bg-white relative z-10">
        <div className="mb-10">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-3xl">🛵</span>
            <span className="text-xl font-bold text-gray-900 tracking-tight">DeliveryApp</span>
          </div>
          <p className="text-gray-400 text-sm ml-11">Panel de gestión</p>
        </div>

        <h1 className="text-2xl font-bold text-gray-900 mb-1">Bienvenido</h1>
        <p className="text-gray-500 text-sm mb-8">Selecciona tu rol y restaurante para continuar</p>

        <form onSubmit={handleSubmit} className="space-y-5">
          {loginError && (
            <div className="bg-red-50 border border-red-200 rounded-xl p-3 text-sm text-red-700">
              {loginError}
            </div>
          )}

          {/* Rol */}
          <div>
            <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Rol</label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => { setRole('restaurante'); setLoginError(''); }}
                className={`flex items-center justify-center gap-2 py-3 rounded-xl text-sm font-medium border-2 transition-all ${
                  role === 'restaurante'
                    ? 'border-blue-600 bg-blue-50 text-blue-700 shadow-sm'
                    : 'border-gray-200 text-gray-500 hover:border-gray-300 hover:bg-gray-50'
                }`}
              >
                🏪 Restaurante
              </button>
              <button
                type="button"
                onClick={() => { setRole('domiciliario'); setLoginError(''); }}
                className={`flex items-center justify-center gap-2 py-3 rounded-xl text-sm font-medium border-2 transition-all ${
                  role === 'domiciliario'
                    ? 'border-blue-600 bg-blue-50 text-blue-700 shadow-sm'
                    : 'border-gray-200 text-gray-500 hover:border-gray-300 hover:bg-gray-50'
                }`}
              >
                🛵 Domiciliario
              </button>
            </div>
          </div>

          {/* Restaurante */}
          <div>
            <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Restaurante</label>
            <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
              {restaurantes?.map((r) => (
                <label
                  key={r.id}
                  className={`flex items-center gap-3 p-3 rounded-xl border-2 cursor-pointer transition-all ${
                    selectedRestaurante?.id === r.id
                      ? 'border-blue-600 bg-blue-50/60'
                      : 'border-gray-100 hover:border-gray-200 hover:bg-gray-50'
                  }`}
                >
                  <input
                    type="radio"
                    name="restaurante"
                    checked={selectedRestaurante?.id === r.id}
                    onChange={() => setSelectedRestaurante(r)}
                    className="accent-blue-600 w-4 h-4"
                  />
                  <div className="flex-1 min-w-0">
                    <p className={`text-sm font-medium truncate ${
                      selectedRestaurante?.id === r.id ? 'text-blue-700' : 'text-gray-800'
                    }`}>{r.nombre}</p>
                    {r.direccion && <p className="text-xs text-gray-400 truncate">{r.direccion}</p>}
                  </div>
                </label>
              ))}
              {!restaurantes?.length && (
                <p className="text-sm text-gray-400 text-center py-4">Cargando restaurantes…</p>
              )}
            </div>
          </div>

          {/* Cédula domiciliario */}
          {role === 'domiciliario' && (
            <div>
              <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                Número de cédula
              </label>
              <input
                type="text"
                value={cedula}
                onChange={(e) => setCedula(e.target.value)}
                className="w-full border-2 border-gray-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all"
                placeholder="Ej: 1234567890"
                required
              />
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-3 rounded-xl font-semibold text-sm hover:bg-blue-700 active:scale-[0.98] transition-all shadow-lg shadow-blue-600/25 disabled:opacity-50"
          >
            {loading ? 'Verificando…' : 'Ingresar'}
          </button>
        </form>

        <p className="text-xs text-gray-300 mt-10 text-center">© 2026 DeliveryApp — Panel de gestión</p>
      </div>

      {/* ── Lado derecho: imagen ── */}
      <div className="hidden lg:flex flex-1 relative overflow-hidden">
        <img
          src={bgImage}
          alt={selectedRestaurante?.nombre ?? 'Restaurante'}
          className="absolute inset-0 w-full h-full object-cover transition-all duration-700"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />
        <div className="relative z-10 flex flex-col justify-end p-10 text-white">
          {selectedRestaurante ? (
            <>
              <p className="text-sm font-medium uppercase tracking-widest text-white/70 mb-2">Restaurante seleccionado</p>
              <h2 className="text-4xl font-bold mb-2 drop-shadow-lg">{selectedRestaurante.nombre}</h2>
              {selectedRestaurante.direccion && (
                <p className="text-white/80 flex items-center gap-1.5 text-sm">📍 {selectedRestaurante.direccion}</p>
              )}
              {selectedRestaurante.telefono && (
                <p className="text-white/80 flex items-center gap-1.5 text-sm mt-1">📞 {selectedRestaurante.telefono}</p>
              )}
            </>
          ) : (
            <>
              <h2 className="text-3xl font-bold mb-2 drop-shadow-lg">Gestiona tus pedidos</h2>
              <p className="text-white/70 text-sm">Selecciona un restaurante para comenzar</p>
            </>
          )}
        </div>
        <div className="absolute top-8 right-8 grid grid-cols-3 gap-2 opacity-20">
          {Array.from({ length: 9 }).map((_, i) => (
            <div key={i} className="w-2 h-2 rounded-full bg-white" />
          ))}
        </div>
      </div>
    </div>
  );
}

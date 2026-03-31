import { useQuery } from '@tanstack/react-query';
import { restauranteService } from '../services/restaurantes';
import { useRestaurante } from '../context/RestauranteContext';

export function RestauranteSelector() {
  const { restaurante, setRestaurante } = useRestaurante();
  const { data: restaurantes, isLoading } = useQuery({ queryKey: ['restaurantes'], queryFn: restauranteService.listar });
  if (isLoading) return <p className="text-[var(--text-muted)] text-sm">Cargando…</p>;
  return (
    <select value={restaurante?.id ?? ''} onChange={(e) => { const r = restaurantes?.find((r) => r.id === Number(e.target.value)); setRestaurante(r ?? null); }}
      className="bg-white border border-[var(--border)] text-[var(--text-primary)] rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-[var(--accent)] outline-none shadow-sm" aria-label="Seleccionar restaurante">
      <option value="">Seleccionar restaurante</option>
      {restaurantes?.map((r) => <option key={r.id} value={r.id}>{r.nombre}</option>)}
    </select>
  );
}

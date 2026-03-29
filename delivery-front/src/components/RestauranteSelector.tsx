import { useQuery } from '@tanstack/react-query';
import { restauranteService } from '../services/restaurantes';
import { useRestaurante } from '../context/RestauranteContext';

export function RestauranteSelector() {
  const { restaurante, setRestaurante } = useRestaurante();
  const { data: restaurantes, isLoading } = useQuery({
    queryKey: ['restaurantes'],
    queryFn: restauranteService.listar,
  });

  if (isLoading) return <p className="text-gray-500 text-sm">Cargando restaurantes…</p>;

  return (
    <select
      value={restaurante?.id ?? ''}
      onChange={(e) => {
        const r = restaurantes?.find((r) => r.id === Number(e.target.value));
        setRestaurante(r ?? null);
      }}
      className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
      aria-label="Seleccionar restaurante"
    >
      <option value="">Seleccionar restaurante</option>
      {restaurantes?.map((r) => (
        <option key={r.id} value={r.id}>{r.nombre}</option>
      ))}
    </select>
  );
}

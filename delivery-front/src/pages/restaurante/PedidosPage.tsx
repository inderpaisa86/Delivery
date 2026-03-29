import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { pedidoService } from '../../services/pedidos';
import { useRestaurante } from '../../context/RestauranteContext';
import { useToast } from '../../components/Toast';
import { EstadoBadge } from '../../components/EstadoBadge';
import { StatCard } from '../../components/StatCard';
import { RestauranteSelector } from '../../components/RestauranteSelector';
import { Modal } from '../../components/Modal';
import { EstadoPedido, type PedidoResponse } from '../../types';

const TRANSITIONS: Partial<Record<EstadoPedido, EstadoPedido[]>> = {
  [EstadoPedido.NUEVO]: [EstadoPedido.CONFIRMADO, EstadoPedido.CANCELADO],
  [EstadoPedido.CONFIRMADO]: [EstadoPedido.PREPARANDO, EstadoPedido.CANCELADO],
  [EstadoPedido.PREPARANDO]: [EstadoPedido.LISTO, EstadoPedido.CANCELADO],
  [EstadoPedido.LISTO]: [EstadoPedido.EN_CAMINO],
  [EstadoPedido.EN_CAMINO]: [EstadoPedido.ENTREGADO],
};

const ACTION_LABELS: Partial<Record<EstadoPedido, string>> = {
  [EstadoPedido.CONFIRMADO]: '✅ Confirmar',
  [EstadoPedido.PREPARANDO]: '👨‍🍳 Preparar',
  [EstadoPedido.LISTO]: '📦 Listo',
  [EstadoPedido.EN_CAMINO]: '🛵 En camino',
  [EstadoPedido.ENTREGADO]: '🏁 Entregado',
  [EstadoPedido.CANCELADO]: '❌ Cancelar',
};

export function PedidosPage() {
  const { restaurante } = useRestaurante();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [filtroEstado, setFiltroEstado] = useState<EstadoPedido | ''>('');
  const [detalle, setDetalle] = useState<PedidoResponse | null>(null);
  const prevCountRef = useRef(0);

  const { data, isLoading } = useQuery({
    queryKey: ['pedidos', restaurante?.id],
    queryFn: () => pedidoService.listarPorRestaurante(restaurante!.id, 0, 100),
    refetchInterval: 5000,
    enabled: !!restaurante,
  });

  const pedidos = data?.content ?? [];

  // Notificación de nuevos pedidos
  useEffect(() => {
    const nuevos = pedidos.filter((p) => p.estado === EstadoPedido.NUEVO).length;
    if (nuevos > prevCountRef.current && prevCountRef.current > 0) {
      toast('🔔 Nuevo pedido recibido', 'info');
    }
    prevCountRef.current = nuevos;
  }, [pedidos, toast]);

  const cambiarEstado = useMutation({
    mutationFn: ({ id, estado }: { id: number; estado: EstadoPedido }) =>
      pedidoService.cambiarEstado(id, estado),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pedidos'] });
      toast('Estado actualizado', 'success');
    },
    onError: (err: Error) => toast(err.message, 'error'),
  });

  const filtered = filtroEstado
    ? pedidos.filter((p) => p.estado === filtroEstado)
    : pedidos;

  const stats = {
    total: pedidos.length,
    enCamino: pedidos.filter((p) => p.estado === EstadoPedido.EN_CAMINO).length,
    entregados: pedidos.filter((p) => p.estado === EstadoPedido.ENTREGADO).length,
    nuevos: pedidos.filter((p) => p.estado === EstadoPedido.NUEVO).length,
  };

  if (!restaurante) {
    return (
      <div className="p-6">
        <h1 className="text-xl font-bold mb-4">Panel de Pedidos</h1>
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-yellow-800">
          <RestauranteSelector />
          <p className="mt-2 text-sm">Selecciona un restaurante para ver los pedidos.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-6 max-w-7xl mx-auto">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
        <h1 className="text-xl font-bold text-gray-800">📋 Pedidos</h1>
        <RestauranteSelector />
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
        <StatCard label="Total" value={stats.total} icon="📊" />
        <StatCard label="Nuevos" value={stats.nuevos} icon="🆕" color="bg-blue-50 text-blue-700" />
        <StatCard label="En camino" value={stats.enCamino} icon="🛵" color="bg-orange-50 text-orange-700" />
        <StatCard label="Entregados" value={stats.entregados} icon="🏁" color="bg-green-50 text-green-700" />
      </div>

      {/* Filtros */}
      <div className="flex flex-wrap gap-2 mb-4">
        <button
          onClick={() => setFiltroEstado('')}
          className={`px-3 py-1 rounded-full text-xs font-medium transition ${
            filtroEstado === '' ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          Todos
        </button>
        {Object.values(EstadoPedido).map((e) => (
          <button
            key={e}
            onClick={() => setFiltroEstado(e)}
            className={`px-3 py-1 rounded-full text-xs font-medium transition ${
              filtroEstado === e ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {e}
          </button>
        ))}
      </div>

      {/* Tabla */}
      {isLoading ? (
        <p className="text-gray-500">Cargando pedidos…</p>
      ) : filtered.length === 0 ? (
        <p className="text-gray-400 text-center py-8">No hay pedidos</p>
      ) : (
        <div className="overflow-x-auto bg-white rounded-xl shadow">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-600">
              <tr>
                <th className="px-4 py-3 text-left">#</th>
                <th className="px-4 py-3 text-left">Cliente</th>
                <th className="px-4 py-3 text-left">Dirección</th>
                <th className="px-4 py-3 text-right">Total</th>
                <th className="px-4 py-3 text-center">Estado</th>
                <th className="px-4 py-3 text-center">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {filtered.map((p) => (
                <tr key={p.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-xs">{p.id}</td>
                  <td className="px-4 py-3">
                    <p className="font-medium">{p.clienteNombre || p.clienteTelefono}</p>
                    <p className="text-xs text-gray-400">{p.clienteTelefono}</p>
                  </td>
                  <td className="px-4 py-3 text-gray-600 max-w-[200px] truncate">{p.direccion}</td>
                  <td className="px-4 py-3 text-right font-medium">${Number(p.total).toLocaleString()}</td>
                  <td className="px-4 py-3 text-center"><EstadoBadge estado={p.estado} /></td>
                  <td className="px-4 py-3 text-center">
                    <div className="flex items-center justify-center gap-1 flex-wrap">
                      <button
                        onClick={() => setDetalle(p)}
                        className="text-indigo-600 hover:underline text-xs"
                      >
                        Ver
                      </button>
                      {TRANSITIONS[p.estado]?.map((next) => (
                        <button
                          key={next}
                          onClick={() => cambiarEstado.mutate({ id: p.id, estado: next })}
                          disabled={cambiarEstado.isPending}
                          className={`text-xs px-2 py-1 rounded ${
                            next === EstadoPedido.CANCELADO
                              ? 'bg-red-50 text-red-600 hover:bg-red-100'
                              : 'bg-indigo-50 text-indigo-600 hover:bg-indigo-100'
                          }`}
                        >
                          {ACTION_LABELS[next]}
                        </button>
                      ))}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal detalle */}
      <Modal open={!!detalle} onClose={() => setDetalle(null)} title={`Pedido #${detalle?.id}`}>
        {detalle && (
          <div className="space-y-3 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-500">Estado</span>
              <EstadoBadge estado={detalle.estado} />
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">Cliente</span>
              <span>{detalle.clienteNombre} ({detalle.clienteTelefono})</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">Dirección</span>
              <span className="text-right max-w-[200px]">{detalle.direccion}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">Fecha</span>
              <span>{new Date(detalle.fecha).toLocaleString()}</span>
            </div>
            <hr />
            <h4 className="font-medium">Productos</h4>
            <ul className="space-y-1">
              {detalle.detalles.map((d, i) => (
                <li key={i} className="flex justify-between">
                  <span>{d.cantidad}x {d.producto}</span>
                  <span className="font-medium">${Number(d.precio).toLocaleString()}</span>
                </li>
              ))}
            </ul>
            <hr />
            <div className="flex justify-between font-bold">
              <span>Total</span>
              <span>${Number(detalle.total).toLocaleString()}</span>
            </div>
            {detalle.trackingToken && (
              <p className="text-xs text-gray-400 break-all">Tracking: {detalle.trackingToken}</p>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}

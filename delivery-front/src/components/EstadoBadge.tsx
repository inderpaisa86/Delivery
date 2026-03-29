import { EstadoPedido } from '../types';

const colors: Record<EstadoPedido, string> = {
  [EstadoPedido.NUEVO]: 'bg-blue-100 text-blue-800',
  [EstadoPedido.CONFIRMADO]: 'bg-indigo-100 text-indigo-800',
  [EstadoPedido.PREPARANDO]: 'bg-yellow-100 text-yellow-800',
  [EstadoPedido.LISTO]: 'bg-purple-100 text-purple-800',
  [EstadoPedido.EN_CAMINO]: 'bg-orange-100 text-orange-800',
  [EstadoPedido.ENTREGADO]: 'bg-green-100 text-green-800',
  [EstadoPedido.CANCELADO]: 'bg-red-100 text-red-800',
};

const labels: Record<EstadoPedido, string> = {
  [EstadoPedido.NUEVO]: '🆕 Nuevo',
  [EstadoPedido.CONFIRMADO]: '✅ Confirmado',
  [EstadoPedido.PREPARANDO]: '👨‍🍳 Preparando',
  [EstadoPedido.LISTO]: '📦 Listo',
  [EstadoPedido.EN_CAMINO]: '🛵 En camino',
  [EstadoPedido.ENTREGADO]: '🏁 Entregado',
  [EstadoPedido.CANCELADO]: '❌ Cancelado',
};

export function EstadoBadge({ estado }: { estado: EstadoPedido }) {
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${colors[estado]}`}>
      {labels[estado]}
    </span>
  );
}

import { EstadoPedido } from '../types';

const styles: Record<EstadoPedido, string> = {
  [EstadoPedido.NUEVO]: 'bg-blue-50 text-blue-600 border-blue-200',
  [EstadoPedido.CONFIRMADO]: 'bg-indigo-50 text-indigo-600 border-indigo-200',
  [EstadoPedido.PREPARANDO]: 'bg-amber-50 text-amber-600 border-amber-200',
  [EstadoPedido.LISTO]: 'bg-purple-50 text-purple-600 border-purple-200',
  [EstadoPedido.EN_CAMINO]: 'bg-orange-50 text-orange-600 border-orange-200',
  [EstadoPedido.ENTREGADO]: 'bg-green-50 text-green-600 border-green-200',
  [EstadoPedido.CANCELADO]: 'bg-red-50 text-red-600 border-red-200',
};
const icons: Record<EstadoPedido, string> = {
  [EstadoPedido.NUEVO]: '🆕', [EstadoPedido.CONFIRMADO]: '✅', [EstadoPedido.PREPARANDO]: '👨‍🍳',
  [EstadoPedido.LISTO]: '📦', [EstadoPedido.EN_CAMINO]: '🛵', [EstadoPedido.ENTREGADO]: '🏁', [EstadoPedido.CANCELADO]: '❌',
};

export function EstadoBadge({ estado }: { estado: EstadoPedido }) {
  return (
    <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold border ${styles[estado]}`}>
      {icons[estado]} {estado.replace('_', ' ')}
    </span>
  );
}

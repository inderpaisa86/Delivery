import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { pedidoService } from '../../services/pedidos';
import { useRestaurante } from '../../context/RestauranteContext';
import { useToast } from '../../components/Toast';
import { EstadoBadge } from '../../components/EstadoBadge';
import { StatCard } from '../../components/StatCard';
import { Modal } from '../../components/Modal';
import { EstadoPedido, type PedidoResponse } from '../../types';

const FLOW: EstadoPedido[] = [EstadoPedido.NUEVO, EstadoPedido.CONFIRMADO, EstadoPedido.PREPARANDO, EstadoPedido.LISTO, EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO];
const STEP_ICONS = ['🆕', '✅', '👨‍🍳', '📦', '🛵', '🏁'];
const NEXT: Partial<Record<EstadoPedido, { estado: EstadoPedido; label: string }>> = {
  [EstadoPedido.NUEVO]: { estado: EstadoPedido.CONFIRMADO, label: 'Confirmar' },
  [EstadoPedido.CONFIRMADO]: { estado: EstadoPedido.PREPARANDO, label: 'Preparar' },
  [EstadoPedido.PREPARANDO]: { estado: EstadoPedido.LISTO, label: 'Listo' },
  [EstadoPedido.LISTO]: { estado: EstadoPedido.EN_CAMINO, label: 'Despachar' },
  [EstadoPedido.EN_CAMINO]: { estado: EstadoPedido.ENTREGADO, label: 'Entregado' },
};

function Stepper({ estado }: { estado: EstadoPedido }) {
  const idx = FLOW.indexOf(estado);
  return (
    <div className="flex items-center gap-1 my-3">
      {FLOW.map((_, i) => (
        <div key={i} className="flex items-center flex-1">
          <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
            i <= idx ? 'bg-[var(--accent)] text-white shadow-[0_0_12px_var(--accent-glow)]' : 'bg-gray-100 text-gray-400'
          }`}>{STEP_ICONS[i]}</div>
          {i < FLOW.length - 1 && <div className={`flex-1 h-1 mx-1 rounded-full transition-all ${i < idx ? 'bg-[var(--accent)]' : 'bg-gray-100'}`} />}
        </div>
      ))}
    </div>
  );
}

export function PedidosPage() {
  const { restaurante } = useRestaurante();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [filtro, setFiltro] = useState<EstadoPedido | ''>('');
  const [detalle, setDetalle] = useState<PedidoResponse | null>(null);
  const prevRef = useRef(0);

  const { data, isLoading } = useQuery({ queryKey: ['pedidos', restaurante?.id], queryFn: () => pedidoService.listarHoy(restaurante!.id), refetchInterval: 5000, enabled: !!restaurante });
  const pedidos = data?.content ?? [];

  useEffect(() => { const n = pedidos.filter((p) => p.estado === EstadoPedido.NUEVO).length; if (n > prevRef.current && prevRef.current > 0) toast('🔔 Nuevo pedido', 'info'); prevRef.current = n; }, [pedidos, toast]);

  const cambiar = useMutation({
    mutationFn: ({ id, estado }: { id: number; estado: EstadoPedido }) => pedidoService.cambiarEstado(id, estado),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['pedidos'] }); toast('Actualizado', 'success'); },
    onError: (err: Error) => toast(err.message, 'error'),
  });

  const filtered = filtro ? pedidos.filter((p) => p.estado === filtro) : pedidos;
  const s = { total: pedidos.length, nuevos: pedidos.filter((p) => p.estado === EstadoPedido.NUEVO).length, enCamino: pedidos.filter((p) => p.estado === EstadoPedido.EN_CAMINO).length, entregados: pedidos.filter((p) => p.estado === EstadoPedido.ENTREGADO).length };

  if (!restaurante) return <div className="animate-fade-up"><h1 className="text-2xl font-bold mb-4">Pedidos</h1><p className="text-[var(--text-muted)]">Selecciona un restaurante.</p></div>;

  return (
    <div className="animate-fade-up">
      <div className="flex items-center justify-between mb-6">
        <div><h1 className="text-2xl font-bold">📋 Pedidos del día</h1><p className="text-sm text-[var(--text-muted)] mt-1">Gestión en tiempo real</p></div>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard label="Total hoy" value={s.total} icon="📊" color="from-slate-500 to-slate-600" />
        <StatCard label="Nuevos" value={s.nuevos} icon="🆕" color="from-blue-500 to-blue-600" />
        <StatCard label="En camino" value={s.enCamino} icon="🛵" color="from-orange-500 to-orange-600" />
        <StatCard label="Entregados" value={s.entregados} icon="🏁" color="from-green-500 to-green-600" />
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        <button onClick={() => setFiltro('')} className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${filtro === '' ? 'bg-[var(--accent)] text-white btn-glow' : 'bg-white text-[var(--text-secondary)] border border-[var(--border)] hover:border-[var(--border-hover)]'}`}>Todos ({pedidos.length})</button>
        {Object.values(EstadoPedido).map((e) => { const c = pedidos.filter((p) => p.estado === e).length; if (!c) return null; return (
          <button key={e} onClick={() => setFiltro(e)} className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${filtro === e ? 'bg-[var(--accent)] text-white btn-glow' : 'bg-white text-[var(--text-secondary)] border border-[var(--border)] hover:border-[var(--border-hover)]'}`}>{e.replace('_',' ')} ({c})</button>
        ); })}
      </div>

      {isLoading ? <p className="text-[var(--text-muted)]">Cargando…</p> : filtered.length === 0 ? (
        <div className="text-center py-20"><span className="text-6xl block mb-4">📭</span><p className="text-[var(--text-muted)] text-lg">No hay pedidos</p></div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {filtered.map((p) => (
            <div key={p.id} className="bg-white rounded-2xl border border-[var(--border)] shadow-[var(--shadow-sm)] hover:shadow-[var(--shadow-lg)] transition-all cursor-pointer group" onClick={() => setDetalle(p)}>
              <div className="p-5">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-lg font-bold text-[var(--accent)]">#{p.id}</span>
                  <span className="text-xs text-[var(--text-muted)]">{new Date(p.fecha).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })}</span>
                </div>
                <Stepper estado={p.estado} />
                <div className="flex items-center justify-between mb-3">
                  <EstadoBadge estado={p.estado} />
                  <span className="text-xl font-bold">${Number(p.total).toLocaleString()}</span>
                </div>
                <p className="text-sm font-semibold">{p.clienteNombre || p.clienteTelefono}</p>
                <p className="text-xs text-[var(--text-muted)] truncate">📍 {p.direccion}</p>
                <p className="text-xs text-[var(--text-muted)] mt-1">{p.detalles.map((d, i) => <span key={i}>{i > 0 && ' · '}{d.cantidad}x {d.producto}</span>)}</p>
              </div>
              <div className="border-t border-[var(--border)] px-5 py-3 flex gap-2">
                {NEXT[p.estado] && (
                  <button onClick={(e) => { e.stopPropagation(); cambiar.mutate({ id: p.id, estado: NEXT[p.estado]!.estado }); }}
                    disabled={cambiar.isPending}
                    className="flex-1 bg-[var(--accent)] text-white text-sm py-2 rounded-xl font-medium hover:brightness-110 transition btn-glow disabled:opacity-50">
                    {NEXT[p.estado]!.label}
                  </button>
                )}
                {p.estado !== EstadoPedido.ENTREGADO && p.estado !== EstadoPedido.CANCELADO && (
                  <button onClick={(e) => { e.stopPropagation(); cambiar.mutate({ id: p.id, estado: EstadoPedido.CANCELADO }); }}
                    disabled={cambiar.isPending} className="bg-red-50 text-red-500 text-sm px-3 py-2 rounded-xl hover:bg-red-100 transition font-medium">✕</button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={!!detalle} onClose={() => setDetalle(null)} title={`Pedido #${detalle?.id}`}>
        {detalle && (
          <div className="space-y-4 text-sm">
            <Stepper estado={detalle.estado} />
            <div className="flex justify-between"><span className="text-[var(--text-muted)]">Estado</span><EstadoBadge estado={detalle.estado} /></div>
            <div className="flex justify-between"><span className="text-[var(--text-muted)]">Cliente</span><span className="font-medium">{detalle.clienteNombre} ({detalle.clienteTelefono})</span></div>
            <div className="flex justify-between"><span className="text-[var(--text-muted)]">Dirección</span><span className="text-right max-w-[220px]">{detalle.direccion}</span></div>
            <div className="flex justify-between"><span className="text-[var(--text-muted)]">Hora</span><span>{new Date(detalle.fecha).toLocaleString()}</span></div>
            <hr className="border-[var(--border)]" />
            {detalle.detalles.map((d, i) => <div key={i} className="flex justify-between py-1"><span>{d.cantidad}x {d.producto}</span><span className="font-semibold text-[var(--accent)]">${Number(d.precio).toLocaleString()}</span></div>)}
            <hr className="border-[var(--border)]" />
            <div className="flex justify-between text-lg font-bold"><span>Total</span><span className="text-[var(--accent)]">${Number(detalle.total).toLocaleString()}</span></div>
          </div>
        )}
      </Modal>
    </div>
  );
}

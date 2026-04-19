import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { pedidoService } from '../../services/pedidos';
import { useRestaurante } from '../../context/RestauranteContext';
import { EstadoBadge } from '../../components/EstadoBadge';
import { Modal } from '../../components/Modal';
import type { PedidoResponse } from '../../types';

function today() { return new Date().toISOString().split('T')[0]; }

export function HistorialPage() {
  const { restaurante } = useRestaurante();
  const [desde, setDesde] = useState(today());
  const [hasta, setHasta] = useState(today());
  const [detalle, setDetalle] = useState<PedidoResponse | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['historial', restaurante?.id, desde, hasta],
    queryFn: () => pedidoService.listarHistorial(restaurante!.id, desde, hasta),
    enabled: !!restaurante && !!desde && !!hasta,
  });

  const pedidos = data?.content ?? [];
  const totalVentas = pedidos.reduce((sum, p) => sum + Number(p.total), 0);

  if (!restaurante) return <div className="animate-fade-up"><h1 className="text-2xl font-bold mb-4">Historial</h1><p className="text-[var(--text-muted)]">Selecciona un restaurante.</p></div>;

  const inputClass = 'bg-white border border-[var(--border)] rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-[var(--accent)] outline-none';

  return (
    <div className="animate-fade-up">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold">📊 Historial de Pedidos</h1>
          <p className="text-sm text-[var(--text-muted)] mt-1">Consulta pedidos por rango de fechas</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <label className="text-xs font-semibold text-[var(--text-muted)]">Desde</label>
            <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} className={inputClass} />
          </div>
          <div className="flex items-center gap-2">
            <label className="text-xs font-semibold text-[var(--text-muted)]">Hasta</label>
            <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} className={inputClass} />
          </div>
        </div>
      </div>

      {/* Resumen */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-white rounded-2xl border border-[var(--border)] shadow-[var(--shadow-sm)] p-5">
          <p className="text-3xl font-bold">{pedidos.length}</p>
          <p className="text-sm text-[var(--text-muted)]">Total pedidos</p>
        </div>
        <div className="bg-white rounded-2xl border border-[var(--border)] shadow-[var(--shadow-sm)] p-5">
          <p className="text-3xl font-bold text-[var(--accent)]">${totalVentas.toLocaleString()}</p>
          <p className="text-sm text-[var(--text-muted)]">Total ventas</p>
        </div>
        <div className="bg-white rounded-2xl border border-[var(--border)] shadow-[var(--shadow-sm)] p-5">
          <p className="text-3xl font-bold">{pedidos.length > 0 ? Math.round(totalVentas / pedidos.length).toLocaleString() : 0}</p>
          <p className="text-sm text-[var(--text-muted)]">Promedio por pedido</p>
        </div>
      </div>

      {/* Tabla */}
      {isLoading ? <p className="text-[var(--text-muted)]">Cargando…</p> : pedidos.length === 0 ? (
        <div className="text-center py-16"><span className="text-5xl block mb-3">📭</span><p className="text-[var(--text-muted)]">No hay pedidos en este rango</p></div>
      ) : (
        <div className="bg-white rounded-2xl border border-[var(--border)] shadow-[var(--shadow-sm)] overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-[var(--text-muted)]">
              <tr>
                <th className="px-4 py-3 text-left">#</th>
                <th className="px-4 py-3 text-left">Cliente</th>
                <th className="px-4 py-3 text-left">Teléfono</th>
                <th className="px-4 py-3 text-left">Domiciliario</th>
                <th className="px-4 py-3 text-left">Hora pedido</th>
                <th className="px-4 py-3 text-left">Hora entrega</th>
                <th className="px-4 py-3 text-center">Estado</th>
                <th className="px-4 py-3 text-right">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border)]">
              {pedidos.map((p) => {
                const horaFmt = (d: string | null) => d ? new Date(d).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' }) : '—';
                return (
                <tr key={p.id} className="hover:bg-gray-50 cursor-pointer transition" onClick={() => setDetalle(p)}>
                  <td className="px-4 py-3 font-mono text-xs font-bold text-[var(--accent)]">{p.numeroDiario ?? p.id}</td>
                  <td className="px-4 py-3 font-medium">{p.clienteNombre || 'Sin nombre'}</td>
                  <td className="px-4 py-3 text-[var(--text-secondary)]">{p.clienteTelefono}</td>
                  <td className="px-4 py-3 text-[var(--text-secondary)]">{p.domiciliarioNombre || '—'}</td>
                  <td className="px-4 py-3 text-[var(--text-secondary)]">{horaFmt(p.fecha)}</td>
                  <td className="px-4 py-3 text-[var(--text-secondary)]">{horaFmt(p.fechaEntrega)}</td>
                  <td className="px-4 py-3 text-center"><EstadoBadge estado={p.estado} /></td>
                  <td className="px-4 py-3 text-right font-bold">${Number(p.total).toLocaleString()}</td>
                </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal detalle */}
      <Modal open={!!detalle} onClose={() => setDetalle(null)} title={`Pedido #${detalle?.numeroDiario ?? detalle?.id} — ${detalle?.clienteNombre || detalle?.clienteTelefono}`}>
        {detalle && (
          <div className="space-y-3 text-sm">
            <div className="flex justify-between"><span className="text-[var(--text-muted)]">Estado</span><EstadoBadge estado={detalle.estado} /></div>
            <div className="flex justify-between"><span className="text-[var(--text-muted)]">Cliente</span><span className="font-medium">{detalle.clienteNombre || 'Sin nombre'}</span></div>
            <div className="flex justify-between"><span className="text-[var(--text-muted)]">Teléfono</span><span>{detalle.clienteTelefono}</span></div>
            <div className="flex justify-between"><span className="text-[var(--text-muted)]">Dirección</span><span className="text-right max-w-[220px]">{detalle.direccion}</span></div>
            <div className="flex justify-between"><span className="text-[var(--text-muted)]">Domiciliario</span><span className="font-medium">{detalle.domiciliarioNombre || 'Sin asignar'}</span></div>
            <hr className="border-[var(--border)]" />
            <div className="flex justify-between"><span className="text-[var(--text-muted)]">Hora pedido</span><span>{new Date(detalle.fecha).toLocaleString()}</span></div>
            {detalle.fechaAsignacion && <div className="flex justify-between"><span className="text-[var(--text-muted)]">Hora asignación</span><span>{new Date(detalle.fechaAsignacion).toLocaleString()}</span></div>}
            {detalle.fechaEntrega && <div className="flex justify-between"><span className="text-[var(--text-muted)]">Hora entrega</span><span>{new Date(detalle.fechaEntrega).toLocaleString()}</span></div>}
            {detalle.fechaEntrega && detalle.fechaAsignacion && (
              <div className="flex justify-between"><span className="text-[var(--text-muted)]">Tiempo de entrega</span><span className="font-semibold text-[var(--accent)]">{Math.round((new Date(detalle.fechaEntrega).getTime() - new Date(detalle.fechaAsignacion).getTime()) / 60000)} min</span></div>
            )}
            <hr className="border-[var(--border)]" />
            {detalle.detalles.map((d, i) => (
              <div key={i} className="flex justify-between py-1">
                <span>{d.cantidad}x {d.producto}</span>
                <span className="font-semibold text-[var(--accent)]">${(d.cantidad * d.precio).toLocaleString()}</span>
              </div>
            ))}
            <hr className="border-[var(--border)]" />
            <div className="flex justify-between text-lg font-bold"><span>Total</span><span className="text-[var(--accent)]">${Number(detalle.total).toLocaleString()}</span></div>
          </div>
        )}
      </Modal>
    </div>
  );
}

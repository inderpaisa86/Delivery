import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productoService } from '../../services/productos';
import { useRestaurante } from '../../context/RestauranteContext';
import { useToast } from '../../components/Toast';
import { Modal } from '../../components/Modal';
import type { ProductoResponse } from '../../types';

const inputClass = 'w-full bg-[var(--bg-primary)] border border-[var(--border)] text-[var(--text-primary)] rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-[var(--accent)] outline-none transition';

export function ProductosPage() {
  const { restaurante } = useRestaurante();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editando, setEditando] = useState<ProductoResponse | null>(null);
  const [nombre, setNombre] = useState(''); const [precio, setPrecio] = useState('');

  const { data, isLoading } = useQuery({ queryKey: ['productos', restaurante?.id], queryFn: () => productoService.listar(restaurante!.id), enabled: !!restaurante });
  const productos = data?.content ?? [];
  const crear = useMutation({ mutationFn: () => productoService.crear({ restauranteId: restaurante!.id, nombre, precio: Number(precio) }), onSuccess: () => { qc.invalidateQueries({ queryKey: ['productos'] }); toast('Creado', 'success'); closeForm(); }, onError: (e: Error) => toast(e.message, 'error') });
  const actualizar = useMutation({ mutationFn: () => productoService.actualizar(editando!.id, { restauranteId: restaurante!.id, nombre, precio: Number(precio) }), onSuccess: () => { qc.invalidateQueries({ queryKey: ['productos'] }); toast('Actualizado', 'success'); closeForm(); }, onError: (e: Error) => toast(e.message, 'error') });
  const desactivar = useMutation({ mutationFn: (id: number) => productoService.desactivar(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['productos'] }); toast('Desactivado', 'success'); }, onError: (e: Error) => toast(e.message, 'error') });
  const closeForm = () => { setShowForm(false); setEditando(null); setNombre(''); setPrecio(''); };
  const openEdit = (p: ProductoResponse) => { setEditando(p); setNombre(p.nombre); setPrecio(String(p.precio)); setShowForm(true); };

  if (!restaurante) return <div className="animate-fade-up"><h1 className="text-2xl font-bold mb-4">Productos</h1><p className="text-[var(--text-muted)]">Selecciona un restaurante.</p></div>;

  return (
    <div className="animate-fade-up">
      <div className="flex items-center justify-between mb-6">
        <div><h1 className="text-2xl font-bold">🍔 Productos</h1><p className="text-sm text-[var(--text-muted)] mt-1">Gestiona el menú</p></div>
        <button onClick={() => { closeForm(); setShowForm(true); }} className="bg-[var(--accent)] text-white px-5 py-2.5 rounded-xl text-sm font-semibold hover:brightness-110 transition btn-glow">+ Nuevo</button>
      </div>
      {isLoading ? <p className="text-[var(--text-muted)]">Cargando…</p> : !productos.length ? <div className="text-center py-20"><span className="text-6xl block mb-4">🍽️</span><p className="text-[var(--text-muted)]">No hay productos</p></div> : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {productos.map((p) => (
            <div key={p.id} className={`bg-white rounded-2xl border border-[var(--border)] shadow-[var(--shadow-sm)] hover:shadow-[var(--shadow-md)] transition-all p-5 ${!p.activo ? 'opacity-50' : ''}`}>
              <div className="flex justify-between items-start mb-3">
                <div><h3 className="font-bold text-lg">{p.nombre}</h3><p className="text-2xl font-bold text-[var(--accent)] mt-1">${Number(p.precio).toLocaleString()}</p></div>
                <span className={`text-xs px-3 py-1 rounded-full font-semibold ${p.activo ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-500'}`}>{p.activo ? 'Activo' : 'Inactivo'}</span>
              </div>
              <div className="flex gap-3 pt-3 border-t border-[var(--border)]">
                <button onClick={() => openEdit(p)} className="text-sm text-[var(--accent)] font-medium hover:underline">Editar</button>
                {p.activo && <button onClick={() => desactivar.mutate(p.id)} className="text-sm text-red-500 font-medium hover:underline">Desactivar</button>}
              </div>
            </div>
          ))}
        </div>
      )}
      <Modal open={showForm} onClose={closeForm} title={editando ? 'Editar Producto' : 'Nuevo Producto'}>
        <form onSubmit={(e) => { e.preventDefault(); editando ? actualizar.mutate() : crear.mutate(); }} className="space-y-4">
          <div><label className="block text-sm font-semibold text-[var(--text-secondary)] mb-1.5">Nombre</label><input value={nombre} onChange={(e) => setNombre(e.target.value)} className={inputClass} required /></div>
          <div><label className="block text-sm font-semibold text-[var(--text-secondary)] mb-1.5">Precio</label><input type="number" step="0.01" min="0" value={precio} onChange={(e) => setPrecio(e.target.value)} className={inputClass} required /></div>
          <button type="submit" className="w-full bg-[var(--accent)] text-white py-3 rounded-xl text-sm font-semibold hover:brightness-110 transition btn-glow">{editando ? 'Actualizar' : 'Crear'}</button>
        </form>
      </Modal>
    </div>
  );
}

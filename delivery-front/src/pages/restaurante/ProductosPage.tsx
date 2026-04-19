import { useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productoService } from '../../services/productos';
import { useRestaurante } from '../../context/RestauranteContext';
import { useToast } from '../../components/Toast';
import { Modal } from '../../components/Modal';
import type { ProductoResponse } from '../../types';
import * as XLSX from 'xlsx';

const inputClass = 'w-full bg-[var(--bg-primary)] border border-[var(--border)] text-[var(--text-primary)] rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-[var(--accent)] outline-none transition';

export function ProductosPage() {
  const { restaurante } = useRestaurante();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editando, setEditando] = useState<ProductoResponse | null>(null);
  const [nombre, setNombre] = useState('');
  const [precio, setPrecio] = useState('');
  const [uploading, setUploading] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [excelData, setExcelData] = useState<{ nombre: string; precio: number }[]>([]);
  const fileRef = useRef<HTMLInputElement>(null);

  const { data, isLoading } = useQuery({ queryKey: ['productos', restaurante?.id], queryFn: () => productoService.listar(restaurante!.id), enabled: !!restaurante });
  const productos = data?.content ?? [];

  const crear = useMutation({ mutationFn: () => productoService.crear({ restauranteId: restaurante!.id, nombre, precio: Number(precio) }), onSuccess: () => { qc.invalidateQueries({ queryKey: ['productos'] }); toast('Creado', 'success'); closeForm(); }, onError: (e: Error) => toast(e.message, 'error') });
  const actualizar = useMutation({ mutationFn: () => productoService.actualizar(editando!.id, { restauranteId: restaurante!.id, nombre, precio: Number(precio) }), onSuccess: () => { qc.invalidateQueries({ queryKey: ['productos'] }); toast('Actualizado', 'success'); closeForm(); }, onError: (e: Error) => toast(e.message, 'error') });
  const desactivar = useMutation({ mutationFn: (id: number) => productoService.desactivar(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['productos'] }); toast('Desactivado', 'success'); }, onError: (e: Error) => toast(e.message, 'error') });

  const closeForm = () => { setShowForm(false); setEditando(null); setNombre(''); setPrecio(''); };
  const openEdit = (p: ProductoResponse) => { setEditando(p); setNombre(p.nombre); setPrecio(String(p.precio)); setShowForm(true); };

  // ── Excel upload ──
  const handleExcelFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (evt) => {
      try {
        const wb = XLSX.read(evt.target?.result, { type: 'binary' });
        const sheet = wb.Sheets[wb.SheetNames[0]];
        const rows = XLSX.utils.sheet_to_json<Record<string, unknown>>(sheet);

        const parsed: { nombre: string; precio: number }[] = [];
        for (const row of rows) {
          // Buscar columnas flexiblemente (nombre/producto, precio/valor)
          const nombre = String(row['nombre'] || row['Nombre'] || row['producto'] || row['Producto'] || '').trim();
          const precioRaw = row['precio'] || row['Precio'] || row['valor'] || row['Valor'] || 0;
          const precio = Number(precioRaw);

          if (nombre && precio > 0) {
            parsed.push({ nombre, precio });
          }
        }

        if (parsed.length === 0) {
          toast('No se encontraron productos válidos. El Excel debe tener columnas "nombre" y "precio".', 'error');
          return;
        }

        setExcelData(parsed);
        setShowPreview(true);
      } catch {
        toast('Error leyendo el archivo Excel', 'error');
      }
    };
    reader.readAsBinaryString(file);
    if (fileRef.current) fileRef.current.value = '';
  };

  const uploadExcelProducts = async () => {
    if (!restaurante) return;
    setUploading(true);
    let created = 0;
    let errors = 0;

    for (const item of excelData) {
      try {
        await productoService.crear({ restauranteId: restaurante.id, nombre: item.nombre, precio: item.precio });
        created++;
      } catch {
        errors++;
      }
    }

    setUploading(false);
    setShowPreview(false);
    setExcelData([]);
    qc.invalidateQueries({ queryKey: ['productos'] });
    toast(`✅ ${created} productos creados${errors > 0 ? `, ${errors} errores` : ''}`, created > 0 ? 'success' : 'error');
  };

  if (!restaurante) return <div className="animate-fade-up"><h1 className="text-2xl font-bold mb-4">Productos</h1><p className="text-[var(--text-muted)]">Selecciona un restaurante.</p></div>;

  return (
    <div className="animate-fade-up">
      <div className="flex items-center justify-between mb-6">
        <div><h1 className="text-2xl font-bold">🍔 Productos</h1><p className="text-sm text-[var(--text-muted)] mt-1">Gestiona el menú</p></div>
        <div className="flex gap-2">
          <input ref={fileRef} type="file" accept=".xlsx,.xls,.csv" onChange={handleExcelFile} className="hidden" />
          <button onClick={() => fileRef.current?.click()} className="bg-white border border-[var(--border)] text-[var(--text-secondary)] px-4 py-2.5 rounded-xl text-sm font-medium hover:border-[var(--accent)] hover:text-[var(--accent)] transition">
            📊 Cargar Excel
          </button>
          <button onClick={() => { closeForm(); setShowForm(true); }} className="bg-[var(--accent)] text-white px-5 py-2.5 rounded-xl text-sm font-semibold hover:brightness-110 transition btn-glow">+ Nuevo</button>
        </div>
      </div>

      {/* Formato esperado */}
      <div className="bg-blue-50 border border-blue-200 rounded-xl p-3 mb-6 text-xs text-blue-700 hidden" id="excel-help">
        El Excel debe tener columnas: <strong>nombre</strong> y <strong>precio</strong>
      </div>

      {isLoading ? <p className="text-[var(--text-muted)]">Cargando…</p> : !productos.length ? <div className="text-center py-20"><span className="text-6xl block mb-4">🍽️</span><p className="text-[var(--text-muted)]">No hay productos</p><p className="text-xs text-[var(--text-muted)] mt-2">Crea productos manualmente o carga un Excel con columnas "nombre" y "precio"</p></div> : (
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

      {/* Modal crear/editar */}
      <Modal open={showForm} onClose={closeForm} title={editando ? 'Editar Producto' : 'Nuevo Producto'}>
        <form onSubmit={(e) => { e.preventDefault(); editando ? actualizar.mutate() : crear.mutate(); }} className="space-y-4">
          <div><label className="block text-sm font-semibold text-[var(--text-secondary)] mb-1.5">Nombre</label><input value={nombre} onChange={(e) => setNombre(e.target.value)} className={inputClass} required /></div>
          <div><label className="block text-sm font-semibold text-[var(--text-secondary)] mb-1.5">Precio</label><input type="number" step="0.01" min="0" value={precio} onChange={(e) => setPrecio(e.target.value)} className={inputClass} required /></div>
          <button type="submit" className="w-full bg-[var(--accent)] text-white py-3 rounded-xl text-sm font-semibold hover:brightness-110 transition btn-glow">{editando ? 'Actualizar' : 'Crear'}</button>
        </form>
      </Modal>

      {/* Modal preview Excel */}
      <Modal open={showPreview} onClose={() => { setShowPreview(false); setExcelData([]); }} title={`📊 Cargar ${excelData.length} productos desde Excel`}>
        <div className="space-y-4">
          <div className="max-h-60 overflow-y-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-[var(--text-muted)] sticky top-0">
                <tr>
                  <th className="px-3 py-2 text-left">#</th>
                  <th className="px-3 py-2 text-left">Nombre</th>
                  <th className="px-3 py-2 text-right">Precio</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--border)]">
                {excelData.map((item, i) => (
                  <tr key={i}>
                    <td className="px-3 py-2 text-[var(--text-muted)]">{i + 1}</td>
                    <td className="px-3 py-2 font-medium">{item.nombre}</td>
                    <td className="px-3 py-2 text-right text-[var(--accent)] font-semibold">${item.precio.toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex gap-3">
            <button onClick={() => { setShowPreview(false); setExcelData([]); }} className="flex-1 bg-gray-100 text-[var(--text-secondary)] py-2.5 rounded-xl text-sm font-medium hover:bg-gray-200 transition">
              Cancelar
            </button>
            <button onClick={uploadExcelProducts} disabled={uploading} className="flex-1 bg-[var(--accent)] text-white py-2.5 rounded-xl text-sm font-semibold hover:brightness-110 transition btn-glow disabled:opacity-50">
              {uploading ? `Creando... (${excelData.length})` : `Crear ${excelData.length} productos`}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

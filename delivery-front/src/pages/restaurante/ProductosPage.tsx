import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productoService } from '../../services/productos';
import { useRestaurante } from '../../context/RestauranteContext';
import { useToast } from '../../components/Toast';
import { Modal } from '../../components/Modal';
import { RestauranteSelector } from '../../components/RestauranteSelector';
import type { ProductoResponse } from '../../types';

export function ProductosPage() {
  const { restaurante } = useRestaurante();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editando, setEditando] = useState<ProductoResponse | null>(null);
  const [nombre, setNombre] = useState('');
  const [precio, setPrecio] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['productos', restaurante?.id],
    queryFn: () => productoService.listar(restaurante!.id),
    enabled: !!restaurante,
  });

  const productos = data?.content ?? [];

  const crear = useMutation({
    mutationFn: () =>
      productoService.crear({ restauranteId: restaurante!.id, nombre, precio: Number(precio) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'] });
      toast('Producto creado', 'success');
      closeForm();
    },
    onError: (err: Error) => toast(err.message, 'error'),
  });

  const actualizar = useMutation({
    mutationFn: () =>
      productoService.actualizar(editando!.id, {
        restauranteId: restaurante!.id,
        nombre,
        precio: Number(precio),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'] });
      toast('Producto actualizado', 'success');
      closeForm();
    },
    onError: (err: Error) => toast(err.message, 'error'),
  });

  const desactivar = useMutation({
    mutationFn: (id: number) => productoService.desactivar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productos'] });
      toast('Producto desactivado', 'success');
    },
    onError: (err: Error) => toast(err.message, 'error'),
  });

  const closeForm = () => {
    setShowForm(false);
    setEditando(null);
    setNombre('');
    setPrecio('');
  };

  const openEdit = (p: ProductoResponse) => {
    setEditando(p);
    setNombre(p.nombre);
    setPrecio(String(p.precio));
    setShowForm(true);
  };

  if (!restaurante) {
    return (
      <div className="p-6">
        <h1 className="text-xl font-bold mb-4">Productos</h1>
        <RestauranteSelector />
      </div>
    );
  }

  return (
    <div className="p-4 md:p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-bold text-gray-800">🍔 Productos</h1>
        <div className="flex gap-2">
          <RestauranteSelector />
          <button
            onClick={() => { closeForm(); setShowForm(true); }}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-indigo-700"
          >
            + Nuevo
          </button>
        </div>
      </div>

      {isLoading ? (
        <p className="text-gray-500">Cargando…</p>
      ) : productos.length === 0 ? (
        <p className="text-gray-400 text-center py-8">No hay productos</p>
      ) : (
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {productos.map((p) => (
            <div key={p.id} className={`bg-white rounded-xl shadow p-4 ${!p.activo ? 'opacity-50' : ''}`}>
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="font-medium">{p.nombre}</h3>
                  <p className="text-lg font-bold text-indigo-600">${Number(p.precio).toLocaleString()}</p>
                </div>
                <span className={`text-xs px-2 py-0.5 rounded-full ${p.activo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                  {p.activo ? 'Activo' : 'Inactivo'}
                </span>
              </div>
              <div className="flex gap-2 mt-3">
                <button onClick={() => openEdit(p)} className="text-xs text-indigo-600 hover:underline">Editar</button>
                {p.activo && (
                  <button onClick={() => desactivar.mutate(p.id)} className="text-xs text-red-600 hover:underline">Desactivar</button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={showForm} onClose={closeForm} title={editando ? 'Editar Producto' : 'Nuevo Producto'}>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            editando ? actualizar.mutate() : crear.mutate();
          }}
          className="space-y-4"
        >
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Nombre</label>
            <input
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Precio</label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={precio}
              onChange={(e) => setPrecio(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full bg-indigo-600 text-white py-2 rounded-lg text-sm hover:bg-indigo-700"
          >
            {editando ? 'Actualizar' : 'Crear'}
          </button>
        </form>
      </Modal>
    </div>
  );
}

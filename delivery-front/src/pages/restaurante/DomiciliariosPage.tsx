import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { domiciliarioService } from '../../services/domiciliarios';
import { useRestaurante } from '../../context/RestauranteContext';
import { useToast } from '../../components/Toast';
import { Modal } from '../../components/Modal';
import { RestauranteSelector } from '../../components/RestauranteSelector';
import { AvatarUpload, DEFAULT_AVATAR } from '../../components/AvatarUpload';
import type { DomiciliarioResponse } from '../../types';

export function DomiciliariosPage() {
  const { restaurante } = useRestaurante();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editando, setEditando] = useState<DomiciliarioResponse | null>(null);
  const [nombre, setNombre] = useState('');
  const [cedula, setCedula] = useState('');
  const [telefono, setTelefono] = useState('');
  const [foto, setFoto] = useState<string | null>(null);
  const [formError, setFormError] = useState('');

  const { data: domiciliarios, isLoading } = useQuery({
    queryKey: ['domiciliarios', restaurante?.id],
    queryFn: () => domiciliarioService.disponibles(restaurante!.id),
    refetchInterval: 10000,
    enabled: !!restaurante,
  });

  const crear = useMutation({
    mutationFn: () => {
      setFormError('');
      return domiciliarioService.crear({
        restauranteId: restaurante!.id,
        nombre,
        cedula,
        foto,
        telefono,
        lat: 4.711,
        lng: -74.0721,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['domiciliarios', restaurante?.id] });
      toast('Domiciliario registrado', 'success');
      closeForm();
    },
    onError: (err: Error) => {
      const msg = err.message || 'Error al crear domiciliario';
      setFormError(msg);
      toast(msg, 'error');
    },
  });

  const actualizar = useMutation({
    mutationFn: () => {
      setFormError('');
      return domiciliarioService.actualizar(editando!.id, {
        restauranteId: restaurante!.id,
        nombre,
        cedula: editando!.cedula,
        foto,
        telefono,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['domiciliarios', restaurante?.id] });
      toast('Domiciliario actualizado', 'success');
      closeForm();
    },
    onError: (err: Error) => {
      const msg = err.message || 'Error al actualizar';
      setFormError(msg);
      toast(msg, 'error');
    },
  });

  const closeForm = () => {
    setShowForm(false);
    setEditando(null);
    setNombre('');
    setCedula('');
    setTelefono('');
    setFoto(null);
    setFormError('');
  };

  const openEdit = (d: DomiciliarioResponse) => {
    setEditando(d);
    setNombre(d.nombre);
    setCedula(d.cedula);
    setTelefono(d.telefono);
    setFoto(d.foto);
    setShowForm(true);
  };

  const isEditing = !!editando;
  const isPending = crear.isPending || actualizar.isPending;

  if (!restaurante) {
    return (
      <div className="p-6">
        <h1 className="text-xl font-bold mb-4">Domiciliarios</h1>
        <RestauranteSelector />
      </div>
    );
  }

  return (
    <div className="p-4 md:p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-bold text-gray-800">🛵 Domiciliarios</h1>
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
      ) : !domiciliarios?.length ? (
        <p className="text-gray-400 text-center py-8">No hay domiciliarios disponibles</p>
      ) : (
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {domiciliarios.map((d) => (
            <div key={d.id} className="bg-white rounded-xl shadow p-4">
              <div className="flex items-center gap-3">
                <img
                  src={d.foto || DEFAULT_AVATAR}
                  alt={d.nombre}
                  className="w-12 h-12 rounded-full object-cover border-2 border-gray-200"
                />
                <div className="flex-1 min-w-0">
                  <h3 className="font-medium">{d.nombre}</h3>
                  <p className="text-xs text-gray-400">CC: {d.cedula} · {d.telefono}</p>
                </div>
                <button
                  onClick={() => openEdit(d)}
                  className="text-indigo-600 hover:bg-indigo-50 p-1.5 rounded-lg text-xs"
                  title="Editar"
                >
                  ✏️
                </button>
              </div>
              <div className="mt-3 flex items-center justify-between text-xs">
                <span className={`px-2 py-0.5 rounded-full ${d.disponible ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                  {d.disponible ? 'Disponible' : 'Ocupado'}
                </span>
                {d.lat && d.lng && (
                  <span className="text-gray-400">📍 {d.lat.toFixed(4)}, {d.lng.toFixed(4)}</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={showForm} onClose={closeForm} title={isEditing ? 'Editar Domiciliario' : 'Nuevo Domiciliario'}>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (telefono.length < 10) {
              setFormError('El teléfono debe tener mínimo 10 dígitos');
              return;
            }
            isEditing ? actualizar.mutate() : crear.mutate();
          }}
          className="space-y-4"
        >
          {formError && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">
              {formError}
            </div>
          )}

          <AvatarUpload value={foto} onChange={setFoto} />

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
            <label className="block text-sm font-medium text-gray-700 mb-1">Cédula</label>
            <input
              value={cedula}
              onChange={(e) => !isEditing && setCedula(e.target.value.replace(/\D/g, ''))}
              className={`w-full border rounded-lg px-3 py-2 text-sm ${isEditing ? 'bg-gray-100 text-gray-500 cursor-not-allowed' : ''}`}
              placeholder="Ej: 1234567890"
              inputMode="numeric"
              pattern="[0-9]*"
              readOnly={isEditing}
              required
            />
            {isEditing && <p className="text-xs text-gray-400 mt-1">La cédula no se puede modificar</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Teléfono</label>
            <input
              value={telefono}
              onChange={(e) => setTelefono(e.target.value.replace(/\D/g, ''))}
              className="w-full border rounded-lg px-3 py-2 text-sm"
              placeholder="Ej: 3201234567"
              inputMode="numeric"
              pattern="[0-9]*"
              minLength={10}
              required
            />
            {telefono.length > 0 && telefono.length < 10 && (
              <p className="text-xs text-red-500 mt-1">Mínimo 10 dígitos ({telefono.length}/10)</p>
            )}
          </div>
          <button
            type="submit"
            disabled={isPending}
            className="w-full bg-indigo-600 text-white py-2 rounded-lg text-sm hover:bg-indigo-700 disabled:opacity-50"
          >
            {isPending ? (isEditing ? 'Actualizando…' : 'Registrando…') : (isEditing ? 'Actualizar' : 'Registrar')}
          </button>
        </form>
      </Modal>
    </div>
  );
}

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usuarioService, type UsuarioResponse } from '../../services/usuarios';
import { useRestaurante } from '../../context/RestauranteContext';
import { useToast } from '../../components/Toast';
import { Modal } from '../../components/Modal';
import { RestauranteSelector } from '../../components/RestauranteSelector';
import { AvatarUpload, DEFAULT_AVATAR } from '../../components/AvatarUpload';

export function UsuariosPage() {
  const { restaurante } = useRestaurante();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editando, setEditando] = useState<UsuarioResponse | null>(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [foto, setFoto] = useState<string | null>(null);
  const [formError, setFormError] = useState('');

  const { data: usuarios, isLoading } = useQuery({
    queryKey: ['usuarios', restaurante?.id],
    queryFn: () => usuarioService.listar(restaurante!.id),
    enabled: !!restaurante,
  });

  const crear = useMutation({
    mutationFn: () => {
      setFormError('');
      return usuarioService.crear({
        username, password, foto, restauranteId: restaurante!.id,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['usuarios', restaurante?.id] });
      toast('Usuario creado', 'success');
      closeForm();
    },
    onError: (err: Error) => { setFormError(err.message); toast(err.message, 'error'); },
  });

  const actualizar = useMutation({
    mutationFn: () => {
      setFormError('');
      return usuarioService.actualizar(editando!.id, {
        username, password, foto, restauranteId: restaurante!.id,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['usuarios', restaurante?.id] });
      toast('Usuario actualizado', 'success');
      closeForm();
    },
    onError: (err: Error) => { setFormError(err.message); toast(err.message, 'error'); },
  });

  const desactivar = useMutation({
    mutationFn: (id: number) => usuarioService.desactivar(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['usuarios', restaurante?.id] });
      toast('Usuario desactivado', 'success');
    },
    onError: (err: Error) => toast(err.message, 'error'),
  });

  const closeForm = () => {
    setShowForm(false);
    setEditando(null);
    setUsername('');
    setPassword('');
    setFoto(null);
    setFormError('');
  };

  const openEdit = (u: UsuarioResponse) => {
    setEditando(u);
    setUsername(u.username);
    setPassword('');
    setFoto(u.foto);
    setShowForm(true);
  };

  const isEditing = !!editando;
  const isPending = crear.isPending || actualizar.isPending;

  if (!restaurante) {
    return (
      <div className="p-6">
        <h1 className="text-xl font-bold mb-4">Usuarios</h1>
        <RestauranteSelector />
      </div>
    );
  }

  return (
    <div className="p-4 md:p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-bold text-gray-800">👤 Usuarios</h1>
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
      ) : !usuarios?.length ? (
        <p className="text-gray-400 text-center py-8">No hay usuarios</p>
      ) : (
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {usuarios.map((u) => (
            <div key={u.id} className="bg-white rounded-xl shadow p-4">
              <div className="flex items-center gap-3">
                <img
                  src={u.foto || DEFAULT_AVATAR}
                  alt={u.username}
                  className="w-12 h-12 rounded-full object-cover border-2 border-gray-200"
                />
                <div className="flex-1 min-w-0">
                  <h3 className="font-medium">{u.username}</h3>
                  <p className="text-xs text-gray-400">{u.rol}</p>
                </div>
                <div className="flex gap-1">
                  <button onClick={() => openEdit(u)} className="text-indigo-600 hover:bg-indigo-50 p-1.5 rounded-lg text-xs" title="Editar">✏️</button>
                  <button onClick={() => desactivar.mutate(u.id)} className="text-red-500 hover:bg-red-50 p-1.5 rounded-lg text-xs" title="Desactivar">🗑️</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={showForm} onClose={closeForm} title={isEditing ? 'Editar Usuario' : 'Nuevo Usuario'}>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (!isEditing && password.length < 6) {
              setFormError('La contraseña debe tener mínimo 6 caracteres');
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
            <label className="block text-sm font-medium text-gray-700 mb-1">Usuario</label>
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className={`w-full border rounded-lg px-3 py-2 text-sm ${isEditing ? 'bg-gray-100 text-gray-500 cursor-not-allowed' : ''}`}
              readOnly={isEditing}
              required
            />
            {isEditing && <p className="text-xs text-gray-400 mt-1">El usuario no se puede modificar</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {isEditing ? 'Nueva contraseña (dejar vacío para no cambiar)' : 'Contraseña'}
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm"
              placeholder={isEditing ? '••••••••' : 'Mínimo 6 caracteres'}
              required={!isEditing}
              minLength={isEditing ? 0 : 6}
            />
          </div>
          <button
            type="submit"
            disabled={isPending}
            className="w-full bg-indigo-600 text-white py-2 rounded-lg text-sm hover:bg-indigo-700 disabled:opacity-50"
          >
            {isPending ? (isEditing ? 'Actualizando…' : 'Creando…') : (isEditing ? 'Actualizar' : 'Crear')}
          </button>
        </form>
      </Modal>
    </div>
  );
}

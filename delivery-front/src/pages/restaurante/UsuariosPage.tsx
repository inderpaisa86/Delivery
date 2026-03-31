import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usuarioService, type UsuarioResponse } from '../../services/usuarios';
import { useRestaurante } from '../../context/RestauranteContext';
import { useToast } from '../../components/Toast';
import { Modal } from '../../components/Modal';
import { AvatarUpload, DEFAULT_AVATAR } from '../../components/AvatarUpload';

const inputClass = 'w-full bg-[var(--bg-primary)] border border-[var(--border)] text-[var(--text-primary)] rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-[var(--accent)] outline-none transition';

export function UsuariosPage() {
  const { restaurante } = useRestaurante();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editando, setEditando] = useState<UsuarioResponse | null>(null);
  const [username, setUsername] = useState(''); const [password, setPassword] = useState(''); const [foto, setFoto] = useState<string | null>(null); const [formError, setFormError] = useState('');

  const { data: usuarios, isLoading } = useQuery({ queryKey: ['usuarios', restaurante?.id], queryFn: () => usuarioService.listar(restaurante!.id), enabled: !!restaurante });
  const crear = useMutation({ mutationFn: () => { setFormError(''); return usuarioService.crear({ username, password, foto, restauranteId: restaurante!.id }); }, onSuccess: () => { qc.invalidateQueries({ queryKey: ['usuarios', restaurante?.id] }); toast('Creado', 'success'); closeForm(); }, onError: (e: Error) => { setFormError(e.message); toast(e.message, 'error'); } });
  const actualizar = useMutation({ mutationFn: () => { setFormError(''); return usuarioService.actualizar(editando!.id, { username, password, foto, restauranteId: restaurante!.id }); }, onSuccess: () => { qc.invalidateQueries({ queryKey: ['usuarios', restaurante?.id] }); toast('Actualizado', 'success'); closeForm(); }, onError: (e: Error) => { setFormError(e.message); toast(e.message, 'error'); } });
  const desactivar = useMutation({ mutationFn: (id: number) => usuarioService.desactivar(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['usuarios', restaurante?.id] }); toast('Desactivado', 'success'); }, onError: (e: Error) => toast(e.message, 'error') });
  const closeForm = () => { setShowForm(false); setEditando(null); setUsername(''); setPassword(''); setFoto(null); setFormError(''); };
  const openEdit = (u: UsuarioResponse) => { setEditando(u); setUsername(u.username); setPassword(''); setFoto(u.foto); setShowForm(true); };
  const isEditing = !!editando;

  if (!restaurante) return <div className="animate-fade-up"><h1 className="text-2xl font-bold mb-4">Usuarios</h1><p className="text-[var(--text-muted)]">Selecciona un restaurante.</p></div>;

  return (
    <div className="animate-fade-up">
      <div className="flex items-center justify-between mb-6">
        <div><h1 className="text-2xl font-bold">👤 Usuarios</h1><p className="text-sm text-[var(--text-muted)] mt-1">Accesos al sistema</p></div>
        <button onClick={() => { closeForm(); setShowForm(true); }} className="bg-[var(--accent)] text-white px-5 py-2.5 rounded-xl text-sm font-semibold hover:brightness-110 transition btn-glow">+ Nuevo</button>
      </div>
      {isLoading ? <p className="text-[var(--text-muted)]">Cargando…</p> : !usuarios?.length ? <div className="text-center py-20"><span className="text-6xl block mb-4">👤</span><p className="text-[var(--text-muted)]">No hay usuarios</p></div> : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {usuarios.map((u) => (
            <div key={u.id} className="bg-white rounded-2xl border border-[var(--border)] shadow-[var(--shadow-sm)] hover:shadow-[var(--shadow-md)] transition-all p-5">
              <div className="flex items-center gap-4">
                <img src={u.foto || DEFAULT_AVATAR} alt={u.username} className="w-14 h-14 rounded-2xl object-cover border-2 border-[var(--border)] shadow-sm" />
                <div className="flex-1 min-w-0"><h3 className="font-bold">{u.username}</h3><p className="text-xs text-[var(--text-muted)]">{u.rol}</p></div>
                <div className="flex gap-1">
                  <button onClick={() => openEdit(u)} className="w-9 h-9 rounded-xl bg-[var(--accent-light)] text-[var(--accent)] flex items-center justify-center hover:bg-[var(--accent-dim)] transition text-sm">✏️</button>
                  <button onClick={() => desactivar.mutate(u.id)} className="w-9 h-9 rounded-xl bg-red-50 text-red-500 flex items-center justify-center hover:bg-red-100 transition text-sm">🗑️</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
      <Modal open={showForm} onClose={closeForm} title={isEditing ? 'Editar Usuario' : 'Nuevo Usuario'}>
        <form onSubmit={(e) => { e.preventDefault(); if (!isEditing && password.length < 6) { setFormError('Contraseña mínimo 6 caracteres'); return; } isEditing ? actualizar.mutate() : crear.mutate(); }} className="space-y-4">
          {formError && <div className="bg-red-50 border border-red-200 rounded-xl p-3 text-sm text-red-600">{formError}</div>}
          <AvatarUpload value={foto} onChange={setFoto} />
          <div><label className="block text-sm font-semibold text-[var(--text-secondary)] mb-1.5">Usuario</label><input value={username} onChange={(e) => setUsername(e.target.value)} className={`${inputClass} ${isEditing ? 'opacity-50 cursor-not-allowed' : ''}`} readOnly={isEditing} required />{isEditing && <p className="text-xs text-[var(--text-muted)] mt-1">No editable</p>}</div>
          <div><label className="block text-sm font-semibold text-[var(--text-secondary)] mb-1.5">{isEditing ? 'Nueva contraseña (vacío = sin cambio)' : 'Contraseña'}</label><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className={inputClass} required={!isEditing} /></div>
          <button type="submit" className="w-full bg-[var(--accent)] text-white py-3 rounded-xl text-sm font-semibold hover:brightness-110 transition btn-glow">{isEditing ? 'Actualizar' : 'Crear'}</button>
        </form>
      </Modal>
    </div>
  );
}

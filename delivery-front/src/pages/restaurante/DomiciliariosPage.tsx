import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { domiciliarioService } from '../../services/domiciliarios';
import { useRestaurante } from '../../context/RestauranteContext';
import { useToast } from '../../components/Toast';
import { Modal } from '../../components/Modal';
import { AvatarUpload, DEFAULT_AVATAR } from '../../components/AvatarUpload';
import type { DomiciliarioResponse } from '../../types';

const inputClass = 'w-full bg-[var(--bg-primary)] border border-[var(--border)] text-[var(--text-primary)] rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-[var(--accent)] outline-none transition';

export function DomiciliariosPage() {
  const { restaurante } = useRestaurante();
  const { toast } = useToast();
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editando, setEditando] = useState<DomiciliarioResponse | null>(null);
  const [nombre, setNombre] = useState(''); const [cedula, setCedula] = useState(''); const [telefono, setTelefono] = useState(''); const [foto, setFoto] = useState<string | null>(null); const [formError, setFormError] = useState('');

  const { data: domiciliarios, isLoading } = useQuery({ queryKey: ['domiciliarios', restaurante?.id], queryFn: () => domiciliarioService.disponibles(restaurante!.id), refetchInterval: 10000, enabled: !!restaurante });
  const crear = useMutation({ mutationFn: () => { setFormError(''); return domiciliarioService.crear({ restauranteId: restaurante!.id, nombre, cedula, foto, telefono, lat: 4.711, lng: -74.0721 }); }, onSuccess: () => { qc.invalidateQueries({ queryKey: ['domiciliarios', restaurante?.id] }); toast('Registrado', 'success'); closeForm(); }, onError: (e: Error) => { setFormError(e.message); toast(e.message, 'error'); } });
  const actualizar = useMutation({ mutationFn: () => { setFormError(''); return domiciliarioService.actualizar(editando!.id, { restauranteId: restaurante!.id, nombre, cedula: editando!.cedula, foto, telefono }); }, onSuccess: () => { qc.invalidateQueries({ queryKey: ['domiciliarios', restaurante?.id] }); toast('Actualizado', 'success'); closeForm(); }, onError: (e: Error) => { setFormError(e.message); toast(e.message, 'error'); } });
  const closeForm = () => { setShowForm(false); setEditando(null); setNombre(''); setCedula(''); setTelefono(''); setFoto(null); setFormError(''); };
  const openEdit = (d: DomiciliarioResponse) => { setEditando(d); setNombre(d.nombre); setCedula(d.cedula); setTelefono(d.telefono); setFoto(d.foto); setShowForm(true); };
  const isEditing = !!editando;

  if (!restaurante) return <div className="animate-fade-up"><h1 className="text-2xl font-bold mb-4">Domiciliarios</h1><p className="text-[var(--text-muted)]">Selecciona un restaurante.</p></div>;

  return (
    <div className="animate-fade-up">
      <div className="flex items-center justify-between mb-6">
        <div><h1 className="text-2xl font-bold">🛵 Domiciliarios</h1><p className="text-sm text-[var(--text-muted)] mt-1">Tu equipo de entregas</p></div>
        <button onClick={() => { closeForm(); setShowForm(true); }} className="bg-[var(--accent)] text-white px-5 py-2.5 rounded-xl text-sm font-semibold hover:brightness-110 transition btn-glow">+ Nuevo</button>
      </div>
      {isLoading ? <p className="text-[var(--text-muted)]">Cargando…</p> : !domiciliarios?.length ? <div className="text-center py-20"><span className="text-6xl block mb-4">🛵</span><p className="text-[var(--text-muted)]">No hay domiciliarios</p></div> : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {domiciliarios.map((d) => (
            <div key={d.id} className="bg-white rounded-2xl border border-[var(--border)] shadow-[var(--shadow-sm)] hover:shadow-[var(--shadow-md)] transition-all p-5">
              <div className="flex items-center gap-4">
                <img src={d.foto || DEFAULT_AVATAR} alt={d.nombre} className="w-14 h-14 rounded-2xl object-cover border-2 border-[var(--border)] shadow-sm" />
                <div className="flex-1 min-w-0"><h3 className="font-bold">{d.nombre}</h3><p className="text-xs text-[var(--text-muted)]">CC: {d.cedula} · 📞 {d.telefono}</p></div>
                <button onClick={() => openEdit(d)} className="w-9 h-9 rounded-xl bg-[var(--accent-light)] text-[var(--accent)] flex items-center justify-center hover:bg-[var(--accent-dim)] transition text-sm">✏️</button>
              </div>
              <div className="mt-4 flex items-center justify-between">
                <span className={`text-xs px-3 py-1 rounded-full font-semibold ${d.disponible ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-500'}`}>{d.disponible ? '🟢 Disponible' : '🔴 Ocupado'}</span>
                {d.lat && d.lng && <span className="text-xs text-[var(--text-muted)]">📍 {d.lat.toFixed(3)}, {d.lng.toFixed(3)}</span>}
              </div>
            </div>
          ))}
        </div>
      )}
      <Modal open={showForm} onClose={closeForm} title={isEditing ? 'Editar Domiciliario' : 'Nuevo Domiciliario'}>
        <form onSubmit={(e) => { e.preventDefault(); if (telefono.length < 10) { setFormError('Teléfono mínimo 10 dígitos'); return; } isEditing ? actualizar.mutate() : crear.mutate(); }} className="space-y-4">
          {formError && <div className="bg-red-50 border border-red-200 rounded-xl p-3 text-sm text-red-600">{formError}</div>}
          <AvatarUpload value={foto} onChange={setFoto} />
          <div><label className="block text-sm font-semibold text-[var(--text-secondary)] mb-1.5">Nombre</label><input value={nombre} onChange={(e) => setNombre(e.target.value)} className={inputClass} required /></div>
          <div><label className="block text-sm font-semibold text-[var(--text-secondary)] mb-1.5">Cédula</label><input value={cedula} onChange={(e) => !isEditing && setCedula(e.target.value.replace(/\D/g, ''))} className={`${inputClass} ${isEditing ? 'opacity-50 cursor-not-allowed' : ''}`} readOnly={isEditing} inputMode="numeric" required />{isEditing && <p className="text-xs text-[var(--text-muted)] mt-1">No editable</p>}</div>
          <div><label className="block text-sm font-semibold text-[var(--text-secondary)] mb-1.5">Teléfono</label><input value={telefono} onChange={(e) => setTelefono(e.target.value.replace(/\D/g, ''))} className={inputClass} inputMode="numeric" minLength={10} required />{telefono.length > 0 && telefono.length < 10 && <p className="text-xs text-red-500 mt-1">Mínimo 10 dígitos ({telefono.length}/10)</p>}</div>
          <button type="submit" className="w-full bg-[var(--accent)] text-white py-3 rounded-xl text-sm font-semibold hover:brightness-110 transition btn-glow">{isEditing ? 'Actualizar' : 'Registrar'}</button>
        </form>
      </Modal>
    </div>
  );
}

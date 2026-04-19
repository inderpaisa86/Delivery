export type SessionStep = 'idle' | 'waiting_name' | 'waiting_address_confirm' | 'waiting_location_choice' | 'waiting_location' | 'waiting_typed_address' | 'waiting_details' | 'waiting_contact_phone';

export interface UserSession {
  step: SessionStep;
  pendingPedidoId?: number;
  pendingLat?: number;
  pendingLng?: number;
  pendingAddress?: string;
  clienteNombre?: string;
  clienteDireccion?: string;
  contactPhone?: string;
  restauranteId: number;
}

const sessions = new Map<string, UserSession>();

const DEFAULT_RESTAURANTE_ID = Number(process.env.RESTAURANTE_ID || '1');

export function getSession(phone: string): UserSession {
  if (!sessions.has(phone)) {
    sessions.set(phone, { step: 'idle', restauranteId: DEFAULT_RESTAURANTE_ID });
  }
  return sessions.get(phone)!;
}

export function updateSession(phone: string, data: Partial<UserSession>) {
  const session = getSession(phone);
  Object.assign(session, data);
}

export function resetSession(phone: string) {
  sessions.set(phone, { step: 'idle', restauranteId: DEFAULT_RESTAURANTE_ID });
}

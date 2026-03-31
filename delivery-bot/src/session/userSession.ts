export type SessionStep = 'idle' | 'waiting_location';

export interface UserSession {
  step: SessionStep;
  pendingPedidoId?: number;
  restauranteId: number;
}

const sessions = new Map<string, UserSession>();

/** Restaurante por defecto — cambiar según tu configuración */
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

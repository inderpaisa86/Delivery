const BASE_URL = process.env.API_URL || 'http://localhost:8080';
const API_TOKEN = process.env.API_TOKEN || 'delivery-internal-token';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${API_TOKEN}`,
      ...init?.headers,
    },
  });
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(`API ${res.status}: ${body || res.statusText}`);
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text);
}

/* ── Tipos ── */
export interface Producto {
  id: number;
  nombre: string;
  precio: number;
  activo: boolean;
}

export interface PedidoResponse {
  id: number;
  numeroDiario: number | null;
  restauranteNombre: string;
  clienteTelefono: string;
  clienteNombre: string;
  direccion: string;
  estado: string;
  total: number;
  trackingToken: string;
  detalles: { producto: string; cantidad: number; precio: number }[];
}

export interface ClienteResponse {
  id: number;
  telefono: string;
  nombre: string | null;
  direccion: string | null;
  restauranteId: number;
}

interface Page<T> {
  content: T[];
}

/* ── API ── */
export const api = {
  listarProductos: (restauranteId: number) =>
    request<Page<Producto>>(`/productos?restauranteId=${restauranteId}&size=50`)
      .then((p) => p.content.filter((pr) => pr.activo)),

  crearPedido: (data: {
    restauranteId: number;
    telefono: string;
    nombre?: string;
    direccion: string;
    lat?: number;
    lng?: number;
    detalles: { productoId: number; cantidad: number }[];
  }) => request<PedidoResponse>('/pedidos', { method: 'POST', body: JSON.stringify(data) }),

  actualizarEstado: (pedidoId: number, estado: string) =>
    request<PedidoResponse>(`/pedidos/${pedidoId}/estado?estado=${estado}`, { method: 'PUT' }),

  actualizarUbicacion: (pedidoId: number, lat: number, lng: number, direccion?: string, telefonoContacto?: string) => {
    const params = new URLSearchParams({ lat: String(lat), lng: String(lng) });
    if (direccion) params.set('direccion', direccion);
    if (telefonoContacto) params.set('telefonoContacto', telefonoContacto);
    return request<PedidoResponse>(`/pedidos/${pedidoId}/ubicacion?${params}`, { method: 'PUT' });
  },

  obtenerPedido: (pedidoId: number) =>
    request<PedidoResponse>(`/pedidos/${pedidoId}`),

  listarPedidosHoy: (restauranteId: number) =>
    request<Page<PedidoResponse>>(`/pedidos/hoy?restauranteId=${restauranteId}&size=100&sort=fecha,desc`)
      .then((p) => p.content),

  buscarCliente: async (telefono: string, restauranteId: number): Promise<ClienteResponse | null> => {
    try {
      return await request<ClienteResponse>(`/clientes/buscar?telefono=${telefono}&restauranteId=${restauranteId}`);
    } catch {
      return null;
    }
  },
};

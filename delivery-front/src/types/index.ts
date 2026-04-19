/* ── Enums ── */
export enum EstadoPedido {
  NUEVO = 'NUEVO',
  CONFIRMADO = 'CONFIRMADO',
  PREPARANDO = 'PREPARANDO',
  LISTO = 'LISTO',
  EN_CAMINO = 'EN_CAMINO',
  ENTREGADO = 'ENTREGADO',
  CANCELADO = 'CANCELADO',
}

export enum EstadoAsignacion {
  ASIGNADO = 'ASIGNADO',
  EN_CAMINO = 'EN_CAMINO',
  ENTREGADO = 'ENTREGADO',
  CANCELADO = 'CANCELADO',
}

/* ── Restaurante ── */
export interface RestauranteRequest {
  nombre: string;
  telefono?: string;
  direccion?: string;
  lat?: number;
  lng?: number;
  whatsappPhoneId?: string;
}

export interface RestauranteResponse {
  id: number;
  nombre: string;
  telefono: string;
  direccion: string;
  lat: number;
  lng: number;
  activo: boolean;
  whatsappPhoneId: string;
}

/* ── Producto ── */
export interface ProductoRequest {
  restauranteId: number;
  nombre: string;
  precio: number;
}

export interface ProductoResponse {
  id: number;
  nombre: string;
  precio: number;
  activo: boolean;
  restauranteId: number;
}

/* ── Pedido ── */
export interface DetallePedidoRequest {
  productoId: number;
  cantidad: number;
}

export interface PedidoRequest {
  restauranteId: number;
  telefono: string;
  nombre?: string;
  direccion: string;
  lat?: number;
  lng?: number;
  detalles: DetallePedidoRequest[];
}

export interface DetalleResponse {
  producto: string;
  cantidad: number;
  precio: number;
}

export interface PedidoResponse {
  id: number;
  numeroDiario: number | null;
  restauranteId: number;
  restauranteNombre: string;
  clienteTelefono: string;
  clienteNombre: string;
  direccion: string;
  lat: number | null;
  lng: number | null;
  estado: EstadoPedido;
  total: number;
  trackingToken: string;
  fecha: string;
  domiciliarioNombre: string | null;
  fechaAsignacion: string | null;
  fechaEntrega: string | null;
  detalles: DetalleResponse[];
}

/* ── Domiciliario ── */
export interface DomiciliarioRequest {
  restauranteId: number;
  nombre: string;
  cedula: string;
  foto?: string | null;
  telefono: string;
  lat?: number;
  lng?: number;
}

export interface DomiciliarioResponse {
  id: number;
  nombre: string;
  cedula: string;
  foto: string | null;
  telefono: string;
  disponible: boolean;
  lat: number;
  lng: number;
  restauranteId: number;
}

/* ── Tracking ── */
export interface TrackingResponse {
  pedidoId: number;
  estado: EstadoPedido;
  direccion: string;
  pedidoLat: number;
  pedidoLng: number;
  domiciliarioLat: number;
  domiciliarioLng: number;
  domiciliarioNombre: string;
}

export interface UbicacionRequest {
  domiciliarioId: number;
  lat: number;
  lng: number;
}

export interface UbicacionResponse {
  domiciliarioId: number;
  nombre: string;
  lat: number;
  lng: number;
}

/* ── Paginación Spring ── */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

import { api } from './api';
import type { PedidoRequest, PedidoResponse, Page, EstadoPedido } from '../types';

export const pedidoService = {
  crear: (data: PedidoRequest) => api.post<PedidoResponse>('/pedidos', data),
  obtener: (id: number) => api.get<PedidoResponse>(`/pedidos/${id}`),
  listarPorRestaurante: (restauranteId: number, page = 0, size = 20) =>
    api.get<Page<PedidoResponse>>(
      `/pedidos?restauranteId=${restauranteId}&page=${page}&size=${size}&sort=fecha,desc`,
    ),
  listarPorCliente: (telefono: string, page = 0, size = 20) =>
    api.get<Page<PedidoResponse>>(
      `/pedidos/cliente/${telefono}?page=${page}&size=${size}&sort=fecha,desc`,
    ),
  cambiarEstado: (id: number, estado: EstadoPedido) =>
    api.put<PedidoResponse>(`/pedidos/${id}/estado?estado=${estado}`),
};

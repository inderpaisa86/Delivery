import { api } from './api';
import type { ProductoRequest, ProductoResponse, Page } from '../types';

export const productoService = {
  listar: (restauranteId: number, page = 0, size = 50) =>
    api.get<Page<ProductoResponse>>(
      `/productos?restauranteId=${restauranteId}&page=${page}&size=${size}`,
    ),
  crear: (data: ProductoRequest) => api.post<ProductoResponse>('/productos', data),
  actualizar: (id: number, data: ProductoRequest) =>
    api.put<ProductoResponse>(`/productos/${id}`, data),
  desactivar: (id: number) => api.delete<void>(`/productos/${id}`),
};

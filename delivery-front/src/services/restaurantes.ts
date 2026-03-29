import { api } from './api';
import type { RestauranteRequest, RestauranteResponse } from '../types';

export const restauranteService = {
  listar: () => api.get<RestauranteResponse[]>('/restaurantes'),
  obtener: (id: number) => api.get<RestauranteResponse>(`/restaurantes/${id}`),
  crear: (data: RestauranteRequest) => api.post<RestauranteResponse>('/restaurantes', data),
  actualizar: (id: number, data: RestauranteRequest) =>
    api.put<RestauranteResponse>(`/restaurantes/${id}`, data),
};

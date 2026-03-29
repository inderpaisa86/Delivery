import { api } from './api';
import type { DomiciliarioRequest, DomiciliarioResponse, UbicacionRequest } from '../types';

export const domiciliarioService = {
  crear: (data: DomiciliarioRequest) =>
    api.post<DomiciliarioResponse>('/domiciliarios', data),
  actualizar: (id: number, data: DomiciliarioRequest) =>
    api.put<DomiciliarioResponse>(`/domiciliarios/${id}`, data),
  disponibles: (restauranteId: number) =>
    api.get<DomiciliarioResponse[]>(`/domiciliarios/disponibles?restauranteId=${restauranteId}`),
  actualizarUbicacion: (data: UbicacionRequest) =>
    api.post<void>('/domiciliarios/ubicacion', data),
  buscarPorCedula: (cedula: string) =>
    api.get<DomiciliarioResponse>(`/domiciliarios/cedula/${cedula}`),
};

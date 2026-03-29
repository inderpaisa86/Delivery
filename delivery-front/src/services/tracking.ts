import { api } from './api';
import type { TrackingResponse, UbicacionResponse } from '../types';

export const trackingService = {
  obtener: (token: string) => api.get<TrackingResponse>(`/track/${token}`),
  ubicacion: (domiciliarioId: number) =>
    api.get<UbicacionResponse>(`/ubicacion/${domiciliarioId}`),
};

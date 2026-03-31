import { api } from './api';

export interface UsuarioRequest {
  username: string;
  password: string;
  foto?: string | null;
  restauranteId: number;
}

export interface UsuarioResponse {
  id: number;
  username: string;
  foto: string | null;
  rol: string;
  activo: boolean;
  restauranteId: number;
  restauranteNombre: string;
}

export const usuarioService = {
  listar: (restauranteId: number) =>
    api.get<UsuarioResponse[]>(`/usuarios?restauranteId=${restauranteId}`),
  crear: (data: UsuarioRequest) =>
    api.post<UsuarioResponse>('/usuarios', data),
  actualizar: (id: number, data: UsuarioRequest) =>
    api.put<UsuarioResponse>(`/usuarios/${id}`, data),
  desactivar: (id: number) =>
    api.delete<void>(`/usuarios/${id}`),
};

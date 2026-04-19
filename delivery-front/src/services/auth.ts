const BASE = '/api';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  userId: number;
  username: string;
  rol: string;
  perfil: string;
  restauranteId: number | null;
  restauranteNombre: string | null;
  token: string;
}

export const authService = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const res = await fetch(`${BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const body = await res.text().catch(() => '');
      throw new Error(body || 'Usuario o contraseña incorrectos');
    }
    return res.json();
  },
};

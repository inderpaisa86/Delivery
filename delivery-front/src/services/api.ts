const BASE = '/api';

function getToken(): string {
  return localStorage.getItem('api_token') ?? '';
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getToken()}`,
      ...init?.headers,
    },
  });
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(`${res.status}: ${body || res.statusText}`);
  }
  // Sin contenido
  if (res.status === 204 || res.headers.get('content-length') === '0') {
    return undefined as T;
  }
  // Intentar parsear JSON, si falla devolver undefined
  const text = await res.text();
  if (!text) return undefined as T;
  try {
    return JSON.parse(text);
  } catch {
    return undefined as T;
  }
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};

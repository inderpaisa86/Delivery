# 🛵 Delivery Front

Frontend para el sistema de delivery. Panel unificado para restaurantes y domiciliarios con tracking en tiempo real.

## Tecnologías

- React 19 + TypeScript
- Vite 8
- Tailwind CSS 4
- React Router 7
- TanStack React Query (polling en tiempo real)
- Leaflet + React Leaflet (mapas OpenStreetMap)

## Diseño

Estilo moderno con sidebar naranja coral, fondo claro, tarjetas con stepper visual de estados, botones con efecto glow y animaciones suaves.

## Requisitos

- Node.js 18+
- Backend `delivery-core-service` corriendo en `http://localhost:8080`

## Instalación y ejecución

```bash
cd delivery-front
npm install
npm run dev
```

Abre `http://localhost:5173`. El proxy de Vite redirige `/api/*` al backend en `localhost:8080`.

## Build de producción

```bash
npm run build
npm run preview
```

## Variables de entorno

Crear `.env` en la raíz de `delivery-front/`:

| Variable | Descripción | Default |
|---|---|---|
| `VITE_API_TOKEN` | Token de autenticación del backend | `delivery-internal-token` |

## Estructura

```
src/
├── components/          → Reutilizables (Sidebar, Modal, Toast, EstadoBadge, StatCard, AvatarUpload)
├── context/             → AuthContext, RestauranteContext
├── hooks/               → usePolling
├── pages/
│   ├── LoginPage        → Login split: usuario/contraseña (restaurante) o cédula (domiciliario)
│   ├── TrackingPage     → Tracking público con mapa
│   ├── restaurante/
│   │   ├── PedidosPage      → Pedidos del día con stepper, filtros y stats
│   │   ├── ProductosPage    → CRUD de productos
│   │   ├── DomiciliariosPage → CRUD con foto, cédula, teléfono
│   │   └── UsuariosPage     → CRUD con foto y contraseña
│   └── domiciliario/
│       └── DomiciliarioPanel → Mapa con destinos, geolocalización, acciones
├── services/            → Clientes API (pedidos, productos, restaurantes, domiciliarios, usuarios, auth, tracking)
└── types/               → Interfaces y enums TypeScript
```

## Funcionalidades

- Login con usuario/contraseña para restaurante, cédula para domiciliario
- Sidebar lateral naranja coral con navegación por secciones
- Dashboard de pedidos del día con stepper visual de 6 estados
- Filtros por estado como pills con glow
- Stats: total, nuevos, en camino, entregados
- CRUD completo de productos, domiciliarios y usuarios
- Upload de foto para domiciliarios y usuarios (base64 en BD)
- Validaciones: cédula/teléfono solo numéricos, teléfono mínimo 10 dígitos, contraseña mínimo 6 caracteres
- Panel domiciliario con mapa OpenStreetMap, geolocalización, botón de actualizar ubicación
- Tracking público en `/track/:token` sin autenticación
- Polling cada 5s para pedidos, notificación de nuevos pedidos

## Rutas

| Ruta | Acceso | Descripción |
|---|---|---|
| `/login` | Público | Página de ingreso |
| `/restaurante/pedidos` | Restaurante | Pedidos del día |
| `/restaurante/productos` | Restaurante | Gestión de menú |
| `/restaurante/domiciliarios` | Restaurante | Equipo de entregas |
| `/restaurante/usuarios` | Restaurante | Accesos al sistema |
| `/domiciliario` | Domiciliario | Mapa y pedidos asignados |
| `/track/:token` | Público | Tracking de pedido |

## Proxy

El proxy de Vite redirige `/api/*` al backend:

```
Frontend (5173) → /api/pedidos → Backend (8080) → /pedidos
```

En producción, configurar reverse proxy (nginx) para redirigir `/api` al backend.

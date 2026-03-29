# 🛵 Delivery Front

Frontend para el sistema de delivery. Panel unificado para restaurantes y domiciliarios con tracking en tiempo real.

## Tecnologías

- React 19 + TypeScript
- Vite 8
- Tailwind CSS 4
- React Router 7
- TanStack React Query (polling en tiempo real)
- Leaflet + React Leaflet (mapas OpenStreetMap)

## Requisitos

- Node.js 18+
- Backend `delivery-core-service` corriendo en `http://localhost:8080`

## Instalación

```bash
cd delivery-front
npm install
```

## Levantar en desarrollo

```bash
npm run dev
```

Abre `http://localhost:5173`. El proxy de Vite redirige `/api/*` al backend en `localhost:8080`.

## Build de producción

```bash
npm run build
npm run preview
```

## Variables de entorno

Crear archivo `.env` en la raíz de `delivery-front/`:

| Variable | Descripción | Default |
|---|---|---|
| `VITE_API_TOKEN` | Token de autenticación del backend | `delivery-internal-token` |

## Estructura del proyecto

```
src/
├── components/          → Componentes reutilizables
│   ├── AvatarUpload     → Upload de foto con preview
│   ├── EstadoBadge      → Badge de estado del pedido
│   ├── Modal            → Modal genérico
│   ├── Navbar           → Barra de navegación
│   ├── RestauranteSelector → Selector multi-restaurante
│   ├── StatCard         → Tarjeta de estadística
│   └── Toast            → Sistema de notificaciones
├── context/             → Contextos React
│   ├── AuthContext       → Autenticación y sesión
│   └── RestauranteContext → Restaurante seleccionado
├── hooks/               → Hooks personalizados
│   └── usePolling        → Polling genérico con React Query
├── pages/               → Páginas/vistas
│   ├── LoginPage         → Login split con selección de rol
│   ├── TrackingPage      → Tracking público con mapa
│   ├── restaurante/
│   │   ├── PedidosPage       → Gestión de pedidos
│   │   ├── ProductosPage     → CRUD de productos
│   │   └── DomiciliariosPage → CRUD de domiciliarios
│   └── domiciliario/
│       └── DomiciliarioPanel → Panel con mapa y pedidos
├── services/            → Clientes API REST
│   ├── api              → Cliente HTTP base
│   ├── pedidos          → Servicio de pedidos
│   ├── productos        → Servicio de productos
│   ├── restaurantes     → Servicio de restaurantes
│   ├── domiciliarios    → Servicio de domiciliarios
│   └── tracking         → Servicio de tracking
└── types/               → Tipos TypeScript
    └── index            → Interfaces y enums
```

## Funcionalidades

### Login

- Selección de rol: Restaurante o Domiciliario
- Selector de restaurante con imagen dinámica (layout split)
- Domiciliario ingresa con número de cédula (validado contra el backend)
- Token de API configurado por variable de entorno

### Panel Restaurante

- Lista de pedidos en tiempo real (polling cada 5s)
- Máquina de estados: NUEVO → CONFIRMADO → PREPARANDO → LISTO → EN_CAMINO → ENTREGADO
- Filtros por estado
- Estadísticas: total, nuevos, en camino, entregados
- Modal de detalle del pedido con productos y total
- Notificación automática cuando llega un pedido nuevo
- Selector multi-restaurante

### Productos

- Listado por restaurante con tarjetas
- Crear, editar y desactivar productos
- Validación de precio positivo

### Domiciliarios

- Listado con foto, cédula, teléfono y estado
- Crear domiciliario con upload de foto (guardada en BD como base64)
- Editar nombre, teléfono y foto (cédula no editable)
- Validación: cédula y teléfono solo numéricos, teléfono mínimo 10 dígitos

### Panel Domiciliario

- Mapa interactivo centrado en ubicación actual (OpenStreetMap/Leaflet)
- Geolocalización del navegador con watchPosition
- Botón manual de actualizar ubicación
- Lista de pedidos asignados (LISTO y EN_CAMINO)
- Botones de acción: Recoger (→ EN_CAMINO) y Entregar (→ ENTREGADO)
- Detalle expandible de productos por pedido

### Tracking Público

- Ruta: `/track/:token`
- No requiere autenticación
- Mapa con ubicación del pedido y del domiciliario
- Estado del pedido en tiempo real (polling cada 5s)

## Rutas

| Ruta | Acceso | Descripción |
|---|---|---|
| `/login` | Público | Página de ingreso |
| `/restaurante/pedidos` | Restaurante | Gestión de pedidos |
| `/restaurante/productos` | Restaurante | Gestión de productos |
| `/restaurante/domiciliarios` | Restaurante | Gestión de domiciliarios |
| `/domiciliario` | Domiciliario | Panel con mapa y pedidos |
| `/track/:token` | Público | Tracking de pedido |

## Conexión con el Backend

El proxy de Vite (`vite.config.ts`) redirige todas las peticiones `/api/*` al backend:

```
Frontend (5173) → /api/pedidos → Backend (8080) → /pedidos
```

En producción, configurar el reverse proxy (nginx, etc.) para redirigir `/api` al backend.

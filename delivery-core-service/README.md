# 🛵 Delivery Core Service

Backend para sistema de pedidos por WhatsApp con asignación automática de domiciliarios, tracking en tiempo real y soporte multi-tenant (SaaS ready).

## Tecnologías

- Java 21
- Spring Boot 3.3.5
- PostgreSQL 16
- Flyway (migraciones de BD)
- JPA / Hibernate + Lombok
- Testcontainers (tests con PostgreSQL real)
- SpringDoc OpenAPI (Swagger)
- Bucket4j (rate limiting)
- Actuator (health check)
- Logstash (logs JSON en producción)
- SonarQube + JaCoCo (coverage 97.5%)
- Docker + Docker Compose
- Gradle (Kotlin DSL)

## Arquitectura

Clean Architecture con separación estricta de responsabilidades:

```
org.delivery/
├── domain/                                → Entidades JPA + enums
│   ├── entity/
│   │   ├── Restaurante.java               (tenant)
│   │   ├── Cliente.java
│   │   ├── Producto.java
│   │   ├── Pedido.java
│   │   ├── DetallePedido.java
│   │   ├── Domiciliario.java
│   │   └── AsignacionDomicilio.java
│   └── enums/
│       ├── EstadoPedido.java
│       └── EstadoAsignacion.java
├── application/                           → Casos de uso
│   ├── service/
│   │   ├── PedidoService.java
│   │   ├── PedidoStateMachine.java
│   │   ├── AsignacionService.java
│   │   ├── DomiciliarioService.java
│   │   ├── TrackingService.java
│   │   ├── RestauranteService.java
│   │   ├── ProductoService.java
│   │   └── WhatsAppMessageService.java
│   ├── port/
│   │   ├── WhatsAppPort.java
│   │   └── GeoPort.java
│   └── dto/
├── infrastructure/
│   ├── persistence/repository/
│   ├── external/
│   │   ├── whatsapp/
│   │   │   ├── WhatsAppAdapter.java
│   │   │   └── WhatsAppPayloadParser.java
│   │   └── geo/
│   │       └── HaversineAdapter.java
│   ├── config/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── SecurityFilter.java
│   │   ├── RateLimitFilter.java
│   │   ├── AsyncConfig.java
│   │   └── OpenApiConfig.java
└── interfaces/
    ├── rest/
    │   ├── PedidoController.java
    │   ├── DomiciliarioController.java
    │   ├── TrackingController.java
    │   ├── RestauranteController.java
    │   └── ProductoController.java
    └── webhook/
        └── WhatsAppWebhookController.java
```

## Requisitos previos

- Java 21+
- Docker y Docker Compose

## Configuración de variables de entorno

```bash
# Copiar el template y configurar
cp .env.example .env
```

Editar `.env` con tus valores. El archivo `.env` NO se sube al repositorio (está en `.gitignore`).

## Perfiles de ejecución

| Perfil | BD | Swagger | Logs | ddl-auto |
|---|---|---|---|---|
| `dev` (default) | localhost:5432 | habilitado | DEBUG texto | update |
| `prod` | variable de entorno | deshabilitado | INFO JSON | validate |

## Levantar el proyecto

### Desarrollo local

```bash
# 1. Levantar PostgreSQL
docker compose up -d postgres

# 2. Levantar la app (perfil dev por defecto)
./gradlew bootRun
```

### Con Docker Compose completo (producción)

```bash
# Configurar .env con SPRING_PROFILES_ACTIVE=prod y las variables de BD
docker compose up -d
```

### Swagger UI (solo en dev)

```
http://localhost:8080/swagger-ui.html
```

### Health check

```
http://localhost:8080/actuator/health
```

## Variables de entorno

| Variable | Descripción | Default |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `dev` |
| `SERVER_PORT` | Puerto del servidor | `8080` |
| `DB_URL` | URL de PostgreSQL | `jdbc:postgresql://localhost:5432/delivery_db` |
| `DB_USERNAME` | Usuario de BD | `postgres` |
| `DB_PASSWORD` | Contraseña de BD | `postgres` |
| `WHATSAPP_VERIFY_TOKEN` | Token verificación webhook | `mi-token-secreto` |
| `WHATSAPP_API_URL` | URL WhatsApp Business API | `https://graph.facebook.com/v18.0` |
| `WHATSAPP_API_TOKEN` | Token de acceso WhatsApp | (vacío) |
| `WHATSAPP_PHONE_NUMBER_ID` | ID teléfono en Meta | (vacío) |
| `APP_API_TOKEN` | Token endpoints internos | `delivery-internal-token` |
| `TRACKING_SECRET` | Secret para tokens de tracking | `tracking-secret-key` |
| `RATE_LIMIT_WEBHOOK` | Requests/min al webhook | `60` |

## Endpoints

### Webhook WhatsApp (públicos)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/webhook` | Verificación de Meta |
| `POST` | `/webhook` | Recibir mensajes (rate limited) |

### Pedidos (requieren token)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/pedidos` | Crear pedido |
| `GET` | `/pedidos/{id}` | Obtener pedido |
| `GET` | `/pedidos?restauranteId=1` | Listar por restaurante (paginado) |
| `GET` | `/pedidos/cliente/{telefono}` | Historial por cliente (paginado) |
| `PUT` | `/pedidos/{id}/estado?estado=CONFIRMADO` | Cambiar estado |

### Restaurantes (requieren token)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/restaurantes` | Crear restaurante |
| `PUT` | `/restaurantes/{id}` | Actualizar restaurante |
| `GET` | `/restaurantes/{id}` | Obtener restaurante |
| `GET` | `/restaurantes` | Listar activos |

### Productos (requieren token)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/productos` | Crear producto |
| `PUT` | `/productos/{id}` | Actualizar producto |
| `DELETE` | `/productos/{id}` | Desactivar producto |
| `GET` | `/productos?restauranteId=1` | Listar por restaurante (paginado) |

### Domiciliarios (requieren token)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/domiciliarios` | Registrar domiciliario (con foto base64) |
| `PUT` | `/domiciliarios/{id}` | Actualizar domiciliario (preserva lat/lng) |
| `GET` | `/domiciliarios/disponibles?restauranteId=1` | Listar disponibles con ubicación |
| `GET` | `/domiciliarios/cedula/{cedula}` | Buscar por cédula (login domiciliario) |
| `POST` | `/domiciliarios/ubicacion` | Actualizar ubicación GPS |

### Usuarios (requieren token)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/usuarios` | Crear usuario (con foto base64) |
| `PUT` | `/usuarios/{id}` | Actualizar usuario |
| `GET` | `/usuarios?restauranteId=1` | Listar por restaurante |
| `DELETE` | `/usuarios/{id}` | Desactivar usuario |

### Auth (público)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/auth/login` | Login con username/password |

### Pedidos (requieren token)

Nota: se agregó `GET /pedidos/hoy` que filtra solo pedidos del día actual.

### Tracking (públicos)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/track/{token}` | Tracking público del pedido |
| `GET` | `/ubicacion/{domiciliarioId}` | Ubicación del domiciliario |

### Monitoreo (públicos)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/actuator/health` | Health check |

## Autenticación

Endpoints internos requieren:

```
Authorization: Bearer delivery-internal-token
```

Públicos: `/webhook`, `/track/`, `/swagger-ui`, `/actuator`

## Ejemplos cURL

### Crear pedido

```bash
curl -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer delivery-internal-token" \
  -d '{
    "restauranteId": 1,
    "telefono": "573001234567",
    "nombre": "Juan Pérez",
    "direccion": "Calle 100 #15-20, Bogotá",
    "lat": 4.6097,
    "lng": -74.0817,
    "detalles": [
      { "productoId": 1, "cantidad": 2 },
      { "productoId": 5, "cantidad": 1 }
    ]
  }'
```

### Cambiar estado

```bash
curl -X PUT "http://localhost:8080/pedidos/1/estado?estado=CONFIRMADO" \
  -H "Authorization: Bearer delivery-internal-token"
```

### Mensaje WhatsApp

```bash
curl -X POST http://localhost:8080/webhook \
  -H "Content-Type: application/json" \
  -d '{"entry":[{"changes":[{"value":{"messages":[{
    "from":"573001234567","type":"text",
    "text":{"body":"2 hamburguesas, 1 gaseosa"}}]}}]}]}'
```

### Menú por WhatsApp

```bash
curl -X POST http://localhost:8080/webhook \
  -H "Content-Type: application/json" \
  -d '{"entry":[{"changes":[{"value":{"messages":[{
    "from":"573001234567","type":"text",
    "text":{"body":"menu"}}]}}]}]}'
```

### Tracking

```bash
curl http://localhost:8080/track/{tracking-token}
```

## Flujo del sistema

```
Cliente escribe por WhatsApp
        │
        ▼
  POST /webhook → WhatsAppPayloadParser (rate limited)
        │
        ▼
  "menu" → envía lista de productos con precios
  "2 hamburguesas" → parsea pedido
        │
        ▼
  PedidoService.crearPedido() → envía resumen al cliente
        │
        ▼
  Restaurante cambia estado vía API:
  CONFIRMADO → PREPARANDO → LISTO
        │
        ▼
  PedidoStateMachine detecta LISTO
        → AsignacionService.asignarDomiciliario() (@Async + Haversine)
        → Estado cambia a EN_CAMINO
        → Notificación al cliente
        │
        ▼
  Domiciliario actualiza ubicación → Cliente hace tracking
        │
        ▼
  ENTREGADO → Liberar domiciliario + Notificación final
```

## Estados del pedido

```
NUEVO → CONFIRMADO → PREPARANDO → LISTO → EN_CAMINO → ENTREGADO
  │         │            │          │         │
  └─────────┴────────────┴──────────┴─────────┴──→ CANCELADO
```

## Migraciones de BD

Flyway gestiona las migraciones automáticamente al arrancar:

```
src/main/resources/db/migration/
├── V1__init_schema.sql    → Tablas
└── V2__seed_data.sql      → Datos de prueba
```

## Tests

```bash
./gradlew test
```

Los tests usan Testcontainers con PostgreSQL real (requiere Docker corriendo).

## SonarQube

```bash
# Levantar SonarQube
docker compose up -d sonarqube sonar-db

# Ejecutar análisis
SONAR_TOKEN=tu-token ./gradlew test jacocoTestReport sonar
```

## Docker

```bash
docker compose up -d postgres          # solo BD para dev local
docker compose up -d                   # todo (app + BD + sonar)
docker compose down                    # apagar (mantiene datos)
docker compose down -v                 # apagar y borrar datos
```

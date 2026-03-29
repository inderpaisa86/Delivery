# 🛵 Delivery Core Service

Backend para sistema de pedidos por WhatsApp con asignación automática de domiciliarios, tracking en tiempo real y soporte multi-tenant (SaaS ready).

## Tecnologías

- Java 21
- Spring Boot 3.3.5
- PostgreSQL 16
- JPA / Hibernate
- SpringDoc OpenAPI (Swagger)
- Docker Compose
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
│   │   └── WhatsAppMessageService.java
│   ├── port/
│   │   ├── WhatsAppPort.java              (interfaz)
│   │   └── GeoPort.java                   (interfaz)
│   └── dto/
├── infrastructure/                        → Implementaciones externas
│   ├── persistence/repository/
│   ├── external/
│   │   ├── whatsapp/
│   │   │   ├── WhatsAppAdapter.java       (implementa WhatsAppPort)
│   │   │   └── WhatsAppPayloadParser.java
│   │   └── geo/
│   │       └── HaversineAdapter.java      (implementa GeoPort)
│   ├── config/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── OpenApiConfig.java
│   │   └── SecurityFilter.java
└── interfaces/                            → Controllers REST
    ├── rest/
    │   ├── PedidoController.java
    │   ├── DomiciliarioController.java
    │   └── TrackingController.java
    └── webhook/
        └── WhatsAppWebhookController.java
```

## Principios aplicados

- Clean Architecture: domain → application → infrastructure → interfaces
- SOLID: interfaces desacopladas (WhatsAppPort, GeoPort), SRP en cada clase
- Controllers sin lógica de negocio
- Services contienen los casos de uso
- Repositories solo acceso a datos
- DTOs en toda la API (entidades nunca expuestas)
- Multi-tenant: cada entidad tiene restaurante_id
- @Async en asignación de domiciliarios para no bloquear el flujo principal
- Máquina de estados centralizada con acciones automáticas por transición
- Resolución dinámica de restaurante por número de WhatsApp (SaaS ready)

## Requisitos previos

- Java 21+
- Docker y Docker Compose

## Levantar el proyecto

### 1. Levantar PostgreSQL

```bash
docker compose up -d
```

Crea la BD `delivery_db`, todas las tablas y datos de prueba (1 restaurante, 6 productos, 3 domiciliarios).

### 2. Levantar la aplicación

```bash
./gradlew bootRun
```

Arranca en `http://localhost:8080`.

### 3. Swagger UI

```
http://localhost:8080/swagger-ui.html
```

## Variables de entorno

| Variable | Descripción | Default |
|---|---|---|
| `SERVER_PORT` | Puerto del servidor | `8080` |
| `DB_URL` | URL de PostgreSQL | `jdbc:postgresql://localhost:5432/delivery_db` |
| `DB_USERNAME` | Usuario de BD | `postgres` |
| `DB_PASSWORD` | Contraseña de BD | `postgres` |
| `WHATSAPP_VERIFY_TOKEN` | Token verificación webhook | `mi-token-secreto` |
| `WHATSAPP_API_URL` | URL WhatsApp Business API | `https://graph.facebook.com/v18.0` |
| `WHATSAPP_API_TOKEN` | Token de acceso WhatsApp | (vacío) |
| `WHATSAPP_PHONE_NUMBER_ID` | ID teléfono en Meta | (vacío) |
| `APP_API_TOKEN` | Token endpoints internos | `delivery-internal-token` |

## Endpoints

### Webhook WhatsApp (públicos)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/webhook` | Verificación de Meta |
| `POST` | `/webhook` | Recibir mensajes |

### Pedidos (requieren token)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/pedidos` | Crear pedido |
| `GET` | `/pedidos/{id}` | Obtener pedido |
| `PUT` | `/pedidos/{id}/estado?estado=CONFIRMADO` | Cambiar estado |

### Domiciliarios (requieren token)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/domiciliarios/disponibles?restauranteId=1` | Listar disponibles |
| `POST` | `/domiciliarios/ubicacion` | Actualizar ubicación |

### Tracking (públicos)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/track/{token}` | Tracking público |
| `GET` | `/ubicacion/{domiciliarioId}` | Ubicación domiciliario |

## Autenticación

Endpoints internos requieren:

```
Authorization: Bearer delivery-internal-token
```

Públicos: `/webhook`, `/track/`, `/swagger-ui`

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

### Tracking

```bash
curl http://localhost:8080/track/{tracking-token}
```

## Flujo del sistema

```
Cliente escribe por WhatsApp
        │
        ▼
  POST /webhook → WhatsAppPayloadParser
        │
        ▼
  WhatsAppMessageService (parsea "2 hamburguesas")
        │
        ▼
  PedidoService.crearPedido() → WhatsAppPort.notificar()
        │
        ▼
  Restaurante cambia estado vía API:
  CONFIRMADO → PREPARANDO → LISTO
        │
        ▼
  PedidoStateMachine detecta LISTO
        → AsignacionService.asignarDomiciliario() (@Async + Haversine)
        → Estado cambia a EN_CAMINO
        → WhatsAppPort.notificar()
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

## Tests

```bash
./gradlew test
```

## Docker

```bash
docker compose up -d       # levantar
docker compose down        # apagar (mantiene datos)
docker compose down -v     # apagar y borrar datos
```

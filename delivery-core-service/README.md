# 🛵 Delivery Core Service

Backend para sistema de pedidos por WhatsApp con asignación automática de domiciliarios y tracking en tiempo real.

## Tecnologías

- Java 21
- Spring Boot 3.3.5
- PostgreSQL 16
- JPA / Hibernate
- SpringDoc OpenAPI (Swagger)
- Docker Compose
- Gradle (Kotlin DSL)

## Arquitectura

Clean Architecture con capas desacopladas:

```
org.delivery/
├── config/          → Seguridad, manejo de errores, OpenAPI
├── controller/      → Endpoints REST y Webhook
├── service/         → Interfaces de negocio
│   └── impl/        → Implementaciones
├── repository/      → Acceso a datos (JPA)
├── domain/          → Entidades JPA
│   └── enums/       → Estados (pedido, asignación)
├── dto/             → Objetos de transferencia
└── parser/          → Parseo del payload de WhatsApp
```

## Requisitos previos

- Java 21+
- Docker y Docker Compose
- Gradle (incluido via wrapper)

## Levantar el proyecto

### 1. Clonar el repositorio

```bash
git clone <url-del-repo>
cd delivery-core-service
```

### 2. Levantar PostgreSQL con Docker

```bash
docker compose up -d
```

Esto crea la base de datos `delivery_db`, todas las tablas y carga datos de prueba (6 productos y 3 domiciliarios).

Verificar que está corriendo:

```bash
docker compose ps
```

### 3. Levantar la aplicación

```bash
./gradlew bootRun
```

La app arranca en `http://localhost:8080`.

### 4. Abrir Swagger UI

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
| `WHATSAPP_VERIFY_TOKEN` | Token de verificación del webhook | `mi-token-secreto` |
| `WHATSAPP_API_URL` | URL de WhatsApp Business API | `https://graph.facebook.com/v18.0` |
| `WHATSAPP_API_TOKEN` | Token de acceso de WhatsApp API | (vacío) |
| `WHATSAPP_PHONE_NUMBER_ID` | ID del número de teléfono en Meta | (vacío) |
| `APP_API_TOKEN` | Token para endpoints internos | `delivery-internal-token` |
| `TRACKING_SECRET` | Secret para tokens de tracking | `tracking-secret-key` |

## Endpoints

### Webhook WhatsApp (públicos)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/webhook` | Verificación de Meta |
| `POST` | `/webhook` | Recibir mensajes de WhatsApp |

### Pedidos (requieren token)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/pedidos` | Crear pedido |
| `GET` | `/pedidos/{id}` | Obtener pedido |
| `PUT` | `/pedidos/{id}/estado?estado=CONFIRMADO` | Cambiar estado |

### Domiciliarios (requieren token)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/domiciliarios/disponibles` | Listar disponibles |
| `POST` | `/domiciliarios/ubicacion` | Actualizar ubicación |

### Tracking (públicos)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/track/{token}` | Tracking público del pedido |
| `GET` | `/ubicacion/{domiciliarioId}` | Ubicación del domiciliario |

## Autenticación

Los endpoints internos requieren el header:

```
Authorization: Bearer delivery-internal-token
```

Los endpoints de `/webhook`, `/track/` y `/swagger-ui` son públicos.

## Ejemplos con cURL

### Verificación del webhook

```bash
curl "http://localhost:8080/webhook?hub.mode=subscribe&hub.verify_token=mi-token-secreto&hub.challenge=test123"
```

### Recibir mensaje de WhatsApp

```bash
curl -X POST http://localhost:8080/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "entry": [{
      "changes": [{
        "value": {
          "messages": [{
            "from": "573001234567",
            "type": "text",
            "text": { "body": "2 hamburguesas" }
          }]
        }
      }]
    }]
  }'
```

### Crear pedido vía API

```bash
curl -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer delivery-internal-token" \
  -d '{
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

### Cambiar estado de pedido

```bash
curl -X PUT "http://localhost:8080/pedidos/1/estado?estado=CONFIRMADO" \
  -H "Authorization: Bearer delivery-internal-token"
```

### Actualizar ubicación de domiciliario

```bash
curl -X POST http://localhost:8080/domiciliarios/ubicacion \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer delivery-internal-token" \
  -d '{ "domiciliarioId": 1, "lat": 4.6120, "lng": -74.0800 }'
```

### Tracking público de pedido

```bash
curl http://localhost:8080/track/{tracking-token}
```

## Flujo del sistema

```
Cliente escribe por WhatsApp
        │
        ▼
  POST /webhook (Meta envía el mensaje)
        │
        ▼
  WhatsAppPayloadParser (extrae from + body)
        │
        ▼
  WhatsAppMessageService (parsea "2 hamburguesas")
        │
        ▼
  PedidoService.crearPedido() → notifica al cliente
        │
        ▼
  Restaurante cambia estado: CONFIRMADO → PREPARANDO → LISTO
        │
        ▼
  Al marcar LISTO → AsignacionService asigna domiciliario más cercano (Haversine)
        │
        ▼
  Domiciliario actualiza ubicación → cliente hace tracking en tiempo real
        │
        ▼
  Estado ENTREGADO → notificación final al cliente
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

## Apagar PostgreSQL

```bash
docker compose down       # mantiene datos
docker compose down -v    # borra datos y volúmenes
```

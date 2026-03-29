# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versionado con [Semantic Versioning](https://semver.org/lang/es/).

## [1.0.0] - 2026-03-28

### Agregado
- Webhook WhatsApp Business (GET verificación + POST mensajes)
- Creación de pedidos vía API REST y WhatsApp
- Máquina de estados: NUEVO → CONFIRMADO → PREPARANDO → LISTO → EN_CAMINO → ENTREGADO / CANCELADO
- Asignación automática de domiciliarios (Haversine, @Async)
- Liberación automática de domiciliario en ENTREGADO y CANCELADO
- Tracking en tiempo real con token único por pedido
- Notificaciones automáticas por WhatsApp en cada cambio de estado
- Soporte multi-restaurante (SaaS ready)
- Seguridad por token en endpoints internos
- Swagger UI en /swagger-ui.html
- Docker Compose con PostgreSQL y SonarQube
- JaCoCo coverage > 90%
- Clean Architecture: domain → application → infrastructure → interfaces
- Lombok en entidades y services

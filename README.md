# 🛵 Delivery App

Sistema de gestión de pedidos para restaurantes con asignación automática de domiciliarios, tracking en tiempo real e integración con WhatsApp.

## Arquitectura

```
┌─────────────────┐     REST API      ┌──────────────────────┐
│  delivery-front │ ───────────────── │ delivery-core-service │
│  React + TS     │    /api proxy     │ Spring Boot + JPA     │
│  Tailwind CSS   │                   │ PostgreSQL            │
│  Leaflet Maps   │                   │ WhatsApp Business API │
└─────────────────┘                   └──────────────────────┘
     :5173                                   :8080
```

## Módulos

| Módulo | Tecnología | Descripción |
|---|---|---|
| `delivery-core-service` | Java 21, Spring Boot, PostgreSQL | Backend REST API, lógica de negocio, WhatsApp |
| `delivery-front` | React 19, TypeScript, Tailwind, Leaflet | Frontend SPA, panel restaurante y domiciliario |

## Inicio rápido

### Requisitos

- Java 21+
- Node.js 18+
- Docker y Docker Compose

### 1. Base de datos

```bash
cd delivery-core-service
docker compose up -d postgres
```

### 2. Backend

```bash
cd delivery-core-service
./gradlew bootRun
```

Backend disponible en `http://localhost:8080`
Swagger UI en `http://localhost:8080/swagger-ui.html`

### 3. Frontend

```bash
cd delivery-front
npm install
npm run dev
```

Frontend disponible en `http://localhost:5173`

## Funcionalidades principales

- Panel de restaurante: gestión de pedidos con máquina de estados, productos y domiciliarios
- Panel de domiciliario: mapa en tiempo real, geolocalización, gestión de entregas
- Tracking público de pedidos con mapa interactivo
- Integración WhatsApp Business API para recepción de pedidos
- Soporte multi-restaurante (multi-tenant)
- Asignación automática de domiciliarios por proximidad (Haversine)

## Estados del pedido

```
NUEVO → CONFIRMADO → PREPARANDO → LISTO → EN_CAMINO → ENTREGADO
  │         │            │          │         │
  └─────────┴────────────┴──────────┴─────────┴──→ CANCELADO
```

## Documentación detallada

- [Backend — delivery-core-service](delivery-core-service/README.md)
- [Frontend — delivery-front](delivery-front/README.md)

# 🛵 Delivery App

Sistema de gestión de pedidos para restaurantes con asignación automática de domiciliarios, tracking en tiempo real, bot de WhatsApp y panel administrativo.

## Arquitectura

```
┌─────────────────┐     REST API      ┌──────────────────────┐
│  delivery-front │ ───────────────── │ delivery-core-service │
│  React + TS     │    /api proxy     │ Spring Boot + JPA     │
│  Tailwind CSS   │                   │ PostgreSQL            │
│  Leaflet Maps   │                   └──────────┬───────────┘
└─────────────────┘                              │ REST API
     :5173                            ┌──────────┴───────────┐
                                      │    delivery-bot       │
                                      │    Node.js + TS       │
                                      │    whatsapp-web.js    │
                                      └──────────────────────┘
                                           WhatsApp Web
```

## Módulos

| Módulo | Tecnología | Descripción |
|---|---|---|
| `delivery-core-service` | Java 21, Spring Boot 3, PostgreSQL 16 | Backend REST API, lógica de negocio, autenticación |
| `delivery-front` | React 19, TypeScript, Tailwind CSS 4, Leaflet | Frontend SPA con panel restaurante y domiciliario |
| `delivery-bot` | Node.js, TypeScript, whatsapp-web.js | Bot de WhatsApp para recepción de pedidos vía chat |

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

Backend en `http://localhost:8080` — Swagger en `http://localhost:8080/swagger-ui.html`

### 3. Frontend

```bash
cd delivery-front
npm install
npm run dev
```

Frontend en `http://localhost:5173`

### 4. Bot de WhatsApp (opcional)

```bash
cd delivery-bot
npm install
npm start
```

Escanea el QR que aparece en la terminal con WhatsApp.

## Funcionalidades

- Autenticación con usuario/contraseña para restaurante, cédula para domiciliario
- Gestión de usuarios del restaurante con foto de perfil
- Panel de pedidos con stepper visual de estados y filtros
- CRUD de productos, domiciliarios y usuarios
- Panel de domiciliario con mapa interactivo (OpenStreetMap), geolocalización y actualización de ubicación
- Tracking público de pedidos en `/track/:token`
- Bot de WhatsApp: menú, pedidos por texto, ubicación por GPS, seguimiento
- Soporte multi-restaurante (multi-tenant)
- Asignación automática de domiciliarios por proximidad (Haversine)
- Normalización de plurales en español para pedidos por WhatsApp
- Fotos de perfil para domiciliarios y usuarios (base64 en BD)
- Dashboard filtra solo pedidos del día

## Estados del pedido

```
NUEVO → CONFIRMADO → PREPARANDO → LISTO → EN_CAMINO → ENTREGADO
  │         │            │          │         │
  └─────────┴────────────┴──────────┴─────────┴──→ CANCELADO
```

## Flujo WhatsApp

```
Cliente: "menu"           → Bot responde con lista de productos
Cliente: "2 hamburguesas" → Bot crea pedido y pide ubicación
Cliente: (📍 ubicación)   → Bot confirma dirección y link de tracking
Restaurante confirma      → Cliente recibe notificación
Domiciliario entrega      → Cliente recibe confirmación final
```

## Documentación detallada

- [Backend — delivery-core-service](delivery-core-service/README.md)
- [Frontend — delivery-front](delivery-front/README.md)
- [Bot WhatsApp — delivery-bot](delivery-bot/README.md)

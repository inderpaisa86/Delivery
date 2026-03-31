# 🤖 Delivery Bot

Bot de WhatsApp para recepción de pedidos. Se conecta vía WhatsApp Web y consume la API del backend.

## Requisitos

- Node.js 18+
- Backend `delivery-core-service` corriendo en `http://localhost:8080`
- Un número de WhatsApp para el bot

## Instalación

```bash
cd delivery-bot
npm install
```

## Levantar

```bash
npm start
```

1. Aparece un código QR en la terminal
2. Abre WhatsApp en tu celular → Dispositivos vinculados → Vincular dispositivo
3. Escanea el QR
4. El bot queda conectado y listo

La sesión se guarda en `.wwebjs_auth/`, no necesitas escanear el QR de nuevo.

## Variables de entorno

| Variable | Descripción | Default |
|---|---|---|
| `API_URL` | URL del backend | `http://localhost:8080` |
| `API_TOKEN` | Token de autenticación | `delivery-internal-token` |
| `RESTAURANTE_ID` | ID del restaurante por defecto | `1` |
| `TRACKING_URL` | URL base del frontend para tracking | `http://localhost:5173` |

## Comandos del bot

| Mensaje | Respuesta |
|---|---|
| `hola` | Mensaje de bienvenida |
| `menu` | Lista de productos con precios |
| `2 hamburguesas, 1 gaseosa` | Crea pedido y pide ubicación |
| (ubicación compartida) | Confirma pedido con dirección |
| `cancelar` | Cancela pedido pendiente |

## Flujo de conversación

```
Cliente: "hola"
Bot: "¡Bienvenido! Escribe menu o envía tu pedido"

Cliente: "menu"
Bot: "📋 Menú: Hamburguesa $15,000 ..."

Cliente: "2 hamburguesas, 1 gaseosa"
Bot: "✅ Pedido #42 recibido. Total: $35,000. Envía tu ubicación."

Cliente: (comparte ubicación)
Bot: "✅ Ubicación recibida. Te avisaremos cuando esté listo."
```

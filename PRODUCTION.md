# 🚀 Guía de Despliegue a Producción

## 1. Variables de entorno obligatorias

### Backend (delivery-core-service)

```bash
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# Base de datos — CAMBIAR
DB_URL=jdbc:postgresql://tu-host-db:5432/delivery_db
DB_USERNAME=delivery_user
DB_PASSWORD=contraseña-segura-generada

# JWT — CAMBIAR el secret (mínimo 32 caracteres, aleatorio)
JWT_SECRET=generar-con-openssl-rand-base64-32
JWT_EXPIRATION_MS=86400000

# Token legacy para el bot — CAMBIAR
APP_API_TOKEN=generar-token-aleatorio-largo

# WhatsApp
WHATSAPP_VERIFY_TOKEN=tu-token-verificacion
WHATSAPP_API_URL=https://graph.facebook.com/v18.0
WHATSAPP_API_TOKEN=tu-token-meta
WHATSAPP_PHONE_NUMBER_ID=tu-phone-id

# Tracking
TRACKING_SECRET=generar-secret-aleatorio

# Rate limiting
RATE_LIMIT_WEBHOOK=60
```

### Frontend (delivery-front)

```bash
VITE_API_TOKEN=mismo-valor-que-APP_API_TOKEN
```

### Bot (delivery-bot)

```bash
API_URL=https://tu-dominio.com
API_TOKEN=mismo-valor-que-APP_API_TOKEN
RESTAURANTE_ID=1
TRACKING_URL=https://tu-dominio.com
```

## 2. Generar secrets seguros

```bash
# JWT Secret
openssl rand -base64 32

# API Token
openssl rand -hex 32

# Tracking Secret
openssl rand -hex 16

# Contraseña de BD
openssl rand -base64 24
```

## 3. Base de datos

- Usar PostgreSQL 16+ en un servicio gestionado (AWS RDS, Google Cloud SQL, etc.) o servidor dedicado
- NO usar `ddl-auto: update` en producción (el perfil `prod` ya usa `validate`)
- Ejecutar migraciones Flyway antes de desplegar: `./gradlew flywayMigrate`
- Configurar backups automáticos diarios
- Crear un usuario de BD con permisos limitados (no usar `postgres`)

```sql
CREATE USER delivery_user WITH PASSWORD 'contraseña-segura';
GRANT CONNECT ON DATABASE delivery_db TO delivery_user;
GRANT USAGE ON SCHEMA public TO delivery_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO delivery_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO delivery_user;
```

## 4. HTTPS

Obligatorio. Opciones:
- Nginx como reverse proxy con Let's Encrypt (certbot)
- Cloudflare (proxy DNS)
- AWS ALB/ELB con certificado ACM

Ejemplo nginx:

```nginx
server {
    listen 443 ssl;
    server_name tu-dominio.com;

    ssl_certificate /etc/letsencrypt/live/tu-dominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/tu-dominio.com/privkey.pem;

    # Frontend
    location / {
        root /var/www/delivery-front/dist;
        try_files $uri $uri/ /index.html;
    }

    # API Backend
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Webhook WhatsApp (público)
    location /webhook {
        proxy_pass http://localhost:8080/webhook;
    }

    # Tracking público
    location /track/ {
        root /var/www/delivery-front/dist;
        try_files $uri /index.html;
    }
}

server {
    listen 80;
    server_name tu-dominio.com;
    return 301 https://$host$request_uri;
}
```

## 5. Build del frontend

```bash
cd delivery-front
npm run build
```

Los archivos estáticos quedan en `delivery-front/dist/`. Copiar al servidor web.

## 6. Build del backend

```bash
cd delivery-core-service
./gradlew bootJar
```

El JAR queda en `build/libs/`. Ejecutar con:

```bash
java -jar delivery-core-service-*.jar
```

O usar Docker:

```bash
docker compose -f compose.yaml up -d
```

## 7. Checklist de seguridad

- [ ] HTTPS habilitado en todo el dominio
- [ ] JWT_SECRET cambiado (no usar el default)
- [ ] APP_API_TOKEN cambiado (no usar el default)
- [ ] Contraseñas de BD cambiadas
- [ ] TRACKING_SECRET cambiado
- [ ] Perfil `prod` activo (Swagger deshabilitado, logs JSON, ddl-auto: validate)
- [ ] Contraseñas de usuarios hasheadas con BCrypt (el login las migra automáticamente)
- [ ] CORS configurado solo para tu dominio
- [ ] Rate limiting activo en webhook
- [ ] Backups de BD configurados
- [ ] Monitoreo de health check (`/actuator/health`)

## 8. CORS (si frontend y backend están en dominios diferentes)

Agregar configuración CORS en el backend. Crear archivo:

```java
// src/main/java/org/delivery/infrastructure/config/CorsConfig.java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("https://tu-dominio.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*");
    }
}
```

## 9. Monitoreo

- Health check: `GET /actuator/health`
- Logs en formato JSON (perfil prod) — enviar a ELK, CloudWatch o similar
- Configurar alertas para errores 5xx
- Monitorear uso de CPU/memoria del servidor

## 10. Bot de WhatsApp

- El bot corre como proceso independiente en el servidor
- Usar `pm2` o `systemd` para mantenerlo corriendo:

```bash
# Con pm2
npm install -g pm2
cd delivery-bot
pm2 start npm --name "delivery-bot" -- start
pm2 save
pm2 startup
```

- La sesión de WhatsApp se guarda en `.wwebjs_auth/` — hacer backup de esa carpeta
- Si el bot se reinicia, puede necesitar re-escanear el QR

## 11. Estructura de despliegue recomendada

```
Servidor / VPS
├── /opt/delivery/
│   ├── backend/
│   │   ├── delivery-core-service.jar
│   │   └── .env
│   ├── frontend/
│   │   └── dist/          ← archivos estáticos
│   ├── bot/
│   │   ├── src/
│   │   ├── node_modules/
│   │   ├── .wwebjs_auth/  ← sesión WhatsApp
│   │   └── package.json
│   └── nginx/
│       └── delivery.conf
├── PostgreSQL (local o remoto)
└── Nginx (reverse proxy + SSL)
```

## 12. Docker Compose para producción

```bash
cd delivery-core-service
docker compose up -d
```

Asegurarse de que el `.env` tenga `SPRING_PROFILES_ACTIVE=prod` y todas las variables configuradas.

## 13. Costos estimados (servidor mínimo)

- VPS básico (2 CPU, 4GB RAM): ~$10-20/mes (DigitalOcean, Hetzner, AWS Lightsail)
- Dominio: ~$10-15/año
- SSL: gratis con Let's Encrypt
- PostgreSQL: incluido en el VPS o ~$15/mes gestionado
- WhatsApp Business API: gratis las primeras 1000 conversaciones/mes

CREATE TABLE IF NOT EXISTS restaurantes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    direccion VARCHAR(255),
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    whatsapp_phone_id VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    telefono VARCHAR(20) NOT NULL,
    nombre VARCHAR(100),
    direccion VARCHAR(255),
    restaurante_id BIGINT NOT NULL REFERENCES restaurantes(id),
    UNIQUE(telefono, restaurante_id)
);

CREATE TABLE IF NOT EXISTS productos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    precio NUMERIC(10, 2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    restaurante_id BIGINT NOT NULL REFERENCES restaurantes(id)
);

CREATE TABLE IF NOT EXISTS pedidos (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    restaurante_id BIGINT NOT NULL REFERENCES restaurantes(id),
    direccion VARCHAR(255),
    estado VARCHAR(20) NOT NULL DEFAULT 'NUEVO',
    total NUMERIC(12, 2) NOT NULL DEFAULT 0,
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    fecha TIMESTAMP NOT NULL DEFAULT NOW(),
    tracking_token VARCHAR(64) UNIQUE
);

CREATE TABLE IF NOT EXISTS detalle_pedidos (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedidos(id),
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    cantidad INTEGER NOT NULL,
    precio NUMERIC(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS domiciliarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    disponible BOOLEAN NOT NULL DEFAULT TRUE,
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    restaurante_id BIGINT NOT NULL REFERENCES restaurantes(id)
);

CREATE TABLE IF NOT EXISTS asignaciones_domicilio (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL UNIQUE REFERENCES pedidos(id),
    domiciliario_id BIGINT NOT NULL REFERENCES domiciliarios(id),
    estado VARCHAR(20) NOT NULL DEFAULT 'ASIGNADO',
    fecha TIMESTAMP NOT NULL DEFAULT NOW()
);

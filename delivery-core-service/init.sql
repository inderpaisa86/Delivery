-- Tablas del sistema de delivery

CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    telefono VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100),
    direccion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS productos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    precio NUMERIC(10, 2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS pedidos (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
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
    lng DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS asignaciones_domicilio (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL UNIQUE REFERENCES pedidos(id),
    domiciliario_id BIGINT NOT NULL REFERENCES domiciliarios(id),
    estado VARCHAR(20) NOT NULL DEFAULT 'ASIGNADO',
    fecha TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Datos de prueba

INSERT INTO productos (nombre, precio, activo) VALUES
    ('hamburguesa', 15000, true),
    ('pizza', 25000, true),
    ('perro', 10000, true),
    ('empanada', 3000, true),
    ('gaseosa', 4000, true),
    ('jugo', 5000, true);

INSERT INTO domiciliarios (nombre, telefono, disponible, lat, lng) VALUES
    ('Carlos Ruiz', '573101111111', true, 4.6097, -74.0817),
    ('Ana López', '573102222222', true, 4.6200, -74.0750),
    ('Pedro Gómez', '573103333333', true, 4.6150, -74.0900);

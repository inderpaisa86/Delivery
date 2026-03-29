INSERT INTO restaurantes (nombre, telefono, direccion, lat, lng, activo, whatsapp_phone_id) VALUES
    ('La Cocina Invisible', '573001000000', 'Calle 85 #15-10, Bogotá', 4.6740, -74.0540, true, 'default')
ON CONFLICT DO NOTHING;

INSERT INTO productos (nombre, precio, activo, restaurante_id) VALUES
    ('hamburguesa', 15000, true, 1),
    ('pizza', 25000, true, 1),
    ('perro', 10000, true, 1),
    ('empanada', 3000, true, 1),
    ('gaseosa', 4000, true, 1),
    ('jugo', 5000, true, 1)
ON CONFLICT DO NOTHING;

INSERT INTO domiciliarios (nombre, telefono, disponible, lat, lng, restaurante_id) VALUES
    ('Carlos Ruiz', '573101111111', true, 4.6097, -74.0817, 1),
    ('Ana López', '573102222222', true, 4.6200, -74.0750, 1),
    ('Pedro Gómez', '573103333333', true, 4.6150, -74.0900, 1)
ON CONFLICT DO NOTHING;

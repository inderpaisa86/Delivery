package org.delivery.infrastructure.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        // Soportar contraseñas legacy (texto plano) durante migración
        if (!hashedPassword.startsWith("$2")) {
            return rawPassword.equals(hashedPassword);
        }
        return encoder.matches(rawPassword, hashedPassword);
    }
}

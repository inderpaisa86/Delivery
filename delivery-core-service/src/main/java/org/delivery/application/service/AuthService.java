package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.LoginRequest;
import org.delivery.application.dto.LoginResponse;
import org.delivery.domain.entity.Usuario;
import org.delivery.infrastructure.config.JwtService;
import org.delivery.infrastructure.config.PasswordService;
import org.delivery.infrastructure.persistence.repository.IUsuarioRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final IUsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordService passwordService;

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña incorrectos"));

        if (!passwordService.matches(request.password(), usuario.getPassword())) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos");
        }

        // Si la contraseña es legacy (texto plano), migrar a BCrypt
        if (!usuario.getPassword().startsWith("$2")) {
            usuario.setPassword(passwordService.hash(request.password()));
            usuarioRepository.save(usuario);
        }

        Long restauranteId = usuario.getRestaurante() != null ? usuario.getRestaurante().getId() : null;
        String restauranteNombre = usuario.getRestaurante() != null ? usuario.getRestaurante().getNombre() : null;

        String token = jwtService.generateToken(
                usuario.getId(), usuario.getUsername(), usuario.getRol(), restauranteId);

        return new LoginResponse(
                usuario.getId(), usuario.getUsername(), usuario.getRol(),
                usuario.getPerfil(),
                restauranteId, restauranteNombre, token);
    }
}

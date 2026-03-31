package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.LoginRequest;
import org.delivery.application.dto.LoginResponse;
import org.delivery.domain.entity.Usuario;
import org.delivery.infrastructure.persistence.repository.IUsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final IUsuarioRepository usuarioRepository;

    @Value("${app.api.token}")
    private String apiToken;

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña incorrectos"));

        if (!usuario.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos");
        }

        String restauranteNombre = usuario.getRestaurante() != null
                ? usuario.getRestaurante().getNombre() : null;
        Long restauranteId = usuario.getRestaurante() != null
                ? usuario.getRestaurante().getId() : null;

        return new LoginResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getRol(),
                restauranteId,
                restauranteNombre,
                apiToken
        );
    }
}

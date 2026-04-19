package org.delivery.application.service;

import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.UsuarioRequest;
import org.delivery.application.dto.UsuarioResponse;
import org.delivery.domain.entity.Restaurante;
import org.delivery.domain.entity.Usuario;
import org.delivery.infrastructure.config.PasswordService;
import org.delivery.infrastructure.persistence.repository.IRestauranteRepository;
import org.delivery.infrastructure.persistence.repository.IUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final IUsuarioRepository usuarioRepository;
    private final IRestauranteRepository restauranteRepository;
    private final PasswordService passwordService;

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.findByUsernameAndActivoTrue(request.username()).isPresent()) {
            throw new IllegalArgumentException("El usuario ya existe: " + request.username());
        }
        Restaurante r = restauranteRepository.findById(request.restauranteId())
                .orElseThrow(() -> new IllegalArgumentException("Restaurante no encontrado"));

        Usuario u = Usuario.builder()
                .username(request.username())
                .password(passwordService.hash(request.password()))
                .foto(request.foto())
                .rol("restaurante")
                .perfil(request.perfil() != null ? request.perfil() : "operario")
                .restaurante(r)
                .build();
        return toResponse(usuarioRepository.save(u));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        u.setUsername(request.username());
        if (request.password() != null && !request.password().isBlank()) {
            u.setPassword(passwordService.hash(request.password()));
        }
        u.setFoto(request.foto());
        return toResponse(usuarioRepository.save(u));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarPorRestaurante(Long restauranteId) {
        return usuarioRepository.findByRestauranteIdAndActivoTrue(restauranteId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void desactivar(Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        u.setActivo(false);
        usuarioRepository.save(u);
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getUsername(), u.getFoto(), u.getRol(), u.getPerfil(), u.isActivo(),
                u.getRestaurante() != null ? u.getRestaurante().getId() : null,
                u.getRestaurante() != null ? u.getRestaurante().getNombre() : null);
    }
}

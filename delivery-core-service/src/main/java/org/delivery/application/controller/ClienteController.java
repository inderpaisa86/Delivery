package org.delivery.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.ClienteResponse;
import org.delivery.domain.entity.Cliente;
import org.delivery.infrastructure.persistence.repository.IClienteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Consulta de clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final IClienteRepository clienteRepository;

    @GetMapping("/buscar")
    @Operation(summary = "Buscar cliente por teléfono y restaurante")
    public ResponseEntity<ClienteResponse> buscar(
            @RequestParam String telefono,
            @RequestParam Long restauranteId) {
        return clienteRepository.findByTelefonoAndRestauranteId(telefono, restauranteId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private ClienteResponse toResponse(Cliente c) {
        return new ClienteResponse(
                c.getId(), c.getTelefono(), c.getNombre(), c.getDireccion(),
                c.getRestaurante().getId());
    }
}

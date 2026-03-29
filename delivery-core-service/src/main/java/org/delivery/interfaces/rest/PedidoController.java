package org.delivery.interfaces.rest;

import org.delivery.application.dto.PedidoRequest;
import org.delivery.application.dto.PedidoResponse;
import org.delivery.application.service.PedidoService;
import org.delivery.domain.enums.EstadoPedido;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * API REST de pedidos. Sin lógica de negocio, solo delega al service.
 */
@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos", description = "Gestión del ciclo de vida de pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @Operation(summary = "Crear pedido", description = "Crea un pedido con detalles de productos para un restaurante")
    public ResponseEntity<PedidoResponse> crearPedido(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crearPedido(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener pedido", description = "Retorna el detalle completo de un pedido")
    public ResponseEntity<PedidoResponse> obtenerPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPedido(id));
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado", description = "Cambia el estado del pedido. Dispara acciones automáticas (asignación, notificaciones)")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoPedido estado) {
        return ResponseEntity.ok(pedidoService.cambiarEstado(id, estado));
    }
}

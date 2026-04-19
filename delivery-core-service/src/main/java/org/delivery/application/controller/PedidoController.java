package org.delivery.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.delivery.application.dto.PedidoRequest;
import org.delivery.application.dto.PedidoResponse;
import org.delivery.application.service.PedidoService;
import org.delivery.domain.enums.EstadoPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos", description = "Gestión del ciclo de vida de pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @Operation(summary = "Crear pedido")
    public ResponseEntity<PedidoResponse> crearPedido(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crearPedido(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener pedido")
    public ResponseEntity<PedidoResponse> obtenerPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPedido(id));
    }

    @GetMapping
    @Operation(summary = "Listar pedidos por restaurante (paginado)")
    public ResponseEntity<Page<PedidoResponse>> listarPorRestaurante(
            @RequestParam Long restauranteId, Pageable pageable) {
        return ResponseEntity.ok(pedidoService.listarPorRestaurante(restauranteId, pageable));
    }

    @GetMapping("/hoy")
    @Operation(summary = "Listar pedidos del día por restaurante (paginado)")
    public ResponseEntity<Page<PedidoResponse>> listarHoy(
            @RequestParam Long restauranteId, Pageable pageable) {
        return ResponseEntity.ok(pedidoService.listarPorRestauranteHoy(restauranteId, pageable));
    }

    @GetMapping("/cliente/{telefono}")
    @Operation(summary = "Historial de pedidos por cliente")
    public ResponseEntity<Page<PedidoResponse>> listarPorCliente(
            @PathVariable String telefono, Pageable pageable) {
        return ResponseEntity.ok(pedidoService.listarPorCliente(telefono, pageable));
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado", description = "Dispara acciones automáticas según el estado")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoPedido estado) {
        return ResponseEntity.ok(pedidoService.cambiarEstado(id, estado));
    }

    @PutMapping("/{id}/ubicacion")
    @Operation(summary = "Actualizar ubicación de entrega del pedido")
    public ResponseEntity<PedidoResponse> actualizarUbicacion(
            @PathVariable Long id,
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false) String direccion) {
        return ResponseEntity.ok(pedidoService.actualizarUbicacionPorId(id, lat, lng, direccion));
    }
}

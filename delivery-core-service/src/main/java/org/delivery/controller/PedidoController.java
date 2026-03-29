package org.delivery.controller;

import org.delivery.domain.enums.EstadoPedido;
import org.delivery.dto.PedidoRequest;
import org.delivery.dto.PedidoResponse;
import org.delivery.service.AsignacionService;
import org.delivery.service.PedidoService;
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

import jakarta.validation.Valid;

/**
 * API REST para gestión de pedidos.
 */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final AsignacionService asignacionService;

    public PedidoController(PedidoService pedidoService, AsignacionService asignacionService) {
        this.pedidoService = pedidoService;
        this.asignacionService = asignacionService;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(@Valid @RequestBody PedidoRequest request) {
        PedidoResponse response = pedidoService.crearPedido(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtenerPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPedido(id));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoPedido estado) {
        PedidoResponse response = pedidoService.cambiarEstado(id, estado);

        // Si el pedido está LISTO, intentar asignar domiciliario automáticamente
        if (estado == EstadoPedido.LISTO) {
            asignacionService.asignarDomiciliario(id);
        }

        return ResponseEntity.ok(response);
    }
}

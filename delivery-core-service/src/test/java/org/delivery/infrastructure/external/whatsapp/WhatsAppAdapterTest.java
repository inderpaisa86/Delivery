package org.delivery.infrastructure.external.whatsapp;

import org.delivery.domain.enums.EstadoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("WhatsAppAdapter")
class WhatsAppAdapterTest {

    @Test
    @DisplayName("No envía si phoneNumberId está vacío")
    void shouldNotSendWhenNotConfigured() {
        WhatsAppAdapter adapter = new WhatsAppAdapter(
                "https://graph.facebook.com/v18.0", "token", "");
        assertDoesNotThrow(() -> adapter.enviarMensaje("573001234567", "Hola"));
    }

    @Test
    @DisplayName("Genera mensaje para cada estado")
    void shouldNotifyAllStates() {
        WhatsAppAdapter adapter = new WhatsAppAdapter(
                "https://graph.facebook.com/v18.0", "token", "");
        for (EstadoPedido estado : EstadoPedido.values()) {
            assertDoesNotThrow(() ->
                    adapter.notificarCambioEstado("573001234567", 1L, estado));
        }
    }
}

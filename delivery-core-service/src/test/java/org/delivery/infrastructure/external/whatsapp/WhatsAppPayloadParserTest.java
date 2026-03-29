package org.delivery.infrastructure.external.whatsapp;

import java.util.List;
import java.util.Map;

import org.delivery.application.dto.WhatsAppMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WhatsAppPayloadParser")
class WhatsAppPayloadParserTest {

    @Test
    @DisplayName("Extrae mensaje de texto correctamente")
    void shouldExtractTextMessage() {
        Map<String, Object> payload = Map.of(
                "entry", List.of(Map.of(
                        "changes", List.of(Map.of(
                                "value", Map.of(
                                        "messages", List.of(Map.of(
                                                "from", "573001234567",
                                                "type", "text",
                                                "text", Map.of("body", "2 hamburguesas")
                                        ))))))));

        List<WhatsAppMessage> messages = WhatsAppPayloadParser.extractMessages(payload);

        assertEquals(1, messages.size());
        assertEquals("573001234567", messages.get(0).from());
        assertEquals("2 hamburguesas", messages.get(0).body());
    }

    @Test
    @DisplayName("Retorna vacío cuando no hay entry")
    void shouldReturnEmptyWhenNoEntry() {
        assertTrue(WhatsAppPayloadParser.extractMessages(Map.of()).isEmpty());
    }

    @Test
    @DisplayName("Ignora mensajes no-text")
    void shouldIgnoreNonTextMessages() {
        Map<String, Object> payload = Map.of(
                "entry", List.of(Map.of(
                        "changes", List.of(Map.of(
                                "value", Map.of(
                                        "messages", List.of(Map.of(
                                                "from", "573001234567",
                                                "type", "image"))))))));

        assertTrue(WhatsAppPayloadParser.extractMessages(payload).isEmpty());
    }

    @Test
    @DisplayName("Extrae múltiples mensajes")
    void shouldExtractMultipleMessages() {
        Map<String, Object> payload = Map.of(
                "entry", List.of(Map.of(
                        "changes", List.of(Map.of(
                                "value", Map.of(
                                        "messages", List.of(
                                                Map.of("from", "111", "type", "text",
                                                        "text", Map.of("body", "Hola")),
                                                Map.of("from", "222", "type", "text",
                                                        "text", Map.of("body", "Menú")))))))));

        assertEquals(2, WhatsAppPayloadParser.extractMessages(payload).size());
    }
}

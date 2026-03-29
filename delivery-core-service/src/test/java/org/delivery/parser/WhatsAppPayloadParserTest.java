package org.delivery.parser;

import java.util.List;
import java.util.Map;

import org.delivery.dto.WhatsAppMessage;
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
                                                "text", Map.of("body", "Quiero un combo 1")
                                        ))
                                )
                        ))
                ))
        );

        List<WhatsAppMessage> messages = WhatsAppPayloadParser.extractMessages(payload);

        assertEquals(1, messages.size());
        assertEquals("573001234567", messages.get(0).from());
        assertEquals("Quiero un combo 1", messages.get(0).body());
    }

    @Test
    @DisplayName("Retorna lista vacía cuando no hay entry")
    void shouldReturnEmptyWhenNoEntry() {
        List<WhatsAppMessage> messages = WhatsAppPayloadParser.extractMessages(Map.of());

        assertTrue(messages.isEmpty());
    }

    @Test
    @DisplayName("Retorna lista vacía cuando entry es null")
    void shouldReturnEmptyWhenEntryIsNull() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("entry", null);

        List<WhatsAppMessage> messages = WhatsAppPayloadParser.extractMessages(payload);

        assertTrue(messages.isEmpty());
    }

    @Test
    @DisplayName("Ignora mensajes que no son de tipo text")
    void shouldIgnoreNonTextMessages() {
        Map<String, Object> payload = Map.of(
                "entry", List.of(Map.of(
                        "changes", List.of(Map.of(
                                "value", Map.of(
                                        "messages", List.of(Map.of(
                                                "from", "573001234567",
                                                "type", "image"
                                        ))
                                )
                        ))
                ))
        );

        List<WhatsAppMessage> messages = WhatsAppPayloadParser.extractMessages(payload);

        assertTrue(messages.isEmpty());
    }

    @Test
    @DisplayName("Extrae múltiples mensajes de un mismo payload")
    void shouldExtractMultipleMessages() {
        Map<String, Object> payload = Map.of(
                "entry", List.of(Map.of(
                        "changes", List.of(Map.of(
                                "value", Map.of(
                                        "messages", List.of(
                                                Map.of("from", "573001111111", "type", "text",
                                                        "text", Map.of("body", "Hola")),
                                                Map.of("from", "573002222222", "type", "text",
                                                        "text", Map.of("body", "Menú"))
                                        )
                                )
                        ))
                ))
        );

        List<WhatsAppMessage> messages = WhatsAppPayloadParser.extractMessages(payload);

        assertEquals(2, messages.size());
    }

    @Test
    @DisplayName("Maneja value sin key messages")
    void shouldHandleValueWithoutMessages() {
        Map<String, Object> payload = Map.of(
                "entry", List.of(Map.of(
                        "changes", List.of(Map.of(
                                "value", Map.of("statuses", List.of())
                        ))
                ))
        );

        List<WhatsAppMessage> messages = WhatsAppPayloadParser.extractMessages(payload);

        assertTrue(messages.isEmpty());
    }
}

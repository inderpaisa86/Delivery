package org.delivery.infrastructure.external.whatsapp;

import java.util.List;
import java.util.Map;

import org.delivery.application.dto.WhatsAppMessage;

/**
 * Parsea el payload JSON que envía Meta en el webhook de WhatsApp Business.
 */
public final class WhatsAppPayloadParser {

    private WhatsAppPayloadParser() {}

    @SuppressWarnings("unchecked")
    public static List<WhatsAppMessage> extractMessages(Map<String, Object> payload) {
        List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
        if (entries == null) return List.of();

        return entries.stream()
                .flatMap(entry -> {
                    List<Map<String, Object>> changes =
                            (List<Map<String, Object>>) entry.get("changes");
                    return changes != null ? changes.stream() : java.util.stream.Stream.empty();
                })
                .map(change -> (Map<String, Object>) change.get("value"))
                .filter(value -> value != null && value.containsKey("messages"))
                .flatMap(value -> {
                    List<Map<String, Object>> msgs =
                            (List<Map<String, Object>>) value.get("messages");
                    return msgs != null ? msgs.stream() : java.util.stream.Stream.empty();
                })
                .filter(msg -> "text".equals(msg.get("type")))
                .map(msg -> {
                    String from = (String) msg.get("from");
                    Map<String, String> text = (Map<String, String>) msg.get("text");
                    String body = text != null ? text.get("body") : "";
                    return new WhatsAppMessage(from, body);
                })
                .toList();
    }
}

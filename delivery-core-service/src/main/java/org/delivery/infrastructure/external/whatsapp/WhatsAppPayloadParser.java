package org.delivery.infrastructure.external.whatsapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.delivery.application.dto.WhatsAppMessage;

/**
 * Parsea el payload JSON que envía Meta en el webhook de WhatsApp Business.
 * Soporta mensajes de texto y de ubicación.
 */
public final class WhatsAppPayloadParser {

    private WhatsAppPayloadParser() {}

    @SuppressWarnings("unchecked")
    public static List<WhatsAppMessage> extractMessages(Map<String, Object> payload) {
        List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
        if (entries == null) return List.of();

        List<WhatsAppMessage> result = new ArrayList<>();

        for (Map<String, Object> entry : entries) {
            List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
            if (changes == null) continue;

            for (Map<String, Object> change : changes) {
                Map<String, Object> value = (Map<String, Object>) change.get("value");
                if (value == null || !value.containsKey("messages")) continue;

                List<Map<String, Object>> msgs = (List<Map<String, Object>>) value.get("messages");
                if (msgs == null) continue;

                for (Map<String, Object> msg : msgs) {
                    String from = (String) msg.get("from");
                    String type = (String) msg.get("type");

                    if ("text".equals(type)) {
                        Map<String, String> text = (Map<String, String>) msg.get("text");
                        String body = text != null ? text.get("body") : "";
                        result.add(new WhatsAppMessage(from, body));
                    } else if ("location".equals(type)) {
                        Map<String, Object> location = (Map<String, Object>) msg.get("location");
                        if (location != null) {
                            Double lat = toDouble(location.get("latitude"));
                            Double lng = toDouble(location.get("longitude"));
                            String address = toString(location.get("address"));
                            String name = toString(location.get("name"));
                            // Construir dirección: preferir address, complementar con name
                            String fullAddress = buildAddress(name, address);
                            if (lat != null && lng != null) {
                                result.add(new WhatsAppMessage(from, lat, lng, fullAddress));
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static String toString(Object value) {
        return value instanceof String s && !s.isBlank() ? s : null;
    }

    private static String buildAddress(String name, String address) {
        if (address != null && name != null) return name + ", " + address;
        if (address != null) return address;
        if (name != null) return name;
        return null;
    }
}

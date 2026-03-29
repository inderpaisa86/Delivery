package org.delivery.application.dto;

public record WhatsAppMessage(
        String from,
        String type,
        String body,
        Double latitude,
        Double longitude,
        String address
) {
    /** Constructor para mensajes de texto */
    public WhatsAppMessage(String from, String body) {
        this(from, "text", body, null, null, null);
    }

    /** Constructor para mensajes de ubicación */
    public WhatsAppMessage(String from, Double latitude, Double longitude, String address) {
        this(from, "location", null, latitude, longitude, address);
    }

    public boolean isLocation() {
        return "location".equals(type);
    }

    public boolean isText() {
        return "text".equals(type);
    }
}

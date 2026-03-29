package org.delivery.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WhatsAppMessage")
class WhatsAppMessageTest {

    @Test
    @DisplayName("Crea record con from y body correctos")
    void shouldCreateRecordWithCorrectValues() {
        WhatsAppMessage message = new WhatsAppMessage("573001234567", "Hola");

        assertEquals("573001234567", message.from());
        assertEquals("Hola", message.body());
    }

    @Test
    @DisplayName("Dos mensajes con mismos valores son iguales")
    void shouldBeEqualWhenSameValues() {
        WhatsAppMessage msg1 = new WhatsAppMessage("573001234567", "Hola");
        WhatsAppMessage msg2 = new WhatsAppMessage("573001234567", "Hola");

        assertEquals(msg1, msg2);
    }

    @Test
    @DisplayName("Dos mensajes con valores distintos no son iguales")
    void shouldNotBeEqualWhenDifferentValues() {
        WhatsAppMessage msg1 = new WhatsAppMessage("573001234567", "Hola");
        WhatsAppMessage msg2 = new WhatsAppMessage("573009999999", "Adiós");

        assertNotEquals(msg1, msg2);
    }
}

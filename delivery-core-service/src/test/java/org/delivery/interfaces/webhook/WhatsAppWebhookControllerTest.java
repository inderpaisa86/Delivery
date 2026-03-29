package org.delivery.interfaces.webhook;

import org.delivery.Main;
import org.delivery.application.service.WhatsAppMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WhatsAppWebhookController.class)
@ContextConfiguration(classes = Main.class)
class WhatsAppWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WhatsAppMessageService messageService;

    @Nested
    @DisplayName("GET /webhook - Verificación")
    class Verify {

        @Test
        @DisplayName("Retorna challenge con token válido")
        void shouldReturnChallenge() throws Exception {
            mockMvc.perform(get("/webhook")
                            .param("hub.mode", "subscribe")
                            .param("hub.verify_token", "mi-token-secreto")
                            .param("hub.challenge", "abc123"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("abc123"));
        }

        @Test
        @DisplayName("Retorna 403 con token inválido")
        void shouldReturn403WithBadToken() throws Exception {
            mockMvc.perform(get("/webhook")
                            .param("hub.mode", "subscribe")
                            .param("hub.verify_token", "wrong")
                            .param("hub.challenge", "abc123"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /webhook - Mensajes")
    class Messages {

        @Test
        @DisplayName("Retorna 200 con payload válido")
        void shouldReturn200() throws Exception {
            String payload = """
                    {"entry":[{"changes":[{"value":{"messages":[{
                      "from":"573001234567","type":"text",
                      "text":{"body":"2 hamburguesas"}}]}}]}]}
                    """;

            mockMvc.perform(post("/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Retorna 200 con payload vacío")
        void shouldReturn200WithEmpty() throws Exception {
            mockMvc.perform(post("/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }
}

package org.delivery.controller;

import org.delivery.Main;
import org.delivery.service.WhatsAppMessageService;
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
    class VerifyEndpoint {

        @Test
        @DisplayName("Retorna challenge cuando token y mode son válidos")
        void shouldReturnChallengeWhenTokenIsValid() throws Exception {
            mockMvc.perform(get("/webhook")
                            .param("hub.mode", "subscribe")
                            .param("hub.verify_token", "mi-token-secreto")
                            .param("hub.challenge", "challenge123"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("challenge123"));
        }

        @Test
        @DisplayName("Retorna 403 cuando el token es inválido")
        void shouldReturn403WhenTokenIsInvalid() throws Exception {
            mockMvc.perform(get("/webhook")
                            .param("hub.mode", "subscribe")
                            .param("hub.verify_token", "token-incorrecto")
                            .param("hub.challenge", "challenge123"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Retorna 403 cuando el mode no es subscribe")
        void shouldReturn403WhenModeIsNotSubscribe() throws Exception {
            mockMvc.perform(get("/webhook")
                            .param("hub.mode", "otro-mode")
                            .param("hub.verify_token", "mi-token-secreto")
                            .param("hub.challenge", "challenge123"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /webhook - Recibir mensajes")
    class ReceiveMessageEndpoint {

        @Test
        @DisplayName("Retorna 200 OK con payload válido")
        void shouldReturn200WithValidPayload() throws Exception {
            String payload = """
                    {
                      "entry": [{
                        "changes": [{
                          "value": {
                            "messages": [{
                              "from": "573001234567",
                              "type": "text",
                              "text": { "body": "2 hamburguesas" }
                            }]
                          }
                        }]
                      }]
                    }
                    """;

            mockMvc.perform(post("/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Retorna 200 OK con payload vacío")
        void shouldReturn200WithEmptyPayload() throws Exception {
            mockMvc.perform(post("/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }
}

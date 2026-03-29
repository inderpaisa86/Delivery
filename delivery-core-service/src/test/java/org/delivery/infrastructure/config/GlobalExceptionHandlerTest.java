package org.delivery.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Maneja IllegalArgumentException como 404")
    void shouldHandle404() {
        var response = handler.handleNotFound(new IllegalArgumentException("No encontrado"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("No encontrado", response.getBody().get("error"));
    }

    @Test
    @DisplayName("Maneja IllegalStateException como 400")
    void shouldHandle400() {
        var response = handler.handleBadState(new IllegalStateException("Estado inválido"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Maneja validación como 400")
    void shouldHandleValidation() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(
                List.of(new FieldError("obj", "campo", "no puede ser nulo")));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        var response = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("campo"));
    }

    @Test
    @DisplayName("Maneja excepción general como 500")
    void shouldHandle500() {
        var response = handler.handleGeneral(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error interno del servidor", response.getBody().get("error"));
    }
}

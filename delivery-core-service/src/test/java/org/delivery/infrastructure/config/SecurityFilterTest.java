package org.delivery.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@DisplayName("SecurityFilter")
class SecurityFilterTest {

    private final SecurityFilter filter = new SecurityFilter("my-secret-token");

    @Test
    @DisplayName("Permite acceso a /webhook sin token")
    void shouldAllowWebhook() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/webhook");
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("Permite acceso a /track/ sin token")
    void shouldAllowTrack() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/track/abc-123");
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("Permite acceso con token válido")
    void shouldAllowWithValidToken() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/pedidos");
        when(req.getHeader("Authorization")).thenReturn("Bearer my-secret-token");
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("Rechaza acceso sin token")
    void shouldRejectWithoutToken() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/pedidos");
        when(req.getHeader("Authorization")).thenReturn(null);
        when(res.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(req, res, chain);
        verify(res).setStatus(401);
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    @DisplayName("Rechaza acceso con token inválido")
    void shouldRejectWithBadToken() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/pedidos");
        when(req.getHeader("Authorization")).thenReturn("Bearer wrong-token");
        when(res.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(req, res, chain);
        verify(res).setStatus(401);
    }
}

package org.delivery.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@DisplayName("RateLimitFilter")
class RateLimitFilterTest {

    @Test
    @DisplayName("Permite requests normales al webhook")
    void shouldAllowNormalRequests() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(60);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/webhook");
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("Bloquea cuando se excede el rate limit")
    void shouldBlockWhenRateLimitExceeded() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/webhook");
        when(res.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // Primera request pasa
        filter.doFilter(req, res, chain);
        verify(chain, times(1)).doFilter(req, res);

        // Segunda request bloqueada
        filter.doFilter(req, res, chain);
        verify(res).setStatus(429);
    }

    @Test
    @DisplayName("No aplica rate limit a otros endpoints")
    void shouldNotLimitOtherEndpoints() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRequestURI()).thenReturn("/pedidos");
        filter.doFilter(req, res, chain);
        filter.doFilter(req, res, chain);
        verify(chain, times(2)).doFilter(req, res);
    }
}

package org.delivery.config;

import java.io.IOException;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de seguridad básico por token para endpoints internos.
 * Los endpoints públicos (/webhook, /track) no requieren token.
 */
@Component
@Order(1)
public class SecurityFilter implements Filter {

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/webhook", "/track/",
            "/swagger-ui", "/v3/api-docs", "/swagger-resources"
    );

    private final String apiToken;

    public SecurityFilter(@Value("${app.api.token}") String apiToken) {
        this.apiToken = apiToken;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Endpoints públicos no requieren autenticación
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Validar token en header Authorization
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.equals("Bearer " + apiToken)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.getWriter().write("No autorizado");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }
}

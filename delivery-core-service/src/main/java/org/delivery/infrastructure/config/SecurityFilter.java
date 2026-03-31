package org.delivery.infrastructure.config;

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
 * Filtro de seguridad que acepta:
 * 1. JWT válido (Bearer eyJ...)
 * 2. Token estático legacy (Bearer delivery-internal-token)
 * Endpoints públicos no requieren autenticación.
 */
@Component
@Order(1)
public class SecurityFilter implements Filter {

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/webhook", "/track/", "/auth/",
            "/swagger-ui", "/v3/api-docs", "/swagger-resources",
            "/actuator"
    );

    private final String apiToken;
    private final JwtService jwtService;

    public SecurityFilter(
            @Value("${app.api.token}") String apiToken,
            JwtService jwtService) {
        this.apiToken = apiToken;
        this.jwtService = jwtService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Aceptar token estático legacy
            if (token.equals(apiToken)) {
                chain.doFilter(request, response);
                return;
            }

            // Validar JWT
            if (jwtService.isValid(token)) {
                chain.doFilter(request, response);
                return;
            }
        }

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.getWriter().write("No autorizado");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }
}

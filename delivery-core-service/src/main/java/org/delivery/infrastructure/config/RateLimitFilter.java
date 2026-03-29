package org.delivery.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limiter para el webhook de WhatsApp.
 * Protege contra abuso y reintentos excesivos de Meta.
 */
@Component
@Order(0)
public class RateLimitFilter implements Filter {

    private final Bucket bucket;

    public RateLimitFilter(
            @Value("${app.rate-limit.webhook-requests-per-minute:60}") int requestsPerMinute) {
        this.bucket = Bucket.builder()
                .addLimit(Bandwidth.classic(requestsPerMinute, Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (httpRequest.getRequestURI().startsWith("/webhook")) {
            if (!bucket.tryConsume(1)) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(429);
                httpResponse.getWriter().write("Too many requests");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}

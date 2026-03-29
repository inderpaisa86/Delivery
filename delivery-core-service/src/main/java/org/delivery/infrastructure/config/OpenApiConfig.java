package org.delivery.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deliveryOpenAPI() {
        String schemeName = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Delivery Core Service API")
                        .description("Sistema de pedidos por WhatsApp con asignación automática de domiciliarios y tracking en tiempo real. Multi-tenant (SaaS ready).")
                        .version("2.0.0")
                        .contact(new Contact().name("Delivery Team")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .schemaRequirement(schemeName, new SecurityScheme()
                        .name(schemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("Token"));
    }
}

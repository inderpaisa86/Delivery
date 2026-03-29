package org.delivery.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita procesamiento asíncrono para tareas como asignación de domiciliarios.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

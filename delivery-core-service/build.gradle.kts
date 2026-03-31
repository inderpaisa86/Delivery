plugins {
    id("java")
    id("jacoco")
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.sonarqube") version "5.1.0.4882"
}

group = "org.delivery"
version = project.findProperty("projectVersion") ?: "0.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // JPA + PostgreSQL
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    // Flyway migraciones
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // WebClient para WhatsApp API
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Actuator (health check)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Rate limiting
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // Swagger / OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // BCrypt (Spring Security crypto only)
    implementation("org.springframework.security:spring-security-crypto")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // Logs JSON
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.flywaydb:flyway-core")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("net.jqwik:jqwik:1.9.1")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
    }
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude(
                "**/config/OpenApiConfig.class",
                "**/config/AsyncConfig.class",
                "**/config/RateLimitFilter.class",
                "**/external/whatsapp/WhatsAppAdapter.class",
                "**/Main.class"
            )
        }
    }))
}

sonar {
    properties {
        property("sonar.projectKey", "delivery-core-service")
        property("sonar.projectName", "Delivery Core Service")
        property("sonar.host.url", "http://localhost:9000")
        property("sonar.token", System.getenv("SONAR_TOKEN") ?: "")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")

        // Excluir clases del análisis de Sonar (código y coverage)
        property("sonar.exclusions", listOf(
            "**/config/OpenApiConfig.java",
            "**/config/AsyncConfig.java",
            "**/config/RateLimitFilter.java",
            "**/external/whatsapp/WhatsAppAdapter.java",
            "**/Main.java"
        ).joinToString(","))

        // Excluir del cálculo de coverage
        property("sonar.coverage.exclusions", listOf(
            "**/dto/**",
            "**/domain/entity/**",
            "**/domain/enums/**",
            "**/config/**",
            "**/Main.java"
        ).joinToString(","))
    }
}

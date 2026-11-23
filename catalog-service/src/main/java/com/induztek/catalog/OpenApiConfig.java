package com.induztek.catalog;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI / Swagger para el microservicio de Catálogo.
 * Define la documentación del inventario de equipos e instrumentos de medición, y el esquema JWT Bearer.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI catalogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Induztek - Servicio de Catálogo")
                        .description("Microservicio encargado del catálogo de equipos industriales (transformadores, interruptores) e instrumentos de medición con control de calibración.")
                        .version("1.0.0")
                        .contact(new Contact().name("Induztek Dev Team").email("soporte@induztek.local")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese el token JWT obtenido tras autenticarse en /api/auth/login")));
    }
}

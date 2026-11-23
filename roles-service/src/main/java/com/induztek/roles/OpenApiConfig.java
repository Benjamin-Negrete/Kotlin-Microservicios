package com.induztek.roles;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI / Swagger para el microservicio de Roles.
 * Define la documentación global, información de la API y el esquema de seguridad JWT Bearer.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rolesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Induztek - Servicio de Roles y Permisos")
                        .description("Microservicio encargado de la administración del catálogo de perfiles (roles) y permisos del sistema Induztek.")
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

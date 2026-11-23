package com.induztek.identity;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI / Swagger para el microservicio de Identidad y Autenticación.
 * Documenta los endpoints de registro, login JWT, recuperación de contraseña y gestión de usuarios.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI identityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Induztek - Servicio de Identidad y Autenticación")
                        .description("Microservicio central de autenticación y autorización. Genera tokens JWT (HS256), administra el ciclo de vida de usuarios, aprobación de registros y subida de imágenes de perfil.")
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

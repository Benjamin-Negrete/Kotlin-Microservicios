package com.induztek.operations;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI / Swagger para el microservicio de Operaciones.
 * Documenta los módulos de visitas técnicas, protocolos de mantenimiento, pruebas eléctricas,
 * actas de entrega y almacenamiento de evidencias fotográficas.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI operationsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Induztek - Servicio de Operaciones y Mantenimiento")
                        .description("Microservicio núcleo para el registro de visitas en terreno, ejecución de pruebas eléctricas con mediciones JSON, formalización de protocolos y emisión de actas de servicio.")
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

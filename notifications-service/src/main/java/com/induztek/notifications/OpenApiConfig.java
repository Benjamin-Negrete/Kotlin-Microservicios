package com.induztek.notifications;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI / Swagger para el microservicio de Notificaciones.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Induztek - Servicio de Notificaciones")
                        .description("Microservicio interno responsable del envío de correos electrónicos transaccionales (recuperación de contraseña, avisos).")
                        .version("1.0.0")
                        .contact(new Contact().name("Induztek Dev Team").email("soporte@induztek.local")));
    }
}

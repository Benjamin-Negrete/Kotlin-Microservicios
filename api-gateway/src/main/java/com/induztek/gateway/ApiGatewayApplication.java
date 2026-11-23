package com.induztek.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Puerta de enlace API Gateway (Spring Cloud Gateway WebFlux).
 * Es la única entrada HTTP pública al ecosistema de microservicios (puerto 8080).
 * Enruta dinámicamente las peticiones hacia identity-service, roles-service, catalog-service,
 * operations-service y notifications-service, y centraliza la interfaz interactiva de Swagger UI.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    /**
     * Punto de entrada de la aplicación API Gateway.
     *
     * @param args Argumentos de la línea de comandos.
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

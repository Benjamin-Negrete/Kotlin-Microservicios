package com.induztek.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de inicio para el microservicio de Identidad.
 * Inicializa el contexto de Spring Boot, conecta a MySQL (induztek_identity) y se registra en Eureka.
 */
@SpringBootApplication
public class IdentityServiceApplication {

    /**
     * Punto de entrada de la aplicación.
     *
     * @param args Argumentos de la línea de comandos.
     */
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}

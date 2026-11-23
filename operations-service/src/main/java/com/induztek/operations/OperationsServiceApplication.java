package com.induztek.operations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de inicio para el microservicio de Operaciones.
 * Levanta el contexto Spring Boot, conecta a MySQL (induztek_operations) y se registra en Eureka.
 */
@SpringBootApplication
public class OperationsServiceApplication {

    /**
     * Punto de entrada de la aplicación.
     *
     * @param args Argumentos de la línea de comandos.
     */
    public static void main(String[] args) {
        SpringApplication.run(OperationsServiceApplication.class, args);
    }
}

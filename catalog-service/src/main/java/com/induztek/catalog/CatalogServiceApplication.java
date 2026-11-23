package com.induztek.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de inicio para el microservicio de Catálogo.
 * Inicializa el contexto de Spring Boot, conecta a MySQL (induztek_catalog) y se registra en Eureka.
 */
@SpringBootApplication
public class CatalogServiceApplication {

    /**
     * Punto de entrada de la aplicación.
     *
     * @param args Argumentos de la línea de comandos.
     */
    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}

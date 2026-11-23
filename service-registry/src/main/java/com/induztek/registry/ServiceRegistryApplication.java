package com.induztek.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor de descubrimiento de servicios Spring Cloud Netflix Eureka.
 * Permite que los microservicios registren dinámicamente sus instancias y se comuniquen mediante nombres lógicos.
 * Accesible en el puerto por defecto 8761.
 */
@EnableEurekaServer
@SpringBootApplication
public class ServiceRegistryApplication {

    /**
     * Punto de entrada del servidor de registro Eureka.
     *
     * @param args Argumentos de la línea de comandos.
     */
    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}

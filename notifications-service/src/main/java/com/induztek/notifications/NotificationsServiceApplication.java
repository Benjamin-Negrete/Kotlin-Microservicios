package com.induztek.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de inicio para el microservicio de Notificaciones.
 * Configura JavaMailSender para entrega vía SMTP y registro en Eureka.
 */
@SpringBootApplication
public class NotificationsServiceApplication {

    /**
     * Punto de entrada de la aplicación.
     *
     * @param args Argumentos de la línea de comandos.
     */
    public static void main(String[] args) {
        SpringApplication.run(NotificationsServiceApplication.class, args);
    }
}

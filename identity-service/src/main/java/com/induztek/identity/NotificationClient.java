package com.induztek.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Cliente HTTP reactivo (WebClient) para comunicarse con el microservicio de Notificaciones.
 * Solicita el envío asíncrono de correos con el código de recuperación de contraseña.
 */
@Component
public class NotificationClient {

    private final WebClient webClient;

    /**
     * Constructor con inyección del constructor WebClient y la URL base de notifications-service.
     */
    public NotificationClient(WebClient.Builder builder, @Value("${services.notifications.url}") String notificationsUrl) {
        this.webClient = builder.baseUrl(notificationsUrl).build();
    }

    /**
     * Solicita al microservicio de notificaciones el envío del código por correo.
     *
     * @param email Destinatario.
     * @param code Código de 6 dígitos.
     * @throws org.springframework.web.server.ResponseStatusException con estado 502 si falla la entrega.
     */
    public void sendPasswordReset(String email, String code) {
        try {
            webClient.post()
                    .uri("/internal/notificaciones/recuperacion")
                    .bodyValue(new RecoveryEmailRequest(email, code))
                    .retrieve()
                    .toBodilessEntity()
                    .block(java.time.Duration.ofSeconds(5));
        } catch (RuntimeException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Could not deliver the password recovery email", exception);
        }
    }

    /** DTO para la petición de envío de correo. */
    public record RecoveryEmailRequest(String email, String code) {
    }
}

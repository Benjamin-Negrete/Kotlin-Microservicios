package com.induztek.identity;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cliente HTTP hacia notifications-service.
 *
 * <p>Errores que puede producir (los traduce el GlobalExceptionHandler al JSON estándar):
 * <ul>
 *   <li><b>502 BAD_GATEWAY</b>: notifications-service respondió con error (p. ej. el SMTP rechazó el envío).</li>
 *   <li><b>503 SERVICE_UNAVAILABLE</b>: no se pudo conectar con notifications-service (caído o URL mal configurada).</li>
 *   <li><b>504 GATEWAY_TIMEOUT</b>: notifications-service no respondió dentro de {@code services.notifications.timeout-seconds}.</li>
 * </ul>
 *
 * <p>El timeout por defecto (50 s) debe superar el peor caso de notifications-service: 2 intentos SMTP
 * con timeouts de 10 s (conexión, lectura, escritura). Antes era 5 s fijos y la primera conexión TLS con Gmail
 * lo superaba, lo que provocaba el 502 al pedir el código de recuperación.
 */
@Component
public class NotificationClient {
    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final WebClient webClient;
    private final Duration timeout;

    public NotificationClient(
            WebClient.Builder builder,
            @Value("${services.notifications.url}") String notificationsUrl,
            @Value("${services.notifications.timeout-seconds:50}") long timeoutSeconds) {
        this.webClient = builder.baseUrl(notificationsUrl).build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public void sendPasswordReset(String email, String code) {
        try {
            webClient.post()
                    .uri("/internal/notificaciones/recuperacion")
                    .bodyValue(new RecoveryEmailRequest(email, code))
                    .retrieve()
                    .toBodilessEntity()
                    .block(timeout);
        } catch (WebClientResponseException exception) {
            log.warn("notifications-service respondió {} al enviar el correo de recuperación: {}",
                    exception.getStatusCode().value(), exception.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not deliver the password recovery email", exception);
        } catch (WebClientRequestException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Notification service is unavailable", exception);
        } catch (RuntimeException exception) {
            if (hasCause(exception, TimeoutException.class)) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                        "The notification service took too long to send the recovery email", exception);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not deliver the password recovery email", exception);
        }
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    public record RecoveryEmailRequest(String email, String code) {
    }
}

package com.induztek.notifications;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador REST para el envío de notificaciones y correos electrónicos.
 * Es un servicio interno llamado principalmente por identity-service.
 */
@RestController
@RequestMapping("/internal/notificaciones")
@Tag(name = "Notificaciones", description = "Endpoints internos para la entrega de correos electrónicos transaccionales")
public class NotificationController {

    private final JavaMailSender mailSender;
    private final String sender;

    /**
     * Constructor con inyección del emisor de correos y la dirección configurada.
     *
     * @param mailSender Cliente de envío de correo de Spring Mail.
     * @param sender Dirección remitente por defecto obtenida de la propiedad notifications.sender.
     */
    public NotificationController(JavaMailSender mailSender, @Value("${notifications.sender}") String sender) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    /**
     * Envía un correo con el código de recuperación de contraseña al usuario solicitante.
     *
     * @param request Datos del destinatario y el código generado.
     * @throws ResponseStatusException con estado 502 si ocurre un error de autenticación SMTP o fallo de entrega.
     */
    @PostMapping("/recuperacion")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Enviar correo de recuperación", description = "Envía un mensaje SMTP con el código de 6 dígitos para restablecer la contraseña.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitud de correo aceptada y enviada a la cola"),
            @ApiResponse(responseCode = "400", description = "Email o código inválidos"),
            @ApiResponse(responseCode = "502", description = "Error en la conexión con el servidor SMTP")
    })
    public void sendRecoveryEmail(@Valid @RequestBody RecoveryRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(request.email());
        message.setSubject("Código para recuperar tu contraseña");
        message.setText("Tu código de recuperación es " + request.code()
                + ". Expira en 10 minutos. Si no solicitaste el cambio, ignora este correo.");
        try {
            mailSender.send(message);
        } catch (MailAuthenticationException | MailSendException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Email delivery failed", exception);
        }
    }

    /**
     * DTO de petición para el envío del correo de recuperación.
     *
     * @param email Dirección de correo electrónico del usuario.
     * @param code Código de recuperación numérico o alfanumérico.
     */
    public record RecoveryRequest(
            @Email @NotBlank String email,
            @NotBlank String code) {
    }
}

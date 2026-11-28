package com.induztek.notifications;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * Al arrancar abre (en segundo plano) una conexión de prueba con el SMTP.
 *
 * <p>Motivo: la primera conexión TLS de una JVM recién iniciada es lenta y Gmail podía cortarla
 * ("Remote host terminated the handshake"), devolviendo 502 al primer usuario que pedía un código.
 * Además, si las credenciales SMTP están mal, queda registrado en el log al iniciar.
 */
@Component
public class SmtpWarmup {
    private static final Logger log = LoggerFactory.getLogger(SmtpWarmup.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;

    public SmtpWarmup(JavaMailSender mailSender, @Value("${notifications.smtp-warmup:true}") boolean enabled) {
        this.mailSender = mailSender;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (!enabled || !(mailSender instanceof JavaMailSenderImpl sender)
                || sender.getUsername() == null || sender.getUsername().isBlank()) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                sender.testConnection();
                log.info("Conexion SMTP verificada con {}:{}", sender.getHost(), sender.getPort());
            } catch (MessagingException exception) {
                log.warn("No se pudo verificar la conexion SMTP con {}:{} al iniciar: {}",
                        sender.getHost(), sender.getPort(), exception.getMessage());
            }
        }, "smtp-warmup");
        thread.setDaemon(true);
        thread.start();
    }
}

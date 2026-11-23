package com.induztek.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Inicializador de cuenta de administrador bootstrap.
 * Crea automáticamente un usuario con rol ADMIN al arrancar la aplicación si se configuran
 * las variables BOOTSTRAP_ADMIN_EMAIL y BOOTSTRAP_ADMIN_PASSWORD en las variables de entorno.
 */
@Configuration
public class BootstrapAdmin {

    /**
     * Tarea que evalúa las variables de entorno del admin inicial y lo persiste si aún no existe.
     */
    @Bean
    ApplicationRunner bootstrapAdminRunner(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${identity.bootstrap-admin.email:}") String email,
            @Value("${identity.bootstrap-admin.password:}") String password) {
        return args -> {
            if (email.isBlank() && password.isBlank()) {
                return;
            }
            if (email.isBlank() || password.isBlank() || password.length() < 12) {
                throw new IllegalStateException(
                        "Set both BOOTSTRAP_ADMIN_EMAIL and a BOOTSTRAP_ADMIN_PASSWORD of at least 12 characters");
            }
            String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
            if (users.existsByEmailIgnoreCase(normalizedEmail)) {
                return;
            }
            users.save(new UserEntity(
                    java.util.UUID.randomUUID().toString(),
                    normalizedEmail,
                    passwordEncoder.encode(password),
                    "Administrador Induztek",
                    "ADMIN",
                    null,
                    java.time.Instant.now(),
                    true));
        };
    }
}

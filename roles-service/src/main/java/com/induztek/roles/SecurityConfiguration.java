package com.induztek.roles;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad web para el microservicio de Roles.
 * Configura la validación de tokens JWT mediante Resource Server, permite acceso público
 * a endpoints de salud (/actuator/health), documentación OpenAPI (/v3/api-docs/**, /swagger-ui/**)
 * y consultas internas entre microservicios (/api/roles/internal/**).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    /**
     * Define la cadena de filtros de seguridad HTTP y las reglas de autorización por ruta.
     *
     * @param http Objeto {@link HttpSecurity} para construir la configuración.
     * @return {@link SecurityFilterChain} configurado.
     * @throws Exception Si ocurre un error durante la construcción de la cadena.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/roles/internal/**"
                        )
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * Conversor para transformar los claims del token JWT en autoridades de Spring Security.
     * Extrae el claim 'role' y le antepone el prefijo 'ROLE_' requerido por hasAuthority/hasRole.
     *
     * @return Instancia de {@link JwtAuthenticationConverter}.
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }
}

package com.induztek.identity;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad para el microservicio de Identidad.
 * Configura la emisión y validación de tokens JWT (HS256), codificación de contraseñas con BCrypt,
 * y define las rutas públicas (auth, swagger, imágenes estáticas) y protegidas.
 */
@Configuration
@EnableMethodSecurity
public class IdentitySecurityConfiguration {

    /**
     * Define la cadena de filtros de seguridad HTTP.
     */
    @Bean
    SecurityFilterChain identityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/password/recovery",
                                "/api/auth/password/verify",
                                "/api/auth/password/reset"
                        )
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/imagenes/**", "/api/usuarios/imagenes/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * Mapea los roles del JWT a las autoridades de Spring Security.
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

    /**
     * Bean para la firma y codificación de tokens JWT usando clave simétrica HMAC SHA-256.
     */
    @Bean
    JwtEncoder jwtEncoder(org.springframework.core.env.Environment environment) {
        String secret = environment.getRequiredProperty("identity.jwt.secret");
        byte[] key = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (key.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /**
     * Bean de cifrado de contraseñas con BCrypt (fuerza por defecto 10 rounds).
     */
    @Bean
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}

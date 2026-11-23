package com.induztek.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Autoconfiguración compartida de seguridad para la decodificación y validación de tokens JWT.
 * Proporciona un bean {@link JwtDecoder} configurado con la clave simétrica compartida JWT_SECRET
 * mediante el algoritmo HMAC-SHA256 (HS256).
 */
@AutoConfiguration
public class JwtDecoderAutoConfiguration {

    /**
     * Construye e inicializa el decodificador de tokens JWT.
     * Valida que la clave secreta compartida contenga al menos 32 bytes de longitud requeridos por HS256.
     *
     * @param secret Clave secreta obtenida de la propiedad identity.jwt.secret.
     * @return Instancia de {@link JwtDecoder} configurada.
     * @throws IllegalStateException Si la clave posee menos de 32 bytes de longitud.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${identity.jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}

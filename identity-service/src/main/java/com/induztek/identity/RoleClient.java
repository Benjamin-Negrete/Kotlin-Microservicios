package com.induztek.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Cliente HTTP reactivo (WebClient) para comunicarse con el microservicio de Roles (roles-service).
 * Permite validar códigos de rol asignados durante el registro o edición de usuarios.
 */
@Component
public class RoleClient {

    private final WebClient webClient;

    /**
     * Constructor con inyección del constructor WebClient y la URL base del microservicio de roles.
     */
    public RoleClient(WebClient.Builder builder, @Value("${services.roles.url}") String rolesUrl) {
        this.webClient = builder.baseUrl(rolesUrl).build();
    }

    /**
     * Consulta el microservicio de roles para obtener los datos de un rol por su código.
     *
     * @param code Código del rol (ej: CLIENT, ADMIN).
     * @return {@link RoleResponse} con los datos del rol.
     * @throws org.springframework.web.server.ResponseStatusException con estado 503 si el servicio no responde.
     */
    public RoleResponse findByCode(String code) {
        try {
            return webClient.get()
                    .uri("/api/roles/internal/{code}", code)
                    .retrieve()
                    .bodyToMono(RoleResponse.class)
                    .block(java.time.Duration.ofSeconds(3));
        } catch (RuntimeException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Role service is unavailable", exception);
        }
    }

    /** DTO para la respuesta obtenida del microservicio de roles. */
    public record RoleResponse(String id, String code, String name, String description) {
    }
}

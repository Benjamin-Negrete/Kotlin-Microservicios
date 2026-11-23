package com.induztek.operations;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cliente HTTP reactivo (WebClient) para comunicarse con el microservicio de Catálogo.
 * Valida la existencia de equipos antes de crear protocolos o pruebas en operaciones.
 */
@Component
public class CatalogClient {

    private final WebClient webClient;

    /**
     * Constructor con inyección del WebClient.Builder y la URL del servicio de catálogo.
     */
    public CatalogClient(WebClient.Builder builder, @Value("${services.catalog.url}") String catalogUrl) {
        this.webClient = builder.baseUrl(catalogUrl).build();
    }

    /**
     * Valida obligatoriamente que el equipo exista en el catálogo.
     * Si no existe o el servicio de catálogo no responde, lanza una excepción controlada.
     *
     * @param code Código del equipo a validar.
     * @throws ResponseStatusException 404 si el equipo no existe, o 503 si el servicio está caído.
     */
    public void requireEquipment(String code) {
        try {
            ExistsResponse response = webClient.get()
                    .uri("/api/equipos/internal/{code}", code)
                    .retrieve()
                    .bodyToMono(ExistsResponse.class)
                    .block(Duration.ofSeconds(3));
            if (response == null || !response.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found: " + code);
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Catalog service is unavailable", exception);
        }
    }

    private record ExistsResponse(boolean exists) {
    }
}

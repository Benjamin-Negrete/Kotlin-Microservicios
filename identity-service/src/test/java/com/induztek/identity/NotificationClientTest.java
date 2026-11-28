package com.induztek.identity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

/** Verifica cómo se traduce cada fallo de notifications-service a un código HTTP (502 / 503 / 504). */
class NotificationClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startServer(int status, long delayMillis, AtomicReference<String> body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/notificaciones/recuperacion", exchange -> {
            if (body != null) {
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            }
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static NotificationClient client(String url, long timeoutSeconds) {
        return new NotificationClient(WebClient.builder(), url, timeoutSeconds);
    }

    @Test
    void sendsEmailAndCodeWhenNotificationsAccepts() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        String url = startServer(202, 0, body);

        assertDoesNotThrow(() -> client(url, 5).sendPasswordReset("user@example.com", "123456"));
        assertTrue(body.get().contains("\"email\":\"user@example.com\""));
        assertTrue(body.get().contains("\"code\":\"123456\""));
    }

    @Test
    void smtpFailureInNotificationsIsBadGateway() throws IOException {
        String url = startServer(502, 0, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> client(url, 5).sendPasswordReset("user@example.com", "123456"));
        assertEquals(502, error.getStatusCode().value());
    }

    @Test
    void slowNotificationsIsGatewayTimeout() throws IOException {
        String url = startServer(202, 2_500, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> client(url, 1).sendPasswordReset("user@example.com", "123456"));
        assertEquals(504, error.getStatusCode().value());
    }

    @Test
    void responseSlowerThanOldFiveSecondLimitStillSucceeds() throws IOException {
        // Regresión del 502: antes cualquier respuesta de más de 5 s fallaba.
        String url = startServer(202, 5_500, null);

        assertDoesNotThrow(() -> client(url, 30).sendPasswordReset("user@example.com", "123456"));
    }

    @Test
    void unreachableNotificationsIsServiceUnavailable() throws IOException {
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        }

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> client("http://127.0.0.1:" + freePort, 5).sendPasswordReset("user@example.com", "123456"));
        assertEquals(503, error.getStatusCode().value());
    }
}

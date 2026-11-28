# Backend Induztek

## Repositorios

- Aplicación Android (Kotlin): [Eliassss123/Kotlin](https://github.com/Eliassss123/Kotlin/tree/kotlin)
- Backend de microservicios: [Benjamin-Negrete/Kotlin-Microservicios](https://github.com/Benjamin-Negrete/Kotlin-Microservicios)

Backend Java 21 / Spring Boot 3.5 con servicios independientes, Eureka, un API Gateway, MySQL y Swagger UI centralizada.

## Servicios

- `identity-service`: acceso, usuarios, aceptación de términos, recuperación de contraseña y gestión de fotos de perfil (`fotoPerfil`).
- `roles-service`: catálogo de perfiles y permisos.
- `catalog-service`: equipos e instrumentos.
- `operations-service`: visitas, protocolos, mediciones estructuradas en JSON, pruebas y almacenamiento directo de fotos de evidencia fotográfica.
- `notifications-service`: entrega de correos de recuperación mediante SMTP.
- `service-registry`: registro Eureka.
- `api-gateway`: única entrada HTTP para Android y túneles.

Cada servicio dueño de datos usa su propio esquema MySQL. La app móvil no se conecta a MySQL. Las llamadas internas usan `WebClient` con descubrimiento por Eureka; no se usa `RestTemplate`.

## Ejecución local

1. Copiar `.env.example` a `.env` y reemplazar contraseñas y `JWT_SECRET` por valores aleatorios. Para Gmail, activar la verificación en dos pasos y generar una contraseña de aplicación; poner la cuenta en `SMTP_USER` y `SMTP_FROM`, y esa contraseña de aplicación (no la contraseña normal de Gmail) en `SMTP_PASSWORD`. No subir `.env` al repositorio.
2. Desde esta carpeta, ejecutar `docker compose up --build`.
3. La app se expone en `http://localhost:8080`; Eureka queda en `http://localhost:8761`.
4. Swagger: `http://localhost:8080/swagger-ui.html`.

Para Laragon/XAMPP, crear las bases `induztek_identity`, `induztek_roles`, `induztek_catalog` e `induztek_operations`, dar permisos al usuario configurado y establecer `IDENTITY_DATABASE_URL`, `ROLES_DATABASE_URL`, `CATALOG_DATABASE_URL` and `OPERATIONS_DATABASE_URL` en `.env`. Desde contenedores Windows, usa `host.docker.internal` como host de MySQL; no uses `localhost`.

## Documentación de la API y códigos de error

Swagger UI (`http://localhost:8080/swagger-ui.html`) junta la documentación de todos los servicios. Cada endpoint indica su descripción, el rol que necesita y **todas las respuestas posibles con un ejemplo real**, incluidos los errores. Para probar endpoints protegidos: hacer `POST /api/auth/login`, copiar el `token` y pulsar **Authorize** (esquema `bearerAuth`).

Todos los servicios (salvo notifications-service, que es interno) responden los errores con el mismo JSON:

```json
{
  "timestamp": "2026-11-23T21:30:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "NOT_FOUND",
  "message": "Account not found",
  "path": "/api/usuarios/me",
  "details": []
}
```

`details` solo trae datos en errores de validación: una lista de `{ "field", "message" }`. Los errores 500 nunca incluyen trazas ni detalles internos; eso queda solo en el log del servicio.

### Respuestas exitosas (2xx)

| HTTP | Cuándo ocurre | Ejemplos |
|------|---------------|----------|
| 200 | OK: consulta o actualización correcta; el cuerpo trae el recurso. | `POST /api/auth/login`, `GET /api/usuarios/me`, `PATCH /api/usuarios/{id}/rol`, `POST /api/imagenes`, listados |
| 201 | Created: se creó un recurso; el cuerpo trae el recurso creado. | `POST /api/auth/register`, `POST /api/equipos`, `POST /api/instrumentos`, `POST /api/visitas`, `POST /api/protocolos`, `POST /api/pruebas`, `POST /api/actas`, `POST /api/roles` |
| 202 | Accepted: la solicitud fue aceptada. | `POST /api/auth/password/recovery` (código enviado; responde 202 aunque el correo no exista) |
| 204 | No Content: operación correcta sin cuerpo. | `POST /api/auth/password/verify`, `DELETE /api/usuarios/{id}` |

### Errores (4xx y 5xx)

| HTTP | `code` | Cuándo ocurre |
|------|--------|---------------|
| 400 | `VALIDATION_ERROR` | Un campo del cuerpo no cumple las reglas (vacío, formato, largo). Ver `details`. |
| 400 | `MALFORMED_REQUEST` | El cuerpo no es JSON válido o un tipo de dato no coincide. |
| 400 | `INVALID_PARAMETER` | Un parámetro de la URL tiene un valor que no se puede convertir. |
| 400 | `MISSING_PARAMETER` | Falta un parámetro de consulta obligatorio. |
| 400 | `MISSING_PART` | En una subida multipart falta la parte `file`. |
| 400 | `BAD_REQUEST` | Regla de negocio incumplida (código de recuperación inválido o agotado, archivo vacío, nombre de imagen inválido). |
| 401 | `UNAUTHORIZED` | Login con correo o contraseña incorrectos. |
| 401 | (sin cuerpo) | Token JWT ausente, inválido o vencido: lo responde Spring Security solo con el encabezado `WWW-Authenticate`. |
| 403 | `FORBIDDEN` | El rol del token no puede hacer esa acción, o la cuenta está pendiente de aprobación o desactivada. |
| 404 | `NOT_FOUND` | El recurso no existe, o es de otra empresa y quien consulta es `CLIENT`. |
| 405 | `METHOD_NOT_ALLOWED` | El método HTTP no existe para esa ruta. |
| 406 | `NOT_ACCEPTABLE` | El formato de respuesta pedido no está disponible. |
| 409 | `CONFLICT` | Duplicado: correo, código de rol, código de equipo, id existente, protocolo ya formalizado, segunda acta de una visita. |
| 413 | `PAYLOAD_TOO_LARGE` | La imagen supera los 10 MB. |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | El archivo no es JPEG o PNG, o el `Content-Type` de la petición no se acepta. |
| 429 | `TOO_MANY_REQUESTS` | 5 intentos de login fallidos seguidos para ese correo: bloqueo de 15 minutos. |
| 500 | `INTERNAL_ERROR` | Error inesperado. |
| 502 | `BAD_GATEWAY` | El SMTP rechazó o no completó el envío del correo de recuperación (credenciales, TLS, destinatario). La causa exacta queda en el log de notifications-service. |
| 502 | (sin `code`) | Lo devuelve el **API Gateway** o el túnel si no logra conectar con el microservicio (contenedor caído o reiniciando). |
| 503 | `SERVICE_UNAVAILABLE` | Un servicio del que depende la operación no responde (roles-service, catalog-service o notifications-service). |
| 504 | `GATEWAY_TIMEOUT` | notifications-service no respondió dentro de `NOTIFICATIONS_TIMEOUT_SECONDS` (50 s) al enviar el código de recuperación. Se puede reintentar. |

### Recuperación de contraseña: tiempos de espera

La cadena es App → túnel → API Gateway → identity-service → notifications-service → SMTP (Gmail). Cada eslabón espera más que el siguiente para que el error lo informe quien realmente falló:

| Eslabón | Espera | Configuración |
|---------|--------|---------------|
| SMTP (conexión / lectura / escritura) | 10 s c/u, **1 reintento** si falla la conexión | `SMTP_CONNECTION_TIMEOUT_MS`, `SMTP_READ_TIMEOUT_MS`, `SMTP_WRITE_TIMEOUT_MS` |
| identity-service → notifications-service | 50 s | `NOTIFICATIONS_TIMEOUT_SECONDS` |
| App Android (OkHttp) | 60 s lectura, 15 s conexión | `RetrofitClient.READ_TIMEOUT_SECONDS` |

Al arrancar, notifications-service abre una conexión de prueba con el SMTP (`notifications.smtp-warmup`, activo por defecto) para que la primera conexión TLS no la pague el primer usuario, y registra en el log si las credenciales SMTP son incorrectas. Si el correo falla, el código generado queda anulado y el usuario debe pedir uno nuevo.

Los endpoints `/internal/**` son solo para llamadas entre servicios: el API Gateway responde 404 si se piden desde afuera.

## Android

- Emulador Android: `http://10.0.2.2:8080/`.
- Teléfono físico: IP LAN del equipo para la URL de API y `PUBLIC_BASE_URL`, por ejemplo `http://192.168.x.x:8080/`.
- Túnel: configurar la URL HTTPS pública del Gateway tanto en la app como en `PUBLIC_BASE_URL`; no exponer MySQL ni los puertos de los microservicios.

La URL se configura mediante `INDUZTEK_API_BASE_URL` en `gradle.properties` local o en `local.properties`; no guardar una URL de túnel temporal en el código.

## Seguridad y datos

Las claves para cargar cuentas de demostración se configuran externamente mediante `IDENTITY_DEMO_USERS_ADMIN_PASSWORD`, `IDENTITY_DEMO_USERS_TECHNICIAN_PASSWORD`, `IDENTITY_DEMO_USERS_LAMPA_PASSWORD`, `IDENTITY_DEMO_USERS_ESCONDIDA_PASSWORD` y `IDENTITY_DEMO_USERS_VENTANAS_PASSWORD`. Cada cuenta se omite si su clave no está configurada o tiene menos de 10 caracteres.

El registro exige aceptar términos y asigna estado pendiente si `REGISTRATION_REQUIRE_APPROVAL=true`. Solo un administrador bootstrap configurado puede aprobar cuentas. Las contraseñas se guardan con BCrypt, y los tokens son JWT firmados con `JWT_SECRET`.

Las imágenes tienen límite de 10 MB y se almacenan en un volumen Docker persistente gestionado por `identity-service` (fotos de perfil) y `operations-service` (fotos de evidencia de pruebas). SMTP y credenciales de despliegue son configuración externa; nunca deben añadirse al repositorio.

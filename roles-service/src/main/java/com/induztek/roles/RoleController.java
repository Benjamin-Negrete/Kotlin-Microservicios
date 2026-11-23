package com.induztek.roles;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador REST para la gestión del catálogo de roles y perfiles del sistema.
 * Proporciona operaciones para consultar roles disponibles y registrar nuevos roles (solo administradores).
 */
@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Endpoints para la consulta y administración de perfiles y roles de usuario")
public class RoleController {

    private final RoleRepository roles;

    /**
     * Constructor con inyección de dependencias del repositorio de roles.
     *
     * @param roles Repositorio JPA de roles.
     */
    public RoleController(RoleRepository roles) {
        this.roles = roles;
    }

    /**
     * Obtiene el listado completo de roles definidos en el sistema.
     *
     * @return Lista de objetos {@link RoleResponse} con los datos de cada rol.
     */
    @GetMapping
    @Operation(summary = "Listar roles", description = "Devuelve todos los roles activos registrados en la base de datos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido")
    })
    public List<RoleResponse> list() {
        return roles.findAll().stream().map(RoleResponse::from).toList();
    }

    /**
     * Consulta el detalle de un rol específico por su código identificador.
     *
     * @param code Código único del rol (ej: FIELD_TECHNICIAN, TEST_ENGINEER, ADMIN, CLIENT).
     * @return Objeto {@link RoleResponse} con los datos del rol solicitado.
     * @throws ResponseStatusException con estado 404 si el rol no existe.
     */
    @GetMapping("/{code}")
    @Operation(summary = "Obtener rol por código", description = "Busca y devuelve la información de un rol específico mediante su código.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol encontrado"),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado con el código especificado")
    })
    public RoleResponse get(
            @Parameter(description = "Código del rol a consultar (ej: ADMIN, CLIENT)", required = true)
            @PathVariable String code) {
        return roles.findByCode(code)
                .map(RoleResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    /**
     * Endpoint interno para consulta de rol sin requerir autenticación JWT previa.
     * Utilizado para comunicación entre microservicios (por ejemplo, desde identity-service).
     *
     * @param code Código del rol a validar o consultar.
     * @return Objeto {@link RoleResponse}.
     */
    @GetMapping("/internal/{code}")
    @Operation(summary = "Consulta interna de rol", description = "Endpoint de uso interno entre microservicios para resolver datos de un rol.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol resuelto correctamente"),
            @ApiResponse(responseCode = "404", description = "Rol no existe")
    })
    public RoleResponse getInternal(
            @Parameter(description = "Código del rol", required = true)
            @PathVariable String code) {
        return get(code);
    }

    /**
     * Registra un nuevo rol en el sistema.
     * Operación restringida exclusivamente a usuarios con rol ADMIN.
     *
     * @param request Datos del nuevo rol a crear.
     * @return Objeto {@link RoleResponse} del rol creado.
     * @throws ResponseStatusException con estado 409 si ya existe un rol con ese código.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear nuevo rol", description = "Crea un nuevo rol en el catálogo. Requiere permisos de administrador (ROLE_ADMIN).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rol creado satisfactoriamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: requiere rol ADMIN"),
            @ApiResponse(responseCode = "409", description = "El código de rol ya se encuentra registrado")
    })
    public RoleResponse create(@Valid @RequestBody RoleRequest request) {
        if (roles.findByCode(request.code()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role code already exists");
        }
        return RoleResponse.from(roles.save(new RoleEntity(
                request.id(), request.code(), request.name(), request.description())));
    }

    /**
     * DTO de petición para la creación de un nuevo rol.
     *
     * @param id Identificador único alfanumérico (ej: P-05).
     * @param code Código representativo en mayúsculas (ej: SUPERVISOR).
     * @param name Nombre legible del rol (ej: Supervisor de Operaciones).
     * @param description Descripción detallada de las responsabilidades del rol.
     */
    public record RoleRequest(
            @NotBlank String id,
            @NotBlank String code,
            @NotBlank String name,
            String description) {
    }

    /**
     * DTO de respuesta que expone la información de un rol.
     *
     * @param id Identificador único del rol.
     * @param code Código del rol.
     * @param name Nombre descriptivo.
     * @param description Detalle de permisos o alcance.
     */
    public record RoleResponse(String id, String code, String name, String description) {
        /**
         * Mapea una entidad JPA {@link RoleEntity} a su DTO de respuesta.
         *
         * @param role Entidad de base de datos.
         * @return Instancia de {@link RoleResponse}.
         */
        static RoleResponse from(RoleEntity role) {
            return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDescription());
        }
    }
}

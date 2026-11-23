package com.induztek.catalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador REST para el catálogo de equipos técnicos e instrumentos de prueba.
 * Gestiona inventario, detección de duplicados y control de calibración de instrumentos.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Catálogo", description = "Endpoints para la gestión de equipos técnicos e instrumental de medición")
public class CatalogController {

    private final EquipoRepository equipos;
    private final InstrumentoRepository instrumentos;

    /**
     * Constructor con inyección de repositorios.
     *
     * @param equipos Repositorio de equipos.
     * @param instrumentos Repositorio de instrumentos de medición.
     */
    public CatalogController(EquipoRepository equipos, InstrumentoRepository instrumentos) {
        this.equipos = equipos;
        this.instrumentos = instrumentos;
    }

    /**
     * Obtiene la lista de equipos registrados.
     * Si el usuario autenticado tiene rol CLIENT, el listado se restringe automáticamente a los equipos de su empresa.
     *
     * @param cliente Filtro opcional por nombre de empresa/cliente (aplicable a personal interno).
     * @param jwt Token JWT del usuario autenticado.
     * @return Lista de {@link EquipoResponse}.
     */
    @GetMapping("/equipos")
    @Operation(summary = "Listar equipos", description = "Obtiene los equipos registrados. Clientes solo acceden a los equipos de su propia compañía.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de equipos obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public List<EquipoResponse> listEquipment(
            @Parameter(description = "Nombre del cliente para filtrar equipos (opcional)")
            @RequestParam(required = false) String cliente,
            @AuthenticationPrincipal Jwt jwt) {
        String scope = "CLIENT".equals(jwt.getClaimAsString("role")) ? jwt.getClaimAsString("company") : cliente;
        return equipos.findAll().stream()
                .filter(equipo -> scope == null || scope.equalsIgnoreCase(equipo.getCliente()))
                .map(EquipoResponse::from)
                .toList();
    }

    /**
     * Consulta el detalle de un equipo a partir de su código único.
     *
     * @param codigo Código identificador del equipo.
     * @param jwt Token JWT del usuario.
     * @return {@link EquipoResponse} con los datos técnicos del equipo.
     * @throws ResponseStatusException 404 si el equipo no existe o si pertenece a otra empresa y el usuario es CLIENT.
     */
    @GetMapping("/equipos/{codigo}")
    @Operation(summary = "Obtener equipo por código", description = "Devuelve las especificaciones técnicas completas de un equipo por su código único.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipo encontrado"),
            @ApiResponse(responseCode = "404", description = "Equipo no encontrado o sin acceso")
    })
    public EquipoResponse getEquipment(
            @Parameter(description = "Código del equipo (ej: TR-01)", required = true)
            @PathVariable String codigo,
            @AuthenticationPrincipal Jwt jwt) {
        EquipoEntity entity = equipos.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));
        if ("CLIENT".equals(jwt.getClaimAsString("role"))
                && !java.util.Objects.equals(entity.getCliente(), jwt.getClaimAsString("company"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found");
        }
        return EquipoResponse.from(entity);
    }

    /**
     * Endpoint interno para verificar la existencia de un equipo por código.
     * Utilizado principalmente por operations-service para validar antes de registrar protocolos.
     *
     * @param codigo Código del equipo a consultar.
     * @return {@link EquipmentExistsResponse} indicando si existe o no.
     */
    @GetMapping("/equipos/internal/{codigo}")
    @Operation(summary = "Validación interna de equipo", description = "Verifica si un equipo existe por código (uso interno entre microservicios).")
    @ApiResponse(responseCode = "200", description = "Resultado de la verificación")
    public EquipmentExistsResponse findEquipmentInternal(
            @Parameter(description = "Código del equipo a comprobar", required = true)
            @PathVariable String codigo) {
        return new EquipmentExistsResponse(equipos.findByCodigo(codigo).isPresent());
    }

    /**
     * Comprueba si un equipo ya se encuentra registrado comparando ubicación, tag, número de serie o código.
     *
     * @param ubicacion Ubicación física del equipo.
     * @param tag Tag asignado por el cliente.
     * @param numeroSerie Número de serie de fábrica.
     * @param codigo Código interno.
     * @return {@link DuplicateResponse} con el resultado y datos del equipo duplicado si existe.
     */
    @GetMapping("/equipos/duplicados")
    @Operation(summary = "Verificar equipos duplicados", description = "Evalúa si ya existe un equipo con coincidencia de ubicación, tag, serie o código.")
    @ApiResponse(responseCode = "200", description = "Resultado del análisis de duplicidad")
    public DuplicateResponse duplicateCheck(
            @Parameter(description = "Ubicación o subestación", required = true) @RequestParam String ubicacion,
            @Parameter(description = "Tag de cliente") @RequestParam(required = false, defaultValue = "") String tag,
            @Parameter(description = "Número de serie") @RequestParam(required = false, defaultValue = "") String numeroSerie,
            @Parameter(description = "Código de equipo") @RequestParam(required = false, defaultValue = "") String codigo) {
        return equipos.findAll().stream()
                .filter(equipo -> ubicacion.equalsIgnoreCase(equipo.getUbicacion()))
                .filter(equipo -> (!tag.isBlank() && tag.equalsIgnoreCase(equipo.getTagCliente()))
                        || (!numeroSerie.isBlank() && numeroSerie.equalsIgnoreCase(equipo.getNumeroSerie()))
                        || (!codigo.isBlank() && codigo.equalsIgnoreCase(equipo.getCodigo())))
                .findFirst()
                .map(EquipmentDuplicate::from)
                .map(duplicate -> new DuplicateResponse(true, duplicate))
                .orElseGet(() -> new DuplicateResponse(false, null));
    }

    /**
     * Registra un nuevo equipo técnico en el catálogo.
     * Requiere rol ADMIN o FIELD_TECHNICIAN.
     *
     * @param request Datos del equipo a ingresar.
     * @return {@link EquipoResponse} del equipo creado.
     * @throws ResponseStatusException 409 si el código de equipo ya existe.
     */
    @PostMapping("/equipos")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FIELD_TECHNICIAN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear equipo", description = "Registra un nuevo equipo industrial en el catálogo. Requiere rol ADMIN o FIELD_TECHNICIAN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Equipo registrado con éxito"),
            @ApiResponse(responseCode = "400", description = "Parámetros de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "El código de equipo ya existe")
    })
    public EquipoResponse createEquipment(@Valid @RequestBody EquipoRequest request) {
        if (equipos.findByCodigo(request.codigo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Equipment code already exists");
        }
        return EquipoResponse.from(equipos.save(request.toEntity(UUID.randomUUID().toString())));
    }

    /**
     * Actualiza la información técnica de un equipo existente.
     * Requiere rol ADMIN o FIELD_TECHNICIAN.
     *
     * @param codigo Código actual del equipo.
     * @param request Nuevos datos para el equipo.
     * @return {@link EquipoResponse} actualizado.
     */
    @PutMapping("/equipos/{codigo}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FIELD_TECHNICIAN')")
    @Operation(summary = "Actualizar equipo", description = "Modifica los datos técnicos de un equipo existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipo actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "Equipo no encontrado"),
            @ApiResponse(responseCode = "409", description = "El nuevo código colisiona con otro equipo existente")
    })
    public EquipoResponse updateEquipment(
            @Parameter(description = "Código actual del equipo", required = true) @PathVariable String codigo,
            @Valid @RequestBody EquipoRequest request) {
        EquipoEntity entity = equipos.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found"));
        if (!codigo.equals(request.codigo()) && equipos.findByCodigo(request.codigo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Equipment code already exists");
        }
        entity.update(request);
        return EquipoResponse.from(equipos.save(entity));
    }

    /**
     * Lista los instrumentos de medición registrados.
     *
     * @param activos Si es true, retorna únicamente los instrumentos activos/habilitados.
     * @return Lista de {@link InstrumentoResponse}.
     */
    @GetMapping("/instrumentos")
    @Operation(summary = "Listar instrumentos", description = "Obtiene los instrumentos de medición disponibles, con opción de filtrar solo activos.")
    @ApiResponse(responseCode = "200", description = "Lista de instrumentos obtenida")
    public List<InstrumentoResponse> listInstruments(
            @Parameter(description = "Filtrar únicamente instrumentos activos")
            @RequestParam(required = false, defaultValue = "false") boolean activos) {
        return (activos ? instrumentos.findByActivoTrue() : instrumentos.findAll())
                .stream().map(InstrumentoResponse::from).toList();
    }

    /**
     * Consulta un instrumento por su identificador único.
     *
     * @param id Identificador del instrumento.
     * @return {@link InstrumentoResponse} con los datos del instrumento.
     */
    @GetMapping("/instrumentos/{id}")
    @Operation(summary = "Obtener instrumento por ID", description = "Devuelve los datos y estado de calibración de un instrumento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instrumento encontrado"),
            @ApiResponse(responseCode = "404", description = "Instrumento no encontrado")
    })
    public InstrumentoResponse getInstrument(
            @Parameter(description = "ID del instrumento", required = true) @PathVariable String id) {
        return instrumentos.findById(id).map(InstrumentoResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
    }

    /**
     * Registra un nuevo instrumento de medición.
     * Requiere rol ADMIN o TEST_ENGINEER.
     *
     * @param request Datos del instrumento.
     * @return {@link InstrumentoResponse} creado.
     */
    @PostMapping("/instrumentos")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_TEST_ENGINEER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear instrumento", description = "Registra un nuevo instrumento con su fecha de vencimiento de calibración.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Instrumento registrado exitosamente"),
            @ApiResponse(responseCode = "409", description = "El ID de instrumento ya existe")
    })
    public InstrumentoResponse createInstrument(@Valid @RequestBody InstrumentoRequest request) {
        String id = request.id() == null || request.id().isBlank() ? UUID.randomUUID().toString() : request.id();
        if (instrumentos.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instrument ID already exists");
        }
        return InstrumentoResponse.from(instrumentos.save(new InstrumentoEntity(
                id, request.tipo(), request.marca(), request.modelo(), request.numeroSerie(),
                request.vencimientoCalibracion(), request.activo() == null || request.activo())));
    }

    /**
     * Actualiza la fecha de vencimiento de calibración de un instrumento.
     * Requiere rol ADMIN o TEST_ENGINEER.
     *
     * @param id ID del instrumento.
     * @param request Nueva fecha de vencimiento.
     * @return {@link InstrumentoResponse} con la nueva fecha de calibración.
     */
    @PutMapping("/instrumentos/{id}/calibracion")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_TEST_ENGINEER')")
    @Operation(summary = "Actualizar calibración", description = "Modifica la fecha de vencimiento de calibración de un instrumento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calibración actualizada"),
            @ApiResponse(responseCode = "404", description = "Instrumento no encontrado")
    })
    public InstrumentoResponse updateCalibration(
            @Parameter(description = "ID del instrumento", required = true) @PathVariable String id,
            @Valid @RequestBody CalibrationRequest request) {
        InstrumentoEntity entity = instrumentos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
        entity.setVencimientoCalibracion(request.vencimientoCalibracion());
        return InstrumentoResponse.from(instrumentos.save(entity));
    }

    /**
     * Habilita o deshabilita un instrumento de medición.
     * Requiere rol ADMIN.
     *
     * @param id ID del instrumento.
     * @param request Estado booleano de activación.
     * @return {@link InstrumentoResponse} con el nuevo estado.
     */
    @PutMapping("/instrumentos/{id}/estado")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Cambiar estado de instrumento", description = "Activa o desactiva un instrumento. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado modificado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Instrumento no encontrado")
    })
    public InstrumentoResponse setInstrumentActive(
            @Parameter(description = "ID del instrumento", required = true) @PathVariable String id,
            @Valid @RequestBody ActiveRequest request) {
        InstrumentoEntity entity = instrumentos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
        entity.setActivo(request.activo());
        return InstrumentoResponse.from(instrumentos.save(entity));
    }

    /** DTO para creación/actualización de un equipo técnico. */
    public record EquipoRequest(
            @NotBlank String codigo,
            @Size(max = 80) String tagCliente,
            @NotBlank String tipo,
            String cliente,
            @NotBlank String ubicacion,
            String marca,
            String modelo,
            String numeroSerie,
            String potenciaTension,
            String anio,
            String tensiones,
            String tap,
            String conexion,
            @NotBlank String ultimoEstado) {
        EquipoEntity toEntity(String id) {
            return new EquipoEntity(id, codigo, tagCliente, tipo, cliente, ubicacion, marca, modelo,
                    numeroSerie, potenciaTension, anio, tensiones, tap, conexion, ultimoEstado);
        }
    }

    /** DTO de respuesta con la información de un equipo técnico. */
    public record EquipoResponse(
            String id, String codigo, String tagCliente, String tipo, String cliente, String ubicacion,
            String marca, String modelo, String numeroSerie, String potenciaTension, String anio,
            String tensiones, String tap, String conexion, String ultimoEstado) {
        static EquipoResponse from(EquipoEntity entity) {
            return new EquipoResponse(entity.getId(), entity.getCodigo(), entity.getTagCliente(), entity.getTipo(),
                    entity.getCliente(), entity.getUbicacion(), entity.getMarca(), entity.getModelo(),
                    entity.getNumeroSerie(), entity.getPotenciaTension(), entity.getAnio(), entity.getTensiones(),
                    entity.getTap(), entity.getConexion(), entity.getUltimoEstado());
        }
    }

    /** DTO para responder la comprobación de existencia de equipo. */
    public record EquipmentExistsResponse(boolean exists) {
    }

    /** DTO que representa un equipo coincidente en la detección de duplicados. */
    public record EquipmentDuplicate(String id, String codigo, String tagCliente, String numeroSerie) {
        static EquipmentDuplicate from(EquipoEntity entity) {
            return new EquipmentDuplicate(entity.getId(), entity.getCodigo(), entity.getTagCliente(), entity.getNumeroSerie());
        }
    }

    /** DTO de respuesta para la validación de duplicados. */
    public record DuplicateResponse(boolean duplicate, EquipmentDuplicate equipment) {
    }

    /** DTO para la creación de un instrumento de medición. */
    public record InstrumentoRequest(
            String id,
            @NotBlank String tipo,
            String marca,
            String modelo,
            String numeroSerie,
            String vencimientoCalibracion,
            Boolean activo) {
    }

    /** DTO de respuesta con los datos de un instrumento. */
    public record InstrumentoResponse(
            String id, String tipo, String marca, String modelo, String numeroSerie,
            String vencimientoCalibracion, boolean activo) {
        static InstrumentoResponse from(InstrumentoEntity entity) {
            return new InstrumentoResponse(entity.getId(), entity.getTipo(), entity.getMarca(), entity.getModelo(),
                    entity.getNumeroSerie(), entity.getVencimientoCalibracion(), entity.isActivo());
        }
    }

    /** DTO para actualizar la fecha de calibración. */
    public record CalibrationRequest(@NotBlank String vencimientoCalibracion) {
    }

    /** DTO para modificar el estado activo de un instrumento. */
    public record ActiveRequest(@NotNull Boolean activo) {
    }
}

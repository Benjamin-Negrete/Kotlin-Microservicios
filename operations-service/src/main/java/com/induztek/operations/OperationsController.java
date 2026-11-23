package com.induztek.operations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador REST principal para la gestión de operaciones técnicas de terreno.
 * Administra el ciclo de vida de visitas técnicas, protocolos de mantenimiento,
 * pruebas eléctricas especializadas (con mediciones estructuradas en JSON) y actas de entrega.
 */
@RestController
@Tag(name = "Operaciones", description = "Endpoints para el registro y consulta de visitas técnicas, protocolos, pruebas y actas")
public class OperationsController {

    private static final DateTimeFormatter LOCAL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final TypeReference<List<Map<String, Object>>> JSON_OBJECT_LIST = new TypeReference<>() {};
    private final VisitRepository visits;
    private final ProtocolRepository protocols;
    private final TestRepository tests;
    private final ActaRepository actas;
    private final CatalogClient catalog;
    private final ObjectMapper mapper;

    /**
     * Constructor con inyección de repositorios, cliente de catálogo y mapper JSON.
     */
    public OperationsController(
            VisitRepository visits, ProtocolRepository protocols, TestRepository tests,
            ActaRepository actas, CatalogClient catalog, ObjectMapper mapper) {
        this.visits = visits;
        this.protocols = protocols;
        this.tests = tests;
        this.actas = actas;
        this.catalog = catalog;
        this.mapper = mapper;
    }

    /**
     * Lista las visitas técnicas registradas.
     * Si el usuario es de rol CLIENT, se filtran automáticamente las visitas de su empresa.
     *
     * @param jwt Token JWT del usuario autenticado.
     * @return Lista de {@link VisitResponse}.
     */
    @GetMapping("/api/visitas")
    @Operation(summary = "Listar visitas", description = "Obtiene las visitas a terreno. Los clientes solo visualizan las correspondientes a su empresa.")
    @ApiResponse(responseCode = "200", description = "Listado de visitas obtenido exitosamente")
    public List<VisitResponse> listVisits(@AuthenticationPrincipal Jwt jwt) {
        List<VisitEntity> result = isClient(jwt)
                ? visits.findByClienteIgnoreCase(jwt.getClaimAsString("company"))
                : visits.findAll();
        return result.stream().map(VisitResponse::from).toList();
    }

    /**
     * Consulta el detalle de una visita técnica por su ID.
     *
     * @param id ID de la visita.
     * @param jwt Token JWT autenticado.
     * @return {@link VisitResponse} con la información de la visita.
     */
    @GetMapping("/api/visitas/{id}")
    @Operation(summary = "Obtener visita por ID", description = "Devuelve el detalle de una visita técnica garantizando control de acceso por cliente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visita encontrada"),
            @ApiResponse(responseCode = "404", description = "Visita no encontrada o sin acceso")
    })
    public VisitResponse getVisit(
            @Parameter(description = "ID de la visita", required = true) @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        VisitEntity visit = visits.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found"));
        ensureVisitAccess(visit, jwt);
        return VisitResponse.from(visit);
    }

    /**
     * Registra una nueva visita técnica en terreno.
     * Requiere rol ADMIN o FIELD_TECHNICIAN.
     *
     * @param request Datos de la visita (orden de trabajo, fecha, cliente, cuadrilla).
     * @return {@link VisitResponse} creada.
     */
    @PostMapping("/api/visitas")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FIELD_TECHNICIAN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear visita", description = "Registra una nueva visita técnica. Requiere rol ADMIN o FIELD_TECHNICIAN.")
    @ApiResponse(responseCode = "201", description = "Visita creada correctamente")
    public VisitResponse createVisit(@Valid @RequestBody VisitRequest request) {
        return VisitResponse.from(visits.save(new VisitEntity(request)));
    }

    /**
     * Marca una visita técnica como finalizada/cerrada.
     * Requiere rol ADMIN, FIELD_TECHNICIAN o TEST_ENGINEER.
     *
     * @param id ID de la visita a cerrar.
     * @return {@link VisitResponse} con el estado cerrada = true.
     */
    @PatchMapping("/api/visitas/{id}/cerrar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FIELD_TECHNICIAN','ROLE_TEST_ENGINEER')")
    @Operation(summary = "Cerrar visita", description = "Cambia el estado de una visita a cerrada. Impide modificaciones posteriores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visita cerrada"),
            @ApiResponse(responseCode = "404", description = "Visita no encontrada")
    })
    public VisitResponse closeVisit(
            @Parameter(description = "ID de la visita a cerrar", required = true) @PathVariable String id) {
        VisitEntity visit = visits.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found"));
        visit.close();
        return VisitResponse.from(visits.save(visit));
    }

    /**
     * Lista los protocolos de prueba, opcionalmente filtrados por visita o equipo.
     *
     * @param visitaId Filtro opcional por ID de visita.
     * @param equipoCodigo Filtro opcional por código de equipo.
     * @param jwt Token JWT del usuario.
     * @return Lista de {@link ProtocolResponse}.
     */
    @GetMapping("/api/protocolos")
    @Operation(summary = "Listar protocolos", description = "Obtiene los protocolos registrados, con soporte para filtrar por visita o equipo.")
    @ApiResponse(responseCode = "200", description = "Lista de protocolos")
    public List<ProtocolResponse> listProtocols(
            @Parameter(description = "ID de visita técnica") @RequestParam(required = false) String visitaId,
            @Parameter(description = "Código de equipo") @RequestParam(required = false) String equipoCodigo,
            @AuthenticationPrincipal Jwt jwt) {
        List<ProtocolEntity> result;
        if (isClient(jwt)) {
            List<String> visitIds = visits.findByClienteIgnoreCase(jwt.getClaimAsString("company")).stream()
                    .map(VisitEntity::getId).toList();
            result = visitIds.isEmpty() ? List.of() : protocols.findByVisitaIdIn(visitIds);
            if (visitaId != null) {
                result = result.stream().filter(protocol -> visitaId.equals(protocol.getVisitaId())).toList();
            }
            if (equipoCodigo != null) {
                result = result.stream().filter(protocol -> equipoCodigo.equals(protocol.getEquipoCodigo())).toList();
            }
        } else {
            result = visitaId != null
                    ? protocols.findByVisitaId(visitaId)
                    : equipoCodigo != null ? protocols.findByEquipoCodigo(equipoCodigo) : protocols.findAll();
        }
        return result.stream().map(this::protocolResponse).toList();
    }

    /**
     * Obtiene el detalle completo de un protocolo de prueba.
     *
     * @param id ID del protocolo.
     * @param jwt Token JWT.
     * @return {@link ProtocolResponse}.
     */
    @GetMapping("/api/protocolos/{id}")
    @Operation(summary = "Obtener protocolo por ID", description = "Devuelve el protocolo completo incluyendo mediciones estructuradas y URLs de evidencia fotográfica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Protocolo encontrado"),
            @ApiResponse(responseCode = "404", description = "Protocolo no encontrado o sin permisos")
    })
    public ProtocolResponse getProtocol(
            @Parameter(description = "ID del protocolo", required = true) @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        ProtocolEntity protocol = protocols.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol not found"));
        ensureProtocolAccess(protocol, jwt);
        return protocolResponse(protocol);
    }

    /**
     * Registra un nuevo protocolo para un equipo dentro de una visita técnica.
     * Valida la existencia previa del equipo en catalog-service.
     * Requiere rol ADMIN, FIELD_TECHNICIAN o TEST_ENGINEER.
     *
     * @param request Datos del protocolo.
     * @return {@link ProtocolResponse} creado.
     */
    @PostMapping("/api/protocolos")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FIELD_TECHNICIAN','ROLE_TEST_ENGINEER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear protocolo", description = "Registra un nuevo protocolo de pruebas. Valida con catalog-service que el equipo exista.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Protocolo registrado"),
            @ApiResponse(responseCode = "404", description = "Equipo no existe en el catálogo"),
            @ApiResponse(responseCode = "409", description = "Ya existe un protocolo para este equipo en esta visita")
    })
    public ProtocolResponse createProtocol(@Valid @RequestBody ProtocolRequest request) {
        catalog.requireEquipment(request.equipoCodigo());
        if (protocols.existsByVisitaIdAndEquipoCodigo(request.visitaId(), request.equipoCodigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A protocol already exists for this equipment and visit");
        }
        return protocolResponse(protocols.save(new ProtocolEntity(request, mapper)));
    }

    /**
     * Formaliza un protocolo de pruebas (aprobación técnica final).
     * Requiere rol ADMIN o TEST_ENGINEER.
     *
     * @param id ID del protocolo a formalizar.
     * @param request Datos de formalización (nombre/código del usuario responsable).
     * @return {@link ProtocolResponse} con estado FORMALIZADO.
     */
    @PostMapping("/api/protocolos/{id}/formalizar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_TEST_ENGINEER')")
    @Operation(summary = "Formalizar protocolo", description = "Aprobación técnica de un protocolo. Cambia el estado a FORMALIZADO y estampa fecha y firma técnica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Protocolo formalizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Protocolo no encontrado")
    })
    public ProtocolResponse formalizeProtocol(
            @Parameter(description = "ID del protocolo", required = true) @PathVariable String id,
            @Valid @RequestBody FormalizeRequest request) {
        ProtocolEntity protocol = protocols.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol not found"));
        protocol.formalize(request.usuario(), LocalDateTime.now().format(LOCAL_DATE_TIME));
        return protocolResponse(protocols.save(protocol));
    }

    /**
     * Lista las pruebas técnicas registradas, con filtros por código de equipo o protocolo.
     *
     * @param codigo Código de equipo en path (opcional).
     * @param protocoloId ID de protocolo en query (opcional).
     * @param equipoCodigo Código de equipo en query (opcional).
     * @param jwt Token JWT.
     * @return Lista de {@link TestResponse}.
     */
    @GetMapping({"/api/equipos/{codigo}/pruebas", "/api/pruebas"})
    @Operation(summary = "Listar pruebas", description = "Devuelve las pruebas técnicas filtradas por protocolo o código de equipo.")
    @ApiResponse(responseCode = "200", description = "Lista de pruebas")
    public List<TestResponse> listTests(
            @Parameter(description = "Código del equipo en la ruta") @PathVariable(required = false) String codigo,
            @Parameter(description = "ID de protocolo") @RequestParam(required = false) String protocoloId,
            @Parameter(description = "Código de equipo en query") @RequestParam(required = false) String equipoCodigo,
            @AuthenticationPrincipal Jwt jwt) {
        String code = codigo != null ? codigo : equipoCodigo;
        List<TestEntity> result = protocoloId != null
                ? tests.findByProtocoloId(protocoloId)
                : code != null ? tests.findByEquipoCodigo(code) : tests.findAll();
        if (isClient(jwt)) {
            result = result.stream().filter(test -> canAccessProtocol(test.getProtocoloId(), jwt)).toList();
        }
        return result.stream().map(this::testResponse).toList();
    }

    /**
     * Consulta una prueba específica por su ID.
     *
     * @param id ID de la prueba.
     * @param jwt Token JWT.
     * @return {@link TestResponse}.
     */
    @GetMapping("/api/pruebas/{id}")
    @Operation(summary = "Obtener prueba por ID", description = "Devuelve las mediciones, estado y verificación de desviación histórica (>25%) de una prueba.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prueba encontrada"),
            @ApiResponse(responseCode = "404", description = "Prueba no encontrada")
    })
    public TestResponse getTest(
            @Parameter(description = "ID de la prueba", required = true) @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        TestEntity test = tests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));
        if (isClient(jwt) && !canAccessProtocol(test.getProtocoloId(), jwt)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found");
        }
        return testResponse(test);
    }

    /**
     * Registra una nueva prueba técnica individual.
     * Valida que el equipo exista en catalog-service.
     *
     * @param request Datos de la prueba y mediciones.
     * @return {@link TestResponse} creado.
     */
    @PostMapping("/api/pruebas")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FIELD_TECHNICIAN','ROLE_TEST_ENGINEER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear prueba", description = "Registra una prueba técnica individual para un equipo.")
    @ApiResponse(responseCode = "201", description = "Prueba registrada correctamente")
    public TestResponse createTest(@Valid @RequestBody TestRequest request) {
        catalog.requireEquipment(request.equipoCodigo());
        return testResponse(tests.save(new TestEntity(request, mapper)));
    }

    /**
     * Lista las actas de servicio emitidas.
     *
     * @param visitaId Filtro opcional por visita técnica.
     * @param jwt Token JWT.
     * @return Lista de {@link ActaResponse}.
     */
    @GetMapping("/api/actas")
    @Operation(summary = "Listar actas", description = "Devuelve las actas de entrega emitidas al cliente tras el servicio.")
    @ApiResponse(responseCode = "200", description = "Lista de actas")
    public List<ActaResponse> listActs(
            @Parameter(description = "ID de visita técnica") @RequestParam(required = false) String visitaId,
            @AuthenticationPrincipal Jwt jwt) {
        if (visitaId != null) {
            List<ActaEntity> result = actas.findByVisitaId(visitaId).map(List::of).orElseGet(List::of);
            if (isClient(jwt)) {
                result = result.stream().filter(act -> sameCompany(act.getCliente(), jwt)).toList();
            }
            return result.stream().map(ActaResponse::from).toList();
        }
        return actas.findAll().stream()
                .filter(act -> !isClient(jwt) || sameCompany(act.getCliente(), jwt))
                .map(ActaResponse::from).toList();
    }

    /**
     * Obtiene el detalle de un acta de servicio por su ID.
     *
     * @param id ID del acta.
     * @param jwt Token JWT.
     * @return {@link ActaResponse}.
     */
    @GetMapping("/api/actas/{id}")
    @Operation(summary = "Obtener acta por ID", description = "Consulta un acta específica de entrega de servicio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acta encontrada"),
            @ApiResponse(responseCode = "404", description = "Acta no encontrada")
    })
    public ActaResponse getAct(
            @Parameter(description = "ID del acta", required = true) @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        ActaEntity act = actas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Act not found"));
        if (isClient(jwt) && !sameCompany(act.getCliente(), jwt)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Act not found");
        }
        return ActaResponse.from(act);
    }

    /**
     * Genera una nueva acta de entrega de servicio técnico.
     * Requiere rol ADMIN, FIELD_TECHNICIAN o TEST_ENGINEER.
     *
     * @param request Datos del acta (técnico de entrega, recibido por, resumen).
     * @return {@link ActaResponse} emitida.
     */
    @PostMapping("/api/actas")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FIELD_TECHNICIAN','ROLE_TEST_ENGINEER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear acta", description = "Emite un acta de entrega de servicio asociada a una visita.")
    @ApiResponse(responseCode = "201", description = "Acta creada exitosamente")
    public ActaResponse createAct(@Valid @RequestBody ActaRequest request) {
        return ActaResponse.from(actas.save(new ActaEntity(request)));
    }

    private ProtocolResponse protocolResponse(ProtocolEntity entity) {
        return new ProtocolResponse(
                entity.getId(), entity.getCodigoProtocolo(), entity.getVisitaId(), entity.getOrdenTrabajo(),
                entity.getEquipoCodigo(), entity.getTipoEquipo(), entity.getFechaHora(), entity.getEstado(),
                entity.getResultadoGlobal(), entity.getTecnico(), entity.getTecnicoCreador(),
                entity.getDescripcionMantenimiento(), entity.getInstrumento(), entity.isCalibracionVigente(),
                entity.getTemperaturaAmbiente(), entity.getHumedad(), entity.isActaEntregadaCliente(),
                entity.getFormalizadoPor(), entity.getFechaFormalizacion(), entity.getSyncStatus(),
                entity.getObservaciones(), parseJsonList(entity.getPruebasJson()),
                parseJsonList(entity.getMedicionesJson()), parseStringList(entity.getFotosJson()));
    }

    private boolean isClient(Jwt jwt) {
        return "CLIENT".equals(jwt.getClaimAsString("role"));
    }

    private boolean sameCompany(String client, Jwt jwt) {
        String company = jwt.getClaimAsString("company");
        return company != null && client != null && company.equalsIgnoreCase(client);
    }

    private void ensureVisitAccess(VisitEntity visit, Jwt jwt) {
        if (isClient(jwt) && !sameCompany(visit.getCliente(), jwt)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found");
        }
    }

    private void ensureProtocolAccess(ProtocolEntity protocol, Jwt jwt) {
        if (isClient(jwt)) {
            VisitEntity visit = visits.findById(protocol.getVisitaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol not found"));
            ensureVisitAccess(visit, jwt);
        }
    }

    private boolean canAccessProtocol(String protocolId, Jwt jwt) {
        if (protocolId == null || protocolId.isBlank()) {
            return false;
        }
        return protocols.findById(protocolId).map(protocol -> {
            try {
                ensureProtocolAccess(protocol, jwt);
                return true;
            } catch (ResponseStatusException exception) {
                return false;
            }
        }).orElse(false);
    }

    private TestResponse testResponse(TestEntity entity) {
        return new TestResponse(
                entity.getId(), entity.getProtocoloId(), entity.getOrdenTrabajo(), entity.getEquipoCodigo(),
                entity.getTipoPrueba(), entity.getUbicacion(), entity.getFechaHora(), entity.getEstadoPrueba(),
                entity.getMotivoNoRealizada(), entity.getResultado(), entity.getInstrumentoId(),
                entity.getInstrumento(), entity.getInstrumentoSerie(), entity.getInstrumentoCalibracion(),
                entity.isCalibracionVigente(), entity.getTemperaturaAmbiente(), entity.getHumedad(),
                parseJsonList(entity.getMedicionesJson()), entity.getObservaciones(),
                entity.getVariacionHistoricaPorcentaje(), entity.isSuperaVariacion25());
    }

    private List<Map<String, Object>> parseJsonList(String value) {
        try {
            return mapper.readValue(value, JSON_OBJECT_LIST);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored operation data is invalid", exception);
        }
    }

    private List<String> parseStringList(String value) {
        try {
            return mapper.readValue(value, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored image URLs are invalid", exception);
        }
    }

    /** DTO para la creación de una visita técnica. */
    public record VisitRequest(
            String id, @NotBlank String ordenTrabajo, String fecha, String horaInicio,
            String cliente, String instalacion, String modalidad, String ingenieroACargo,
            String tecnicos, String temperaturaAmbiente, String humedad, String syncStatus) {
    }

    /** DTO de respuesta con la información de una visita. */
    public record VisitResponse(
            String id, String ordenTrabajo, String fecha, String horaInicio, String cliente,
            String instalacion, String modalidad, String ingenieroACargo, String tecnicos,
            String temperaturaAmbiente, String humedad, boolean cerrada, String syncStatus) {
        static VisitResponse from(VisitEntity entity) {
            return new VisitResponse(entity.getId(), entity.getOrdenTrabajo(), entity.getFecha(), entity.getHoraInicio(),
                    entity.getCliente(), entity.getInstalacion(), entity.getModalidad(), entity.getIngenieroACargo(),
                    entity.getTecnicos(), entity.getTemperaturaAmbiente(), entity.getHumedad(),
                    entity.isCerrada(), entity.getSyncStatus());
        }
    }

    /** DTO para representar una medición técnica estructurada. */
    public record Measurement(String campo, String valorNumerico, String unidad) {
    }

    /** DTO para registrar un protocolo de pruebas con evidencia y mediciones. */
    public record ProtocolRequest(
            String id, @NotBlank String codigoProtocolo, @NotBlank String visitaId,
            String ordenTrabajo, @NotBlank String equipoCodigo, String tipoEquipo, String fechaHora,
            String estado, String resultadoGlobal, String tecnico, String tecnicoCreador,
            String descripcionMantenimiento, String instrumento, boolean calibracionVigente,
            String temperaturaAmbiente, String humedad, boolean actaEntregadaCliente,
            String formalizadoPor, String fechaFormalizacion, String syncStatus, String observaciones,
            List<Map<String, Object>> pruebas, List<Measurement> mediciones, List<String> fotos) {
    }

    /** DTO de respuesta con los datos completos de un protocolo. */
    public record ProtocolResponse(
            String id, String codigoProtocolo, String visitaId, String ordenTrabajo,
            String equipoCodigo, String tipoEquipo, String fechaHora, String estado, String resultadoGlobal,
            String tecnico, String tecnicoCreador, String descripcionMantenimiento, String instrumento,
            boolean calibracionVigente, String temperaturaAmbiente, String humedad,
            boolean actaEntregadaCliente, String formalizadoPor, String fechaFormalizacion,
            String syncStatus, String observaciones, List<Map<String, Object>> pruebas,
            List<Map<String, Object>> mediciones, List<String> fotos) {
    }

    /** DTO para crear una prueba técnica. */
    public record TestRequest(
            String id, String protocoloId, String ordenTrabajo, @NotBlank String equipoCodigo,
            String tipoPrueba, String ubicacion, String fechaHora, String estadoPrueba,
            String motivoNoRealizada, String resultado, String instrumentoId, String instrumento,
            String instrumentoSerie, String instrumentoCalibracion, boolean calibracionVigente,
            String temperaturaAmbiente, String humedad, List<Measurement> mediciones,
            String observaciones, Double variacionHistoricaPorcentaje, boolean superaVariacion25) {
    }

    /** DTO de respuesta para una prueba técnica. */
    public record TestResponse(
            String id, String protocoloId, String ordenTrabajo, String equipoCodigo, String tipoPrueba,
            String ubicacion, String fechaHora, String estadoPrueba, String motivoNoRealizada,
            String resultado, String instrumentoId, String instrumento, String instrumentoSerie,
            String instrumentoCalibracion, boolean calibracionVigente, String temperaturaAmbiente,
            String humedad, List<Map<String, Object>> mediciones, String observaciones,
            Double variacionHistoricaPorcentaje, boolean superaVariacion25) {
    }

    /** DTO para la formalización de protocolo. */
    public record FormalizeRequest(@NotBlank String usuario) {
    }

    /** DTO para registrar un acta de entrega. */
    public record ActaRequest(
            String id, @NotBlank String visitaId, String ordenTrabajo, String cliente, String instalacion,
            String fechaEmision, String tecnicoEntrega, String recibidoPor, boolean marcaPendienteFormalizacion,
            String resumenProtocolos, String syncStatus) {
    }

    /** DTO de respuesta para un acta de entrega. */
    public record ActaResponse(
            String id, String visitaId, String ordenTrabajo, String cliente, String instalacion, String fechaEmision,
            String tecnicoEntrega, String recibidoPor, boolean marcaPendienteFormalizacion,
            String resumenProtocolos, String syncStatus) {
        static ActaResponse from(ActaEntity entity) {
            return new ActaResponse(entity.getId(), entity.getVisitaId(), entity.getOrdenTrabajo(), entity.getCliente(),
                    entity.getInstalacion(), entity.getFechaEmision(), entity.getTecnicoEntrega(),
                    entity.getRecibidoPor(), entity.isMarcaPendienteFormalizacion(),
                    entity.getResumenProtocolos(), entity.getSyncStatus());
        }
    }
}

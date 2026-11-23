package com.induztek.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA que representa un protocolo de mantenimiento en la base de datos induztek_operations.
 * Almacena el resultado global de las pruebas, datos del técnico, estado de formalización
 * y mediciones/pruebas serializadas en columnas LONGTEXT en formato JSON.
 */
@Entity
@Table(name = "protocolos")
public class ProtocolEntity {

    /** Identificador primario UUID del protocolo. */
    @Id
    private String id;

    /** Código de protocolo único visible (ej: PROT-2026-001). */
    @Column(nullable = false, unique = true)
    private String codigoProtocolo;

    /** Identificador de la visita técnica asociada. */
    @Column(nullable = false)
    private String visitaId;

    /** Número de orden de trabajo. */
    private String ordenTrabajo;

    /** Código del equipo inspeccionado (debe coincidir con catalog-service). */
    @Column(nullable = false)
    private String equipoCodigo;

    /** Tipo o familia de equipo. */
    private String tipoEquipo;

    /** Fecha y hora de ejecución (YYYY-MM-DD HH:mm). */
    private String fechaHora;

    /** Estado del protocolo (REGISTRADO_TERRENO, FORMALIZADO, OBSERVADO). */
    private String estado;

    /** Resultado global de la inspección (CONFORME, NO_CONFORME, REQUIERE_REVISION). */
    private String resultadoGlobal;

    /** Nombre del técnico que ejecutó las pruebas. */
    private String tecnico;

    /** Usuario o técnico creador del registro. */
    private String tecnicoCreador;

    /** Descripción de las actividades de mantenimiento realizadas. */
    @Column(columnDefinition = "text")
    private String descripcionMantenimiento;

    /** Nombre y modelo del instrumento principal utilizado. */
    private String instrumento;

    /** Indicador de si el instrumento cuenta con certificado de calibración vigente. */
    private boolean calibracionVigente;

    /** Temperatura ambiental durante las pruebas (°C). */
    private String temperaturaAmbiente;

    /** Humedad relativa ambiental (%). */
    private String humedad;

    /** Indica si el acta correspondiente fue entregada al cliente. */
    private boolean actaEntregadaCliente;

    /** Nombre o usuario del ingeniero que formalizó/aprobó el protocolo. */
    private String formalizadoPor;

    /** Fecha y hora exacta de formalización técnica. */
    private String fechaFormalizacion;

    /** Estado de sincronización móvil (PENDIENTE, SINCRONIZADO). */
    private String syncStatus;

    /** Observaciones y recomendaciones técnicas generales. */
    @Column(columnDefinition = "text")
    private String observaciones;

    /** JSON con la lista de pruebas asociadas serializadas. */
    @Column(columnDefinition = "longtext")
    private String pruebasJson;

    /** JSON con las mediciones estructuradas numéricas y unidades. */
    @Column(columnDefinition = "longtext")
    private String medicionesJson;

    /** JSON con las rutas relativas o URLs de fotos de evidencia tomadas. */
    @Column(columnDefinition = "longtext")
    private String fotosJson;

    /** Constructor por defecto requerido por JPA. */
    protected ProtocolEntity() {
    }

    /**
     * Constructor a partir del DTO {@link OperationsController.ProtocolRequest} y el serializador JSON.
     */
    public ProtocolEntity(OperationsController.ProtocolRequest request, ObjectMapper mapper) {
        id = request.id() == null || request.id().isBlank() ? UUID.randomUUID().toString() : request.id();
        codigoProtocolo = request.codigoProtocolo();
        visitaId = request.visitaId();
        ordenTrabajo = request.ordenTrabajo();
        equipoCodigo = request.equipoCodigo();
        tipoEquipo = request.tipoEquipo();
        fechaHora = request.fechaHora();
        estado = request.estado() == null ? "REGISTRADO_TERRENO" : request.estado();
        resultadoGlobal = request.resultadoGlobal();
        tecnico = request.tecnico();
        tecnicoCreador = request.tecnicoCreador();
        descripcionMantenimiento = request.descripcionMantenimiento();
        instrumento = request.instrumento();
        calibracionVigente = request.calibracionVigente();
        temperaturaAmbiente = request.temperaturaAmbiente();
        humedad = request.humedad();
        actaEntregadaCliente = request.actaEntregadaCliente();
        formalizadoPor = request.formalizadoPor();
        fechaFormalizacion = request.fechaFormalizacion();
        syncStatus = request.syncStatus() == null ? "PENDIENTE" : request.syncStatus();
        observaciones = request.observaciones();
        pruebasJson = serialize(mapper, request.pruebas());
        medicionesJson = serialize(mapper, request.mediciones());
        fotosJson = serialize(mapper, request.fotos());
    }

    /**
     * Serializa una estructura de datos Java en formato JSON para almacenamiento en base de datos.
     */
    static String serialize(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize protocol data", exception);
        }
    }

    public String getId() { return id; }
    public String getCodigoProtocolo() { return codigoProtocolo; }
    public String getVisitaId() { return visitaId; }
    public String getOrdenTrabajo() { return ordenTrabajo; }
    public String getEquipoCodigo() { return equipoCodigo; }
    public String getTipoEquipo() { return tipoEquipo; }
    public String getFechaHora() { return fechaHora; }
    public String getEstado() { return estado; }
    public String getResultadoGlobal() { return resultadoGlobal; }
    public String getTecnico() { return tecnico; }
    public String getTecnicoCreador() { return tecnicoCreador; }
    public String getDescripcionMantenimiento() { return descripcionMantenimiento; }
    public String getInstrumento() { return instrumento; }
    public boolean isCalibracionVigente() { return calibracionVigente; }
    public String getTemperaturaAmbiente() { return temperaturaAmbiente; }
    public String getHumedad() { return humedad; }
    public boolean isActaEntregadaCliente() { return actaEntregadaCliente; }
    public String getFormalizadoPor() { return formalizadoPor; }
    public String getFechaFormalizacion() { return fechaFormalizacion; }
    public String getSyncStatus() { return syncStatus; }
    public String getObservaciones() { return observaciones; }
    public String getPruebasJson() { return pruebasJson; }
    public String getMedicionesJson() { return medicionesJson; }
    public String getFotosJson() { return fotosJson; }

    /**
     * Aplica la formalización técnica del protocolo.
     *
     * @param user Nombre del ingeniero revisor.
     * @param date Fecha y hora de formalización.
     */
    public void formalize(String user, String date) {
        formalizadoPor = user;
        fechaFormalizacion = date;
        estado = "FORMALIZADO";
    }
}

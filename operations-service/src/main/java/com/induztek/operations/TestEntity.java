package com.induztek.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Entidad JPA que representa una prueba técnica individual en induztek_operations.
 * Registra tipo de prueba (Aislamiento, Relación de Transformación, Resistencia de Devanados),
 * instrumento utilizado, calibración, mediciones en JSON y evaluación de desviación histórica (>25%).
 */
@Entity
@Table(name = "pruebas")
public class TestEntity {

    /** Identificador primario UUID de la prueba. */
    @Id
    private String id;

    /** ID del protocolo al cual pertenece la prueba. */
    private String protocoloId;

    /** Número de orden de trabajo. */
    private String ordenTrabajo;

    /** Código del equipo ensayado. */
    private String equipoCodigo;

    /** Tipo de prueba técnica realizada. */
    private String tipoPrueba;

    /** Ubicación del ensayo (subestación, celda, patio de llaves). */
    private String ubicacion;

    /** Fecha y hora de ejecución. */
    private String fechaHora;

    /** Estado del ensayo (EJECUTADA, NO_REALIZADA, PENDIENTE). */
    private String estadoPrueba;

    /** Justificación técnica si la prueba no se pudo ejecutar. */
    @Column(columnDefinition = "text")
    private String motivoNoRealizada;

    /** Resultado final de la prueba (APROBADA, RECHAZADA, CON_OBSERVACIONES). */
    private String resultado;

    /** ID del instrumento de medición utilizado. */
    private String instrumentoId;

    /** Tipo o nombre del instrumento. */
    private String instrumento;

    /** Número de serie del instrumento. */
    private String instrumentoSerie;

    /** Fecha de vencimiento de calibración del instrumento. */
    private String instrumentoCalibracion;

    /** Indicador de si la calibración estaba vigente al momento de medir. */
    private boolean calibracionVigente;

    /** Temperatura ambiental durante el ensayo (°C). */
    private String temperaturaAmbiente;

    /** Humedad relativa ambiental (%). */
    private String humedad;

    /** Mediciones estructuradas en formato JSON (valores, parámetros, unidades). */
    @Column(columnDefinition = "longtext")
    private String medicionesJson;

    /** Observaciones técnicas del ensayista. */
    @Column(columnDefinition = "text")
    private String observaciones;

    /** Variación porcentual calculada respecto a mediciones históricas previas. */
    private Double variacionHistoricaPorcentaje;

    /** Indicador booleano de alerta: true si la variación supera el umbral crítico del 25%. */
    private boolean superaVariacion25;

    /** Constructor por defecto requerido por JPA. */
    protected TestEntity() {
    }

    /**
     * Constructor a partir del DTO {@link OperationsController.TestRequest} y el serializador JSON.
     */
    public TestEntity(OperationsController.TestRequest request, ObjectMapper mapper) {
        id = request.id() == null || request.id().isBlank() ? UUID.randomUUID().toString() : request.id();
        protocoloId = request.protocoloId();
        ordenTrabajo = request.ordenTrabajo();
        equipoCodigo = request.equipoCodigo();
        tipoPrueba = request.tipoPrueba();
        ubicacion = request.ubicacion();
        fechaHora = request.fechaHora();
        estadoPrueba = request.estadoPrueba();
        motivoNoRealizada = request.motivoNoRealizada();
        resultado = request.resultado();
        instrumentoId = request.instrumentoId();
        instrumento = request.instrumento();
        instrumentoSerie = request.instrumentoSerie();
        instrumentoCalibracion = request.instrumentoCalibracion();
        calibracionVigente = request.calibracionVigente();
        temperaturaAmbiente = request.temperaturaAmbiente();
        humedad = request.humedad();
        medicionesJson = ProtocolEntity.serialize(mapper, request.mediciones());
        observaciones = request.observaciones();
        variacionHistoricaPorcentaje = request.variacionHistoricaPorcentaje();
        superaVariacion25 = request.superaVariacion25();
    }

    public String getId() { return id; }
    public String getProtocoloId() { return protocoloId; }
    public String getOrdenTrabajo() { return ordenTrabajo; }
    public String getEquipoCodigo() { return equipoCodigo; }
    public String getTipoPrueba() { return tipoPrueba; }
    public String getUbicacion() { return ubicacion; }
    public String getFechaHora() { return fechaHora; }
    public String getEstadoPrueba() { return estadoPrueba; }
    public String getMotivoNoRealizada() { return motivoNoRealizada; }
    public String getResultado() { return resultado; }
    public String getInstrumentoId() { return instrumentoId; }
    public String getInstrumento() { return instrumento; }
    public String getInstrumentoSerie() { return instrumentoSerie; }
    public String getInstrumentoCalibracion() { return instrumentoCalibracion; }
    public boolean isCalibracionVigente() { return calibracionVigente; }
    public String getTemperaturaAmbiente() { return temperaturaAmbiente; }
    public String getHumedad() { return humedad; }
    public String getMedicionesJson() { return medicionesJson; }
    public String getObservaciones() { return observaciones; }
    public Double getVariacionHistoricaPorcentaje() { return variacionHistoricaPorcentaje; }
    public boolean isSuperaVariacion25() { return superaVariacion25; }
}

package com.induztek.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa una visita técnica a terreno en la base de datos induztek_operations.
 * Registra condiciones climáticas (temperatura, humedad), orden de trabajo, cliente y equipo técnico asignado.
 */
@Entity
@Table(name = "visitas")
public class VisitEntity {

    /** Identificador primario UUID de la visita. */
    @Id
    private String id;

    /** Número de orden de trabajo correlativa. */
    @Column(nullable = false)
    private String ordenTrabajo;

    /** Fecha de realización (YYYY-MM-DD). */
    private String fecha;

    /** Hora de inicio del servicio (HH:MM). */
    private String horaInicio;

    /** Nombre de la empresa cliente. */
    private String cliente;

    /** Planta, subestación o dirección técnica del servicio. */
    private String instalacion;

    /** Modalidad contractual (Mantenimiento Preventivo, Correctivo, Comisionamiento). */
    private String modalidad;

    /** Ingeniero de terreno a cargo del equipo técnico. */
    private String ingenieroACargo;

    /** Lista o descripción de técnicos integrantes de la cuadrilla. */
    @Column(columnDefinition = "text")
    private String tecnicos;

    /** Temperatura ambiental medida en °C. */
    private String temperaturaAmbiente;

    /** Humedad relativa ambiental en %. */
    private String humedad;

    /** Indicador de si la visita fue cerrada/concluida. */
    private boolean cerrada;

    /** Estado de sincronización móvil (PENDIENTE, SINCRONIZADO). */
    private String syncStatus;

    /** Constructor por defecto requerido por JPA. */
    protected VisitEntity() {
    }

    /**
     * Constructor a partir del DTO {@link OperationsController.VisitRequest}.
     */
    public VisitEntity(OperationsController.VisitRequest request) {
        id = request.id() == null || request.id().isBlank() ? java.util.UUID.randomUUID().toString() : request.id();
        ordenTrabajo = request.ordenTrabajo();
        fecha = request.fecha();
        horaInicio = request.horaInicio();
        cliente = request.cliente();
        instalacion = request.instalacion();
        modalidad = request.modalidad();
        ingenieroACargo = request.ingenieroACargo();
        tecnicos = request.tecnicos();
        temperaturaAmbiente = request.temperaturaAmbiente();
        humedad = request.humedad();
        cerrada = false;
        syncStatus = request.syncStatus() == null ? "PENDIENTE" : request.syncStatus();
    }

    public String getId() { return id; }
    public String getOrdenTrabajo() { return ordenTrabajo; }
    public String getFecha() { return fecha; }
    public String getHoraInicio() { return horaInicio; }
    public String getCliente() { return cliente; }
    public String getInstalacion() { return instalacion; }
    public String getModalidad() { return modalidad; }
    public String getIngenieroACargo() { return ingenieroACargo; }
    public String getTecnicos() { return tecnicos; }
    public String getTemperaturaAmbiente() { return temperaturaAmbiente; }
    public String getHumedad() { return humedad; }
    public boolean isCerrada() { return cerrada; }
    public String getSyncStatus() { return syncStatus; }

    /** Marca la visita como cerrada impidiendo modificaciones. */
    public void close() { cerrada = true; }
}

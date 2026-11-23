package com.induztek.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa la tabla 'actas' en la base de datos induztek_operations.
 * Almacena las actas de entrega emitidas al cliente tras el servicio técnico, con
 * constancia del técnico que entrega y la persona que recibe conforme.
 */
@Entity
@Table(name = "actas")
public class ActaEntity {

    /** Identificador primario UUID del acta. */
    @Id
    private String id;

    /** ID de la visita técnica vinculada. */
    private String visitaId;

    /** Número correlativo de Orden de Trabajo (OT). */
    private String ordenTrabajo;

    /** Nombre de la empresa cliente receptora. */
    private String cliente;

    /** Instalación, planta o subestación donde se realizó el servicio. */
    private String instalacion;

    /** Fecha de emisión del acta (formato YYYY-MM-DD). */
    private String fechaEmision;

    /** Nombre del técnico responsable de la entrega. */
    private String tecnicoEntrega;

    /** Nombre de la persona o contacto del cliente que recepciona el servicio. */
    private String recibidoPor;

    /** Marca booleana indicando si existen protocolos pendientes de formalización. */
    private boolean marcaPendienteFormalizacion;

    /** Resumen en texto o JSON de los protocolos cubiertos en la visita. */
    @Column(columnDefinition = "text")
    private String resumenProtocolos;

    /** Estado de sincronización móvil (PENDIENTE, SINCRONIZADO). */
    private String syncStatus;

    /** Constructor por defecto requerido por JPA. */
    protected ActaEntity() {
    }

    /**
     * Constructor a partir del DTO {@link OperationsController.ActaRequest}.
     */
    public ActaEntity(OperationsController.ActaRequest request) {
        id = request.id() == null || request.id().isBlank() ? java.util.UUID.randomUUID().toString() : request.id();
        visitaId = request.visitaId();
        ordenTrabajo = request.ordenTrabajo();
        cliente = request.cliente();
        instalacion = request.instalacion();
        fechaEmision = request.fechaEmision();
        tecnicoEntrega = request.tecnicoEntrega();
        recibidoPor = request.recibidoPor();
        marcaPendienteFormalizacion = request.marcaPendienteFormalizacion();
        resumenProtocolos = request.resumenProtocolos();
        syncStatus = request.syncStatus() == null ? "PENDIENTE" : request.syncStatus();
    }

    public String getId() { return id; }
    public String getVisitaId() { return visitaId; }
    public String getOrdenTrabajo() { return ordenTrabajo; }
    public String getCliente() { return cliente; }
    public String getInstalacion() { return instalacion; }
    public String getFechaEmision() { return fechaEmision; }
    public String getTecnicoEntrega() { return tecnicoEntrega; }
    public String getRecibidoPor() { return recibidoPor; }
    public boolean isMarcaPendienteFormalizacion() { return marcaPendienteFormalizacion; }
    public String getResumenProtocolos() { return resumenProtocolos; }
    public String getSyncStatus() { return syncStatus; }
}

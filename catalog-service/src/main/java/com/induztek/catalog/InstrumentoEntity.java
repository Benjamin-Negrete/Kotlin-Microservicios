package com.induztek.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa los instrumentos de medición y prueba en induztek_catalog.
 * Gestiona trazabilidad metrológica y vigencia de certificados de calibración.
 */
@Entity
@Table(name = "instrumentos")
public class InstrumentoEntity {

    /** Identificador único del instrumento. */
    @Id
    private String id;

    /** Tipo de instrumento (Megóhmetro, TTR, Microóhmetro, Analizador de Redes, etc.). */
    @Column(nullable = false)
    private String tipo;

    /** Marca o fabricante del instrumento. */
    private String marca;

    /** Modelo comercial del instrumento. */
    private String modelo;

    /** Número de serie metrológico. */
    private String numeroSerie;

    /** Fecha de vencimiento de la calibración vigente (formato YYYY-MM-DD). */
    private String vencimientoCalibracion;

    /** Estado de disponibilidad operativa del instrumento. */
    private boolean activo;

    /** Constructor por defecto requerido por JPA. */
    protected InstrumentoEntity() {
    }

    /**
     * Constructor con todos los campos.
     */
    public InstrumentoEntity(String id, String tipo, String marca, String modelo, String numeroSerie,
            String vencimientoCalibracion, boolean activo) {
        this.id = id;
        this.tipo = tipo;
        this.marca = marca;
        this.modelo = modelo;
        this.numeroSerie = numeroSerie;
        this.vencimientoCalibracion = vencimientoCalibracion;
        this.activo = activo;
    }

    public String getId() { return id; }
    public String getTipo() { return tipo; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public String getVencimientoCalibracion() { return vencimientoCalibracion; }
    public boolean isActivo() { return activo; }
    public void setVencimientoCalibracion(String value) { vencimientoCalibracion = value; }
    public void setActivo(boolean value) { activo = value; }
}

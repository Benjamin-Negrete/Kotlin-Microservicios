package com.induztek.catalog;

import com.induztek.catalog.CatalogController.EquipoRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa un equipo industrial (transformador, interruptor, etc.) en la base de datos induztek_catalog.
 * Contiene especificaciones técnicas como tensiones, conexión, TAP, potencia y ubicación física.
 */
@Entity
@Table(name = "equipos")
public class EquipoEntity {

    /** Identificador primario UUID del equipo. */
    @Id
    private String id;

    /** Código de identificación único asignado por la empresa (ej: TR-001). */
    @Column(nullable = false, unique = true, length = 80)
    private String codigo;

    /** Identificador o etiqueta (tag) asignada por el cliente en sus planos. */
    private String tagCliente;

    /** Tipo o familia de equipo (Transformador de Poder, Celda de Media Tensión, etc.). */
    @Column(nullable = false)
    private String tipo;

    /** Nombre de la empresa cliente propietaria del equipo. */
    private String cliente;

    /** Subestación, sala eléctrica o ubicación geográfica donde opera el equipo. */
    @Column(nullable = false)
    private String ubicacion;

    /** Marca del fabricante. */
    private String marca;

    /** Modelo comercial del equipo. */
    private String modelo;

    /** Número de serie estampado por el fabricante. */
    private String numeroSerie;

    /** Potencia nominal y tensiones de operación (ej: 2500 kVA - 13.8/0.4 kV). */
    private String potenciaTension;

    /** Año de fabricación o puesta en marcha. */
    private String anio;

    /** Rangos o niveles de tensión. */
    private String tensiones;

    /** Posición o rango del cambiador de tomas (TAP). */
    private String tap;

    /** Grupo de conexión (ej: Dyn11, YNd11). */
    private String conexion;

    /** Último estado operativo registrado (Operativo, Observado, Crítico). */
    private String ultimoEstado;

    /** Constructor por defecto para JPA. */
    protected EquipoEntity() {
    }

    /**
     * Constructor con todos los campos.
     */
    public EquipoEntity(String id, String codigo, String tagCliente, String tipo, String cliente, String ubicacion,
            String marca, String modelo, String numeroSerie, String potenciaTension, String anio, String tensiones,
            String tap, String conexion, String ultimoEstado) {
        this.id = id;
        this.codigo = codigo;
        this.tagCliente = tagCliente;
        this.tipo = tipo;
        this.cliente = cliente;
        this.ubicacion = ubicacion;
        this.marca = marca;
        this.modelo = modelo;
        this.numeroSerie = numeroSerie;
        this.potenciaTension = potenciaTension;
        this.anio = anio;
        this.tensiones = tensiones;
        this.tap = tap;
        this.conexion = conexion;
        this.ultimoEstado = ultimoEstado;
    }

    public String getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getTagCliente() { return tagCliente; }
    public String getTipo() { return tipo; }
    public String getCliente() { return cliente; }
    public String getUbicacion() { return ubicacion; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public String getPotenciaTension() { return potenciaTension; }
    public String getAnio() { return anio; }
    public String getTensiones() { return tensiones; }
    public String getTap() { return tap; }
    public String getConexion() { return conexion; }
    public String getUltimoEstado() { return ultimoEstado; }

    /**
     * Actualiza los datos de este equipo a partir de un DTO {@link EquipoRequest}.
     *
     * @param request Datos actualizados recibidos del cliente.
     */
    public void update(EquipoRequest request) {
        codigo = request.codigo();
        tagCliente = request.tagCliente();
        tipo = request.tipo();
        cliente = request.cliente();
        ubicacion = request.ubicacion();
        marca = request.marca();
        modelo = request.modelo();
        numeroSerie = request.numeroSerie();
        potenciaTension = request.potenciaTension();
        anio = request.anio();
        tensiones = request.tensiones();
        tap = request.tap();
        conexion = request.conexion();
        ultimoEstado = request.ultimoEstado();
    }
}

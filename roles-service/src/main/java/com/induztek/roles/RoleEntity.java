package com.induztek.roles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA que representa la tabla 'roles' en la base de datos induztek_roles.
 * Almacena los roles del sistema, su código único y su descripción.
 */
@Entity
@Table(name = "roles")
public class RoleEntity {

    /** Identificador primario del rol (ej: P-01, P-02). */
    @Id
    private String id;

    /** Código de rol único en mayúsculas (ej: ADMIN, FIELD_TECHNICIAN). */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    /** Nombre descriptivo del rol (ej: Técnico de terreno). */
    @Column(nullable = false, length = 80)
    private String name;

    /** Descripción de las tareas y responsabilidades asociadas. */
    @Column(length = 255)
    private String description;

    /** Constructor por defecto requerido por JPA. */
    protected RoleEntity() {
    }

    /**
     * Constructor con todos los campos.
     *
     * @param id Identificador único.
     * @param code Código único en mayúsculas.
     * @param name Nombre para visualización.
     * @param description Detalle del rol.
     */
    public RoleEntity(String id, String code, String name, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

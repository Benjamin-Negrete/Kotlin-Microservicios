package com.induztek.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entidad JPA que representa la tabla 'usuarios' en la base de datos induztek_identity.
 * Contiene credenciales hasheadas, rol, empresa asociada, aceptación de términos y perfil.
 */
@Entity
@Table(name = "usuarios")
public class UserEntity {

    /** Identificador primario UUID del usuario. */
    @Id
    private String id;

    /** Dirección de correo electrónico única normalizada en minúsculas. */
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    /** Contraseña cifrada mediante algoritmo BCrypt. */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    /** Nombre y apellidos del usuario. */
    @Column(nullable = false, length = 120)
    private String name;

    /** Código del rol asignado (FIELD_TECHNICIAN, TEST_ENGINEER, ADMIN, CLIENT). */
    @Column(nullable = false, length = 40)
    private String roleCode;

    /** Nombre de la empresa cliente si aplica (usado para aislar datos en modo CLIENT). */
    @Column(length = 120)
    private String company;

    /** Indicador obligatorio de aceptación de términos del servicio. */
    @Column(nullable = false)
    private boolean termsAccepted;

    /** Fecha y hora exacta de aceptación de términos. */
    @Column(nullable = false)
    private Instant termsAcceptedAt;

    /** Estado activo de la cuenta. Si es false, requiere aprobación de un administrador. */
    @Column(nullable = false)
    private boolean active;

    /** URL o nombre de la foto de perfil en el sistema. */
    @Column(length = 2048)
    private String fotoPerfil;

    /** URL alternativa de foto de perfil (compatibilidad de versiones). */
    @Column(length = 2048)
    private String photoUrl;

    /** Breve descripción o biografía del usuario. */
    @Column(length = 500)
    private String biography;

    /** Constructor por defecto para JPA. */
    protected UserEntity() {
    }

    /**
     * Constructor con los campos requeridos para el alta de un usuario.
     */
    public UserEntity(String id, String email, String passwordHash, String name, String roleCode,
            String company, Instant termsAcceptedAt, boolean active) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.roleCode = roleCode;
        this.company = company;
        this.termsAccepted = true;
        this.termsAcceptedAt = termsAcceptedAt;
        this.active = active;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public String getRoleCode() { return roleCode; }
    public String getCompany() { return company; }
    public boolean isTermsAccepted() { return termsAccepted; }
    public Instant getTermsAcceptedAt() { return termsAcceptedAt; }
    public boolean isActive() { return active; }
    public String getFotoPerfil() { return fotoPerfil != null ? fotoPerfil : photoUrl; }
    public String getPhotoUrl() { return photoUrl != null ? photoUrl : fotoPerfil; }
    public String getBiography() { return biography; }
    public void setActive(boolean active) { this.active = active; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setCompany(String company) { this.company = company; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; this.photoUrl = fotoPerfil; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; this.fotoPerfil = photoUrl; }
    public void setBiography(String biography) { this.biography = biography; }
}

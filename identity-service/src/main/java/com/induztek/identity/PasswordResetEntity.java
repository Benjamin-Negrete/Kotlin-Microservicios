package com.induztek.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entidad JPA que almacena los códigos temporales de recuperación de contraseña.
 * Almacena el hash seguro del código de 6 dígitos, la fecha de expiración y el estado de uso.
 */
@Entity
@Table(name = "password_reset_codes")
public class PasswordResetEntity {

    /** Identificador primario UUID. */
    @Id
    private String id;

    /** Correo del usuario que solicitó el restablecimiento. */
    @Column(nullable = false, length = 254)
    private String email;

    /** Hash BCrypt del código de recuperación generado. */
    @Column(nullable = false, length = 100)
    private String codeHash;

    /** Timestamp exacto tras el cual el código deja de ser válido. */
    @Column(nullable = false)
    private Instant expiresAt;

    /** Indicador de si el código ya fue consumido. */
    @Column(nullable = false)
    private boolean used;

    /** Constructor por defecto para JPA. */
    protected PasswordResetEntity() {
    }

    /**
     * Constructor para emitir un nuevo código de recuperación.
     */
    public PasswordResetEntity(String id, String email, String codeHash, Instant expiresAt) {
        this.id = id;
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}

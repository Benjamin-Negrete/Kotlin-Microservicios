package com.induztek.identity;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para los códigos de recuperación de contraseña.
 * Permite buscar códigos no usados y ordenados por fecha de expiración.
 */
public interface PasswordResetRepository extends JpaRepository<PasswordResetEntity, String> {

    /**
     * Retorna todos los códigos vigentes y no utilizados asociados a un email.
     */
    List<PasswordResetEntity> findByEmailIgnoreCaseAndUsedFalseOrderByExpiresAtDesc(String email);

    /**
     * Obtiene el código más reciente que aún no ha sido utilizado para el email dado.
     */
    Optional<PasswordResetEntity> findFirstByEmailIgnoreCaseAndUsedFalseOrderByExpiresAtDesc(String email);
}

package com.induztek.identity;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link UserEntity}.
 * Proporciona consultas de usuarios por correo electrónico ignorando mayúsculas/minúsculas.
 */
public interface UserRepository extends JpaRepository<UserEntity, String> {

    /**
     * Busca un usuario por su dirección de email.
     *
     * @param email Correo electrónico a buscar.
     * @return {@link Optional} con el usuario correspondiente.
     */
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    /**
     * Comprueba si ya existe un usuario registrado con ese email.
     *
     * @param email Correo electrónico a evaluar.
     * @return true si ya existe.
     */
    boolean existsByEmailIgnoreCase(String email);
}

package com.induztek.roles;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link RoleEntity}.
 * Proporciona métodos CRUD y consultas por código de rol.
 */
public interface RoleRepository extends JpaRepository<RoleEntity, String> {

    /**
     * Busca un rol a partir de su código único.
     *
     * @param code Código del rol (ej: ADMIN, CLIENT).
     * @return {@link Optional} con el rol si existe.
     */
    Optional<RoleEntity> findByCode(String code);
}

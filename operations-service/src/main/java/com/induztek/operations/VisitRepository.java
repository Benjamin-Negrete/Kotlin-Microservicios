package com.induztek.operations;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link VisitEntity}.
 * Proporciona consultas de visitas por cliente ignorando mayúsculas/minúsculas.
 */
public interface VisitRepository extends JpaRepository<VisitEntity, String> {

    /**
     * Lista todas las visitas asociadas a una empresa cliente.
     *
     * @param cliente Nombre del cliente.
     * @return Lista de visitas técnicas.
     */
    List<VisitEntity> findByClienteIgnoreCase(String cliente);
}

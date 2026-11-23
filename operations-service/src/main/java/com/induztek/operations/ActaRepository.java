package com.induztek.operations;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link ActaEntity}.
 * Proporciona consultas de actas por ID de visita técnica.
 */
public interface ActaRepository extends JpaRepository<ActaEntity, String> {

    /**
     * Busca el acta de entrega asociada a una visita técnica.
     *
     * @param visitaId Identificador de la visita.
     * @return {@link Optional} con el acta si existe.
     */
    Optional<ActaEntity> findByVisitaId(String visitaId);
}

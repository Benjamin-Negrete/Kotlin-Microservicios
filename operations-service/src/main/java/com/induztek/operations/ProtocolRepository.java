package com.induztek.operations;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link ProtocolEntity}.
 * Proporciona consultas por visita técnica, lista de visitas, código de equipo y comprobación de existencia.
 */
public interface ProtocolRepository extends JpaRepository<ProtocolEntity, String> {

    /** Busca protocolos asociados a una visita específica. */
    List<ProtocolEntity> findByVisitaId(String visitaId);

    /** Busca protocolos pertenecientes a cualquiera de las visitas en la lista dada. */
    List<ProtocolEntity> findByVisitaIdIn(List<String> visitaIds);

    /** Busca protocolos realizados sobre un equipo específico. */
    List<ProtocolEntity> findByEquipoCodigo(String equipoCodigo);

    /** Comprueba si ya existe un protocolo para el par visita - equipo. */
    boolean existsByVisitaIdAndEquipoCodigo(String visitaId, String equipoCodigo);
}

package com.induztek.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link InstrumentoEntity}.
 * Proporciona métodos de consulta para instrumentos activos.
 */
public interface InstrumentoRepository extends JpaRepository<InstrumentoEntity, String> {

    /**
     * Retorna únicamente los instrumentos que se encuentran activos/habilitados.
     *
     * @return Lista de {@link InstrumentoEntity} con activo = true.
     */
    List<InstrumentoEntity> findByActivoTrue();
}

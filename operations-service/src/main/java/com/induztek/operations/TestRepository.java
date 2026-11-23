package com.induztek.operations;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link TestEntity}.
 * Permite buscar pruebas por ID de protocolo o código de equipo.
 */
public interface TestRepository extends JpaRepository<TestEntity, String> {

    /** Retorna todas las pruebas asociadas a un protocolo. */
    List<TestEntity> findByProtocoloId(String protocoloId);

    /** Retorna todas las pruebas realizadas sobre un equipo técnico. */
    List<TestEntity> findByEquipoCodigo(String equipoCodigo);
}

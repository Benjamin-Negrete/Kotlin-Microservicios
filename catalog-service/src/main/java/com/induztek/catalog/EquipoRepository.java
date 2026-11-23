package com.induztek.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link EquipoEntity}.
 * Proporciona métodos de búsqueda por código e identificación de posibles duplicados.
 */
public interface EquipoRepository extends JpaRepository<EquipoEntity, String> {

    /**
     * Busca un equipo por su código alfanumérico único.
     *
     * @param codigo Código del equipo.
     * @return {@link Optional} con el equipo si se encuentra.
     */
    Optional<EquipoEntity> findByCodigo(String codigo);

    /**
     * Comprueba si ya existe un equipo con la misma ubicación y tag de cliente.
     */
    boolean existsByUbicacionAndTagCliente(String ubicacion, String tagCliente);

    /**
     * Comprueba si ya existe un equipo con la misma ubicación y número de serie.
     */
    boolean existsByUbicacionAndNumeroSerie(String ubicacion, String numeroSerie);
}

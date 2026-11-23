package com.induztek.roles;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Componente de inicialización (seeding) de datos de roles.
 * Asegura que los cuatro roles base del sistema existan al arrancar el microservicio.
 */
@Configuration
public class RoleSeed {

    /**
     * Tarea que se ejecuta al iniciar la aplicación para insertar los roles por defecto si no existen.
     * Roles iniciales: FIELD_TECHNICIAN, TEST_ENGINEER, ADMIN, CLIENT.
     *
     * @param roles Repositorio de roles.
     * @return {@link ApplicationRunner} con la lógica de inicialización.
     */
    @Bean
    ApplicationRunner seedRoles(RoleRepository roles) {
        return args -> {
            saveIfMissing(roles, "P-01", "FIELD_TECHNICIAN", "Técnico de terreno", "Registro de visitas y protocolos");
            saveIfMissing(roles, "P-02", "TEST_ENGINEER", "Ingeniero de pruebas", "Revisión y formalización de protocolos");
            saveIfMissing(roles, "P-03", "ADMIN", "Jefe técnico", "Administración y formalización de protocolos");
            saveIfMissing(roles, "P-04", "CLIENT", "Cliente", "Consulta de información de su empresa");
        };
    }

    /**
     * Guarda un rol únicamente si aún no se encuentra registrado en la base de datos.
     */
    private void saveIfMissing(RoleRepository roles, String id, String code, String name, String description) {
        if (roles.findByCode(code).isEmpty()) {
            roles.save(new RoleEntity(id, code, name, description));
        }
    }
}

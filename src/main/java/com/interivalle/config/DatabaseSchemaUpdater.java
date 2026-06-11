package com.interivalle.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaUpdater {

    @PersistenceContext
    private EntityManager entityManager;

    @PostConstruct
    @Transactional
    public void ajustarColumnasEstadosCotizacion() {
        // Los nuevos estados APROBADA_CLIENTE/APROBADA_FINAL requieren columnas mas amplias.
        ejecutarAlterSeguro("ALTER TABLE cotizacion MODIFY estado VARCHAR(40) NOT NULL");
        ejecutarAlterSeguro("ALTER TABLE cotizacion_historial_estado MODIFY estado_anterior VARCHAR(40) NOT NULL");
        ejecutarAlterSeguro("ALTER TABLE cotizacion_historial_estado MODIFY estado_nuevo VARCHAR(40) NOT NULL");
    }

    private void ejecutarAlterSeguro(String sql) {
        try {
            entityManager.createNativeQuery(sql).executeUpdate();
        } catch (Exception ignored) {
            // Si el usuario de BD no puede alterar o la columna ya esta correcta, la aplicacion debe seguir iniciando.
        }
    }
}

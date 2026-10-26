package com.denkitronik.clienteservice.config;

import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.repositories.IClienteDao;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Garantiza que exista al menos una Region al arrancar.
 * En producción no hace nada (import.sql ya carga las regiones); cuando la
 * tabla está vacía (p. ej. con el import.sql vacío de los tests) inserta las regiones base,
 * para que un POST con region {"id": 1} siempre tenga a qué referenciar.
 */
@Component
public class RegionInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RegionInicialRunner.class);

    // Las mismas regiones de import.sql, en el mismo orden (ids 1..8)
    static final List<String> REGIONES = List.of(
            "Suramerica", "Centroamerica", "Norteamerica", "Europa",
            "Asia", "Africa", "Oceania", "Antartida");

    private final IClienteDao clienteDao;
    private final EntityManager em;

    public RegionInicialRunner(IClienteDao clienteDao, EntityManager em) {
        this.clienteDao = clienteDao;
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!clienteDao.findAllRegiones().isEmpty()) {
            return;
        }
        REGIONES.forEach(nombre -> {
            Region region = new Region();
            region.setNombre(nombre);
            em.persist(region);
        });
        log.info("No había regiones: se crearon las {} regiones base", REGIONES.size());
    }
}

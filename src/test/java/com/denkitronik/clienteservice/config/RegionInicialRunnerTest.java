package com.denkitronik.clienteservice.config;

import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.repositories.IClienteDao;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests — RegionInicialRunner")
class RegionInicialRunnerTest {

    @Mock
    private IClienteDao clienteDao;

    @Mock
    private EntityManager em;

    @InjectMocks
    private RegionInicialRunner runner;

    @Test
    @DisplayName("run — sin regiones → persiste las regiones base")
    void run_sinRegiones_debeCrearLasRegionesBase() {
        when(clienteDao.findAllRegiones()).thenReturn(List.of());

        runner.run(null);

        verify(em, times(RegionInicialRunner.REGIONES.size())).persist(any(Region.class));
    }

    @Test
    @DisplayName("run — ya hay regiones (import.sql de producción) → no inserta nada")
    void run_conRegiones_noDebeInsertar() {
        when(clienteDao.findAllRegiones()).thenReturn(List.of(new Region()));

        runner.run(null);

        verify(em, never()).persist(any());
    }
}

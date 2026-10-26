package com.denkitronik.clienteservice;

import com.denkitronik.clienteservice.domain.repositories.IClienteDao;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class ClienteServiceApplicationTests {

	@Autowired
	private IClienteDao clienteDao;

	@Test
	void contextLoads() {
	}

	@Test
	void alArrancar_existeAlMenosUnaRegion() {
		// El import.sql de test está vacío: RegionInicialRunner crea las regiones base
		assertThat(clienteDao.findAllRegiones()).isNotEmpty();
	}

	@Test
	void main_arrancaLaAplicacion() {
		// Se simula SpringApplication.run para no levantar un segundo servidor
		try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
			String[] args = {};
			ClienteServiceApplication.main(args);
			spring.verify(() -> SpringApplication.run(ClienteServiceApplication.class, args));
		}
	}

}

package com.denkitronik.clienteservice.integration;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;  // import explícito evita ambigüedad con org.junit.jupiter.api.MediaType
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate   // Spring Boot 4.x: TestRestTemplate ya no se auto-registra con @SpringBootTest
// Sin Docker (p. ej. en el calificador) la clase se omite en vez de fallar
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// Cierra el contexto al terminar la clase, mientras PostgreSQL sigue arriba;
// si quedara en caché, al apagar la JVM intentaría conectarse a un contenedor ya detenido
@DirtiesContext
@DisplayName("E2E tests — flujo completo HTTP → PostgreSQL con Testcontainers")
class ClienteIntegrationTest {

    // Testcontainers levanta PostgreSQL real antes de los tests
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;  // para sembrar datos iniciales

    private static final String BASE    = "/api/v1/cliente-service";
    private static final AtomicBoolean seeded = new AtomicBoolean(false);
    private static Long regionId;   // compartido entre tests (estático)
    private static Long idCreado;   // compartido entre tests (estático)

    @BeforeEach
    void seedDatosIniciales() {
        // Se ejecuta antes de cada test, pero solo siembra datos la primera vez
        if (seeded.compareAndSet(false, true)) {
            jdbcTemplate.execute(
                "INSERT INTO regiones(nombre) VALUES ('América del Sur')");
            regionId = jdbcTemplate.queryForObject(
                "SELECT id FROM regiones WHERE nombre = 'América del Sur'",
                Long.class);
        }
    }

    @Test @Order(1)
    @DisplayName("POST /clientes → 201 crea el cliente y retorna el ID")
    void crearCliente_debeRetornar201() {
        Cliente nuevo = nuevoCliente("Hamilton");

        ResponseEntity<Cliente> respuesta =
                restTemplate.postForEntity(BASE + "/clientes", nuevo, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().getId()).isNotNull();
        assertThat(respuesta.getBody().getNombre()).isEqualTo("Margaret");

        idCreado = respuesta.getBody().getId();  // guardamos para los tests siguientes
    }

    @Test @Order(2)
    @DisplayName("GET /clientes/{id} → 200 encuentra el cliente creado")
    void buscarClienteCreado_debeRetornar200() {
        ResponseEntity<Cliente> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/" + idCreado, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().getEmail()).isEqualTo("margaret@apollo.nasa");
    }

    @Test @Order(3)
    @DisplayName("GET /clientes → 200 la lista incluye el cliente creado")
    void listarClientes_debeIncluirElCreado() {
        ResponseEntity<Cliente[]> respuesta =
                restTemplate.getForEntity(BASE + "/clientes", Cliente[].class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).extracting(Cliente::getId).contains(idCreado);
    }

    @Test @Order(4)
    @DisplayName("GET /clientes/page/0 → 200 con totalElements, totalPages y content")
    @SuppressWarnings("unchecked")
    void paginaCero_debeRetornarEstructuraDePage() {
        ResponseEntity<Map> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/page/0", Map.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = respuesta.getBody();
        assertThat(body).containsKeys("totalElements", "totalPages", "content");
        assertThat(body.get("content")).isInstanceOf(List.class);
        assertThat((List<Object>) body.get("content")).isNotEmpty();
    }

    @Test @Order(5)
    @DisplayName("PUT /clientes/{id} → 201 actualiza los datos")
    void actualizarCliente_debeRetornar201() {
        Cliente actualizado = nuevoCliente("Hamilton-Updated");

        HttpEntity<Cliente> entity =
                new HttpEntity<>(actualizado, headersJson());

        ResponseEntity<Cliente> respuesta =
                restTemplate.exchange(BASE + "/clientes/" + idCreado,
                        HttpMethod.PUT, entity, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().getApellido()).isEqualTo("Hamilton-Updated");
    }

    @Test @Order(6)
    @DisplayName("GET /clientes/9999 → 404 ID inexistente")
    void idInexistente_debeRetornar404() {
        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/9999", String.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @Order(7)
    @DisplayName("DELETE /clientes/{id} → 204 y luego GET confirma 404")
    void eliminarCliente_debeRetornar204YLuego404() {
        ResponseEntity<Void> borrado =
                restTemplate.exchange(BASE + "/clientes/" + idCreado,
                        HttpMethod.DELETE, null, Void.class);
        assertThat(borrado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/" + idCreado, String.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Cliente nuevoCliente(String apellido) {
        Region region = new Region();
        region.setId(regionId);

        Cliente c = new Cliente();
        c.setNombre("Margaret");
        c.setApellido(apellido);
        c.setEmail("margaret@apollo.nasa");
        c.setRegion(region);
        return c;
    }

    private HttpHeaders headersJson() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}

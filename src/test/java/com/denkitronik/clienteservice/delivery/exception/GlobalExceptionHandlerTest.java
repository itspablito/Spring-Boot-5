package com.denkitronik.clienteservice.delivery.exception;

import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias del GlobalExceptionHandler, sin levantar el contexto de Spring.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unused")
    private void metodoFicticio(Object body) { }

    // Objeto con un campo "nombre" para que el BindingResult acepte rejectValue
    @SuppressWarnings("unused")
    private static class Formulario {
        private String nombre;
        public String getNombre() { return nombre; }
    }

    @Test
    void notFound_devuelve404ConApiErrorResponse() {
        ResponseEntity<ApiErrorResponse> r = handler.manejarNotFound(new ClienteNotFoundException(7L));

        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertEquals(404, r.getBody().getStatus());
        assertEquals("Not Found", r.getBody().getError());
        assertEquals("Cliente con id 7 no encontrado", r.getBody().getMensaje());
        assertNotNull(r.getBody().getTimestamp());
    }

    @Test
    void serviceException_devuelve500SinExponerDetalles() {
        ClienteServiceException ex = new ClienteServiceException("detalle interno de la BD");

        ResponseEntity<ApiErrorResponse> r = handler.manejarServiceException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        assertEquals(500, r.getBody().getStatus());
        assertEquals("Internal Server Error", r.getBody().getError());
        assertFalse(r.getBody().getMensaje().contains("detalle interno"));
    }

    @Test
    void validacion_devuelveMapaCampoMensaje() throws Exception {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Formulario(), "cliente");
        result.rejectValue("nombre", "NotEmpty", "El nombre no puede estar vacío");
        result.reject("global", "Error a nivel de objeto");
        MethodParameter param = new MethodParameter(
                getClass().getDeclaredMethod("metodoFicticio", Object.class), 0);

        ResponseEntity<Map<String, String>> r =
                handler.manejarValidacion(new MethodArgumentNotValidException(param, result));

        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertEquals("El nombre no puede estar vacío", r.getBody().get("nombre"));
        assertEquals("Error a nivel de objeto", r.getBody().get("cliente"));
    }

    @Test
    void excepcionGenerica_devuelve500ConMensajeGenerico() {
        ResponseEntity<ApiErrorResponse> r =
                handler.manejarExcepcionGenerica(new RuntimeException("NullPointer en línea 42"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        assertEquals(500, r.getBody().getStatus());
        assertEquals("Error inesperado. Por favor contacta al administrador.", r.getBody().getMensaje());
    }

    @Test
    void excepcionDeSpringCon4xx_conservaSuCodigo() {
        ResponseEntity<ApiErrorResponse> r =
                handler.manejarExcepcionGenerica(new HttpRequestMethodNotSupportedException("PATCH"));

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, r.getStatusCode());
        assertEquals(405, r.getBody().getStatus());
        assertNotNull(r.getBody().getMensaje());
    }

    @Test
    void excepcionDeSpringCon4xxSinDetalle_usaElTextoDelEstado() {
        ResponseEntity<ApiErrorResponse> r =
                handler.manejarExcepcionGenerica(new ErrorResponseException(HttpStatus.CONFLICT));

        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        assertEquals("Conflict", r.getBody().getMensaje());
    }

    @Test
    void excepcionDeSpringCon5xx_seTrataComo500Generico() {
        ResponseEntity<ApiErrorResponse> r =
                handler.manejarExcepcionGenerica(new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        assertEquals("Error inesperado. Por favor contacta al administrador.", r.getBody().getMensaje());
    }

    @Test
    void excepcionDeSpringCon4xxNoEstandar_seTrataComo500Generico() {
        ResponseEntity<ApiErrorResponse> r =
                handler.manejarExcepcionGenerica(new ErrorResponseException(HttpStatusCode.valueOf(499)));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        assertEquals(500, r.getBody().getStatus());
    }
}

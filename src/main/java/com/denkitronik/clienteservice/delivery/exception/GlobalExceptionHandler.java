package com.denkitronik.clienteservice.delivery.exception;

import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 404 Not Found ─────────────────────────────────────────
    @ExceptionHandler(ClienteNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> manejarNotFound(
            ClienteNotFoundException ex) {

        log.warn("Cliente no encontrado: id={}", ex.getId());

        ApiErrorResponse respuesta = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    // ── 500 Internal Server Error ─────────────────────────────
    @ExceptionHandler(ClienteServiceException.class)
    public ResponseEntity<ApiErrorResponse> manejarServiceException(
            ClienteServiceException ex) {

        log.error("Error en el servicio de clientes", ex);

        ApiErrorResponse respuesta = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Error interno del servidor. Por favor intenta más tarde."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
    }

    // ── 400 Bad Request (validaciones @Valid) ─────────────────
    // Solo se dispara en endpoints con @Valid SIN BindingResult; los del
    // ClienteRestController usan BindingResult y construyen su propio 400.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult()
          .getAllErrors()
          .forEach(error -> {
              // Adicional al codelab: un error a nivel de clase (ObjectError)
              // no es FieldError; el cast directo lanzaría ClassCastException.
              String campo   = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
              String mensaje = error.getDefaultMessage();
              errores.put(campo, mensaje);
          });

        log.debug("Errores de validación: {}", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errores);
    }

    // ── Adicional al codelab: 400 por JSON mal formado ────────
    // Sin este handler, un body ilegible caería en Exception.class -> 500.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> manejarJsonInvalido(
            HttpMessageNotReadableException ex) {

        log.warn("Cuerpo de la petición ilegible: {}", ex.getMessage());

        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es un JSON válido");
    }

    // ── Adicional al codelab: 400 por parámetro de ruta inválido ─
    // Ej.: GET /clientes/abc  (se esperaba un Long)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> manejarTipoInvalido(
            MethodArgumentTypeMismatchException ex) {

        log.warn("Parámetro '{}' con valor inválido: {}", ex.getName(), ex.getValue());

        return construir(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido");
    }

    // ── 500 genérico para cualquier excepción no manejada ─────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> manejarExcepcionGenerica(
            Exception ex) {

        // Adicional al codelab: las excepciones propias de Spring MVC que ya
        // traen un código HTTP (404 de ruta inexistente, 405 método no
        // permitido, 415 media type...) conservan ese código en vez de 500.
        if (ex instanceof ErrorResponse errorResponse
                && errorResponse.getStatusCode().is4xxClientError()) {
            HttpStatusCode codigo = errorResponse.getStatusCode();
            HttpStatus status = HttpStatus.resolve(codigo.value());
            if (status != null) {
                String detalle = errorResponse.getBody().getDetail();
                log.warn("Petición rechazada ({}): {}", codigo.value(), ex.getMessage());
                return construir(status, detalle != null ? detalle : status.getReasonPhrase());
            }
        }

        log.error("Excepción no manejada", ex);

        ApiErrorResponse respuesta = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Error inesperado. Por favor contacta al administrador."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
    }

    private ResponseEntity<ApiErrorResponse> construir(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), status.getReasonPhrase(), mensaje));
    }
}

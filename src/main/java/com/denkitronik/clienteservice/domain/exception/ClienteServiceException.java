package com.denkitronik.clienteservice.domain.exception;

/**
 * Envuelve errores inesperados del servicio (base de datos, integridad, etc.).
 * La causa original se conserva para el log interno; al cliente HTTP
 * solo se le envía un mensaje genérico.
 */
public class ClienteServiceException extends RuntimeException {

    public ClienteServiceException(String mensaje) {
        super(mensaje);
    }

    public ClienteServiceException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

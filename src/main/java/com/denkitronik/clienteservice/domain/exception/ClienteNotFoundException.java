package com.denkitronik.clienteservice.domain.exception;

/**
 * Señal de dominio: el cliente solicitado no existe.
 * No conoce nada de HTTP; el GlobalExceptionHandler la traduce a 404.
 */
public class ClienteNotFoundException extends RuntimeException {

    private final Long id;

    public ClienteNotFoundException(Long id) {
        super("Cliente con id " + id + " no encontrado");
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}

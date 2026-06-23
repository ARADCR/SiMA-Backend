package com.sima.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando una petición tiene datos inválidos o incumple una regla de negocio.
 * Ejemplos: correo duplicado, dispositivo ya asignado, horario duplicado.
 * Retorna HTTP 400.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String mensaje) {
        super(mensaje);
    }
}
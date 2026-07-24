package com.sima.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatResponse {

    private String respuesta;
    private LocalDateTime timestamp;

    public ChatResponse(String respuesta) {
        this.respuesta = respuesta;
        this.timestamp = LocalDateTime.now();
    }
}

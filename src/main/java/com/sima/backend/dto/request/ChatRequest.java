package com.sima.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatRequest {

    @NotNull(message = "El adulto mayor es requerido")
    private Integer idAdulto;

    @NotBlank(message = "El mensaje es requerido")
    @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres")
    private String mensaje;

    @Size(max = 10, message = "El historial no puede superar los 10 mensajes")
    private List<ChatMessageDTO> historial;

    @Getter
    @Setter
    public static class ChatMessageDTO {
        private String rol; // "usuario" | "bot"
        private String texto;
    }
}

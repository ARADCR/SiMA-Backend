package com.sima.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ActualizarPerfilCuidadorRequest {
    private List<String> tags;
}

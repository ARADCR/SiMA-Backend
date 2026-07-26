package com.sima.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuscripcionTelegramResponse {
    private String id;
    private String familiar;
    private String adulto;
    private boolean habilitado;
    private String ultimaNotif;
}

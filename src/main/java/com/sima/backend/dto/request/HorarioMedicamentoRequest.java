package com.sima.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * DTO para crear o editar un horario de toma de medicamento.
 * Se usa dentro de MedicamentoRequest y también de forma independiente.
 */
@Getter
@Setter
@NoArgsConstructor
public class HorarioMedicamentoRequest {

    @NotNull(message = "La hora programada es obligatoria")
    private LocalTime horaProgramada; // Formato: HH:mm (Ej: "08:00", "14:30")
}
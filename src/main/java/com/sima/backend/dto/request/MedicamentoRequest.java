package com.sima.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para crear o editar un medicamento en la agenda del adulto mayor.
 * HU-08: Como Familiar/Cuidador, agregar o editar la agenda de medicamentos.
 * Incluye los horarios de toma para crearlos en la misma operación.
 */
@Getter
@Setter
@NoArgsConstructor
public class MedicamentoRequest {

    @NotNull(message = "El ID del adulto mayor es obligatorio")
    private Integer idAdulto;

    @NotBlank(message = "El nombre del medicamento es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @NotBlank(message = "La dosis es obligatoria")
    @Size(max = 80, message = "La dosis no puede superar 80 caracteres")
    private String dosis;

    @NotNull(message = "La frecuencia en horas es obligatoria")
    @Min(value = 1, message = "La frecuencia debe ser mayor a 0")
    @Max(value = 168, message = "La frecuencia no puede superar 168 horas (1 semana)")
    private Integer frecuenciaHoras;

    private String observaciones;

    @Size(max = 150, message = "El principio activo no puede superar 150 caracteres")
    private String principioActivo;

    private LocalDate fechaFin;

    @Min(value = 0, message = "El stock actual no puede ser negativo")
    private Integer stockActual;

    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Integer stockMinimo;

    @Size(max = 150, message = "El nombre del prescriptor no puede superar 150 caracteres")
    private String prescritoPor;

    // Horarios de toma asociados al medicamento
    // Se crean en la misma transacción que el medicamento
    @NotEmpty(message = "Debe definir al menos un horario de toma")
    @Size(max = 10, message = "No se pueden definir más de 10 horarios por medicamento")
    private List<HorarioMedicamentoRequest> horarios;
}
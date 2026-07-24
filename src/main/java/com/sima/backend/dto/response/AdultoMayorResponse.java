package com.sima.backend.dto.response;

import com.sima.backend.entity.AdultoMayor;
import com.sima.backend.entity.RelacionUsuarioAdulto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * DTO de respuesta para datos de un adulto mayor.
 * Incluye edad calculada para conveniencia del frontend.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdultoMayorResponse {

    private Integer idAdulto;
    private String nombre;
    private String apellido;
    private LocalDate fechaNacimiento;
    private Integer edad; // Calculada al momento de la consulta
    private String condicionesMedicas;
    private String contactoMedico;
    private Boolean activo;
    private LocalDateTime creadoEn;

    // Información del familiar que registró al adulto mayor
    private Integer familiarId;
    private String familiarNombre;

    // Información del cuidador y métricas
    private Integer cuidadorId;
    private String cuidadorNombre;
    private Integer medicamentosActivos;

    public static AdultoMayorResponse from(AdultoMayor a) {
        AdultoMayorResponse dto = new AdultoMayorResponse();
        dto.setIdAdulto(a.getIdAdulto());
        dto.setNombre(a.getNombre());
        dto.setApellido(a.getApellido());
        dto.setFechaNacimiento(a.getFechaNacimiento());
        dto.setCondicionesMedicas(a.getCondicionesMedicas());
        dto.setContactoMedico(a.getContactoMedico());
        dto.setActivo(a.getActivo());
        dto.setCreadoEn(a.getCreadoEn());

        // Calcular edad si tiene fecha de nacimiento
        if (a.getFechaNacimiento() != null) {
            dto.setEdad(Period.between(a.getFechaNacimiento(), LocalDate.now()).getYears());
        }

        // Extraer información de relaciones
        if (a.getRelaciones() != null) {
            a.getRelaciones().stream()
                    .filter(r -> "familiar".equalsIgnoreCase(r.getTipoRelacion()))
                    .findFirst()
                    .ifPresent(r -> {
                        dto.setFamiliarId(r.getUsuario().getIdUsuario());
                        dto.setFamiliarNombre(
                                r.getUsuario().getNombre() + " " + r.getUsuario().getApellido());
                    });

            a.getRelaciones().stream()
                    .filter(r -> "cuidador_asignado".equalsIgnoreCase(r.getTipoRelacion()) || "cuidador".equalsIgnoreCase(r.getTipoRelacion()))
                    .findFirst()
                    .ifPresent(r -> {
                        dto.setCuidadorId(r.getUsuario().getIdUsuario());
                        dto.setCuidadorNombre(
                                r.getUsuario().getNombre() + " " + r.getUsuario().getApellido());
                    });
        }

        // Contar medicamentos activos
        if (a.getMedicamentos() != null) {
            dto.setMedicamentosActivos((int) a.getMedicamentos().stream()
                    .filter(m -> m.getActivo() != null && m.getActivo())
                    .count());
        } else {
            dto.setMedicamentosActivos(0);
        }

        return dto;
    }
}
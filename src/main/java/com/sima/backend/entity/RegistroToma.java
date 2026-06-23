package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "registros_toma")
@Getter
@Setter
@NoArgsConstructor
public class RegistroToma {

    /*
     * Regla de inmutabilidad:
     * - No se permite DELETE sobre esta entidad.
     * - Se permite UPDATE únicamente de: estado, metodoConfirmacion, observacion.
     * - Los errores se corrigen con un nuevo registro correctivo.
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Integer idRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_horario", nullable = false)
    private HorarioMedicamento horario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_adulto", nullable = false)
    private AdultoMayor adulto;

    // NULL cuando el sistema o IoT confirma automáticamente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_confirmador")
    private Usuario usuarioConfirmador;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "pendiente";
    // Valores: "pendiente" | "tomado" | "omitido" | "confirmado_manual"

    @Column(name = "metodo_confirmacion", length = 40)
    private String metodoConfirmacion;
    // Valores: "app" | "chatbot" | "iot_pastillero" | "manual_cuidador"

    @Column(name = "fecha_hora_programada", nullable = false, updatable = false)
    private LocalDateTime fechaHoraProgramada;

    @Column(name = "fecha_hora_registro")
    private LocalDateTime fechaHoraRegistro;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    // Relaciones
    @OneToMany(mappedBy = "registro", fetch = FetchType.LAZY)
    private List<Alerta> alertas;
}
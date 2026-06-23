package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "horarios_medicamento", uniqueConstraints = {
        // Evita horarios duplicados para el mismo medicamento
        @UniqueConstraint(name = "uq_medicamento_hora", columnNames = { "id_medicamento", "hora_programada" })
})
@Getter
@Setter
@NoArgsConstructor
public class HorarioMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Integer idHorario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medicamento", nullable = false)
    private Medicamento medicamento;

    @Column(name = "hora_programada", nullable = false)
    private LocalTime horaProgramada;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // Relaciones
    @OneToMany(mappedBy = "horario", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<RegistroToma> registrosToma;
}
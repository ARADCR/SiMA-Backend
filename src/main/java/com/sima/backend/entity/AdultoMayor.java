package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "adultos_mayores")
@Getter
@Setter
@NoArgsConstructor
public class AdultoMayor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adulto")
    private Integer idAdulto;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "condiciones_medicas", columnDefinition = "TEXT")
    private String condicionesMedicas;

    @Column(name = "contacto_medico", length = 255)
    private String contactoMedico;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    // Relaciones
    @OneToMany(mappedBy = "adulto", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<RelacionUsuarioAdulto> relaciones;

    @OneToMany(mappedBy = "adulto", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<DispositivoIot> dispositivos;

    @OneToMany(mappedBy = "adulto", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Medicamento> medicamentos;

    @OneToMany(mappedBy = "adulto", fetch = FetchType.LAZY)
    private List<RegistroToma> registrosToma;

    @OneToMany(mappedBy = "adulto", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Alerta> alertas;

    @OneToMany(mappedBy = "adulto", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ObservacionCuidador> observaciones;
}
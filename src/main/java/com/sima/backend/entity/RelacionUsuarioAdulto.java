package com.sima.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "relacion_usuario_adulto")
@Getter
@Setter
@NoArgsConstructor
public class RelacionUsuarioAdulto {

    // ---------------------------------------------------------------
    // Clave primaria compuesta embebida
    // ---------------------------------------------------------------
    @EmbeddedId
    private RelacionUsuarioAdultoId id;

    // ---------------------------------------------------------------
    // Relaciones ManyToOne usando las columnas de la PK compuesta
    // ---------------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUsuario")
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idAdulto")
    @JoinColumn(name = "id_adulto")
    private AdultoMayor adulto;

    // ---------------------------------------------------------------
    // Campos propios de la relación
    // ---------------------------------------------------------------
    @Column(name = "tipo_relacion", nullable = false, length = 50)
    private String tipoRelacion; // "familiar" | "cuidador_asignado"

    @Column(name = "es_contacto_emergencia", nullable = false)
    private Boolean esContactoEmergencia = false;

    // ---------------------------------------------------------------
    // Clase de clave compuesta (debe ser Serializable)
    // ---------------------------------------------------------------
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RelacionUsuarioAdultoId implements Serializable {

        @Column(name = "id_usuario")
        private Integer idUsuario;

        @Column(name = "id_adulto")
        private Integer idAdulto;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof RelacionUsuarioAdultoId that))
                return false;
            return Objects.equals(idUsuario, that.idUsuario) &&
                    Objects.equals(idAdulto, that.idAdulto);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idUsuario, idAdulto);
        }
    }
}
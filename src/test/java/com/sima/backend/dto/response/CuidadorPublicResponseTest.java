package com.sima.backend.dto.response;

import com.sima.backend.entity.PerfilCuidador;
import com.sima.backend.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CuidadorPublicResponseTest {

    @Test
    void from_TarifaHoraCargada_ExponeFormatoReal() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Ana");
        usuario.setApellido("Gomez");

        PerfilCuidador perfil = new PerfilCuidador(1);
        perfil.setTarifaHora(new BigDecimal("20.00"));

        CuidadorPublicResponse response = CuidadorPublicResponse.from(usuario, perfil);

        assertEquals("$20.00/hora", response.getPrecio());
    }

    @Test
    void from_TarifaHoraNula_ExponeFallbackDummy() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Ana");
        usuario.setApellido("Gomez");

        PerfilCuidador perfil = new PerfilCuidador(1);

        CuidadorPublicResponse response = CuidadorPublicResponse.from(usuario, perfil);

        assertEquals("$15/hora", response.getPrecio());
    }
}

package com.sima.backend.service;

import com.sima.backend.dto.request.ActualizarDatosContactoCuidadorRequest;
import com.sima.backend.dto.response.DatosContactoCuidadorResponse;

public interface CuidadorPerfilService {

    DatosContactoCuidadorResponse obtenerPerfil(Integer idUsuario);

    DatosContactoCuidadorResponse actualizarPerfil(Integer idUsuario, ActualizarDatosContactoCuidadorRequest request);
}

package com.sima.backend.service;

import com.sima.backend.dto.request.CrearCredencialRequest;
import com.sima.backend.dto.response.CredencialResponse;

import java.util.List;

public interface CredencialService {
    List<CredencialResponse> obtenerCredencialesPorCuidador(Integer idCuidador);
    CredencialResponse crearCredencial(Integer idCuidador, CrearCredencialRequest request);
}

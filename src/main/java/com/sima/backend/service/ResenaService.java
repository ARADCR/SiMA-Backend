package com.sima.backend.service;

import com.sima.backend.dto.request.CrearResenaRequest;
import com.sima.backend.dto.response.ResenaResponse;

import java.util.List;

public interface ResenaService {
    ResenaResponse crearResena(Integer idFamiliar, CrearResenaRequest request);
    List<ResenaResponse> obtenerResenasCuidador(Integer idCuidador);
}

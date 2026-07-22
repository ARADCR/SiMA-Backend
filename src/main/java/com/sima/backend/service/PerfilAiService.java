package com.sima.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sima.backend.dto.request.ActualizarPerfilCuidadorRequest;
import com.sima.backend.dto.request.AnalisisPerfilRequest;
import com.sima.backend.dto.response.AnalisisPerfilResponse;
import com.sima.backend.dto.response.PerfilCuidadorResponse;
import com.sima.backend.entity.PerfilCuidador;
import com.sima.backend.exception.ResourceNotFoundException;
import com.sima.backend.repository.PerfilCuidadorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PerfilAiService {

    private static final Logger log = LoggerFactory.getLogger(PerfilAiService.class);
    private static final int MAX_TOKENS_JSON = 1200;
    private static final Pattern PRIMER_NUMERO = Pattern.compile("\\d+");

    private static final String SYSTEM_PROMPT = """
            Sos el analizador de perfiles profesionales de cuidadores de SiMA, una plataforma de monitoreo
            de adultos mayores. Recibís la descripción en texto libre que un Cuidador escribió sobre sí mismo
            y extraés información estructurada.

            Respondé ÚNICAMENTE con un JSON válido (sin markdown, sin texto adicional) con esta forma exacta:
            {
              "especialidadesDetectadas": ["string", ...],
              "experienciaEstimada": "string breve, ej. '5 años'",
              "certificacionesDetectadas": ["string", ...],
              "resumenGenerado": "string de 2 a 3 oraciones, en tono profesional, para mostrar en un perfil público",
              "advertencias": ["string", ...],
              "tagsRecomendados": ["string", ...]
            }

            En "advertencias" reportá inconsistencias evidentes en el texto (ej. años de experiencia que no
            cuadran con fechas mencionadas, certificaciones contradictorias). Si no hay inconsistencias, devolvé
            una lista vacía. No inventes certificaciones ni experiencia que no estén sugeridas por el texto.
            """;

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final PerfilCuidadorRepository perfilCuidadorRepository;

    public PerfilAiService(AiService aiService, ObjectMapper objectMapper,
            PerfilCuidadorRepository perfilCuidadorRepository) {
        this.aiService = aiService;
        this.objectMapper = objectMapper;
        this.perfilCuidadorRepository = perfilCuidadorRepository;
    }

    @Transactional
    public AnalisisPerfilResponse analizarPerfil(Integer idUsuario, AnalisisPerfilRequest request) {
        String respuestaLlm = aiService.chat(SYSTEM_PROMPT, request.getDescripcion(), MAX_TOKENS_JSON);

        AnalisisPerfilResponse response = parsear(respuestaLlm);
        if (response == null) {
            response = new AnalisisPerfilResponse();
            response.setEspecialidadesDetectadas(Collections.emptyList());
            response.setExperienciaEstimada(null);
            response.setCertificacionesDetectadas(Collections.emptyList());
            response.setResumenGenerado(null);
            response.setAdvertencias(Collections.emptyList());
            response.setTagsRecomendados(Collections.emptyList());
            guardarPerfil(idUsuario, request.getDescripcion(), response, false);
            return response;
        }

        guardarPerfil(idUsuario, request.getDescripcion(), response, true);
        return response;
    }

    @Transactional(readOnly = true)
    public PerfilCuidadorResponse obtenerPerfil(Integer idUsuario) {
        PerfilCuidador perfil = perfilCuidadorRepository.findByIdUsuario(idUsuario).orElse(null);
        return PerfilCuidadorResponse.from(perfil);
    }

    @Transactional
    public void actualizarRevision(Integer idUsuario, ActualizarPerfilCuidadorRequest request) {
        PerfilCuidador perfil = perfilCuidadorRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de cuidador", "idUsuario", idUsuario));

        perfil.setTags(join(request.getTags()));
        perfilCuidadorRepository.save(perfil);
    }

    private void guardarPerfil(Integer idUsuario, String descripcion, AnalisisPerfilResponse analisis, boolean analizado) {
        PerfilCuidador perfil = perfilCuidadorRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> new PerfilCuidador(idUsuario));

        perfil.setDescripcionPerfil(descripcion);
        perfil.setEspecialidades(join(analisis.getEspecialidadesDetectadas()));
        perfil.setExperienciaAnios(extraerAnios(analisis.getExperienciaEstimada()));
        perfil.setCertificaciones(join(analisis.getCertificacionesDetectadas()));
        perfil.setResumenIa(analisis.getResumenGenerado());
        perfil.setTags(join(analisis.getTagsRecomendados()));
        perfil.setPerfilAnalizado(analizado);

        perfilCuidadorRepository.save(perfil);
    }

    private String join(List<String> valores) {
        return valores == null ? null : String.join(", ", valores);
    }

    private Integer extraerAnios(String experienciaEstimada) {
        if (experienciaEstimada == null) return null;
        Matcher matcher = PRIMER_NUMERO.matcher(experienciaEstimada);
        return matcher.find() ? Integer.parseInt(matcher.group()) : null;
    }

    private AnalisisPerfilResponse parsear(String respuestaLlm) {
        try {
            String json = extraerJson(respuestaLlm);
            return objectMapper.readValue(json, AnalisisPerfilResponse.class);
        } catch (Exception ex) {
            log.error("PerfilAiService: no se pudo parsear la respuesta del LLM como JSON", ex);
            return null;
        }
    }

    private String extraerJson(String texto) {
        int inicio = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');
        if (inicio == -1 || fin == -1 || fin < inicio) {
            throw new IllegalArgumentException("La respuesta del LLM no contiene un JSON válido");
        }
        return texto.substring(inicio, fin + 1);
    }
}

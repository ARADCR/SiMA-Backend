package com.sima.backend.service;

import com.sima.backend.dto.response.HistorialEventoResponse;
import com.sima.backend.entity.Alerta;
import com.sima.backend.entity.EventoIot;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.AlertaRepository;
import com.sima.backend.repository.EventoIotRepository;
import com.sima.backend.repository.RegistroTomaRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistorialService {

    private final RegistroTomaRepository registroRepository;
    private final AlertaRepository alertaRepository;
    private final EventoIotRepository eventoIotRepository;
    private final RelacionUsuarioAdultoRepository relacionRepository;

    public HistorialService(RegistroTomaRepository registroRepository,
                            AlertaRepository alertaRepository,
                            EventoIotRepository eventoIotRepository,
                            RelacionUsuarioAdultoRepository relacionRepository) {
        this.registroRepository = registroRepository;
        this.alertaRepository = alertaRepository;
        this.eventoIotRepository = eventoIotRepository;
        this.relacionRepository = relacionRepository;
    }

    @Transactional(readOnly = true)
    public Page<HistorialEventoResponse> obtenerHistorial(Integer idUsuario, Integer idAdulto,
                                                          String tipoEvento, LocalDateTime fechaInicio,
                                                          LocalDateTime fechaFin, Pageable pageable) {
        
        // 1. Validar acceso
        validarAcceso(idUsuario, idAdulto);

        // 2. Definir rango de fechas (default últimos 30 días si no se especifican)
        LocalDateTime desde = fechaInicio != null ? fechaInicio : LocalDateTime.now().minusDays(30);
        LocalDateTime hasta = fechaFin != null ? fechaFin : LocalDateTime.now();

        List<HistorialEventoResponse> todosLosEventos = new ArrayList<>();

        // 3. Consultar tomas
        if (tipoEvento == null || tipoEvento.equalsIgnoreCase("toma")) {
            List<RegistroToma> tomas = registroRepository.findHistorialByAdultoAndRango(idAdulto, desde, hasta);
            for (RegistroToma t : tomas) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("medicamento", t.getHorario().getMedicamento().getNombre());
                meta.put("dosis", t.getHorario().getMedicamento().getDosis());
                meta.put("confirmador", t.getUsuarioConfirmador() != null ? t.getUsuarioConfirmador().getNombre() : "Sistema");
                
                todosLosEventos.add(HistorialEventoResponse.builder()
                        .id(t.getIdRegistro())
                        .tipo("toma")
                        .subtipo(t.getEstado())
                        .titulo("Medicamento: " + t.getHorario().getMedicamento().getNombre())
                        .descripcion("Estado: " + t.getEstado() + (t.getMetodoConfirmacion() != null ? " (vía " + t.getMetodoConfirmacion() + ")" : ""))
                        .fechaHora(t.getFechaHoraProgramada())
                        .meta(meta)
                        .build());
            }
        }

        // 4. Consultar alertas
        if (tipoEvento == null || tipoEvento.equalsIgnoreCase("alerta")) {
            List<Alerta> alertas = alertaRepository.findByAdultoAndRango(idAdulto, desde, hasta);
            for (Alerta a : alertas) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("resuelta", a.getResuelta());
                meta.put("mensaje", a.getMensaje());
                
                todosLosEventos.add(HistorialEventoResponse.builder()
                        .id(a.getIdAlerta())
                        .tipo("alerta")
                        .subtipo(a.getTipoAlerta())
                        .titulo("Alerta: " + a.getTipoAlerta().replace("_", " "))
                        .descripcion(a.getMensaje())
                        .fechaHora(a.getCreadoEn())
                        .meta(meta)
                        .build());
            }
        }

        // 5. Consultar eventos IoT
        if (tipoEvento == null || tipoEvento.equalsIgnoreCase("actividad_iot")) {
            List<EventoIot> eventosIot = eventoIotRepository.findByAdultoAndRango(idAdulto, desde, hasta);
            for (EventoIot e : eventosIot) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("tipoDispositivo", e.getDispositivo().getTipoDispositivo());
                meta.put("valor", e.getValor());
                
                String valorStr = e.getValor() != null ? " (" + e.getValor() + ")" : "";
                
                todosLosEventos.add(HistorialEventoResponse.builder()
                        .id(e.getIdEvento())
                        .tipo("actividad_iot")
                        .subtipo(e.getTipoEvento())
                        .titulo("Dispositivo IoT: " + e.getTipoEvento().replace("_", " "))
                        .descripcion("Evento registrado por " + e.getDispositivo().getIdentificadorFisico() + valorStr)
                        .fechaHora(e.getFechaHora())
                        .meta(meta)
                        .build());
            }
        }

        // 6. Ordenar por fechaHora descendente
        todosLosEventos.sort((e1, e2) -> e2.getFechaHora().compareTo(e1.getFechaHora()));

        // 7. Paginar en memoria
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), todosLosEventos.size());
        
        List<HistorialEventoResponse> subList = new ArrayList<>();
        if (start < todosLosEventos.size()) {
            subList = todosLosEventos.subList(start, end);
        }

        return new PageImpl<>(subList, pageable, todosLosEventos.size());
    }

    private void validarAcceso(Integer idUsuario, Integer idAdulto) {
        // Validación basada en la tabla pivote de relaciones
        if (relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            return;
        }

        throw new UnauthorizedException("No tienes acceso al historial de salud del adulto con id: " + idAdulto);
    }
}

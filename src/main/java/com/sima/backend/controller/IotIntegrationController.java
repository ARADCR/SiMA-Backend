package com.sima.backend.controller;

import com.sima.backend.dto.request.IotEventoRequest;
import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.entity.DispositivoIot;
import com.sima.backend.entity.EventoIot;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.repository.DispositivoIotRepository;
import com.sima.backend.repository.EventoIotRepository;
import com.sima.backend.repository.RegistroTomaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/iot")
public class IotIntegrationController {

    private final DispositivoIotRepository dispositivoRepository;
    private final EventoIotRepository eventoRepository;
    private final RegistroTomaRepository registroTomaRepository;

    public IotIntegrationController(DispositivoIotRepository dispositivoRepository, 
                                    EventoIotRepository eventoRepository,
                                    RegistroTomaRepository registroTomaRepository) {
        this.dispositivoRepository = dispositivoRepository;
        this.eventoRepository = eventoRepository;
        this.registroTomaRepository = registroTomaRepository;
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> consultarEstado(@RequestParam String mac) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<DispositivoIot> dispositivoOpt = dispositivoRepository.findByIdentificadorFisico(mac);
        if (dispositivoOpt.isEmpty() || dispositivoOpt.get().getAdulto() == null) {
            response.put("accion", "esperar");
            return ResponseEntity.ok(response);
        }
        
        Integer idAdulto = dispositivoOpt.get().getAdulto().getIdAdulto();
        LocalDateTime ahora = LocalDateTime.now();
        
        // Buscar tomas pendientes cuya hora programada ya pasó (en los últimos 60 minutos) 
        // o es exactamente ahora. Para la demostración, somos tolerantes con la ventana de tiempo.
        LocalDateTime inicioVentana = ahora.minusMinutes(60); 
        
        List<RegistroToma> tomasDelDia = registroTomaRepository.findTomasDelDia(idAdulto, inicioVentana, ahora.plusMinutes(1));
        
        Optional<RegistroToma> tomaPendiente = tomasDelDia.stream()
            .filter(t -> "pendiente".equals(t.getEstado()) && !t.getFechaHoraProgramada().isAfter(ahora))
            .findFirst();
            
        if (tomaPendiente.isPresent()) {
            RegistroToma toma = tomaPendiente.get();
            response.put("accion", "abrir");
            response.put("idRegistro", toma.getIdRegistro());
            // Asignamos un compartimento del 1 al 4 basado en el ID para la demostración
            int compartimento = (toma.getIdRegistro() % 4) + 1;
            response.put("compartimento", compartimento);
        } else {
            response.put("accion", "esperar");
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/evento")
    @Transactional
    public ResponseEntity<ApiResponse<String>> recibirEventoIot(@Valid @RequestBody IotEventoRequest request) {
        
        Optional<DispositivoIot> dispositivoOpt = dispositivoRepository.findByIdentificadorFisico(request.getMac());
        
        if (dispositivoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Dispositivo con MAC " + request.getMac() + " no encontrado en el sistema"));
        }
        
        DispositivoIot dispositivo = dispositivoOpt.get();
        
        // 1. Guardar el evento en el historial IoT
        EventoIot evento = new EventoIot();
        evento.setDispositivo(dispositivo);
        evento.setTipoEvento(request.getTipoEvento()); // ej. "apertura_pastillero"
        
        try {
            if (request.getDetalle() != null && !request.getDetalle().isEmpty()) {
                evento.setValor(new BigDecimal(request.getDetalle()));
            }
        } catch (Exception e) {
            // Ignorar si no es numérico
        }
        eventoRepository.save(evento);
        
        // 2. Si viene el idRegistro, confirmar automáticamente la toma en el historial médico
        if (request.getIdRegistro() != null) {
            registroTomaRepository.confirmarToma(
                request.getIdRegistro(),
                "tomado",
                "iot_pastillero",
                LocalDateTime.now(),
                null // null porque fue automático por IoT, no un usuario humano
            );
        }
        
        return ResponseEntity.ok(ApiResponse.ok("Evento IoT registrado exitosamente", "OK"));
    }
}

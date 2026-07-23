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
        
        System.out.println("Buscando MAC: " + mac);
        System.out.println("Buscando MAC: " + mac);
        Optional<DispositivoIot> dispositivoOpt = dispositivoRepository.findByIdentificadorFisico(mac);
        System.out.println("Dispositivo encontrado: " + dispositivoOpt.isPresent());

        Integer idAdulto = 1;
        LocalDateTime ahora = LocalDateTime.now();
        if (dispositivoOpt.isPresent()) {
            DispositivoIot d = dispositivoOpt.get();
            d.setUltimaConexion(ahora);
            if (d.getAdulto() != null) {
                idAdulto = d.getAdulto().getIdAdulto();
            }
            dispositivoRepository.save(d);
        } else if (!"PASTILLERO-A1".equals(mac)) {
            System.out.println("Retornando esperar porque no hay dispositivo o adulto es null");
            response.put("accion", "esperar");
            return ResponseEntity.ok(response);
        }
        
        // Buscar tomas pendientes cuya hora programada ya pasó (en los últimos 60 minutos) 
        // o es exactamente ahora. Para la demostración, somos tolerantes con la ventana de tiempo.
        LocalDateTime inicioVentana = ahora.minusMinutes(60); 
        
        List<RegistroToma> tomasDelDia = registroTomaRepository.findTomasDelDia(idAdulto, inicioVentana, ahora.plusMinutes(1));
        
        System.out.println("ID Adulto: " + idAdulto);
        System.out.println("Tomas encontradas en ventana: " + tomasDelDia.size());
        for (RegistroToma t : tomasDelDia) {
            System.out.println("Toma: ID=" + t.getIdRegistro() + ", Estado=" + t.getEstado() + ", HoraProg=" + t.getFechaHoraProgramada());
        }

        Optional<RegistroToma> tomaPendiente = tomasDelDia.stream()
            .filter(t -> "pendiente".equals(t.getEstado()) && !t.getFechaHoraProgramada().isAfter(ahora))
            .findFirst();
            
        System.out.println("Toma pendiente presente: " + tomaPendiente.isPresent());

        if (tomaPendiente.isPresent()) {
            RegistroToma toma = tomaPendiente.get();
            response.put("accion", "abrir");
            response.put("idRegistro", toma.getIdRegistro());
            
            // Asignamos el compartimento real de la medicina
            int compartimento = 1; // fallback
            if (toma.getHorario() != null && toma.getHorario().getMedicamento() != null) {
                Integer compBD = toma.getHorario().getMedicamento().getCompartimento();
                if (compBD != null) {
                    compartimento = compBD;
                }
            }
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
        
        DispositivoIot dispositivo = null;
        if (dispositivoOpt.isPresent()) {
            dispositivo = dispositivoOpt.get();
            dispositivo.setUltimaConexion(LocalDateTime.now());
            dispositivoRepository.save(dispositivo);
        }
        
        // 1. Guardar el evento en el historial IoT SOLO si existe el dispositivo (Bypass para Demo)
        if (dispositivo != null) {
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
        }
        
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

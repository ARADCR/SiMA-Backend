package com.sima.backend.service;

import com.sima.backend.entity.DispositivoIot;
import com.sima.backend.entity.RelacionUsuarioAdulto;
import com.sima.backend.repository.DispositivoIotRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Job batch (HU-26) que ejecuta el análisis IA de anomalías IoT dos veces al
 * día (8AM y 8PM) para cada adulto mayor con al menos un dispositivo IoT
 * activo, en vez de esperar a que el usuario lo pida manualmente desde la UI.
 * Los resultados quedan persistidos en analisis_iot_ia (ver IotAiService).
 */
@Component
public class IotAnalisisScheduler {

    private static final Logger log = LoggerFactory.getLogger(IotAnalisisScheduler.class);

    private final DispositivoIotRepository dispositivoIotRepository;
    private final RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository;
    private final IotAiService iotAiService;

    public IotAnalisisScheduler(DispositivoIotRepository dispositivoIotRepository,
            RelacionUsuarioAdultoRepository relacionUsuarioAdultoRepository,
            IotAiService iotAiService) {
        this.dispositivoIotRepository = dispositivoIotRepository;
        this.relacionUsuarioAdultoRepository = relacionUsuarioAdultoRepository;
        this.iotAiService = iotAiService;
    }

    // Se ejecuta a las 8AM y 8PM todos los días
    @Scheduled(cron = "0 0 8,20 * * *")
    public void ejecutarAnalisisBatch() {
        List<DispositivoIot> dispositivosActivos = dispositivoIotRepository.findByActivoTrue();

        dispositivosActivos.stream()
                .map(DispositivoIot::getAdulto)
                .filter(adulto -> adulto != null)
                .map(adulto -> adulto.getIdAdulto())
                .distinct()
                .forEach(idAdulto -> {
                    try {
                        Optional<Integer> idUsuario = primerUsuarioVinculado(idAdulto);
                        if (idUsuario.isEmpty()) {
                            log.warn("IotAnalisisScheduler: adulto {} tiene dispositivo IoT pero ningún usuario vinculado, se omite.",
                                    idAdulto);
                            return;
                        }
                        iotAiService.analizar(idUsuario.get(), idAdulto);
                        log.info("IotAnalisisScheduler: análisis batch ejecutado para el adulto {}", idAdulto);
                    } catch (Exception ex) {
                        log.error("IotAnalisisScheduler: error al analizar el adulto {}", idAdulto, ex);
                    }
                });
    }

    private Optional<Integer> primerUsuarioVinculado(Integer idAdulto) {
        List<RelacionUsuarioAdulto> relaciones = relacionUsuarioAdultoRepository
                .findByAdulto_IdAdultoAndTipoRelacion(idAdulto, "familiar");
        if (relaciones.isEmpty()) {
            relaciones = relacionUsuarioAdultoRepository
                    .findByAdulto_IdAdultoAndTipoRelacion(idAdulto, "cuidador_asignado");
        }
        return relaciones.stream().findFirst().map(r -> r.getUsuario().getIdUsuario());
    }
}

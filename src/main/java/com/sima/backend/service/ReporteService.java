package com.sima.backend.service;

import com.sima.backend.dto.response.ReporteMedicionSemanalResponse;
import com.sima.backend.dto.response.ReporteMedicionSemanalResponse.DetalleDiario;
import com.sima.backend.dto.response.ReporteMedicionSemanalResponse.MedicamentoOmitido;
import com.sima.backend.entity.RegistroToma;
import com.sima.backend.exception.UnauthorizedException;
import com.sima.backend.repository.RegistroTomaRepository;
import com.sima.backend.repository.RelacionUsuarioAdultoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ReporteService {

    private final RegistroTomaRepository registroRepository;
    private final RelacionUsuarioAdultoRepository relacionRepository;

    public ReporteService(RegistroTomaRepository registroRepository,
                          RelacionUsuarioAdultoRepository relacionRepository) {
        this.registroRepository = registroRepository;
        this.relacionRepository = relacionRepository;
    }

    @Transactional(readOnly = true)
    public ReporteMedicionSemanalResponse generarReporteSemanal(Integer idUsuario, Integer idAdulto) {
        // 1. Validar acceso del usuario autenticado sobre el adulto
        if (!relacionRepository.validarAccesoUsuarioAdulto(idUsuario, idAdulto)) {
            throw new UnauthorizedException(
                    "No tienes acceso al reporte del adulto con id: " + idAdulto);
        }

        // 2. Rango: últimos 7 días completos hasta ahora
        LocalDateTime hasta = LocalDateTime.now();
        LocalDateTime desde = hasta.minusDays(7).toLocalDate().atStartOfDay();

        // 3. Recuperar todos los registros del adulto en el rango
        List<RegistroToma> registros = registroRepository.findHistorialByAdultoAndRango(idAdulto, desde, hasta);

        // 4. Separar en totales globales
        int totalProgramadas = 0;
        int totalTomadas = 0;
        int totalOmitidas = 0;

        // Acumuladores por día (TreeMap para ordenamiento cronológico)
        Map<LocalDate, int[]> porDia = new TreeMap<>();

        // Acumuladores de omisiones por medicamento
        Map<String, Integer> omisionesPorMedicamento = new LinkedHashMap<>();

        for (RegistroToma rt : registros) {
            // Solo contamos tomas cuya fecha programada ya pasó (evitar que pendientes
            // futuros del día de hoy distorsionen el índice de adherencia)
            if (rt.getFechaHoraProgramada().isAfter(hasta)) {
                continue;
            }

            totalProgramadas++;
            LocalDate dia = rt.getFechaHoraProgramada().toLocalDate();

            // int[0] = programadas del día, int[1] = tomadas del día
            int[] diaCount = porDia.computeIfAbsent(dia, k -> new int[]{0, 0});
            diaCount[0]++;

            String estadoNormalizado = rt.getEstado() == null ? "pendiente" : rt.getEstado();

            if ("tomado".equals(estadoNormalizado) || "confirmado_manual".equals(estadoNormalizado)) {
                totalTomadas++;
                diaCount[1]++;
            } else if ("omitido".equals(estadoNormalizado)) {
                totalOmitidas++;
                // Contabilizar omisión en el medicamento correspondiente
                String nombreMed = rt.getHorario().getMedicamento().getNombre();
                omisionesPorMedicamento.merge(nombreMed, 1, Integer::sum);
            } else {
                // "pendiente" ya vencido → cuenta como omitido para el reporte
                totalOmitidas++;
                String nombreMed = rt.getHorario().getMedicamento().getNombre();
                omisionesPorMedicamento.merge(nombreMed, 1, Integer::sum);
            }
        }

        // 5. Construir desglose diario (incluye los 7 días aunque no haya registros)
        List<DetalleDiario> desgloseDiario = new ArrayList<>();
        LocalDate diaIter = desde.toLocalDate();
        while (!diaIter.isAfter(hasta.toLocalDate())) {
            int[] cnt = porDia.getOrDefault(diaIter, new int[]{0, 0});
            desgloseDiario.add(DetalleDiario.builder()
                    .fecha(diaIter.toString())
                    .totalProgramadas(cnt[0])
                    .totalTomadas(cnt[1])
                    .build());
            diaIter = diaIter.plusDays(1);
        }

        // 6. Construir lista de medicamentos más omitidos (orden descendente)
        List<MedicamentoOmitido> medicamentosMasOmitidos = omisionesPorMedicamento.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(e -> MedicamentoOmitido.builder()
                        .nombre(e.getKey())
                        .cantidadOmisiones(e.getValue())
                        .build())
                .toList();

        // 7. Calcular porcentaje (evitar división por cero)
        double porcentajeAdherencia = totalProgramadas > 0
                ? (totalTomadas * 100.0) / totalProgramadas
                : 0.0;

        return ReporteMedicionSemanalResponse.builder()
                .porcentajeAdherencia(Math.round(porcentajeAdherencia * 10.0) / 10.0)
                .totalProgramadas(totalProgramadas)
                .totalTomadas(totalTomadas)
                .totalOmitidas(totalOmitidas)
                .desgloseDiario(desgloseDiario)
                .medicamentosMasOmitidos(medicamentosMasOmitidos)
                .build();
    }
}

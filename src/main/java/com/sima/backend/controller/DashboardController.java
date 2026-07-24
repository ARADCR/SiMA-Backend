package com.sima.backend.controller;

import com.sima.backend.dto.response.*;
import com.sima.backend.entity.DispositivoIot;
import com.sima.backend.entity.SolicitudVinculacion;
import com.sima.backend.entity.Usuario;
import com.sima.backend.entity.ReporteGenerado;
import com.sima.backend.entity.Alerta;
import com.sima.backend.repository.*;
import com.sima.backend.service.PdfGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import jakarta.validation.Valid;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdultoMayorRepository adultoMayorRepository;

    @Autowired
    private DispositivoIotRepository dispositivoIotRepository;

    @Autowired
    private AlertaRepository alertaRepository;

    @Autowired
    private SolicitudVinculacionRepository solicitudVinculacionRepository;

    @Autowired
    private ReporteGeneradoRepository reporteGeneradoRepository;

    @Autowired
    private RegistroTomaRepository registroTomaRepository;

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    @GetMapping("/admin")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard() {
        AdminDashboardResponse response = new AdminDashboardResponse();

        // 1. Stats
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setTotalUsuarios(usuarioRepository.count());
        stats.setAdultosActivos(adultoMayorRepository.count());
        stats.setDispositivosConectados(dispositivoIotRepository.count());
        stats.setAlertasActivas(alertaRepository.findByResueltaFalseOrderByCreadoEnDesc().size());
        response.setStats(stats);

        // 2. Usuarios recientes
        List<Usuario> recentUsers = usuarioRepository.findTop5ByOrderByCreadoEnDesc();
        List<DashboardUserDto> userDtos = recentUsers.stream().map(u -> {
            DashboardUserDto dto = new DashboardUserDto();
            dto.setId(u.getIdUsuario());
            
            String init1 = u.getNombre() != null && !u.getNombre().isEmpty() ? u.getNombre().substring(0, 1) : "";
            String init2 = u.getApellido() != null && !u.getApellido().isEmpty() ? u.getApellido().substring(0, 1) : "";
            dto.setInitials((init1 + init2).toUpperCase());
            
            dto.setName(u.getNombre() + " " + u.getApellido());
            dto.setEmail(u.getCorreo());
            
            String rolName = u.getRol() != null ? u.getRol().getNombreRol() : "usuario";
            dto.setRole(rolName.substring(0, 1).toUpperCase() + rolName.substring(1).toLowerCase());
            
            if (rolName.equalsIgnoreCase("familiar")) {
                dto.setRoleBg("#EBF5FB");
                dto.setRoleColor("#1E5F7A");
                dto.setAvatarBg("#2E86AB");
            } else if (rolName.equalsIgnoreCase("cuidador")) {
                dto.setRoleBg("#D8F3DC");
                dto.setRoleColor("#1A7A4A");
                dto.setAvatarBg("#52B788");
            } else {
                dto.setRoleBg("#FCE4EC");
                dto.setRoleColor("#880E4F");
                dto.setAvatarBg("#6C63FF");
            }

            if (u.getUltimoAcceso() != null) {
                dto.setLastAccess(u.getUltimoAcceso().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            } else {
                dto.setLastAccess("Nunca");
            }

            return dto;
        }).collect(Collectors.toList());
        response.setUsuarios(userDtos);

        // 3. Pending Credentials
        List<SolicitudVinculacion> pendingReqs = solicitudVinculacionRepository.findTop5ByEstadoOrderByFechaCreacionDesc("pendiente");
        List<PendingCredentialDto> pendingDtos = pendingReqs.stream().map(s -> {
            PendingCredentialDto dto = new PendingCredentialDto();
            dto.setId(s.getIdSolicitud());
            Usuario c = s.getCuidador();
            
            String init1 = c.getNombre() != null && !c.getNombre().isEmpty() ? c.getNombre().substring(0, 1) : "";
            String init2 = c.getApellido() != null && !c.getApellido().isEmpty() ? c.getApellido().substring(0, 1) : "";
            dto.setInitials((init1 + init2).toUpperCase());
            
            dto.setName(c.getNombre() + " " + c.getApellido());
            dto.setDocType("Revisión de perfil");
            dto.setDate(s.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            return dto;
        }).collect(Collectors.toList());
        response.setPendingCredentials(pendingDtos);

        // 4. Unassigned Devices
        List<DispositivoIot> unassigned = dispositivoIotRepository.findByAdultoIsNullAndActivoTrue();
        List<UnassignedDeviceDto> deviceDtos = unassigned.stream().limit(5).map(d -> {
            UnassignedDeviceDto dto = new UnassignedDeviceDto();
            dto.setId(d.getIdDispositivo());
            String typeName = d.getTipoDispositivo().replace("_", " ");
            dto.setType(typeName.substring(0, 1).toUpperCase() + typeName.substring(1).toLowerCase());
            dto.setMac(d.getIdentificadorFisico());
            return dto;
        }).collect(Collectors.toList());
        response.setUnassignedDevices(deviceDtos);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reportes")
    public ResponseEntity<ReportesDashboardResponse> getReportesDashboard() {
        ReportesDashboardResponse response = new ReportesDashboardResponse();

        // 1. Cumplimiento promedio
        long totalProgramadas = registroTomaRepository.countTotalProgramadas();
        long totalTomadas = registroTomaRepository.countTotalTomadas();
        int cumplimiento = totalProgramadas > 0 ? (int) ((totalTomadas * 100) / totalProgramadas) : 0;
        response.setCumplimientoPromedio(cumplimiento);

        // 2. Tomas registradas (Esta semana)
        LocalDateTime startOfWeek = LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).withHour(0).withMinute(0).withSecond(0);
        response.setTomasRegistradasSemana(registroTomaRepository.countTomasProgramadasDesde(startOfWeek));

        // 3. Alertas generadas y activas
        response.setAlertasGeneradas(alertaRepository.count());
        response.setAlertasActivas(alertaRepository.findByResueltaFalseOrderByCreadoEnDesc().size());

        // 4. Cuidadores activos
        response.setCuidadoresActivos(usuarioRepository.countByRol_NombreRolAndActivoTrue("Cuidador"));

        // 5. Historial de reportes
        List<ReporteGenerado> reportes = reporteGeneradoRepository.findTop10ByOrderByFechaGeneracionDesc();
        List<ReporteDto> historial = new java.util.ArrayList<>();
        for (ReporteGenerado r : reportes) {
            ReporteDto dto = new ReporteDto();
            dto.setId(r.getIdReporte());
            dto.setNombre(r.getNombre());
            dto.setTipo(r.getTipo());
            dto.setFecha(r.getFechaGeneracion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dto.setGeneradoPor(r.getGeneradoPor());
            dto.setEstado(r.getEstado());
            historial.add(dto);
        }
        response.setHistorial(historial);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reportes")
    public ResponseEntity<ApiResponse<Void>> generarReporte(@Valid @RequestBody com.sima.backend.dto.request.ReporteCreateRequest req) {
        ReporteGenerado reporte = new ReporteGenerado();
        reporte.setNombre(req.getNombre());
        reporte.setTipo(req.getTipo());
        reporte.setTipoReporte(req.getTipoReporte());
        reporte.setAdultoMayorId(req.getAdultoMayorId());
        
        // Fetch adult name if specified
        if (req.getAdultoMayorId() != null) {
            adultoMayorRepository.findById(req.getAdultoMayorId()).ifPresent(adulto -> {
                reporte.setAdultoMayorNombre(adulto.getNombre() + " " + adulto.getApellido());
            });
        } else {
            reporte.setAdultoMayorNombre("Todos");
        }

        // Parse date range
        LocalDateTime inicio = null;
        LocalDateTime fin = null;
        if ("Semanal".equalsIgnoreCase(req.getTipo())) {
            fin = LocalDateTime.now();
            inicio = fin.minusDays(7).toLocalDate().atStartOfDay();
        } else if ("Mensual".equalsIgnoreCase(req.getTipo())) {
            fin = LocalDateTime.now();
            inicio = fin.minusDays(30).toLocalDate().atStartOfDay();
        } else if ("Trimestral".equalsIgnoreCase(req.getTipo())) {
            fin = LocalDateTime.now();
            inicio = fin.minusDays(90).toLocalDate().atStartOfDay();
        } else if ("Personalizado".equalsIgnoreCase(req.getTipo())) {
            if (req.getFechaInicio() != null && !req.getFechaInicio().isEmpty()) {
                inicio = java.time.LocalDate.parse(req.getFechaInicio().substring(0, 10)).atStartOfDay();
            }
            if (req.getFechaFin() != null && !req.getFechaFin().isEmpty()) {
                fin = java.time.LocalDate.parse(req.getFechaFin().substring(0, 10)).atTime(23, 59, 59);
            }
        }
        reporte.setFechaInicio(inicio);
        reporte.setFechaFin(fin);

        reporte.setEstado("Completado");
        reporte.setGeneradoPor("Administrador");
        reporteGeneradoRepository.save(reporte);
        
        return ResponseEntity.ok(ApiResponse.ok("Reporte generado exitosamente", null));
    }

    @GetMapping("/reportes/{id}/download")
    public ResponseEntity<byte[]> descargarReporte(@PathVariable Integer id) {
        java.util.Optional<ReporteGenerado> op = reporteGeneradoRepository.findById(id);
        if (op.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ReporteGenerado reporte = op.get();

        // Calculate actual date ranges for querying logs
        LocalDateTime inicio = reporte.getFechaInicio();
        LocalDateTime fin = reporte.getFechaFin();
        if (inicio == null) {
            // Default range fallback if not saved
            fin = LocalDateTime.now();
            inicio = fin.minusDays(30);
        }

        // Fetch logs based on parameters
        List<com.sima.backend.entity.RegistroToma> tomas;
        List<Alerta> alertas;

        if (reporte.getAdultoMayorId() != null) {
            tomas = registroTomaRepository.findHistorialByAdultoAndRango(reporte.getAdultoMayorId(), inicio, fin);
            alertas = alertaRepository.findByAdultoAndRango(reporte.getAdultoMayorId(), inicio, fin);
        } else {
            // General query - all patients
            tomas = registroTomaRepository.findHistorialGeneralAndRango(inicio, fin);
            alertas = alertaRepository.findGeneralAndRango(inicio, fin);
        }

        byte[] pdfBytes = pdfGeneratorService.generateReportPdf(reporte, tomas, alertas);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = "reporte_" + id + ".pdf";
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @DeleteMapping("/reportes/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarReporte(@PathVariable Integer id) {
        if (!reporteGeneradoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        reporteGeneradoRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Reporte eliminado exitosamente", null));
    }
}

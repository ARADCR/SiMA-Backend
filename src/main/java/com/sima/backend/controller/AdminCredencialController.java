package com.sima.backend.controller;

import com.sima.backend.dto.response.ApiResponse;
import com.sima.backend.dto.response.CredencialAdminResponse;
import com.sima.backend.entity.CredencialCuidador;
import com.sima.backend.entity.Usuario;
import com.sima.backend.repository.CredencialCuidadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/credenciales")
@RequiredArgsConstructor
public class AdminCredencialController {

    private final CredencialCuidadorRepository credencialRepository;

    @GetMapping
    @PreAuthorize("hasRole('Administrador')")
    public ApiResponse<List<CredencialAdminResponse>> obtenerTodasLasCredenciales() {
        List<CredencialCuidador> credenciales = credencialRepository.findAllByOrderByFechaSubidaDesc();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        List<CredencialAdminResponse> respuestas = credenciales.stream().map(c -> {
            Usuario u = c.getCuidador();
            String nombreCuidador = u.getNombre() + " " + u.getApellido();
            String iniciales = obtenerIniciales(nombreCuidador);

            return CredencialAdminResponse.builder()
                    .id(c.getIdCredencial())
                    .cuidador(nombreCuidador)
                    .initials(iniciales)
                    .avatarBg("#2E86AB") // Default for now
                    .tipo(c.getTipo())
                    .documento(c.getNombre())
                    .fecha(c.getFechaSubida().format(formatter))
                    .estado(c.getEstado())
                    .archivoUrl(c.getArchivoUrl())
                    .build();
        }).collect(Collectors.toList());

        return ApiResponse.ok("Credenciales obtenidas exitosamente", respuestas);
    }

    @PostMapping("/{id}/estado")
    @PreAuthorize("hasRole('Administrador')")
    public ApiResponse<CredencialAdminResponse> cambiarEstadoCredencial(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        String nuevoEstado = request.get("estado");
        CredencialCuidador credencial = credencialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credencial no encontrada"));
        
        credencial.setEstado(nuevoEstado);
        credencialRepository.save(credencial);
        
        return ApiResponse.ok("Estado actualizado", null);
    }

    private String obtenerIniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "C";
        String[] parts = nombre.trim().split(" ");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }
}

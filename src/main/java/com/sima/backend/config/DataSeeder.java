package com.sima.backend.config;

import com.sima.backend.entity.Rol;
import com.sima.backend.entity.Usuario;
import com.sima.backend.repository.RolRepository;
import com.sima.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;



import com.sima.backend.repository.DispositivoIotRepository;
import com.sima.backend.entity.DispositivoIot;
import com.sima.backend.repository.AdultoMayorRepository;
import com.sima.backend.entity.AdultoMayor;

/**
 * DataSeeder: inserta datos iniciales en la base de datos al arrancar la app.
 * Crea los roles del sistema y los usuarios de prueba por defecto.
 * El rol 'Adulto Mayor' se mantiene en la tabla roles por consistencia del modelo.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    public CommandLineRunner seedDatabase(RolRepository rolRepository,
                                          UsuarioRepository usuarioRepository,
                                          PasswordEncoder passwordEncoder,
                                          JdbcTemplate jdbcTemplate,
                                          DispositivoIotRepository dispositivoRepository,
                                          AdultoMayorRepository adultoRepository) {
        return args -> {

            // ── 1. Crear roles si no existen ──────────────────────────────────
            seedRol(rolRepository, "Administrador", "Administrador del sistema SiMA");
            seedRol(rolRepository, "Familiar",      "Familiar del adulto mayor");
            seedRol(rolRepository, "Cuidador",      "Cuidador profesional");
            seedRol(rolRepository, "Adulto Mayor",  "Adulto mayor monitoreado");

            // ── 2. Crear los 3 usuarios de prueba (misma contraseña: Admin1234) ──────────────────
            String passwordHash = passwordEncoder.encode("Admin1234");

            seedUsuario(usuarioRepository, rolRepository, passwordHash,
                    "Admin",    "SiMA",     "admin@sima.com",    "Administrador");
            seedUsuario(usuarioRepository, rolRepository, passwordHash,
                    "Familiar", "SiMA",     "familiar@sima.com", "Familiar");
            seedUsuario(usuarioRepository, rolRepository, passwordHash,
                    "Cuidador", "SiMA",     "cuidador@sima.com", "Cuidador");

            // ── 3. Insertar datos de prueba para Adulto, Relaciones, y Medicamentos ────────
            try {
                // Adultos (Solo inserta si no existen. ID 1 y 2 para emparejar con frontend)
                jdbcTemplate.execute("INSERT IGNORE INTO adultos_mayores (id_adulto, nombre, apellido, activo, creado_en) VALUES (1, 'Elena', 'Rodríguez', 1, NOW())");
                jdbcTemplate.execute("INSERT IGNORE INTO adultos_mayores (id_adulto, nombre, apellido, activo, creado_en) VALUES (2, 'José', 'Rodríguez', 1, NOW())");

                // Dispositivo IoT Pastillero
                if (dispositivoRepository.findByIdentificadorFisico("PASTILLERO-A1").isEmpty()) {
                    AdultoMayor adulto = adultoRepository.findById(1).orElse(null);
                    if (adulto != null) {
                        DispositivoIot dispositivo = new DispositivoIot();
                        dispositivo.setAdulto(adulto);
                        dispositivo.setTipoDispositivo("pastillero");
                        dispositivo.setIdentificadorFisico("PASTILLERO-A1");
                        dispositivo.setActivo(true);
                        dispositivoRepository.save(dispositivo);
                        log.info("✅ Dispositivo PASTILLERO-A1 creado para adulto 1");
                    }
                }

                Integer idFamiliar = usuarioRepository.findByCorreo("familiar@sima.com").map(Usuario::getIdUsuario).orElse(0);
                Integer idCuidador = usuarioRepository.findByCorreo("cuidador@sima.com").map(Usuario::getIdUsuario).orElse(0);

                if (idFamiliar > 0) {
                    jdbcTemplate.execute("INSERT IGNORE INTO relacion_usuario_adulto (id_usuario, id_adulto, tipo_relacion, es_contacto_emergencia) VALUES (" + idFamiliar + ", 1, 'familiar', 1)");
                    jdbcTemplate.execute("INSERT IGNORE INTO relacion_usuario_adulto (id_usuario, id_adulto, tipo_relacion, es_contacto_emergencia) VALUES (" + idFamiliar + ", 2, 'familiar', 1)");
                }
                if (idCuidador > 0) {
                    jdbcTemplate.execute("INSERT IGNORE INTO relacion_usuario_adulto (id_usuario, id_adulto, tipo_relacion, es_contacto_emergencia) VALUES (" + idCuidador + ", 1, 'cuidador_asignado', 0)");
                    jdbcTemplate.execute("INSERT IGNORE INTO relacion_usuario_adulto (id_usuario, id_adulto, tipo_relacion, es_contacto_emergencia) VALUES (" + idCuidador + ", 2, 'cuidador_asignado', 0)");
                }

                // Medicamentos
                jdbcTemplate.execute("INSERT IGNORE INTO medicamentos (id_medicamento, id_adulto, nombre, dosis, frecuencia_horas, creado_en) VALUES (1, 1, 'Omeprazol', '20mg', 24, NOW())");
                jdbcTemplate.execute("INSERT IGNORE INTO medicamentos (id_medicamento, id_adulto, nombre, dosis, frecuencia_horas, creado_en) VALUES (2, 2, 'Losartán', '50mg', 12, NOW())");
                jdbcTemplate.execute("INSERT IGNORE INTO medicamentos (id_medicamento, id_adulto, nombre, dosis, frecuencia_horas, creado_en) VALUES (3, 1, 'Paracetamol', '500mg', 8, NOW())");

                // Horarios
                jdbcTemplate.execute("INSERT IGNORE INTO horarios_medicamento (id_horario, id_medicamento, hora_programada, activo) VALUES (1, 1, '08:00:00', 1)");
                jdbcTemplate.execute("INSERT IGNORE INTO horarios_medicamento (id_horario, id_medicamento, hora_programada, activo) VALUES (2, 2, '09:00:00', 1)");
                jdbcTemplate.execute("INSERT IGNORE INTO horarios_medicamento (id_horario, id_medicamento, hora_programada, activo) VALUES (3, 3, '14:00:00', 1)");

                // Registros Toma (Historial para el reporte de adherencia de los ultimos 7 dias)
                // Omeprazol (id 1): 2 omitidos, 2 tomados
                jdbcTemplate.execute("INSERT IGNORE INTO registros_toma (id_registro, id_horario, id_adulto, estado, fecha_hora_programada) VALUES (1, 1, 1, 'omitido', DATE_SUB(NOW(), INTERVAL 1 DAY))");
                jdbcTemplate.execute("INSERT IGNORE INTO registros_toma (id_registro, id_horario, id_adulto, estado, fecha_hora_programada, fecha_hora_registro) VALUES (2, 1, 1, 'tomado', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY))");
                jdbcTemplate.execute("INSERT IGNORE INTO registros_toma (id_registro, id_horario, id_adulto, estado, fecha_hora_programada) VALUES (3, 1, 1, 'omitido', DATE_SUB(NOW(), INTERVAL 3 DAY))");
                jdbcTemplate.execute("INSERT IGNORE INTO registros_toma (id_registro, id_horario, id_adulto, estado, fecha_hora_programada, fecha_hora_registro) VALUES (4, 1, 1, 'tomado', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY))");
                // Paracetamol (id 3): 3 omitidos
                jdbcTemplate.execute("INSERT IGNORE INTO registros_toma (id_registro, id_horario, id_adulto, estado, fecha_hora_programada) VALUES (5, 3, 1, 'omitido', DATE_SUB(NOW(), INTERVAL 1 DAY))");
                jdbcTemplate.execute("INSERT IGNORE INTO registros_toma (id_registro, id_horario, id_adulto, estado, fecha_hora_programada) VALUES (6, 3, 1, 'omitido', DATE_SUB(NOW(), INTERVAL 2 DAY))");
                jdbcTemplate.execute("INSERT IGNORE INTO registros_toma (id_registro, id_horario, id_adulto, estado, fecha_hora_programada) VALUES (7, 3, 1, 'omitido', DATE_SUB(NOW(), INTERVAL 3 DAY))");

                // Alertas Activas
                jdbcTemplate.execute("INSERT IGNORE INTO alertas (id_alerta, id_adulto, tipo_alerta, mensaje, resuelta, creado_en) VALUES (1, 1, 'omision_medicacion', 'No se ha registrado la toma del Omeprazol.', 0, DATE_SUB(NOW(), INTERVAL 1 HOUR))");
                jdbcTemplate.execute("INSERT IGNORE INTO alertas (id_alerta, id_adulto, tipo_alerta, mensaje, resuelta, creado_en) VALUES (2, 1, 'caida_detectada', 'El sensor inteligente reportó una posible caída.', 0, DATE_SUB(NOW(), INTERVAL 30 MINUTE))");

                // Observaciones del Cuidador
                if (idCuidador > 0) {
                    jdbcTemplate.execute("INSERT IGNORE INTO observaciones_cuidador (id_observacion, id_cuidador, id_adulto, texto, urgencia, fecha_hora) VALUES (1, " + idCuidador + ", 1, 'La paciente amaneció con excelente estado de ánimo y buena presión arterial.', 'baja', DATE_SUB(NOW(), INTERVAL 4 HOUR))");
                    jdbcTemplate.execute("INSERT IGNORE INTO observaciones_cuidador (id_observacion, id_cuidador, id_adulto, texto, urgencia, fecha_hora) VALUES (2, " + idCuidador + ", 1, 'Se rehusó levemente a tomar la medicina de la mañana, pero terminó accediendo.', 'media', DATE_SUB(NOW(), INTERVAL 1 HOUR))");
                }
                
                // Registrar Dispositivos IoT por defecto para la DEMO en Producción
                jdbcTemplate.execute("INSERT IGNORE INTO dispositivos_iot (identificador_fisico, tipo_dispositivo, id_adulto, activo, fecha_registro, ultima_conexion) VALUES ('PASTILLERO-A1', 'pastillero_esp32', 1, 1, NOW(), NOW())");
                jdbcTemplate.execute("INSERT IGNORE INTO dispositivos_iot (identificador_fisico, tipo_dispositivo, id_adulto, activo, fecha_registro, ultima_conexion) VALUES ('FF:FF:FF:F2:02:00', 'pulsera', 1, 1, NOW(), NOW())");

                log.info("✅ Datos de prueba (Adultos, Relaciones, Medicamentos, Registros, Alertas, Observaciones, Dispositivos) inyectados.");
            } catch (Exception e) {
                log.error("Error al inyectar datos de prueba: {}", e.getMessage());
            }
        };
    }

    private void seedRol(RolRepository repo, String nombre, String descripcion) {
        if (repo.findByNombreRol(nombre).isEmpty()) {
            Rol rol = new Rol();
            rol.setNombreRol(nombre);
            rol.setDescripcion(descripcion);
            repo.save(rol);
            log.info("✅ Rol creado: {}", nombre);
        }
    }

    private void seedUsuario(UsuarioRepository usuarioRepo,
                              RolRepository rolRepo,
                              String passwordHash,
                              String nombre, String apellido,
                              String correo, String nombreRol) {
        if (usuarioRepo.findByCorreo(correo).isEmpty()) {
            Rol rol = rolRepo.findByNombreRol(nombreRol)
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombreRol));
            Usuario u = new Usuario();
            u.setNombre(nombre);
            u.setApellido(apellido);
            u.setCorreo(correo);
            u.setPasswordHash(passwordHash);
            u.setActivo(true);
            u.setRol(rol);
            usuarioRepo.save(u);
            log.info("✅ Usuario creado: {} [{}]", correo, nombreRol);
        } else {
            Usuario u = usuarioRepo.findByCorreo(correo).get();
            u.setPasswordHash(passwordHash);
            u.setActivo(true);
            usuarioRepo.save(u);
            log.info("ℹ️  Usuario ya existe. Contraseña actualizada y cuenta activada: {}", correo);
        }
    }
}

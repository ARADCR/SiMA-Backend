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



/**
 * DataSeeder: inserta datos iniciales en la base de datos al arrancar la app.
 * Crea los roles del sistema y los usuarios de prueba por defecto.
 * El rol 'Adulto Mayor' se mantiene en la tabla roles por consistencia del modelo,
 * pero no se crea ningún usuario con ese rol: el adulto mayor no es un usuario del sistema.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    public CommandLineRunner seedDatabase(RolRepository rolRepository,
                                          UsuarioRepository usuarioRepository,
                                          PasswordEncoder passwordEncoder,
                                          JdbcTemplate jdbcTemplate) {
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

                // Horarios
                jdbcTemplate.execute("INSERT IGNORE INTO horarios_medicamento (id_horario, id_medicamento, hora_programada, activo) VALUES (1, 1, '08:00:00', 1)");
                jdbcTemplate.execute("INSERT IGNORE INTO horarios_medicamento (id_horario, id_medicamento, hora_programada, activo) VALUES (2, 2, '09:00:00', 1)");

                log.info("✅ Datos de prueba (Adultos, Relaciones, Medicamentos) inyectados.");
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

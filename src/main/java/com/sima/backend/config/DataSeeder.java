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



/**
 * DataSeeder: inserta datos iniciales en la base de datos al arrancar la app.
 * Crea los 4 roles del sistema y un usuario Administrador por defecto.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    public CommandLineRunner seedDatabase(RolRepository rolRepository,
                                          UsuarioRepository usuarioRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {

            // ── 1. Crear roles si no existen ──────────────────────────────────
            seedRol(rolRepository, "Administrador", "Administrador del sistema SiMA");
            seedRol(rolRepository, "Familiar",      "Familiar del adulto mayor");
            seedRol(rolRepository, "Cuidador",      "Cuidador profesional");
            seedRol(rolRepository, "Adulto Mayor",  "Adulto mayor monitoreado");

            // ── 2. Crear los 4 usuarios de prueba (misma contraseña: Admin1234) ─
            String passwordHash = passwordEncoder.encode("Admin1234");

            seedUsuario(usuarioRepository, rolRepository, passwordHash,
                    "Admin",    "SiMA",     "admin@sima.com",    "Administrador");
            seedUsuario(usuarioRepository, rolRepository, passwordHash,
                    "Familiar", "SiMA",     "familiar@sima.com", "Familiar");
            seedUsuario(usuarioRepository, rolRepository, passwordHash,
                    "Cuidador", "SiMA",     "cuidador@sima.com", "Cuidador");
            seedUsuario(usuarioRepository, rolRepository, passwordHash,
                    "Adulto",   "Mayor",    "adulto@sima.com",   "Adulto Mayor");
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
            log.info("ℹ️  Usuario ya existe: {}", correo);
        }
    }
}

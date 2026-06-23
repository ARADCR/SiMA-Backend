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

            // ── 2. Crear usuario admin si no existe ───────────────────────────
            if (usuarioRepository.findByCorreo("admin@sima.com").isEmpty()) {
                Rol rolAdmin = rolRepository.findByNombreRol("Administrador")
                        .orElseThrow(() -> new RuntimeException("Rol Administrador no encontrado"));

                Usuario admin = new Usuario();
                admin.setNombre("Admin");
                admin.setApellido("SiMA");
                admin.setCorreo("admin@sima.com");
                admin.setPasswordHash(passwordEncoder.encode("Admin1234"));
                admin.setActivo(true);
                admin.setRol(rolAdmin);

                usuarioRepository.save(admin);
                log.info("✅ Usuario admin creado: admin@sima.com / Admin1234");
            } else {
                log.info("ℹ️  Usuario admin ya existe, omitiendo seed.");
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
}

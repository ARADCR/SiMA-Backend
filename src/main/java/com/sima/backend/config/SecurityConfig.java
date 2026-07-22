package com.sima.backend.config;

import com.sima.backend.security.CustomAuthenticationEntryPoint;
import com.sima.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita @PreAuthorize en controllers y services
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomAuthenticationEntryPoint authenticationEntryPoint,
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Factor 12 = buen balance seguridad/velocidad
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Habilitar CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Deshabilitar CSRF (no aplica para APIs REST stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Retornar 401 en lugar de redirect al login
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint))

                // Sin sesiones HTTP: cada petición se autentica con JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Reglas de acceso por endpoint
                .authorizeHttpRequests(auth -> auth

                        // Endpoints públicos: login y registro
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Lecturas de pulsera: POST abierto para la app Android
                        // TODO: Proteger con API key o JWT para dispositivos antes de producción
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/lecturas-pulsera",
                                "/api/lecturas-pulsera/",
                                "/lecturas-pulsera",
                                "/lecturas-pulsera/"
                        ).permitAll()
                        // Lecturas de pulsera: GET requiere autenticación y rol
                        .requestMatchers(HttpMethod.GET, "/lecturas-pulsera/**")
                                .hasAnyRole("Administrador", "Familiar", "Cuidador")

                        // Solo Administrador puede gestionar usuarios y dispositivos
                        .requestMatchers("/usuarios/**").hasRole("Administrador")
                        .requestMatchers(HttpMethod.GET, "/dispositivos/adulto/**").hasAnyRole("Administrador", "Familiar", "Cuidador")
                        .requestMatchers("/dispositivos/**").hasRole("Administrador")

                        // Familiar y Cuidador gestionan adultos y medicamentos
                        .requestMatchers("/adultos/**").hasAnyRole("Administrador", "Familiar", "Cuidador")
                        .requestMatchers("/medicamentos/**").hasAnyRole("Administrador", "Familiar", "Cuidador")

                        // Registros de toma: Familiar y Cuidador
                        .requestMatchers("/tomas/**").hasAnyRole("Familiar", "Cuidador")

                        // Alertas: Familiar y Cuidador
                        .requestMatchers("/alertas/**").hasAnyRole("Administrador", "Familiar", "Cuidador")

                        // Observaciones del cuidador: Cuidador registra, Familiar y Administrador solo consultan
                        .requestMatchers("/observaciones/**").hasAnyRole("Administrador", "Familiar", "Cuidador")

                        // Notificaciones SSE
                        .requestMatchers("/notifications/**")
                        .hasAnyRole("Administrador", "Familiar", "Cuidador", "Adulto Mayor")

                        // IA: requiere autenticación, RBAC específico se valida a nivel de servicio
                        .requestMatchers("/ai/**").authenticated()

                        // Cualquier otra ruta requiere autenticación
                        .anyRequest().authenticated())

                // Agregar el filtro JWT antes del filtro de usuario/contraseña
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @org.springframework.beans.factory.annotation.Value("${CORS_ALLOWED_ORIGINS:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Mapear los orígenes dinámicos split por comas
        java.util.List<String> origins = java.util.Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
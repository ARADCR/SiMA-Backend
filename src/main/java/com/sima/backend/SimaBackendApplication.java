package com.sima.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * Punto de entrada de la aplicación SiMA.
 * proxyBeanMethods = false evita el conflicto de CGLIB con beans de factory.
 */
@SpringBootApplication(proxyBeanMethods = false)
@EnableScheduling
public class SimaBackendApplication {

    @PostConstruct
    public void init() {
        // Forzar a que el servidor en la nube use la hora de México
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
    }

    public static void main(String[] args) {
        SpringApplication.run(SimaBackendApplication.class, args);
    }
}

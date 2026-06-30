package com.sima.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicación SiMA.
 * proxyBeanMethods = false evita el conflicto de CGLIB con beans de factory.
 */
@SpringBootApplication(proxyBeanMethods = false)
@EnableScheduling
public class SimaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimaBackendApplication.class, args);
    }
}

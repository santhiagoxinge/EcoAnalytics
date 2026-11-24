package com.ecoanalytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EcoAnalyticsAplicacion {

    public static void main(String[] args) {
        SpringApplication.run(EcoAnalyticsAplicacion.class, args);
    }
}

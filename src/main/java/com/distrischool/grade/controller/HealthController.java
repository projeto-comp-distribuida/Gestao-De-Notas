package com.distrischool.grade.controller;

import com.distrischool.grade.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller para health checks do microserviço
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        log.info("GET /api/v1/health - Verificando saúde do serviço");
        
        Map<String, Object> healthInfo = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "service", "DistriSchool Grade Management Service",
            "version", "1.0.0"
        );
        
        return ResponseEntity.ok(ApiResponse.success(healthInfo, "Serviço funcionando corretamente"));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInfo() {
        log.info("GET /api/v1/health/info - Obtendo informações do serviço");
        
        Map<String, Object> serviceInfo = Map.of(
            "name", "DistriSchool Grade Management Service",
            "description", "Microserviço de gestão de notas e avaliações para o sistema de gestão escolar",
            "version", "1.0.0",
            "features", new String[]{
                "Spring Boot 3.2.0",
                "PostgreSQL com Flyway",
                "Redis para cache",
                "Apache Kafka para mensageria",
                "Spring Cloud OpenFeign",
                "Resilience4j Circuit Breaker",
                "Prometheus Metrics",
                "Gestão de Notas",
                "Gestão de Avaliações",
                "Gestão de Assessments"
            }
        );
        
        return ResponseEntity.ok(ApiResponse.success(serviceInfo, "Informações do serviço"));
    }
}


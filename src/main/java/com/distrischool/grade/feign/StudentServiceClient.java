package com.distrischool.grade.feign;

import com.distrischool.grade.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client para comunicação com o microserviço de gestão de estudantes
 */
@FeignClient(
    name = "student-service",
    url = "${microservice.student.url:http://student-management-service-dev:8080}"
)
public interface StudentServiceClient {

    /**
     * Busca um estudante por ID
     * GET /api/v1/students/{id}
     */
    @GetMapping("/api/v1/students/{id}")
    ApiResponse<Map<String, Object>> getStudentById(@PathVariable Long id);

    /**
     * Verifica se um estudante existe
     * GET /api/v1/students/{id}
     * Retorna 200 se existe, 404 se não existe
     */
    @GetMapping("/api/v1/students/{id}")
    ApiResponse<Void> checkStudentExists(@PathVariable Long id);
}


package com.distrischool.grade.feign;

import com.distrischool.grade.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client para comunicação com o microserviço de gestão de professores
 */
@FeignClient(
    name = "teacher-service",
    url = "${microservice.teacher.url:http://microservice-template-dev:8080}"
)
public interface TeacherServiceClient {

    /**
     * Busca um professor por ID
     * GET /api/v1/teachers/{id}
     */
    @GetMapping("/api/v1/teachers/{id}")
    ApiResponse<Map<String, Object>> getTeacherById(@PathVariable Long id);

    /**
     * Verifica se um professor existe
     * GET /api/v1/teachers/{id}
     * Retorna 200 se existe, 404 se não existe
     */
    @GetMapping("/api/v1/teachers/{id}")
    ApiResponse<Void> checkTeacherExists(@PathVariable Long id);
}


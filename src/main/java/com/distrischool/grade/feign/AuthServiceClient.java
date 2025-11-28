package com.distrischool.grade.feign;

import com.distrischool.grade.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client para comunicação com o microserviço de autenticação
 */
@FeignClient(
    name = "auth-service",
    url = "${microservice.auth.url:http://microservice-auth-dev:8080}"
)
public interface AuthServiceClient {

    /**
     * Busca o studentId associado a um userId
     * GET /api/v1/users/{userId}/student-id
     * ou
     * GET /api/v1/users/{userId}
     */
    @GetMapping("/api/v1/users/{userId}/student-id")
    ApiResponse<Map<String, Object>> getStudentIdByUserId(@PathVariable Long userId);
    
    /**
     * Busca informações do usuário (fallback)
     * GET /api/v1/users/{userId}
     */
    @GetMapping("/api/v1/users/{userId}")
    ApiResponse<Map<String, Object>> getUserById(@PathVariable Long userId);
}


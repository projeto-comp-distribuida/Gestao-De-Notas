package com.distrischool.grade.feign;

import com.distrischool.grade.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Feign client para comunicação com o microserviço de autenticação
 */
@FeignClient(
    name = "auth-service",
    url = "${microservice.auth.url:http://auth-service-dev:8080}"
)
public interface AuthServiceClient {

    /**
     * Busca informações do usuário por ID
     * GET /api/v1/users/{userId}
     */
    @GetMapping("/api/v1/users/{userId}")
    ApiResponse<Map<String, Object>> getUserById(@PathVariable Long userId);

    /**
     * Verifica se o usuário tem uma role específica
     * GET /api/v1/users/{userId}/has-role?role={role}
     */
    @GetMapping("/api/v1/users/{userId}/has-role")
    ApiResponse<Boolean> hasRole(@PathVariable Long userId, @RequestParam String role);

    /**
     * Busca usuário por Auth0 ID
     * GET /api/v1/users/auth0/{auth0Id}
     */
    @GetMapping("/api/v1/users/auth0/{auth0Id}")
    ApiResponse<Map<String, Object>> getUserByAuth0Id(@PathVariable String auth0Id);
}


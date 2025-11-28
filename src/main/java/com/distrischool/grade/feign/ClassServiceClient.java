package com.distrischool.grade.feign;

import com.distrischool.grade.dto.ApiResponse;
import com.distrischool.grade.dto.ClassInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client para comunicação com o microserviço de turmas e horários.
 */
@FeignClient(
    name = "class-service",
    url = "${microservice.class.url:http://schedule-management-service-dev:8080}"
)
public interface ClassServiceClient {

    /**
     * Busca uma turma pelo ID.
     */
    @GetMapping("/api/v1/classes/{id}")
    ApiResponse<ClassInfoDTO> getClassById(@PathVariable Long id);
}




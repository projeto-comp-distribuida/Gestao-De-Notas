package com.distrischool.grade.controller;

import com.distrischool.grade.dto.ApiResponse;
import com.distrischool.grade.dto.GradeRequestDTO;
import com.distrischool.grade.dto.GradeResponseDTO;
import com.distrischool.grade.service.GradeService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller REST para gerenciamento de notas
 */
@RestController
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
@Slf4j
public class GradeController {

    private final GradeService gradeService;
    private final com.distrischool.grade.config.SecurityConfig securityConfig;

    /**
     * Cria uma nova nota
     * POST /api/v1/grades
     * Requer role TEACHER ou ADMIN
     */
    @PostMapping
    @Timed(value = "grades.create", description = "Time taken to create a grade")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN') or @securityConfig.isSecurityDisabled()")
    public ResponseEntity<ApiResponse<GradeResponseDTO>> createGrade(
        @Valid @RequestBody GradeRequestDTO request,
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @AuthenticationPrincipal Jwt jwt) {

        String effectiveUserId = userId != null ? userId : (jwt != null ? jwt.getSubject() : "system");
        
        // Extrair roles do JWT (se disponível)
        List<String> roles = List.of();
        String userRole = "SYSTEM";
        if (jwt != null) {
            try {
                roles = jwt.getClaimAsStringList("permissions");
                if (roles == null) {
                    roles = List.of();
                }
                userRole = extractUserRole(roles, jwt);
            } catch (Exception e) {
                log.debug("Não foi possível extrair roles do JWT: {}", e.getMessage());
            }
        }
        
        log.info("Requisição para criar nota - Aluno: {}, Avaliação: {} (by {}, role: {})", 
                 request.getStudentId(), request.getEvaluationId(), effectiveUserId, userRole);
        
        GradeResponseDTO grade = gradeService.createGrade(request, effectiveUserId);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(grade, "Nota criada com sucesso"));
    }

    /**
     * Busca nota por ID
     * GET /api/v1/grades/{id}
     */
    @GetMapping("/{id}")
    @Timed(value = "grades.get", description = "Time taken to get a grade")
    public ResponseEntity<ApiResponse<GradeResponseDTO>> getGradeById(@PathVariable Long id) {
        log.info("Requisição para buscar nota por ID: {}", id);
        GradeResponseDTO grade = gradeService.getGradeById(id);
        return ResponseEntity.ok(ApiResponse.success(grade));
    }

    /**
     * Lista todas as notas com paginação
     * GET /api/v1/grades
     */
    @GetMapping
    @Timed(value = "grades.list", description = "Time taken to list grades")
    public ResponseEntity<ApiResponse<Page<GradeResponseDTO>>> getAllGrades(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "ASC") Sort.Direction direction) {

        log.info("Requisição para listar notas - Página: {}, Tamanho: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<GradeResponseDTO> grades = gradeService.getAllGrades(pageable);

        return ResponseEntity.ok(ApiResponse.success(grades));
    }

    /**
     * Busca notas por aluno
     * GET /api/v1/grades/student/{studentId}
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<Page<GradeResponseDTO>>> getGradesByStudent(
        @PathVariable Long studentId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        log.info("Requisição para buscar notas do aluno: {}", studentId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "gradeDate"));
        Page<GradeResponseDTO> grades = gradeService.getGradesByStudent(studentId, pageable);

        return ResponseEntity.ok(ApiResponse.success(grades));
    }

    /**
     * Busca notas por avaliação
     * GET /api/v1/grades/evaluation/{evaluationId}
     */
    @GetMapping("/evaluation/{evaluationId}")
    public ResponseEntity<ApiResponse<Page<GradeResponseDTO>>> getGradesByEvaluation(
        @PathVariable Long evaluationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        log.info("Requisição para buscar notas da avaliação: {}", evaluationId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "gradeValue"));
        Page<GradeResponseDTO> grades = gradeService.getGradesByEvaluation(evaluationId, pageable);

        return ResponseEntity.ok(ApiResponse.success(grades));
    }

    /**
     * Calcula a média de um aluno
     * GET /api/v1/grades/student/{studentId}/average
     */
    @GetMapping("/student/{studentId}/average")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateAverageGrade(
        @PathVariable Long studentId,
        @RequestParam Integer academicYear,
        @RequestParam Integer academicSemester) {

        log.info("Requisição para calcular média - Aluno: {}, Ano: {}, Semestre: {}", 
                 studentId, academicYear, academicSemester);
        
        BigDecimal average = gradeService.calculateAverageGrade(studentId, academicYear, academicSemester);
        return ResponseEntity.ok(ApiResponse.success(average, "Média calculada com sucesso"));
    }

    /**
     * Atualiza uma nota
     * PUT /api/v1/grades/{id}
     * Requer role TEACHER ou ADMIN
     */
    @PutMapping("/{id}")
    @Timed(value = "grades.update", description = "Time taken to update a grade")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN') or @securityConfig.isSecurityDisabled()")
    public ResponseEntity<ApiResponse<GradeResponseDTO>> updateGrade(
        @PathVariable Long id,
        @Valid @RequestBody GradeRequestDTO request,
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @AuthenticationPrincipal Jwt jwt) {

        String effectiveUserId = userId != null ? userId : (jwt != null ? jwt.getSubject() : "system");
        log.info("Requisição para atualizar nota: ID={} (by {})", id, effectiveUserId);
        
        GradeResponseDTO grade = gradeService.updateGrade(id, request, effectiveUserId);

        return ResponseEntity.ok(ApiResponse.success(grade, "Nota atualizada com sucesso"));
    }

    /**
     * Deleta uma nota (soft delete)
     * DELETE /api/v1/grades/{id}
     * Requer role TEACHER ou ADMIN
     */
    @DeleteMapping("/{id}")
    @Timed(value = "grades.delete", description = "Time taken to delete a grade")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN') or @securityConfig.isSecurityDisabled()")
    public ResponseEntity<ApiResponse<Void>> deleteGrade(
        @PathVariable Long id,
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @AuthenticationPrincipal Jwt jwt) {

        String effectiveUserId = userId != null ? userId : (jwt != null ? jwt.getSubject() : "system");
        log.info("Requisição para deletar nota: ID={} (by {})", id, effectiveUserId);
        
        gradeService.deleteGrade(id, effectiveUserId);

        return ResponseEntity.ok(ApiResponse.success(null, "Nota deletada com sucesso"));
    }

    /**
     * Extrai a role do usuário do JWT
     */
    private String extractUserRole(List<String> permissions, Jwt jwt) {
        if (permissions != null && !permissions.isEmpty()) {
            // Procurar por roles nas permissions
            for (String permission : permissions) {
                if (permission.contains("TEACHER") || permission.contains("ADMIN")) {
                    return permission.contains("ADMIN") ? "ADMIN" : "TEACHER";
                }
            }
        }
        
        // Tentar extrair do claim "roles" do JWT
        if (jwt != null) {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null && !roles.isEmpty()) {
                if (roles.contains("ADMIN")) return "ADMIN";
                if (roles.contains("TEACHER")) return "TEACHER";
            }
        }
        
        return "USER"; // Role padrão
    }
}


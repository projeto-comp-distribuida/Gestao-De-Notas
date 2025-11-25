package com.distrischool.grade.controller;

import com.distrischool.grade.dto.ApiResponse;
import com.distrischool.grade.dto.ClassGradeSummaryDTO;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Controller REST para gerenciamento de notas
 */
@RestController
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
@Slf4j
public class GradeController {

    private final GradeService gradeService;

    /**
     * Cria uma nova nota
     * POST /api/v1/grades
     */
    @PostMapping
    @Timed(value = "grades.create", description = "Time taken to create a grade")
    public ResponseEntity<ApiResponse<GradeResponseDTO>> createGrade(
        @Valid @RequestBody GradeRequestDTO request,
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @AuthenticationPrincipal Jwt jwt) {

        String effectiveUserId = userId != null ? userId : (jwt != null ? jwt.getSubject() : "system");
        
        log.info("Requisição para criar nota - Aluno: {}, Avaliação: {} (by {})", 
                 request.getStudentId(), request.getEvaluationId(), effectiveUserId);
        
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
     * Lista as notas agrupadas por aluno para uma turma específica.
     * GET /api/v1/grades/classes/{classId}/grades
     */
    @GetMapping("/classes/{classId}/grades")
    public ResponseEntity<ApiResponse<ClassGradeSummaryDTO>> getClassGrades(
        @PathVariable Long classId,
        @RequestParam(required = false) Integer academicYear,
        @RequestParam(required = false) Integer academicSemester,
        @RequestParam(defaultValue = "3") int maxGradesPerStudent) {

        log.info("Requisição para listar notas da turma: {}, Ano: {}, Semestre: {}, Limite: {}",
                 classId, academicYear, academicSemester, maxGradesPerStudent);

        ClassGradeSummaryDTO summary = gradeService.getClassGradeDetails(
                classId, academicYear, academicSemester, maxGradesPerStudent);

        return ResponseEntity.ok(ApiResponse.success(summary, "Notas da turma recuperadas com sucesso"));
    }

    /**
     * Calcula a média consolidada de uma turma.
     * GET /api/v1/grades/classes/{classId}/average
     */
    @GetMapping("/classes/{classId}/average")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateClassAverage(
        @PathVariable Long classId,
        @RequestParam(required = false) Integer academicYear,
        @RequestParam(required = false) Integer academicSemester,
        @RequestParam(defaultValue = "3") int maxGradesPerStudent) {

        log.info("Requisição para calcular média da turma: {}, Ano: {}, Semestre: {}",
                 classId, academicYear, academicSemester);

        BigDecimal average = gradeService.calculateClassAverage(
                classId, academicYear, academicSemester, maxGradesPerStudent);

        return ResponseEntity.ok(ApiResponse.success(average, "Média da turma calculada com sucesso"));
    }

    /**
     * Calcula a média global considerando todas as turmas.
     * GET /api/v1/grades/classes/average
     */
    @GetMapping("/classes/average")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateGlobalClassesAverage(
        @RequestParam(required = false) Integer academicYear,
        @RequestParam(required = false) Integer academicSemester,
        @RequestParam(defaultValue = "3") int maxGradesPerStudent) {

        log.info("Requisição para calcular média global entre turmas - Ano: {}, Semestre: {}",
                 academicYear, academicSemester);

        BigDecimal average = gradeService.calculateGlobalClassesAverage(
                academicYear, academicSemester, maxGradesPerStudent);

        return ResponseEntity.ok(ApiResponse.success(average, "Média global calculada com sucesso"));
    }

    /**
     * Atualiza uma nota
     * PUT /api/v1/grades/{id}
     */
    @PutMapping("/{id}")
    @Timed(value = "grades.update", description = "Time taken to update a grade")
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
     */
    @DeleteMapping("/{id}")
    @Timed(value = "grades.delete", description = "Time taken to delete a grade")
    public ResponseEntity<ApiResponse<Void>> deleteGrade(
        @PathVariable Long id,
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @AuthenticationPrincipal Jwt jwt) {

        String effectiveUserId = userId != null ? userId : (jwt != null ? jwt.getSubject() : "system");
        log.info("Requisição para deletar nota: ID={} (by {})", id, effectiveUserId);
        
        gradeService.deleteGrade(id, effectiveUserId);

        return ResponseEntity.ok(ApiResponse.success(null, "Nota deletada com sucesso"));
    }
}


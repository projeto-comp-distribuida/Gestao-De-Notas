package com.distrischool.grade.service;

import com.distrischool.grade.dto.ApiResponse;
import com.distrischool.grade.dto.GradeRequestDTO;
import com.distrischool.grade.dto.GradeResponseDTO;
import com.distrischool.grade.entity.Grade;
import com.distrischool.grade.entity.Grade.GradeStatus;
import com.distrischool.grade.exception.BusinessException;
import com.distrischool.grade.exception.ResourceNotFoundException;
import com.distrischool.grade.feign.StudentServiceClient;
import com.distrischool.grade.feign.TeacherServiceClient;
import com.distrischool.grade.kafka.DistriSchoolEvent;
import com.distrischool.grade.kafka.EventProducer;
import com.distrischool.grade.repository.GradeRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Service para gerenciamento de notas
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GradeService {

    private final GradeRepository gradeRepository;
    private final EventProducer eventProducer;
    private final StudentServiceClient studentServiceClient;
    private final TeacherServiceClient teacherServiceClient;

    @Value("${microservice.kafka.topics.grade-created}")
    private String gradeCreatedTopic;

    @Value("${microservice.kafka.topics.grade-updated}")
    private String gradeUpdatedTopic;

    @Value("${microservice.kafka.topics.grade-deleted}")
    private String gradeDeletedTopic;

    /**
     * Cria uma nova nota
     */
    @Transactional
    @CacheEvict(value = "grades", allEntries = true)
    public GradeResponseDTO createGrade(GradeRequestDTO request, String createdBy) {
        log.info("Criando nova nota - Aluno: {}, Avaliação: {}", request.getStudentId(), request.getEvaluationId());

        // Validações de negócio
        validateGradeRequest(request);

        // Valida se o estudante existe (integração com Student Service)
        validateStudentExists(request.getStudentId());

        // Valida se o professor existe (integração com Teacher Service)
        validateTeacherExists(request.getTeacherId());

        // Verifica se já existe nota para este aluno nesta avaliação
        gradeRepository.findByStudentIdAndEvaluationId(request.getStudentId(), request.getEvaluationId())
                .ifPresent(g -> {
                    throw new BusinessException("Já existe uma nota para este aluno nesta avaliação");
                });

        // Cria a entidade
        Grade grade = Grade.builder()
                .studentId(request.getStudentId())
                .teacherId(request.getTeacherId())
                .evaluationId(request.getEvaluationId())
                .gradeValue(request.getGradeValue())
                .gradeDate(request.getGradeDate())
                .notes(request.getNotes())
                .status(request.getStatus() != null ? request.getStatus() : GradeStatus.REGISTERED)
                .isAutomatic(request.getIsAutomatic() != null ? request.getIsAutomatic() : false)
                .academicYear(request.getAcademicYear())
                .academicSemester(request.getAcademicSemester())
                .build();

        grade.setCreatedBy(createdBy);
        grade.setUpdatedBy(createdBy);

        // Salva no banco
        Grade savedGrade = gradeRepository.save(grade);
        log.info("Nota criada com sucesso: ID={}, Aluno={}, Valor={}", 
                 savedGrade.getId(), savedGrade.getStudentId(), savedGrade.getGradeValue());

        // Publica evento Kafka
        publishGradeCreatedEvent(savedGrade);

        return GradeResponseDTO.fromEntity(savedGrade);
    }

    /**
     * Busca nota por ID
     */
    @Cacheable(value = "grades", key = "#id")
    public GradeResponseDTO getGradeById(Long id) {
        log.debug("Buscando nota por ID: {}", id);
        Grade grade = findGradeByIdOrThrow(id);
        return GradeResponseDTO.fromEntity(grade);
    }

    /**
     * Lista todas as notas com paginação
     */
    public Page<GradeResponseDTO> getAllGrades(Pageable pageable) {
        log.debug("Listando todas as notas - Página: {}", pageable.getPageNumber());
        return gradeRepository.findAllNotDeleted(pageable)
                .map(GradeResponseDTO::fromEntity);
    }

    /**
     * Busca notas por aluno
     */
    public Page<GradeResponseDTO> getGradesByStudent(Long studentId, Pageable pageable) {
        log.debug("Buscando notas do aluno: {}", studentId);
        return gradeRepository.findByStudentId(studentId, pageable)
                .map(GradeResponseDTO::fromEntity);
    }

    /**
     * Busca notas por avaliação
     */
    public Page<GradeResponseDTO> getGradesByEvaluation(Long evaluationId, Pageable pageable) {
        log.debug("Buscando notas da avaliação: {}", evaluationId);
        return gradeRepository.findByEvaluationId(evaluationId, pageable)
                .map(GradeResponseDTO::fromEntity);
    }

    /**
     * Atualiza uma nota
     */
    @Transactional
    @CacheEvict(value = "grades", allEntries = true)
    public GradeResponseDTO updateGrade(Long id, GradeRequestDTO request, String updatedBy) {
        log.info("Atualizando nota: ID={}", id);

        Grade grade = findGradeByIdOrThrow(id);
        validateGradeRequest(request);

        // Atualiza os campos
        grade.setGradeValue(request.getGradeValue());
        grade.setGradeDate(request.getGradeDate());
        grade.setNotes(request.getNotes());
        if (request.getStatus() != null) {
            grade.setStatus(request.getStatus());
        }
        grade.setUpdatedBy(updatedBy);

        Grade updatedGrade = gradeRepository.save(grade);
        log.info("Nota atualizada com sucesso: ID={}", updatedGrade.getId());

        // Publica evento Kafka
        publishGradeUpdatedEvent(updatedGrade);

        return GradeResponseDTO.fromEntity(updatedGrade);
    }

    /**
     * Deleta uma nota (soft delete)
     */
    @Transactional
    @CacheEvict(value = "grades", allEntries = true)
    public void deleteGrade(Long id, String deletedBy) {
        log.info("Deletando nota: ID={}", id);

        Grade grade = findGradeByIdOrThrow(id);
        grade.markAsDeleted(deletedBy);
        gradeRepository.save(grade);

        log.info("Nota deletada com sucesso: ID={}", id);

        // Publica evento Kafka
        publishGradeDeletedEvent(grade);
    }

    /**
     * Calcula a média de um aluno
     */
    public BigDecimal calculateAverageGrade(Long studentId, Integer academicYear, Integer academicSemester) {
        log.debug("Calculando média do aluno: {}, Ano: {}, Semestre: {}", 
                  studentId, academicYear, academicSemester);
        
        BigDecimal average = gradeRepository.calculateAverageGrade(studentId, academicYear, academicSemester);
        return average != null ? average : BigDecimal.ZERO;
    }

    /**
     * Busca nota por ID ou lança exceção
     */
    private Grade findGradeByIdOrThrow(Long id) {
        return gradeRepository.findById(id)
                .filter(g -> !g.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "id", id));
    }

    /**
     * Valida a requisição de nota
     */
    private void validateGradeRequest(GradeRequestDTO request) {
        if (request.getGradeValue() == null) {
            throw new BusinessException("Valor da nota é obrigatório");
        }
        if (request.getGradeValue().compareTo(BigDecimal.ZERO) < 0 || 
            request.getGradeValue().compareTo(new BigDecimal("10.0")) > 0) {
            throw new BusinessException("Nota deve estar entre 0 e 10");
        }
    }

    /**
     * Valida se o estudante existe no microserviço de estudantes
     */
    private void validateStudentExists(Long studentId) {
        try {
            ApiResponse<?> response = studentServiceClient.getStudentById(studentId);
            if (response == null || !response.isSuccess()) {
                throw new BusinessException("Estudante não encontrado com ID: " + studentId);
            }
            log.debug("Estudante validado com sucesso - ID: {}", studentId);
        } catch (FeignException.NotFound e) {
            log.warn("Estudante não encontrado - ID: {}", studentId);
            throw new BusinessException("Estudante não encontrado com ID: " + studentId);
        } catch (FeignException e) {
            log.error("Erro ao validar estudante - ID: {}, Erro: {}", studentId, e.getMessage());
            throw new BusinessException("Erro ao validar estudante. Tente novamente mais tarde.");
        } catch (Exception e) {
            log.error("Erro inesperado ao validar estudante - ID: {}", studentId, e);
            throw new BusinessException("Erro ao validar estudante: " + e.getMessage());
        }
    }

    /**
     * Valida se o professor existe no microserviço de professores
     */
    private void validateTeacherExists(Long teacherId) {
        try {
            ApiResponse<?> response = teacherServiceClient.getTeacherById(teacherId);
            if (response == null || !response.isSuccess()) {
                throw new BusinessException("Professor não encontrado com ID: " + teacherId);
            }
            log.debug("Professor validado com sucesso - ID: {}", teacherId);
        } catch (FeignException.NotFound e) {
            log.warn("Professor não encontrado - ID: {}", teacherId);
            throw new BusinessException("Professor não encontrado com ID: " + teacherId);
        } catch (FeignException e) {
            log.error("Erro ao validar professor - ID: {}, Erro: {}", teacherId, e.getMessage());
            throw new BusinessException("Erro ao validar professor. Tente novamente mais tarde.");
        } catch (Exception e) {
            log.error("Erro inesperado ao validar professor - ID: {}", teacherId, e);
            throw new BusinessException("Erro ao validar professor: " + e.getMessage());
        }
    }

    /**
     * Publica evento de nota criada
     */
    private void publishGradeCreatedEvent(Grade grade) {
        Map<String, Object> data = new HashMap<>();
        data.put("gradeId", grade.getId());
        data.put("studentId", grade.getStudentId());
        data.put("teacherId", grade.getTeacherId());
        data.put("evaluationId", grade.getEvaluationId());
        data.put("gradeValue", grade.getGradeValue());
        data.put("academicYear", grade.getAcademicYear());
        data.put("academicSemester", grade.getAcademicSemester());

        DistriSchoolEvent event = DistriSchoolEvent.of("grade.created", "grade-management-service", data);
        eventProducer.send(gradeCreatedTopic, event);
    }

    /**
     * Publica evento de nota atualizada
     */
    private void publishGradeUpdatedEvent(Grade grade) {
        Map<String, Object> data = new HashMap<>();
        data.put("gradeId", grade.getId());
        data.put("studentId", grade.getStudentId());
        data.put("gradeValue", grade.getGradeValue());
        data.put("status", grade.getStatus().toString());

        DistriSchoolEvent event = DistriSchoolEvent.of("grade.updated", "grade-management-service", data);
        eventProducer.send(gradeUpdatedTopic, event);
    }

    /**
     * Publica evento de nota deletada
     */
    private void publishGradeDeletedEvent(Grade grade) {
        Map<String, Object> data = new HashMap<>();
        data.put("gradeId", grade.getId());
        data.put("studentId", grade.getStudentId());
        data.put("evaluationId", grade.getEvaluationId());

        DistriSchoolEvent event = DistriSchoolEvent.of("grade.deleted", "grade-management-service", data);
        eventProducer.send(gradeDeletedTopic, event);
    }
}


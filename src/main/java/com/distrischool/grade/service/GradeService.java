package com.distrischool.grade.service;

import com.distrischool.grade.dto.ApiResponse;
import com.distrischool.grade.dto.ClassGradeSummaryDTO;
import com.distrischool.grade.dto.ClassInfoDTO;
import com.distrischool.grade.dto.GradeRequestDTO;
import com.distrischool.grade.dto.GradeResponseDTO;
import com.distrischool.grade.entity.Grade;
import com.distrischool.grade.entity.Grade.GradeStatus;
import com.distrischool.grade.exception.BusinessException;
import com.distrischool.grade.exception.ResourceNotFoundException;
import com.distrischool.grade.feign.AuthServiceClient;
import com.distrischool.grade.feign.ClassServiceClient;
import com.distrischool.grade.feign.StudentServiceClient;
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
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final ClassServiceClient classServiceClient;
    private final AuthServiceClient authServiceClient;

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

        // Valida se a turma existe e se o estudante pertence a ela
        validateClassAndStudent(request.getClassId(), request.getStudentId());

        // Verifica se já existe nota para este aluno nesta avaliação
        gradeRepository.findByStudentIdAndEvaluationId(request.getStudentId(), request.getEvaluationId())
                .ifPresent(g -> {
                    throw new BusinessException("Já existe uma nota para este aluno nesta avaliação");
                });

        // Cria a entidade
        Grade grade = Grade.builder()
                .studentId(request.getStudentId())
                .teacherId(request.getTeacherId())
                .classId(request.getClassId())
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
     * Busca notas por userId (busca o studentId associado e retorna as notas)
     */
    public Page<GradeResponseDTO> getGradesByUserId(Long userId, Pageable pageable) {
        log.debug("Buscando notas para userId: {}", userId);
        
        // Busca o studentId através do auth service
        Long studentId = getStudentIdByUserId(userId);
        
        if (studentId == null) {
            throw new BusinessException("Usuário não possui um studentId associado");
        }
        
        log.debug("StudentId encontrado para userId {}: {}", userId, studentId);
        return getGradesByStudent(studentId, pageable);
    }

    /**
     * Busca o studentId associado a um userId através do auth service
     */
    private Long getStudentIdByUserId(Long userId) {
        try {
            // Tenta primeiro o endpoint específico para student-id
            ApiResponse<Map<String, Object>> response = null;
            try {
                response = authServiceClient.getStudentIdByUserId(userId);
            } catch (FeignException.NotFound e) {
                log.debug("Endpoint /student-id não encontrado, tentando endpoint genérico");
                // Se não encontrar, tenta o endpoint genérico
                response = authServiceClient.getUserById(userId);
            }
            
            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.warn("Não foi possível encontrar studentId para userId: {}", userId);
                return null;
            }
            
            Map<String, Object> data = response.getData();
            Object studentIdObj = data.get("studentId");
            
            if (studentIdObj == null) {
                log.warn("Resposta do auth service não contém studentId para userId: {}", userId);
                return null;
            }
            
            // Converte o studentId para Long
            if (studentIdObj instanceof Number) {
                return ((Number) studentIdObj).longValue();
            } else if (studentIdObj instanceof String) {
                try {
                    return Long.parseLong((String) studentIdObj);
                } catch (NumberFormatException e) {
                    log.error("Erro ao converter studentId para Long: {}", studentIdObj, e);
                    return null;
                }
            }
            
            log.warn("Tipo de studentId não suportado: {}", studentIdObj.getClass());
            return null;
        } catch (FeignException.NotFound e) {
            log.warn("Usuário não encontrado - userId: {}", userId);
            return null;
        } catch (FeignException e) {
            log.error("Erro ao buscar studentId - userId: {}, Erro: {}", userId, e.getMessage());
            throw new BusinessException("Erro ao buscar studentId. Tente novamente mais tarde.");
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar studentId - userId: {}", userId, e);
            throw new BusinessException("Erro ao buscar studentId: " + e.getMessage());
        }
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

        if (!Objects.equals(grade.getClassId(), request.getClassId())) {
            validateClassAndStudent(request.getClassId(), grade.getStudentId());
            grade.setClassId(request.getClassId());
        }

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
     * Retorna o detalhamento das notas de uma turma (classe).
     */
    public ClassGradeSummaryDTO getClassGradeDetails(Long classId,
                                                     Integer academicYear,
                                                     Integer academicSemester,
                                                     int maxGradesPerStudent) {
        log.debug("Listando notas da turma: {}, Ano: {}, Semestre: {}, Limite: {}", 
                  classId, academicYear, academicSemester, maxGradesPerStudent);

        int normalizedLimit = normalizeMaxGradesLimit(maxGradesPerStudent);
        ClassInfoDTO classInfo = fetchClassInfo(classId);
        List<Grade> grades = loadGradesForClass(classId, academicYear, academicSemester);

        return buildClassGradeSummary(classInfo, grades, normalizedLimit);
    }

    /**
     * Calcula a média consolidada de uma turma considerando até 3 notas por aluno.
     */
    public BigDecimal calculateClassAverage(Long classId,
                                            Integer academicYear,
                                            Integer academicSemester,
                                            int maxGradesPerStudent) {
        ClassGradeSummaryDTO summary = getClassGradeDetails(classId, academicYear, academicSemester, maxGradesPerStudent);
        return summary.getClassAverage();
    }

    /**
     * Calcula a média geral entre todas as turmas que possuem notas registradas.
     */
    public BigDecimal calculateGlobalClassesAverage(Integer academicYear,
                                                    Integer academicSemester,
                                                    int maxGradesPerStudent) {
        log.debug("Calculando média global entre turmas - Ano: {}, Semestre: {}", academicYear, academicSemester);

        int normalizedLimit = normalizeMaxGradesLimit(maxGradesPerStudent);
        List<Grade> grades = loadAllClassGrades(academicYear, academicSemester);

        if (grades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Map<Long, List<Grade>> gradesByStudent = grades.stream()
                .collect(Collectors.groupingBy(Grade::getStudentId));

        List<BigDecimal> studentAverages = gradesByStudent.values().stream()
                .map(studentGrades -> calculateAverage(selectGradesForStudent(studentGrades, normalizedLimit)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (studentAverages.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = studentAverages.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(studentAverages.size()), 2, RoundingMode.HALF_UP);
    }

    private List<Grade> loadGradesForClass(Long classId, Integer academicYear, Integer academicSemester) {
        return gradeRepository.findClassGrades(classId, academicYear, academicSemester);
    }

    private List<Grade> loadAllClassGrades(Integer academicYear, Integer academicSemester) {
        return gradeRepository.findAllClassGrades(academicYear, academicSemester);
    }

    private ClassGradeSummaryDTO buildClassGradeSummary(ClassInfoDTO classInfo,
                                                        List<Grade> grades,
                                                        int maxGradesPerStudent) {
        Map<Long, List<Grade>> gradesByStudent = grades.stream()
                .collect(Collectors.groupingBy(Grade::getStudentId));

        LinkedHashSet<Long> orderedStudentIds = new LinkedHashSet<>();
        if (classInfo.getStudentIds() != null) {
            orderedStudentIds.addAll(classInfo.getStudentIds());
        }
        gradesByStudent.keySet().stream()
                .filter(studentId -> !orderedStudentIds.contains(studentId))
                .sorted()
                .forEach(orderedStudentIds::add);

        List<ClassGradeSummaryDTO.StudentClassGradeDTO> studentSummaries = orderedStudentIds.stream()
                .map(studentId -> buildStudentSummary(studentId, gradesByStudent.get(studentId), maxGradesPerStudent))
                .collect(Collectors.toList());

        long studentsWithGrades = studentSummaries.stream()
                .filter(dto -> dto.getAverage() != null)
                .count();

        BigDecimal classAverage = studentSummaries.stream()
                .map(ClassGradeSummaryDTO.StudentClassGradeDTO::getAverage)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (studentsWithGrades > 0) {
            classAverage = classAverage.divide(BigDecimal.valueOf(studentsWithGrades), 2, RoundingMode.HALF_UP);
        } else if (!studentSummaries.isEmpty()) {
            classAverage = BigDecimal.ZERO;
        }

        return ClassGradeSummaryDTO.builder()
                .classId(classInfo.getId())
                .className(classInfo.getName())
                .classCode(classInfo.getCode())
                .period(classInfo.getPeriod())
                .academicYear(classInfo.getAcademicYear())
                .totalStudents(orderedStudentIds.size())
                .studentsWithGrades((int) studentsWithGrades)
                .maxGradesPerStudent(maxGradesPerStudent)
                .classAverage(classAverage != null ? classAverage : BigDecimal.ZERO)
                .students(studentSummaries)
                .build();
    }

    private ClassGradeSummaryDTO.StudentClassGradeDTO buildStudentSummary(Long studentId,
                                                                          List<Grade> grades,
                                                                          int maxGradesPerStudent) {
        List<Grade> selectedGrades = selectGradesForStudent(grades != null ? grades : List.of(), maxGradesPerStudent);
        ClassGradeSummaryDTO.StudentClassGradeDTO.StudentClassGradeDTOBuilder builder =
                ClassGradeSummaryDTO.StudentClassGradeDTO.builder()
                        .studentId(studentId)
                        .grades(selectedGrades.stream()
                                .map(this::toGradeSnapshot)
                                .collect(Collectors.toList()));

        if (!selectedGrades.isEmpty()) {
            builder.average(calculateAverage(selectedGrades));
        }

        return builder.build();
    }

    private List<Grade> selectGradesForStudent(List<Grade> grades, int maxGradesPerStudent) {
        if (grades == null || grades.isEmpty()) {
            return List.of();
        }

        return grades.stream()
                .sorted(Comparator
                        .comparing(Grade::getGradeDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Grade::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(maxGradesPerStudent)
                .collect(Collectors.toList());
    }

    private ClassGradeSummaryDTO.GradeSnapshotDTO toGradeSnapshot(Grade grade) {
        return ClassGradeSummaryDTO.GradeSnapshotDTO.builder()
                .gradeId(grade.getId())
                .evaluationId(grade.getEvaluationId())
                .gradeValue(grade.getGradeValue())
                .gradeDate(grade.getGradeDate())
                .academicYear(grade.getAcademicYear())
                .academicSemester(grade.getAcademicSemester())
                .build();
    }

    private BigDecimal calculateAverage(List<Grade> grades) {
        if (grades == null || grades.isEmpty()) {
            return null;
        }

        BigDecimal total = grades.stream()
                .map(Grade::getGradeValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) == 0 && grades.stream().allMatch(g -> g.getGradeValue() == null)) {
            return null;
        }

        int divisor = (int) grades.stream().map(Grade::getGradeValue).filter(Objects::nonNull).count();
        if (divisor == 0) {
            return null;
        }

        return total.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    private int normalizeMaxGradesLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 3);
    }

    private ClassInfoDTO validateClassAndStudent(Long classId, Long studentId) {
        ClassInfoDTO classInfo = fetchClassInfo(classId);
        validateStudentBelongsToClass(studentId, classInfo);
        return classInfo;
    }

    private ClassInfoDTO fetchClassInfo(Long classId) {
        try {
            ApiResponse<ClassInfoDTO> response = classServiceClient.getClassById(classId);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BusinessException("Turma não encontrada com ID: " + classId);
            }
            return response.getData();
        } catch (FeignException.NotFound e) {
            log.warn("Turma não encontrada - ID: {}", classId);
            throw new BusinessException("Turma não encontrada com ID: " + classId);
        } catch (FeignException e) {
            log.error("Erro ao validar turma - ID: {}, Erro: {}", classId, e.getMessage());
            throw new BusinessException("Erro ao validar turma. Tente novamente mais tarde.");
        } catch (Exception e) {
            log.error("Erro inesperado ao validar turma - ID: {}", classId, e);
            throw new BusinessException("Erro ao validar turma: " + e.getMessage());
        }
    }

    private void validateStudentBelongsToClass(Long studentId, ClassInfoDTO classInfo) {
        List<Long> studentIds = classInfo.getStudentIds();
        if (studentId == null) {
            throw new BusinessException("ID do aluno é obrigatório");
        }
        if (studentIds != null && !studentIds.isEmpty() && !studentIds.contains(studentId)) {
            throw new BusinessException(String.format("Aluno %d não pertence à turma %d", studentId, classInfo.getId()));
        }
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
        if (request.getClassId() == null) {
            throw new BusinessException("ID da turma é obrigatório");
        }
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


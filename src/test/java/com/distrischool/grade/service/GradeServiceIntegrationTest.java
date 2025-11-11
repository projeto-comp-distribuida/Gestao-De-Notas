package com.distrischool.grade.service;

import com.distrischool.grade.dto.ApiResponse;
import com.distrischool.grade.dto.GradeRequestDTO;
import com.distrischool.grade.dto.GradeResponseDTO;
import com.distrischool.grade.entity.Grade;
import com.distrischool.grade.feign.StudentServiceClient;
import com.distrischool.grade.feign.TeacherServiceClient;
import com.distrischool.grade.kafka.EventProducer;
import com.distrischool.grade.repository.GradeRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes de integração para GradeService
 * Valida enriquecimento de dados via Feign Clients
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GradeService - Integration Tests")
class GradeServiceIntegrationTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private EventProducer eventProducer;

    @Mock
    private StudentServiceClient studentServiceClient;

    @Mock
    private TeacherServiceClient teacherServiceClient;

    @InjectMocks
    private GradeService gradeService;

    private Grade grade;
    private GradeRequestDTO gradeRequestDTO;
    private Map<String, Object> studentData;
    private Map<String, Object> teacherData;

    @BeforeEach
    void setUp() {
        // Configurar tópicos Kafka
        ReflectionTestUtils.setField(gradeService, "gradeCreatedTopic", "test.grade.created");
        ReflectionTestUtils.setField(gradeService, "gradeUpdatedTopic", "test.grade.updated");
        ReflectionTestUtils.setField(gradeService, "gradeDeletedTopic", "test.grade.deleted");

        // Dados do estudante (simulando resposta do Student Service)
        studentData = new HashMap<>();
        studentData.put("id", 1L);
        studentData.put("fullName", "João Silva");
        studentData.put("email", "joao@test.com");
        studentData.put("registrationNumber", "2024001");
        studentData.put("course", "Ciência da Computação");

        // Dados do professor (simulando resposta do Teacher Service)
        teacherData = new HashMap<>();
        teacherData.put("id", 1L);
        teacherData.put("name", "Professor Teste");
        teacherData.put("email", "prof@test.com");
        teacherData.put("employeeId", "PROF-001");

        // Grade entity
        grade = Grade.builder()
                .id(1L)
                .studentId(1L)
                .teacherId(1L)
                .evaluationId(1L)
                .gradeValue(new BigDecimal("8.5"))
                .gradeDate(LocalDate.now())
                .academicYear(2024)
                .academicSemester(2)
                .status(Grade.GradeStatus.CONFIRMED)
                .build();

        // GradeRequestDTO
        gradeRequestDTO = GradeRequestDTO.builder()
                .studentId(1L)
                .teacherId(1L)
                .evaluationId(1L)
                .gradeValue(new BigDecimal("8.5"))
                .gradeDate(LocalDate.now())
                .academicYear(2024)
                .academicSemester(2)
                .build();
    }

    @Test
    @DisplayName("Deve enriquecer GradeResponseDTO com dados de Student e Teacher")
    void shouldEnrichGradeResponseWithStudentAndTeacherData() {
        // Arrange
        when(gradeRepository.save(any(Grade.class))).thenReturn(grade);
        when(studentServiceClient.getStudentById(1L))
                .thenReturn(ApiResponse.success(studentData));
        when(teacherServiceClient.getTeacherById(1L))
                .thenReturn(ApiResponse.success(teacherData));

        // Act
        GradeResponseDTO result = gradeService.createGrade(gradeRequestDTO, "user-123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStudentId()).isEqualTo(1L);
        assertThat(result.getTeacherId()).isEqualTo(1L);

        // Verificar enriquecimento de Student
        assertThat(result.getStudent()).isNotNull();
        assertThat(result.getStudent().getId()).isEqualTo(1L);
        assertThat(result.getStudent().getFullName()).isEqualTo("João Silva");
        assertThat(result.getStudent().getEmail()).isEqualTo("joao@test.com");
        assertThat(result.getStudent().getRegistrationNumber()).isEqualTo("2024001");
        assertThat(result.getStudent().getCourse()).isEqualTo("Ciência da Computação");

        // Verificar enriquecimento de Teacher
        assertThat(result.getTeacher()).isNotNull();
        assertThat(result.getTeacher().getId()).isEqualTo(1L);
        assertThat(result.getTeacher().getName()).isEqualTo("Professor Teste");
        assertThat(result.getTeacher().getEmail()).isEqualTo("prof@test.com");
        assertThat(result.getTeacher().getEmployeeId()).isEqualTo("PROF-001");

        // Verificar que Feign Clients foram chamados
        verify(studentServiceClient, times(1)).getStudentById(1L);
        verify(teacherServiceClient, times(1)).getTeacherById(1L);
    }

    @Test
    @DisplayName("Deve criar nota mesmo se Student Service não responder")
    void shouldCreateGradeEvenIfStudentServiceFails() {
        // Arrange
        when(gradeRepository.save(any(Grade.class))).thenReturn(grade);
        when(studentServiceClient.getStudentById(1L))
                .thenThrow(new FeignException.NotFound("Student not found", null, null, null));
        when(teacherServiceClient.getTeacherById(1L))
                .thenReturn(ApiResponse.success(teacherData));

        // Act
        GradeResponseDTO result = gradeService.createGrade(gradeRequestDTO, "user-123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStudentId()).isEqualTo(1L);
        
        // Student não deve estar enriquecido (serviço falhou)
        assertThat(result.getStudent()).isNull();
        
        // Teacher deve estar enriquecido
        assertThat(result.getTeacher()).isNotNull();
        assertThat(result.getTeacher().getName()).isEqualTo("Professor Teste");
    }

    @Test
    @DisplayName("Deve criar nota mesmo se Teacher Service não responder")
    void shouldCreateGradeEvenIfTeacherServiceFails() {
        // Arrange
        when(gradeRepository.save(any(Grade.class))).thenReturn(grade);
        when(studentServiceClient.getStudentById(1L))
                .thenReturn(ApiResponse.success(studentData));
        when(teacherServiceClient.getTeacherById(1L))
                .thenThrow(new FeignException.NotFound("Teacher not found", null, null, null));

        // Act
        GradeResponseDTO result = gradeService.createGrade(gradeRequestDTO, "user-123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTeacherId()).isEqualTo(1L);
        
        // Teacher não deve estar enriquecido (serviço falhou)
        assertThat(result.getTeacher()).isNull();
        
        // Student deve estar enriquecido
        assertThat(result.getStudent()).isNotNull();
        assertThat(result.getStudent().getFullName()).isEqualTo("João Silva");
    }

    @Test
    @DisplayName("Deve criar nota mesmo se ambos os serviços falharem")
    void shouldCreateGradeEvenIfBothServicesFail() {
        // Arrange
        when(gradeRepository.save(any(Grade.class))).thenReturn(grade);
        when(studentServiceClient.getStudentById(1L))
                .thenThrow(new FeignException.NotFound("Student not found", null, null, null));
        when(teacherServiceClient.getTeacherById(1L))
                .thenThrow(new FeignException.NotFound("Teacher not found", null, null, null));

        // Act
        GradeResponseDTO result = gradeService.createGrade(gradeRequestDTO, "user-123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStudentId()).isEqualTo(1L);
        assertThat(result.getTeacherId()).isEqualTo(1L);
        
        // Nenhum dado enriquecido, mas nota foi criada
        assertThat(result.getStudent()).isNull();
        assertThat(result.getTeacher()).isNull();
    }

    @Test
    @DisplayName("Deve enriquecer ao buscar nota por ID")
    void shouldEnrichWhenGettingGradeById() {
        // Arrange
        when(gradeRepository.findById(1L)).thenReturn(Optional.of(grade));
        when(studentServiceClient.getStudentById(1L))
                .thenReturn(ApiResponse.success(studentData));
        when(teacherServiceClient.getTeacherById(1L))
                .thenReturn(ApiResponse.success(teacherData));

        // Act
        GradeResponseDTO result = gradeService.getGradeById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStudent()).isNotNull();
        assertThat(result.getTeacher()).isNotNull();
        
        verify(studentServiceClient, times(1)).getStudentById(1L);
        verify(teacherServiceClient, times(1)).getTeacherById(1L);
    }

    @Test
    @DisplayName("Deve tratar resposta vazia do Student Service")
    void shouldHandleEmptyStudentServiceResponse() {
        // Arrange
        when(gradeRepository.save(any(Grade.class))).thenReturn(grade);
        when(studentServiceClient.getStudentById(1L))
                .thenReturn(ApiResponse.success(null));
        when(teacherServiceClient.getTeacherById(1L))
                .thenReturn(ApiResponse.success(teacherData));

        // Act
        GradeResponseDTO result = gradeService.createGrade(gradeRequestDTO, "user-123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStudent()).isNull();
        assertThat(result.getTeacher()).isNotNull();
    }
}


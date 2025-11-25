package com.distrischool.grade.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade Grade (Nota) para o sistema de gestão de notas.
 * Representa uma nota atribuída a um aluno em uma avaliação.
 */
@Entity
@Table(name = "grades", indexes = {
    @Index(name = "idx_grade_student_id", columnList = "student_id"),
    @Index(name = "idx_grade_evaluation_id", columnList = "evaluation_id"),
    @Index(name = "idx_grade_teacher_id", columnList = "teacher_id"),
    @Index(name = "idx_grade_date", columnList = "grade_date"),
    @Index(name = "idx_grade_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Grade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID do aluno (referência externa ao microserviço de alunos)
     */
    @NotNull(message = "ID do aluno é obrigatório")
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * ID do professor (referência externa ao microserviço de professores)
     */
    @NotNull(message = "ID do professor é obrigatório")
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /**
     * ID da turma (integração com microserviço de classes/horários)
     */
    @NotNull(message = "ID da turma é obrigatório")
    @Column(name = "class_id")
    private Long classId;

    /**
     * ID da avaliação (referência à entidade Evaluation)
     */
    @NotNull(message = "ID da avaliação é obrigatório")
    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    /**
     * Valor da nota
     */
    @NotNull(message = "Valor da nota é obrigatório")
    @DecimalMin(value = "0.0", message = "Nota deve ser maior ou igual a 0")
    @DecimalMax(value = "10.0", message = "Nota deve ser menor ou igual a 10")
    @Digits(integer = 2, fraction = 2, message = "Nota deve ter no máximo 2 casas decimais")
    @Column(name = "grade_value", nullable = false, precision = 4, scale = 2)
    private BigDecimal gradeValue;

    /**
     * Data da avaliação/nota
     */
    @NotNull(message = "Data da avaliação é obrigatória")
    @Column(name = "grade_date", nullable = false)
    private LocalDate gradeDate;

    /**
     * Observações sobre a nota
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Status da nota
     */
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private GradeStatus status = GradeStatus.REGISTERED;

    /**
     * Se a nota foi lançada automaticamente ou manualmente
     */
    @Column(name = "is_automatic", nullable = false)
    @Builder.Default
    private Boolean isAutomatic = false;

    /**
     * Data de lançamento da nota
     */
    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    /**
     * Ano letivo
     */
    @NotNull(message = "Ano letivo é obrigatório")
    @Min(value = 2000, message = "Ano letivo deve ser a partir de 2000")
    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    /**
     * Semestre letivo (1 ou 2)
     */
    @NotNull(message = "Semestre letivo é obrigatório")
    @Min(value = 1, message = "Semestre deve ser 1 ou 2")
    @Max(value = 2, message = "Semestre deve ser 1 ou 2")
    @Column(name = "academic_semester", nullable = false)
    private Integer academicSemester;

    public enum GradeStatus {
        REGISTERED,   // Nota registrada
        PENDING,      // Aguardando confirmação
        CONFIRMED,    // Confirmada
        DISPUTED,     // Em disputa/recurso
        CANCELLED     // Cancelada
    }

    /**
     * Verifica se a nota está aprovada (>= 7.0)
     */
    public boolean isApproved() {
        return gradeValue != null && gradeValue.compareTo(new BigDecimal("7.0")) >= 0;
    }

    /**
     * Verifica se a nota está em recuperação (>= 5.0 e < 7.0)
     */
    public boolean isRecovery() {
        return gradeValue != null 
            && gradeValue.compareTo(new BigDecimal("5.0")) >= 0 
            && gradeValue.compareTo(new BigDecimal("7.0")) < 0;
    }

    /**
     * Verifica se a nota está reprovada (< 5.0)
     */
    public boolean isFailed() {
        return gradeValue != null && gradeValue.compareTo(new BigDecimal("5.0")) < 0;
    }

    /**
     * Marca a nota como confirmada
     */
    public void confirm() {
        this.status = GradeStatus.CONFIRMED;
        if (this.postedAt == null) {
            this.postedAt = LocalDateTime.now();
        }
    }
}


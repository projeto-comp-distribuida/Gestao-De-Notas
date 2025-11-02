package com.distrischool.grade.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade Assessment (Avaliação Consolidada) para o sistema de gestão de notas.
 * Representa a avaliação final de um aluno em uma disciplina, consolidando todas as notas.
 */
@Entity
@Table(name = "assessments", indexes = {
    @Index(name = "idx_assessment_student_id", columnList = "student_id"),
    @Index(name = "idx_assessment_subject_id", columnList = "subject_id"),
    @Index(name = "idx_assessment_academic_year", columnList = "academic_year, academic_semester"),
    @Index(name = "idx_assessment_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Assessment extends BaseEntity {

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
     * ID da disciplina (referência externa)
     */
    @NotNull(message = "ID da disciplina é obrigatório")
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    /**
     * ID do professor responsável (referência externa ao microserviço de professores)
     */
    @NotNull(message = "ID do professor é obrigatório")
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /**
     * Nota final calculada
     */
    @DecimalMin(value = "0.0", message = "Nota final deve ser maior ou igual a 0")
    @DecimalMax(value = "10.0", message = "Nota final deve ser menor ou igual a 10")
    @Column(name = "final_grade", precision = 4, scale = 2)
    private BigDecimal finalGrade;

    /**
     * Nota de recuperação (se houver)
     */
    @DecimalMin(value = "0.0", message = "Nota de recuperação deve ser maior ou igual a 0")
    @DecimalMax(value = "10.0", message = "Nota de recuperação deve ser menor ou igual a 10")
    @Column(name = "recovery_grade", precision = 4, scale = 2)
    private BigDecimal recoveryGrade;

    /**
     * Nota final após recuperação
     */
    @DecimalMin(value = "0.0", message = "Nota final após recuperação deve ser maior ou igual a 0")
    @DecimalMax(value = "10.0", message = "Nota final após recuperação deve ser menor ou igual a 10")
    @Column(name = "final_grade_after_recovery", precision = 4, scale = 2)
    private BigDecimal finalGradeAfterRecovery;

    /**
     * Status da avaliação
     */
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AssessmentStatus status = AssessmentStatus.IN_PROGRESS;

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

    /**
     * Data de conclusão da avaliação
     */
    @Column(name = "completion_date")
    private LocalDate completionDate;

    /**
     * Observações
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Frequência do aluno (percentual)
     */
    @DecimalMin(value = "0.0", message = "Frequência deve ser maior ou igual a 0")
    @DecimalMax(value = "100.0", message = "Frequência deve ser menor ou igual a 100")
    @Column(name = "attendance_percentage", precision = 5, scale = 2)
    private BigDecimal attendancePercentage;

    /**
     * Número de faltas
     */
    @Column(name = "absences")
    @Builder.Default
    private Integer absences = 0;

    public enum AssessmentStatus {
        IN_PROGRESS,    // Em andamento
        APPROVED,       // Aprovado
        RECOVERY,       // Em recuperação
        FAILED,         // Reprovado
        FINALIZED       // Finalizado
    }

    /**
     * Verifica se o aluno foi aprovado
     */
    public boolean isApproved() {
        BigDecimal grade = finalGradeAfterRecovery != null ? finalGradeAfterRecovery : finalGrade;
        return grade != null && grade.compareTo(new BigDecimal("7.0")) >= 0;
    }

    /**
     * Verifica se o aluno está em recuperação
     */
    public boolean isInRecovery() {
        BigDecimal grade = finalGrade;
        return grade != null 
            && grade.compareTo(new BigDecimal("5.0")) >= 0 
            && grade.compareTo(new BigDecimal("7.0")) < 0;
    }

    /**
     * Verifica se o aluno foi reprovado
     */
    public boolean isFailed() {
        BigDecimal grade = finalGradeAfterRecovery != null ? finalGradeAfterRecovery : finalGrade;
        return grade != null && grade.compareTo(new BigDecimal("5.0")) < 0;
    }

    /**
     * Calcula e atualiza a nota final
     */
    public void calculateFinalGrade() {
        // Esta lógica será implementada no service que calculará baseado nas notas das avaliações
        // Aqui é apenas um placeholder
        if (finalGradeAfterRecovery != null) {
            if (finalGradeAfterRecovery.compareTo(new BigDecimal("7.0")) >= 0) {
                this.status = AssessmentStatus.APPROVED;
            } else {
                this.status = AssessmentStatus.FAILED;
            }
        } else if (finalGrade != null) {
            if (finalGrade.compareTo(new BigDecimal("7.0")) >= 0) {
                this.status = AssessmentStatus.APPROVED;
            } else if (finalGrade.compareTo(new BigDecimal("5.0")) >= 0) {
                this.status = AssessmentStatus.RECOVERY;
            } else {
                this.status = AssessmentStatus.FAILED;
            }
        }
    }

    /**
     * Finaliza a avaliação
     */
    public void finalize() {
        this.status = AssessmentStatus.FINALIZED;
        if (this.completionDate == null) {
            this.completionDate = LocalDate.now();
        }
    }
}


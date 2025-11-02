package com.distrischool.grade.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade Evaluation (Avaliação) para o sistema de gestão de notas.
 * Representa uma avaliação/exame atribuído a uma turma/disciplina.
 */
@Entity
@Table(name = "evaluations", indexes = {
    @Index(name = "idx_evaluation_subject", columnList = "subject_id"),
    @Index(name = "idx_evaluation_teacher_id", columnList = "teacher_id"),
    @Index(name = "idx_evaluation_date", columnList = "evaluation_date"),
    @Index(name = "idx_evaluation_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "grades")
public class Evaluation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome/título da avaliação
     */
    @NotBlank(message = "Nome da avaliação é obrigatório")
    @Size(max = 255, message = "Nome da avaliação deve ter no máximo 255 caracteres")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Descrição da avaliação
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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
     * Data da avaliação
     */
    @NotNull(message = "Data da avaliação é obrigatória")
    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;

    /**
     * Data limite para lançamento de notas
     */
    @Column(name = "grade_deadline")
    private LocalDate gradeDeadline;

    /**
     * Tipo de avaliação
     */
    @NotNull(message = "Tipo de avaliação é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false, length = 50)
    private EvaluationType evaluationType;

    /**
     * Peso da avaliação (ex: 0.2 para 20% da nota final)
     */
    @NotNull(message = "Peso da avaliação é obrigatório")
    @DecimalMin(value = "0.0", message = "Peso deve ser maior ou igual a 0")
    @DecimalMax(value = "1.0", message = "Peso deve ser menor ou igual a 1")
    @Column(name = "weight", nullable = false, precision = 3, scale = 2)
    private Double weight;

    /**
     * Nota máxima possível
     */
    @NotNull(message = "Nota máxima é obrigatória")
    @Min(value = 1, message = "Nota máxima deve ser pelo menos 1")
    @Column(name = "max_score", nullable = false)
    @Builder.Default
    private Integer maxScore = 10;

    /**
     * Status da avaliação
     */
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EvaluationStatus status = EvaluationStatus.SCHEDULED;

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
     * Turma/disciplina (referência opcional)
     */
    @Column(name = "class_group_id")
    private Long classGroupId;

    /**
     * Lista de notas associadas a esta avaliação
     */
    @OneToMany(mappedBy = "evaluationId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Grade> grades = new ArrayList<>();

    public enum EvaluationType {
        EXAM,              // Prova/Exame
        QUIZ,              // Quiz
        PROJECT,           // Projeto
        ASSIGNMENT,        // Trabalho
        PRESENTATION,      // Apresentação
        LABORATORY,        // Laboratório
        FINAL_EXAM,        // Prova Final
        RECOVERY,          // Recuperação
        EXTRA_ACTIVITY     // Atividade Extra
    }

    public enum EvaluationStatus {
        SCHEDULED,     // Agendada
        IN_PROGRESS,   // Em andamento
        COMPLETED,     // Concluída
        GRADED,        // Notas lançadas
        CANCELLED      // Cancelada
    }

    /**
     * Verifica se a avaliação pode receber notas
     */
    public boolean canReceiveGrades() {
        return status == EvaluationStatus.COMPLETED 
            || status == EvaluationStatus.IN_PROGRESS
            || (gradeDeadline != null && LocalDate.now().isBefore(gradeDeadline) || gradeDeadline == null);
    }

    /**
     * Verifica se a data limite para lançamento de notas já passou
     */
    public boolean isGradeDeadlinePassed() {
        return gradeDeadline != null && LocalDate.now().isAfter(gradeDeadline);
    }

    /**
     * Marca a avaliação como concluída
     */
    public void complete() {
        this.status = EvaluationStatus.COMPLETED;
    }

    /**
     * Marca a avaliação como com notas lançadas
     */
    public void markAsGraded() {
        this.status = EvaluationStatus.GRADED;
    }
}


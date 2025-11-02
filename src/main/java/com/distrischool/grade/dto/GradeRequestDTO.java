package com.distrischool.grade.dto;

import com.distrischool.grade.entity.Grade.GradeStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeRequestDTO {

    @NotNull(message = "ID do aluno é obrigatório")
    private Long studentId;

    @NotNull(message = "ID do professor é obrigatório")
    private Long teacherId;

    @NotNull(message = "ID da avaliação é obrigatório")
    private Long evaluationId;

    @NotNull(message = "Valor da nota é obrigatório")
    @DecimalMin(value = "0.0", message = "Nota deve ser maior ou igual a 0")
    @DecimalMax(value = "10.0", message = "Nota deve ser menor ou igual a 10")
    @Digits(integer = 2, fraction = 2, message = "Nota deve ter no máximo 2 casas decimais")
    private BigDecimal gradeValue;

    @NotNull(message = "Data da avaliação é obrigatória")
    private LocalDate gradeDate;

    private String notes;

    private GradeStatus status;

    private Boolean isAutomatic;

    @NotNull(message = "Ano letivo é obrigatório")
    @Min(value = 2000, message = "Ano letivo deve ser a partir de 2000")
    private Integer academicYear;

    @NotNull(message = "Semestre letivo é obrigatório")
    @Min(value = 1, message = "Semestre deve ser 1 ou 2")
    @Max(value = 2, message = "Semestre deve ser 1 ou 2")
    private Integer academicSemester;
}


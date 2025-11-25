package com.distrischool.grade.dto;

import com.distrischool.grade.entity.Grade;
import com.distrischool.grade.entity.Grade.GradeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeResponseDTO {

    private Long id;
    private Long studentId;
    private Long teacherId;
    private Long classId;
    private Long evaluationId;
    private BigDecimal gradeValue;
    private LocalDate gradeDate;
    private String notes;
    private GradeStatus status;
    private Boolean isAutomatic;
    private LocalDateTime postedAt;
    private Integer academicYear;
    private Integer academicSemester;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static GradeResponseDTO fromEntity(Grade grade) {
        return GradeResponseDTO.builder()
                .id(grade.getId())
                .studentId(grade.getStudentId())
                .teacherId(grade.getTeacherId())
                .classId(grade.getClassId())
                .evaluationId(grade.getEvaluationId())
                .gradeValue(grade.getGradeValue())
                .gradeDate(grade.getGradeDate())
                .notes(grade.getNotes())
                .status(grade.getStatus())
                .isAutomatic(grade.getIsAutomatic())
                .postedAt(grade.getPostedAt())
                .academicYear(grade.getAcademicYear())
                .academicSemester(grade.getAcademicSemester())
                .createdAt(grade.getCreatedAt())
                .updatedAt(grade.getUpdatedAt())
                .createdBy(grade.getCreatedBy())
                .updatedBy(grade.getUpdatedBy())
                .build();
    }
}


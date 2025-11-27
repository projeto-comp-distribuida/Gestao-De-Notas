package com.distrischool.grade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa o detalhamento das notas de uma turma.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassGradeSummaryDTO {

    private Long classId;
    private String className;
    private String classCode;
    private String period;
    private String academicYear;
    private Integer totalStudents;
    private Integer studentsWithGrades;
    private Integer maxGradesPerStudent;
    private BigDecimal classAverage;

    @Builder.Default
    private List<StudentClassGradeDTO> students = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentClassGradeDTO {
        private Long studentId;
        private BigDecimal average;

        @Builder.Default
        private List<GradeSnapshotDTO> grades = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeSnapshotDTO {
        private Long gradeId;
        private Long evaluationId;
        private BigDecimal gradeValue;
        private LocalDate gradeDate;
        private Integer academicYear;
        private Integer academicSemester;
    }
}



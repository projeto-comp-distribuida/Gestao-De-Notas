package com.distrischool.grade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO utilizado para consumir dados do microserviço de turmas/horários.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassInfoDTO {

    private Long id;
    private String name;
    private String code;
    private String academicYear;
    private String period;
    private Integer capacity;
    private Integer currentStudents;
    private Long schoolId;
    private String schoolName;
    private Long shiftId;
    private String shiftName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String room;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<Long> studentIds = new ArrayList<>();

    @Builder.Default
    private List<Long> teacherIds = new ArrayList<>();
}


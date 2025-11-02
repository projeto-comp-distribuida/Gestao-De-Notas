package com.distrischool.grade.repository;

import com.distrischool.grade.entity.Evaluation;
import com.distrischool.grade.entity.Evaluation.EvaluationStatus;
import com.distrischool.grade.entity.Evaluation.EvaluationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Page<Evaluation> findBySubjectId(Long subjectId, Pageable pageable);
    Page<Evaluation> findByTeacherId(Long teacherId, Pageable pageable);
    Page<Evaluation> findByStatus(EvaluationStatus status, Pageable pageable);
    
    List<Evaluation> findBySubjectIdAndAcademicYearAndAcademicSemester(
        Long subjectId, Integer academicYear, Integer academicSemester);
    
    List<Evaluation> findByTeacherIdAndAcademicYearAndAcademicSemester(
        Long teacherId, Integer academicYear, Integer academicSemester);
    
    Optional<Evaluation> findBySubjectIdAndEvaluationDateAndEvaluationType(
        Long subjectId, LocalDate evaluationDate, EvaluationType evaluationType);
    
    @Query("SELECT e FROM Evaluation e WHERE e.deletedAt IS NULL " +
           "AND e.academicYear = :academicYear " +
           "AND e.academicSemester = :academicSemester " +
           "AND (:subjectId IS NULL OR e.subjectId = :subjectId) " +
           "AND (:status IS NULL OR e.status = :status)")
    Page<Evaluation> findEvaluations(@Param("academicYear") Integer academicYear,
                                     @Param("academicSemester") Integer academicSemester,
                                     @Param("subjectId") Long subjectId,
                                     @Param("status") EvaluationStatus status,
                                     Pageable pageable);
    
    @Query("SELECT e FROM Evaluation e WHERE e.deletedAt IS NULL " +
           "AND e.gradeDeadline < :today AND e.status != 'GRADED'")
    List<Evaluation> findEvaluationsWithPassedDeadline(@Param("today") LocalDate today);
    
    @Query("SELECT e FROM Evaluation e WHERE e.deletedAt IS NULL")
    Page<Evaluation> findAllNotDeleted(Pageable pageable);
    
    long countBySubjectIdAndAcademicYearAndAcademicSemester(
        Long subjectId, Integer academicYear, Integer academicSemester);
}


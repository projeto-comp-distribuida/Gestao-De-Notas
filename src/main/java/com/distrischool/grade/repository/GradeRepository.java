package com.distrischool.grade.repository;

import com.distrischool.grade.entity.Grade;
import com.distrischool.grade.entity.Grade.GradeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    Page<Grade> findByStudentId(Long studentId, Pageable pageable);
    Page<Grade> findByTeacherId(Long teacherId, Pageable pageable);
    Page<Grade> findByEvaluationId(Long evaluationId, Pageable pageable);
    
    List<Grade> findByStudentIdAndAcademicYearAndAcademicSemester(
        Long studentId, Integer academicYear, Integer academicSemester);
    
    List<Grade> findByEvaluationIdAndStudentId(Long evaluationId, Long studentId);
    
    Optional<Grade> findByStudentIdAndEvaluationId(Long studentId, Long evaluationId);
    
    Page<Grade> findByStudentIdAndSubjectId(
        Long studentId, Long subjectId, Pageable pageable);
    
    Page<Grade> findByStatus(GradeStatus status, Pageable pageable);
    
    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL " +
           "AND g.studentId = :studentId " +
           "AND g.academicYear = :academicYear " +
           "AND g.academicSemester = :academicSemester " +
           "AND (:subjectId IS NULL OR g.evaluationId IN " +
           "(SELECT e.id FROM Evaluation e WHERE e.subjectId = :subjectId))")
    Page<Grade> findStudentGrades(@Param("studentId") Long studentId,
                                  @Param("academicYear") Integer academicYear,
                                  @Param("academicSemester") Integer academicSemester,
                                  @Param("subjectId") Long subjectId,
                                  Pageable pageable);
    
    @Query("SELECT AVG(g.gradeValue) FROM Grade g WHERE g.studentId = :studentId " +
           "AND g.academicYear = :academicYear AND g.academicSemester = :academicSemester " +
           "AND g.deletedAt IS NULL AND g.status = 'CONFIRMED'")
    BigDecimal calculateAverageGrade(@Param("studentId") Long studentId,
                                     @Param("academicYear") Integer academicYear,
                                     @Param("academicSemester") Integer academicSemester);
    
    long countByStudentIdAndAcademicYearAndAcademicSemester(
        Long studentId, Integer academicYear, Integer academicSemester);
    
    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL")
    Page<Grade> findAllNotDeleted(Pageable pageable);

    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL AND g.classId = :classId " +
           "AND (:academicYear IS NULL OR g.academicYear = :academicYear) " +
           "AND (:academicSemester IS NULL OR g.academicSemester = :academicSemester)")
    List<Grade> findClassGrades(@Param("classId") Long classId,
                                @Param("academicYear") Integer academicYear,
                                @Param("academicSemester") Integer academicSemester);

    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL AND g.classId IS NOT NULL " +
           "AND (:academicYear IS NULL OR g.academicYear = :academicYear) " +
           "AND (:academicSemester IS NULL OR g.academicSemester = :academicSemester)")
    List<Grade> findAllClassGrades(@Param("academicYear") Integer academicYear,
                                   @Param("academicSemester") Integer academicSemester);
}


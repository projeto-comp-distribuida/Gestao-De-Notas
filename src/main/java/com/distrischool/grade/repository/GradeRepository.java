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

    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL AND g.studentId = :studentId")
    Page<Grade> findByStudentId(@Param("studentId") Long studentId, Pageable pageable);
    
    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL AND g.teacherId = :teacherId")
    Page<Grade> findByTeacherId(@Param("teacherId") Long teacherId, Pageable pageable);
    
    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL AND g.evaluationId = :evaluationId")
    Page<Grade> findByEvaluationId(@Param("evaluationId") Long evaluationId, Pageable pageable);
    
    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL " +
           "AND g.studentId = :studentId " +
           "AND g.academicYear = :academicYear " +
           "AND g.academicSemester = :academicSemester")
    List<Grade> findByStudentIdAndAcademicYearAndAcademicSemester(
        @Param("studentId") Long studentId, 
        @Param("academicYear") Integer academicYear, 
        @Param("academicSemester") Integer academicSemester);
    
    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL " +
           "AND g.evaluationId = :evaluationId AND g.studentId = :studentId")
    List<Grade> findByEvaluationIdAndStudentId(
        @Param("evaluationId") Long evaluationId, 
        @Param("studentId") Long studentId);
    
    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL " +
           "AND g.studentId = :studentId AND g.evaluationId = :evaluationId")
    Optional<Grade> findByStudentIdAndEvaluationId(
        @Param("studentId") Long studentId, 
        @Param("evaluationId") Long evaluationId);
    
    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL " +
           "AND g.studentId = :studentId " +
           "AND g.evaluationId IN " +
           "(SELECT e.id FROM Evaluation e WHERE e.deletedAt IS NULL AND e.subjectId = :subjectId)")
    Page<Grade> findGradesByStudentAndSubject(
        @Param("studentId") Long studentId, 
        @Param("subjectId") Long subjectId, 
        Pageable pageable);
    
    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL AND g.status = :status")
    Page<Grade> findByStatus(@Param("status") GradeStatus status, Pageable pageable);
    
    @Query("SELECT g FROM Grade g WHERE g.deletedAt IS NULL " +
           "AND g.studentId = :studentId " +
           "AND g.academicYear = :academicYear " +
           "AND g.academicSemester = :academicSemester " +
           "AND (:subjectId IS NULL OR g.evaluationId IN " +
           "(SELECT e.id FROM Evaluation e WHERE e.deletedAt IS NULL AND e.subjectId = :subjectId))")
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
    
    @Query("SELECT COUNT(g) FROM Grade g WHERE g.deletedAt IS NULL " +
           "AND g.studentId = :studentId " +
           "AND g.academicYear = :academicYear " +
           "AND g.academicSemester = :academicSemester")
    long countByStudentIdAndAcademicYearAndAcademicSemester(
        @Param("studentId") Long studentId, 
        @Param("academicYear") Integer academicYear, 
        @Param("academicSemester") Integer academicSemester);
    
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


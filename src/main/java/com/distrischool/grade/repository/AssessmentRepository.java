package com.distrischool.grade.repository;

import com.distrischool.grade.entity.Assessment;
import com.distrischool.grade.entity.Assessment.AssessmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    Page<Assessment> findByStudentId(Long studentId, Pageable pageable);
    Page<Assessment> findBySubjectId(Long subjectId, Pageable pageable);
    Page<Assessment> findByTeacherId(Long teacherId, Pageable pageable);
    Page<Assessment> findByStatus(AssessmentStatus status, Pageable pageable);
    
    List<Assessment> findByStudentIdAndAcademicYearAndAcademicSemester(
        Long studentId, Integer academicYear, Integer academicSemester);
    
    Optional<Assessment> findByStudentIdAndSubjectIdAndAcademicYearAndAcademicSemester(
        Long studentId, Long subjectId, Integer academicYear, Integer academicSemester);
    
    @Query("SELECT a FROM Assessment a WHERE a.deletedAt IS NULL " +
           "AND a.studentId = :studentId " +
           "AND a.academicYear = :academicYear " +
           "AND a.academicSemester = :academicSemester")
    List<Assessment> findStudentAssessments(@Param("studentId") Long studentId,
                                           @Param("academicYear") Integer academicYear,
                                           @Param("academicSemester") Integer academicSemester);
    
    @Query("SELECT a FROM Assessment a WHERE a.deletedAt IS NULL " +
           "AND a.subjectId = :subjectId " +
           "AND a.academicYear = :academicYear " +
           "AND a.academicSemester = :academicSemester")
    List<Assessment> findSubjectAssessments(@Param("subjectId") Long subjectId,
                                          @Param("academicYear") Integer academicYear,
                                          @Param("academicSemester") Integer academicSemester);
    
    @Query("SELECT a FROM Assessment a WHERE a.deletedAt IS NULL")
    Page<Assessment> findAllNotDeleted(Pageable pageable);
    
    long countByStudentIdAndAcademicYearAndAcademicSemester(
        Long studentId, Integer academicYear, Integer academicSemester);
}


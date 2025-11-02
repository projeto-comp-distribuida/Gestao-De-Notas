-- Migration para criar a tabela de avaliações consolidadas (assessments)
-- Versão 3 - Sistema de Gerenciamento de Assessments

CREATE TABLE assessments (
    id BIGSERIAL PRIMARY KEY,

    -- Referências
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,

    -- Notas
    final_grade DECIMAL(4,2) CHECK (final_grade >= 0 AND final_grade <= 10),
    recovery_grade DECIMAL(4,2) CHECK (recovery_grade >= 0 AND recovery_grade <= 10),
    final_grade_after_recovery DECIMAL(4,2) CHECK (final_grade_after_recovery >= 0 AND final_grade_after_recovery <= 10),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',

    -- Período letivo
    academic_year INTEGER NOT NULL CHECK (academic_year >= 2000),
    academic_semester INTEGER NOT NULL CHECK (academic_semester IN (1, 2)),

    -- Frequência
    attendance_percentage DECIMAL(5,2) CHECK (attendance_percentage >= 0 AND attendance_percentage <= 100),
    absences INTEGER DEFAULT 0,

    -- Datas
    completion_date DATE,

    -- Observações
    notes TEXT,

    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    -- Soft Delete
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),

    -- Constraint único para um aluno em uma disciplina no mesmo período
    UNIQUE(student_id, subject_id, academic_year, academic_semester)
);

-- Índices para performance
CREATE INDEX idx_assessment_student_id ON assessments(student_id);
CREATE INDEX idx_assessment_subject_id ON assessments(subject_id);
CREATE INDEX idx_assessment_teacher_id ON assessments(teacher_id);
CREATE INDEX idx_assessment_academic_year ON assessments(academic_year, academic_semester);
CREATE INDEX idx_assessment_status ON assessments(status);
CREATE INDEX idx_assessment_deleted_at ON assessments(deleted_at);

-- Comentários
COMMENT ON TABLE assessments IS 'Tabela de avaliações consolidadas (nota final) dos alunos em disciplinas';
COMMENT ON COLUMN assessments.status IS 'Status: IN_PROGRESS, APPROVED, RECOVERY, FAILED, FINALIZED';
COMMENT ON COLUMN assessments.final_grade IS 'Nota final calculada baseada nas avaliações';
COMMENT ON COLUMN assessments.recovery_grade IS 'Nota de recuperação (se houver)';
COMMENT ON COLUMN assessments.final_grade_after_recovery IS 'Nota final após recuperação';
COMMENT ON COLUMN assessments.attendance_percentage IS 'Percentual de frequência do aluno';
COMMENT ON COLUMN assessments.deleted_at IS 'Data de exclusão lógica (soft delete)';


-- Migration para criar a tabela de notas (grades)
-- Versão 1 - Sistema de Gerenciamento de Notas

CREATE TABLE grades (
    id BIGSERIAL PRIMARY KEY,

    -- Referências
    student_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    evaluation_id BIGINT NOT NULL,

    -- Dados da nota
    grade_value DECIMAL(4,2) NOT NULL CHECK (grade_value >= 0 AND grade_value <= 10),
    grade_date DATE NOT NULL,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    is_automatic BOOLEAN NOT NULL DEFAULT FALSE,
    posted_at TIMESTAMP,

    -- Período letivo
    academic_year INTEGER NOT NULL CHECK (academic_year >= 2000),
    academic_semester INTEGER NOT NULL CHECK (academic_semester IN (1, 2)),

    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    -- Soft Delete
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255)
);

-- Índices para performance
CREATE INDEX idx_grade_student_id ON grades(student_id);
CREATE INDEX idx_grade_evaluation_id ON grades(evaluation_id);
CREATE INDEX idx_grade_teacher_id ON grades(teacher_id);
CREATE INDEX idx_grade_date ON grades(grade_date);
CREATE INDEX idx_grade_status ON grades(status);
CREATE INDEX idx_grade_academic_year_semester ON grades(academic_year, academic_semester);
CREATE INDEX idx_grade_deleted_at ON grades(deleted_at);

-- Comentários
COMMENT ON TABLE grades IS 'Tabela de notas dos alunos';
COMMENT ON COLUMN grades.status IS 'Status da nota: REGISTERED, PENDING, CONFIRMED, DISPUTED, CANCELLED';
COMMENT ON COLUMN grades.grade_value IS 'Valor da nota (0 a 10)';
COMMENT ON COLUMN grades.deleted_at IS 'Data de exclusão lógica (soft delete)';


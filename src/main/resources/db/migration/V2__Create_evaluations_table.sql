-- Migration para criar a tabela de avaliações (evaluations)
-- Versão 2 - Sistema de Gerenciamento de Avaliações

CREATE TABLE evaluations (
    id BIGSERIAL PRIMARY KEY,

    -- Dados da avaliação
    name VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Referências
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    class_group_id BIGINT,

    -- Datas
    evaluation_date DATE NOT NULL,
    grade_deadline DATE,

    -- Tipo e peso
    evaluation_type VARCHAR(50) NOT NULL,
    weight DECIMAL(3,2) NOT NULL CHECK (weight >= 0 AND weight <= 1),
    max_score INTEGER NOT NULL DEFAULT 10 CHECK (max_score >= 1),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',

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
CREATE INDEX idx_evaluation_subject ON evaluations(subject_id);
CREATE INDEX idx_evaluation_teacher_id ON evaluations(teacher_id);
CREATE INDEX idx_evaluation_date ON evaluations(evaluation_date);
CREATE INDEX idx_evaluation_status ON evaluations(status);
CREATE INDEX idx_evaluation_academic_year_semester ON evaluations(academic_year, academic_semester);
CREATE INDEX idx_evaluation_deleted_at ON evaluations(deleted_at);

-- Comentários
COMMENT ON TABLE evaluations IS 'Tabela de avaliações/exames';
COMMENT ON COLUMN evaluations.status IS 'Status: SCHEDULED, IN_PROGRESS, COMPLETED, GRADED, CANCELLED';
COMMENT ON COLUMN evaluations.evaluation_type IS 'Tipo: EXAM, QUIZ, PROJECT, ASSIGNMENT, PRESENTATION, LABORATORY, FINAL_EXAM, RECOVERY, EXTRA_ACTIVITY';
COMMENT ON COLUMN evaluations.weight IS 'Peso da avaliação na nota final (0 a 1)';
COMMENT ON COLUMN evaluations.deleted_at IS 'Data de exclusão lógica (soft delete)';


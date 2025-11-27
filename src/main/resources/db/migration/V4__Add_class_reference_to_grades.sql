-- Migration para adicionar referência de turma às notas

ALTER TABLE grades
    ADD COLUMN IF NOT EXISTS class_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_grade_class_id ON grades(class_id);

COMMENT ON COLUMN grades.class_id IS 'Identificador da turma (class) associada à nota';



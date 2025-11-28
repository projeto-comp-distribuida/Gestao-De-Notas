-- Script de inicialização para criar todos os bancos de dados necessários
-- Este script é executado automaticamente quando o container PostgreSQL é criado pela primeira vez
-- 
-- IMPORTANTE: Se os bancos não foram criados (ex: volume já existia), execute:
--   ./scripts/init-databases.sh
-- Ou manualmente:
--   docker exec -i postgres-distrischool psql -U distrischool -d postgres < database/init/01-init-databases.sql

-- Criar extensões úteis no banco padrão
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Criar banco de dados para Auth
-- Nota: Se o banco já existir, este comando falhará, mas isso é esperado
-- O script só roda na primeira inicialização do container
CREATE DATABASE distrischool_auth;

-- Criar banco de dados para Gestão de Alunos
CREATE DATABASE distrischool_students;

-- Criar banco de dados para Gestão de Professores
CREATE DATABASE distrischool_teachers;

-- Criar banco de dados para Gestão de Turmas
CREATE DATABASE distrischool_schedules;

-- Criar banco de dados para Gestão de Notas
CREATE DATABASE distrischool_grades;

-- Conceder todas as permissões ao usuário distrischool em todos os bancos
GRANT ALL PRIVILEGES ON DATABASE distrischool_auth TO distrischool;
GRANT ALL PRIVILEGES ON DATABASE distrischool_students TO distrischool;
GRANT ALL PRIVILEGES ON DATABASE distrischool_teachers TO distrischool;
GRANT ALL PRIVILEGES ON DATABASE distrischool_schedules TO distrischool;
GRANT ALL PRIVILEGES ON DATABASE distrischool_grades TO distrischool;

-- Configurar timezone padrão
ALTER DATABASE distrischool_auth SET timezone = 'UTC';
ALTER DATABASE distrischool_students SET timezone = 'UTC';
ALTER DATABASE distrischool_teachers SET timezone = 'UTC';
ALTER DATABASE distrischool_schedules SET timezone = 'UTC';
ALTER DATABASE distrischool_grades SET timezone = 'UTC';

-- Log de inicialização
DO $$
BEGIN
    RAISE NOTICE 'DistriSchool databases initialized successfully!';
    RAISE NOTICE 'Created databases: distrischool_auth, distrischool_students, distrischool_teachers, distrischool_schedules, distrischool_grades';
END $$;


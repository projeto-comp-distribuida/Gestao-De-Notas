#!/bin/bash
# Script auxiliar para inicializar/criar bancos de dados do DistriSchool
# Uso: ./scripts/init-databases.sh

set -e

echo "=========================================="
echo "DistriSchool - Inicialização de Bancos de Dados"
echo "=========================================="
echo ""

# Verificar se o container PostgreSQL está rodando
if ! docker ps | grep -q postgres-distrischool; then
    echo "❌ Erro: Container PostgreSQL não está rodando!"
    echo "   Execute: docker-compose up -d postgres"
    exit 1
fi

echo "✓ Container PostgreSQL está rodando"
echo ""

# Executar script de criação de bancos
echo "Criando bancos de dados..."
docker exec -i postgres-distrischool psql -U distrischool -d postgres <<EOF
-- Criar bancos de dados se não existirem
SELECT 'CREATE DATABASE distrischool_auth' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'distrischool_auth')\gexec
SELECT 'CREATE DATABASE distrischool_students' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'distrischool_students')\gexec
SELECT 'CREATE DATABASE distrischool_teachers' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'distrischool_teachers')\gexec
SELECT 'CREATE DATABASE distrischool_schedules' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'distrischool_schedules')\gexec
SELECT 'CREATE DATABASE distrischool_grades' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'distrischool_grades')\gexec

-- Conceder permissões
GRANT ALL PRIVILEGES ON DATABASE distrischool_auth TO distrischool;
GRANT ALL PRIVILEGES ON DATABASE distrischool_students TO distrischool;
GRANT ALL PRIVILEGES ON DATABASE distrischool_teachers TO distrischool;
GRANT ALL PRIVILEGES ON DATABASE distrischool_schedules TO distrischool;
GRANT ALL PRIVILEGES ON DATABASE distrischool_grades TO distrischool;
EOF

echo ""
echo "Configurando extensões nos bancos de dados..."

# Criar extensões em cada banco
for db in distrischool_auth distrischool_students distrischool_teachers distrischool_schedules distrischool_grades; do
    echo "  → Configurando $db..."
    docker exec -i postgres-distrischool psql -U distrischool -d "$db" <<EOF
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
ALTER DATABASE $db SET timezone = 'UTC';
EOF
done

echo ""
echo "=========================================="
echo "✓ Bancos de dados inicializados com sucesso!"
echo "=========================================="
echo ""
echo "Bancos criados:"
echo "  - distrischool_auth"
echo "  - distrischool_students"
echo "  - distrischool_teachers"
echo "  - distrischool_schedules"
echo "  - distrischool_grades"
echo ""


#!/bin/bash
# Script para criar os bancos de dados do DistriSchool
# Este script pode ser executado manualmente se os bancos não foram criados automaticamente

set -e

# Configurações do PostgreSQL
PGHOST="${PGHOST:-postgres}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-distrischool}"
PGPASSWORD="${PGPASSWORD:-distrischool123}"
PGDATABASE="${PGDATABASE:-postgres}"

export PGPASSWORD

echo "Conectando ao PostgreSQL em ${PGHOST}:${PGPORT}..."

# Lista de bancos de dados a serem criados
DATABASES=(
    "distrischool_auth"
    "distrischool_students"
    "distrischool_teachers"
    "distrischool_schedules"
    "distrischool_grades"
)

# Criar cada banco de dados se não existir
for DB in "${DATABASES[@]}"; do
    echo "Verificando banco de dados: ${DB}"
    
    # Verificar se o banco já existe
    DB_EXISTS=$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -tAc "SELECT 1 FROM pg_database WHERE datname='${DB}'" 2>/dev/null || echo "0")
    
    if [ "$DB_EXISTS" = "1" ]; then
        echo "  ✓ Banco de dados ${DB} já existe"
    else
        echo "  → Criando banco de dados ${DB}..."
        psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c "CREATE DATABASE ${DB};" 2>/dev/null || {
            echo "  ✗ Erro ao criar banco ${DB}"
            continue
        }
        echo "  ✓ Banco de dados ${DB} criado com sucesso"
        
        # Conceder permissões
        psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -c "GRANT ALL PRIVILEGES ON DATABASE ${DB} TO ${PGUSER};" 2>/dev/null || true
        
        # Configurar timezone
        psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$DB" -c "ALTER DATABASE ${DB} SET timezone = 'UTC';" 2>/dev/null || true
    fi
done

# Criar extensões em cada banco
for DB in "${DATABASES[@]}"; do
    echo "Configurando extensões no banco: ${DB}"
    psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$DB" -c "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";" 2>/dev/null || true
    psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$DB" -c "CREATE EXTENSION IF NOT EXISTS \"pg_trgm\";" 2>/dev/null || true
done

unset PGPASSWORD

echo ""
echo "✓ Todos os bancos de dados foram verificados/criados com sucesso!"
echo ""


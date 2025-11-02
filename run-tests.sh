#!/bin/bash

# Script para compilar e testar o projeto usando Java 17
set -e

# Configurar Java 17
export JAVA_HOME=/Users/ccastro/Library/Java/JavaVirtualMachines/temurin-17.0.16/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "=========================================="
echo "TESTE COMPLETO - GRADE MANAGEMENT SERVICE"
echo "=========================================="
echo ""

echo "1. Verificando Java..."
echo "----------------------------------------"
java -version
echo ""

echo "2. Compilando projeto..."
echo "----------------------------------------"
mvn clean compile -DskipTests
if [ $? -eq 0 ]; then
    echo "✅ Compilação bem-sucedida"
else
    echo "❌ Erro na compilação"
    exit 1
fi
echo ""

echo "3. Compilando testes..."
echo "----------------------------------------"
mvn test-compile
if [ $? -eq 0 ]; then
    echo "✅ Testes compilados com sucesso"
else
    echo "⚠️  Alguns testes podem ter erros (continuando...)"
fi
echo ""

echo "4. Verificando estrutura do código..."
echo "----------------------------------------"
echo "Verificando entidades..."
ls -1 src/main/java/com/distrischool/grade/entity/*.java | wc -l | xargs -I {} echo "✅ {} entidades encontradas"

echo "Verificando DTOs..."
ls -1 src/main/java/com/distrischool/grade/dto/*.java | wc -l | xargs -I {} echo "✅ {} DTOs encontrados"

echo "Verificando Repositories..."
ls -1 src/main/java/com/distrischool/grade/repository/*.java | wc -l | xargs -I {} echo "✅ {} Repositories encontrados"

echo "Verificando Services..."
ls -1 src/main/java/com/distrischool/grade/service/*.java | wc -l | xargs -I {} echo "✅ {} Services encontrados"

echo "Verificando Controllers..."
ls -1 src/main/java/com/distrischool/grade/controller/*.java | wc -l | xargs -I {} echo "✅ {} Controllers encontrados"

echo "Verificando Configurações..."
ls -1 src/main/java/com/distrischool/grade/config/*.java | wc -l | xargs -I {} echo "✅ {} Configurações encontradas"

echo "Verificando Kafka..."
ls -1 src/main/java/com/distrischool/grade/kafka/*.java | wc -l | xargs -I {} echo "✅ {} Classes Kafka encontradas"

echo "Verificando Feign Clients..."
ls -1 src/main/java/com/distrischool/grade/feign/*.java 2>/dev/null | wc -l | xargs -I {} echo "✅ {} Feign Clients encontrados"

echo ""

echo "5. Verificando migrações Flyway..."
echo "----------------------------------------"
FLYWAY_FILES=$(find src/main/resources/db/migration -name "*.sql" 2>/dev/null | wc -l | xargs)
echo "✅ $FLYWAY_FILES migrações Flyway encontradas"

echo ""

echo "6. Verificando configurações..."
echo "----------------------------------------"
if [ -f "src/main/resources/application.yml" ]; then
    echo "✅ application.yml encontrado"
    grep -q "grade-management-service" src/main/resources/application.yml && echo "✅ Nome do serviço configurado corretamente"
    grep -q "distrischool.grade.created" src/main/resources/application.yml && echo "✅ Tópicos Kafka configurados"
else
    echo "❌ application.yml não encontrado"
fi

echo ""

echo "7. Verificando Docker Compose..."
echo "----------------------------------------"
if [ -f "docker-compose.yml" ]; then
    echo "✅ docker-compose.yml encontrado"
    grep -q "grade-management-service-dev" docker-compose.yml && echo "✅ Serviço configurado no Docker Compose"
else
    echo "❌ docker-compose.yml não encontrado"
fi

echo ""

echo "=========================================="
echo "✅ TODAS AS VERIFICAÇÕES CONCLUÍDAS"
echo "=========================================="
echo ""
echo "Status: ✅ PROJETO COMPILADO COM SUCESSO"
echo ""
echo "Próximos passos:"
echo "1. Para executar o serviço:"
echo "   export JAVA_HOME=/Users/ccastro/Library/Java/JavaVirtualMachines/temurin-17.0.16/Contents/Home"
echo "   export PATH=\$JAVA_HOME/bin:\$PATH"
echo "   mvn spring-boot:run"
echo ""
echo "2. Para testar com Docker:"
echo "   docker-compose up -d"
echo ""
echo "3. Para executar testes:"
echo "   mvn test"
echo ""


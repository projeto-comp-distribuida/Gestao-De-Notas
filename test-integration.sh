#!/bin/bash

# Script de teste de integração do microserviço de gestão de notas
# Testa requisições HTTP e integração com Kafka

set -e

echo "=========================================="
echo "TESTE DE INTEGRAÇÃO - GRADE MANAGEMENT SERVICE"
echo "=========================================="
echo ""

BASE_URL="${BASE_URL:-http://localhost:8083}"
KAFKA_UI="${KAFKA_UI:-http://localhost:8090}"

echo "1. Testando Health Check..."
echo "----------------------------------------"
HEALTH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$BASE_URL/api/v1/health")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$HEALTH_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Health Check OK"
    echo "Response: $BODY" | jq '.' 2>/dev/null || echo "$BODY"
else
    echo "❌ Health Check FALHOU - HTTP Code: $HTTP_CODE"
    echo "Response: $BODY"
fi
echo ""

echo "2. Testando Health Info..."
echo "----------------------------------------"
INFO_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$BASE_URL/api/v1/health/info")
HTTP_CODE=$(echo "$INFO_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$INFO_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Health Info OK"
    echo "Response: $BODY" | jq '.' 2>/dev/null || echo "$BODY"
else
    echo "❌ Health Info FALHOU - HTTP Code: $HTTP_CODE"
    echo "Response: $BODY"
fi
echo ""

echo "3. Verificando se outros microserviços estão rodando..."
echo "----------------------------------------"

# Verificar Student Service
echo "Verificando Student Service..."
STUDENT_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8082/api/v1/health" || echo "000")
if [ "$STUDENT_HEALTH" = "200" ]; then
    echo "✅ Student Service está rodando"
else
    echo "⚠️  Student Service não está respondendo (HTTP: $STUDENT_HEALTH)"
    echo "   Isso pode causar falhas nas validações de integração"
fi

# Verificar Teacher Service
echo "Verificando Teacher Service..."
TEACHER_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/v1/health" || echo "000")
if [ "$TEACHER_HEALTH" = "200" ]; then
    echo "✅ Teacher Service está rodando"
else
    echo "⚠️  Teacher Service não está respondendo (HTTP: $TEACHER_HEALTH)"
    echo "   Isso pode causar falhas nas validações de integração"
fi
echo ""

echo "4. Verificando Kafka..."
echo "----------------------------------------"
KAFKA_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$KAFKA_UI" || echo "000")
if [ "$KAFKA_HEALTH" = "200" ]; then
    echo "✅ Kafka UI está acessível"
    echo "   Acesse $KAFKA_UI para visualizar tópicos e mensagens"
else
    echo "⚠️  Kafka UI não está acessível (HTTP: $KAFKA_HEALTH)"
    echo "   Verifique se Kafka está rodando"
fi
echo ""

echo "5. Testando criação de nota (requer autenticação)..."
echo "----------------------------------------"
echo "⚠️  Este teste requer autenticação JWT válida"
echo "   Para testar manualmente, use:"
echo ""
echo "   curl -X POST $BASE_URL/api/v1/grades \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -H 'Authorization: Bearer <SEU_TOKEN_JWT>' \\"
echo "     -d '{"
echo "       \"studentId\": 1,"
echo "       \"teacherId\": 1,"
echo "       \"evaluationId\": 1,"
echo "       \"gradeValue\": 8.5,"
echo "       \"gradeDate\": \"2024-11-02\","
echo "       \"academicYear\": 2024,"
echo "       \"academicSemester\": 2"
echo "     }'"
echo ""

echo "6. Testando listagem de notas..."
echo "----------------------------------------"
GRADES_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$BASE_URL/api/v1/grades?page=0&size=10")
HTTP_CODE=$(echo "$GRADES_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$GRADES_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "401" ]; then
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✅ Listagem de notas OK"
        echo "Response: $BODY" | jq '.' 2>/dev/null || echo "$BODY"
    else
        echo "⚠️  Listagem requer autenticação (HTTP 401 - Esperado)"
    fi
else
    echo "❌ Listagem FALHOU - HTTP Code: $HTTP_CODE"
    echo "Response: $BODY"
fi
echo ""

echo "=========================================="
echo "TESTES CONCLUÍDOS"
echo "=========================================="
echo ""
echo "Próximos passos para testes completos:"
echo "1. Certifique-se de que Student Service e Teacher Service estão rodando"
echo "2. Obtenha um token JWT válido do Auth Service"
echo "3. Use o token para fazer requisições autenticadas"
echo "4. Verifique os eventos no Kafka UI: $KAFKA_UI"
echo ""


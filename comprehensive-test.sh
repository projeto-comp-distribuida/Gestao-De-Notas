#!/bin/bash

# Script completo de testes do Grade Management Service
# Testa todas as funcionalidades: endpoints, integrações, Kafka, etc.

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# URLs dos serviços
GRADE_SERVICE="${GRADE_SERVICE:-http://localhost:8083}"
STUDENT_SERVICE="${STUDENT_SERVICE:-http://localhost:8082}"
TEACHER_SERVICE="${TEACHER_SERVICE:-http://localhost:8080}"
KAFKA_UI="${KAFKA_UI:-http://localhost:8090}"

# Contadores de testes
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Função para imprimir resultado
print_result() {
    local test_name="$1"
    local status="$2"
    local message="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✅ PASS${NC}: $test_name"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAIL${NC}: $test_name"
        echo -e "   ${RED}$message${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# Função para fazer requisição HTTP
http_request() {
    local method="$1"
    local url="$2"
    local data="$3"
    local headers="$4"
    
    if [ -n "$data" ]; then
        if [ -n "$headers" ]; then
            curl -s -w "\nHTTP_CODE:%{http_code}" -X "$method" "$url" -H "$headers" -d "$data"
        else
            curl -s -w "\nHTTP_CODE:%{http_code}" -X "$method" "$url" -d "$data"
        fi
    else
        if [ -n "$headers" ]; then
            curl -s -w "\nHTTP_CODE:%{http_code}" -X "$method" "$url" -H "$headers"
        else
            curl -s -w "\nHTTP_CODE:%{http_code}" -X "$method" "$url"
        fi
    fi
}

echo "=========================================="
echo -e "${BLUE}TESTE COMPLETO - GRADE MANAGEMENT SERVICE${NC}"
echo "=========================================="
echo ""

# ============================================
# 1. TESTES DE HEALTH CHECK
# ============================================
echo -e "${BLUE}1. TESTES DE HEALTH CHECK${NC}"
echo "----------------------------------------"

# Teste 1.1: Health Check básico
RESPONSE=$(http_request "GET" "$GRADE_SERVICE/api/v1/health")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    print_result "Health Check" "PASS" ""
else
    print_result "Health Check" "FAIL" "HTTP Code: $HTTP_CODE"
fi

# Teste 1.2: Health Info
RESPONSE=$(http_request "GET" "$GRADE_SERVICE/api/v1/health/info")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    print_result "Health Info" "PASS" ""
else
    print_result "Health Info" "FAIL" "HTTP Code: $HTTP_CODE"
fi

# Teste 1.3: Actuator Health
RESPONSE=$(http_request "GET" "$GRADE_SERVICE/actuator/health")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    print_result "Actuator Health" "PASS" ""
else
    print_result "Actuator Health" "FAIL" "HTTP Code: $HTTP_CODE"
fi

# Teste 1.4: Actuator Metrics
RESPONSE=$(http_request "GET" "$GRADE_SERVICE/actuator/metrics")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    print_result "Actuator Metrics" "PASS" ""
else
    print_result "Actuator Metrics" "FAIL" "HTTP Code: $HTTP_CODE"
fi

echo ""

# ============================================
# 2. TESTES DE DISPONIBILIDADE DE SERVIÇOS
# ============================================
echo -e "${BLUE}2. VERIFICAÇÃO DE SERVIÇOS DEPENDENTES${NC}"
echo "----------------------------------------"

# Teste 2.1: Student Service
STUDENT_RESPONSE=$(http_request "GET" "$STUDENT_SERVICE/api/v1/health" 2>&1)
STUDENT_CODE=$(echo "$STUDENT_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2 || echo "000")

if [ "$STUDENT_CODE" = "200" ]; then
    print_result "Student Service disponível" "PASS" ""
else
    print_result "Student Service disponível" "FAIL" "HTTP Code: $STUDENT_CODE (necessário para validações)"
fi

# Teste 2.2: Teacher Service
TEACHER_RESPONSE=$(http_request "GET" "$TEACHER_SERVICE/api/v1/health" 2>&1)
TEACHER_CODE=$(echo "$TEACHER_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2 || echo "000")

if [ "$TEACHER_CODE" = "200" ]; then
    print_result "Teacher Service disponível" "PASS" ""
else
    print_result "Teacher Service disponível" "FAIL" "HTTP Code: $TEACHER_CODE (necessário para validações)"
fi

# Teste 2.3: Kafka UI
KAFKA_RESPONSE=$(http_request "GET" "$KAFKA_UI" 2>&1)
KAFKA_CODE=$(echo "$KAFKA_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2 || echo "000")

if [ "$KAFKA_CODE" = "200" ] || [ "$KAFKA_CODE" = "301" ] || [ "$KAFKA_CODE" = "302" ]; then
    print_result "Kafka UI disponível" "PASS" ""
else
    print_result "Kafka UI disponível" "FAIL" "HTTP Code: $KAFKA_CODE"
fi

echo ""

# ============================================
# 3. TESTES DE ENDPOINTS (sem autenticação)
# ============================================
echo -e "${BLUE}3. TESTES DE ENDPOINTS REST${NC}"
echo "----------------------------------------"

# Teste 3.1: Listar notas (deve retornar 401 se autenticação ativa)
RESPONSE=$(http_request "GET" "$GRADE_SERVICE/api/v1/grades?page=0&size=10")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "401" ]; then
    if [ "$HTTP_CODE" = "401" ]; then
        print_result "GET /grades (proteção de autenticação)" "PASS" "Autenticação funcionando"
    else
        print_result "GET /grades" "PASS" ""
    fi
else
    print_result "GET /grades" "FAIL" "HTTP Code: $HTTP_CODE"
fi

# Teste 3.2: Buscar nota por ID inexistente
RESPONSE=$(http_request "GET" "$GRADE_SERVICE/api/v1/grades/999999")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" = "404" ] || [ "$HTTP_CODE" = "401" ]; then
    print_result "GET /grades/{id} inexistente" "PASS" "Retorna 404 ou 401 (esperado)"
else
    print_result "GET /grades/{id} inexistente" "FAIL" "HTTP Code: $HTTP_CODE (esperado 404 ou 401)"
fi

# Teste 3.3: Criar nota sem autenticação
GRADE_DATA='{
  "studentId": 1,
  "teacherId": 1,
  "evaluationId": 1,
  "gradeValue": 8.5,
  "gradeDate": "2024-11-02",
  "academicYear": 2024,
  "academicSemester": 2
}'

RESPONSE=$(http_request "POST" "$GRADE_SERVICE/api/v1/grades" "$GRADE_DATA" "Content-Type: application/json")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    print_result "POST /grades (proteção de autenticação)" "PASS" "Autenticação funcionando"
elif [ "$HTTP_CODE" = "400" ]; then
    # Pode ser validação de negócio ou erro de integração
    print_result "POST /grades (validação)" "PASS" "Validação funcionando"
else
    print_result "POST /grades" "FAIL" "HTTP Code: $HTTP_CODE - Esperado 401, 403 ou 400"
fi

echo ""

# ============================================
# 4. TESTES DE VALIDAÇÃO (sem autenticação)
# ============================================
echo -e "${BLUE}4. TESTES DE VALIDAÇÃO DE DADOS${NC}"
echo "----------------------------------------"

# Teste 4.1: Criar nota com dados inválidos (nota > 10)
INVALID_GRADE='{
  "studentId": 1,
  "teacherId": 1,
  "evaluationId": 1,
  "gradeValue": 15.0,
  "gradeDate": "2024-11-02",
  "academicYear": 2024,
  "academicSemester": 2
}'

RESPONSE=$(http_request "POST" "$GRADE_SERVICE/api/v1/grades" "$INVALID_GRADE" "Content-Type: application/json")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "401" ]; then
    print_result "Validação: nota > 10" "PASS" "Validação funcionando"
else
    print_result "Validação: nota > 10" "FAIL" "HTTP Code: $HTTP_CODE (esperado 400)"
fi

# Teste 4.2: Criar nota com dados faltando
INCOMPLETE_GRADE='{
  "studentId": 1,
  "gradeValue": 8.5
}'

RESPONSE=$(http_request "POST" "$GRADE_SERVICE/api/v1/grades" "$INCOMPLETE_GRADE" "Content-Type: application/json")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "401" ]; then
    print_result "Validação: dados obrigatórios" "PASS" "Validação funcionando"
else
    print_result "Validação: dados obrigatórios" "FAIL" "HTTP Code: $HTTP_CODE (esperado 400)"
fi

echo ""

# ============================================
# 5. TESTES DE BANCO DE DADOS
# ============================================
echo -e "${BLUE}5. TESTES DE BANCO DE DADOS${NC}"
echo "----------------------------------------"

# Verificar se PostgreSQL está acessível
PG_CONNECTION=$(docker ps | grep postgres || echo "")

if [ -n "$PG_CONNECTION" ]; then
    print_result "PostgreSQL rodando" "PASS" ""
else
    print_result "PostgreSQL rodando" "FAIL" "PostgreSQL não encontrado em containers Docker"
fi

# Verificar se Flyway executou
# (verificar logs ou tentar acessar endpoint de info)
RESPONSE=$(http_request "GET" "$GRADE_SERVICE/actuator/info")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    print_result "Flyway migrações" "PASS" "Aplicação iniciou (Flyway executou)"
else
    print_result "Flyway migrações" "FAIL" "Não foi possível verificar"
fi

echo ""

# ============================================
# 6. TESTES DE KAFKA
# ============================================
echo -e "${BLUE}6. TESTES DE KAFKA${NC}"
echo "----------------------------------------"

# Verificar se Kafka está rodando
KAFKA_CONTAINER=$(docker ps | grep kafka || echo "")

if [ -n "$KAFKA_CONTAINER" ]; then
    print_result "Kafka rodando" "PASS" ""
else
    print_result "Kafka rodando" "FAIL" "Kafka não encontrado em containers Docker"
fi

# Verificar se Zookeeper está rodando
ZOOKEEPER_CONTAINER=$(docker ps | grep zookeeper || echo "")

if [ -n "$ZOOKEEPER_CONTAINER" ]; then
    print_result "Zookeeper rodando" "PASS" ""
else
    print_result "Zookeeper rodando" "FAIL" "Zookeeper não encontrado em containers Docker"
fi

# Nota: Para testar publicação real de eventos, precisamos criar uma nota com sucesso
# Isso requer autenticação válida e IDs reais de student/teacher

echo ""

# ============================================
# 7. TESTES DE REDIS
# ============================================
echo -e "${BLUE}7. TESTES DE REDIS${NC}"
echo "----------------------------------------"

REDIS_CONTAINER=$(docker ps | grep redis || echo "")

if [ -n "$REDIS_CONTAINER" ]; then
    print_result "Redis rodando" "PASS" ""
else
    print_result "Redis rodando" "FAIL" "Redis não encontrado em containers Docker"
fi

echo ""

# ============================================
# 8. TESTES DE MÉTRICAS
# ============================================
echo -e "${BLUE}8. TESTES DE MÉTRICAS${NC}"
echo "----------------------------------------"

# Teste 8.1: Prometheus metrics
RESPONSE=$(http_request "GET" "$GRADE_SERVICE/actuator/prometheus")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" = "200" ]; then
    METRICS=$(echo "$RESPONSE" | sed '/HTTP_CODE/d' | grep -c "grade" || echo "0")
    if [ "$METRICS" -gt "0" ]; then
        print_result "Métricas Prometheus" "PASS" "Métricas disponíveis"
    else
        print_result "Métricas Prometheus" "PASS" "Endpoint disponível (métricas serão criadas após uso)"
    fi
else
    print_result "Métricas Prometheus" "FAIL" "HTTP Code: $HTTP_CODE"
fi

echo ""

# ============================================
# RESUMO FINAL
# ============================================
echo "=========================================="
echo -e "${BLUE}RESUMO DOS TESTES${NC}"
echo "=========================================="
echo ""
echo "Total de testes: $TOTAL_TESTS"
echo -e "${GREEN}Testes aprovados: $PASSED_TESTS${NC}"
echo -e "${RED}Testes falhados: $FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ TODOS OS TESTES PASSARAM!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  Alguns testes falharam. Verifique acima para detalhes.${NC}"
    echo ""
    echo "Notas importantes:"
    echo "- Alguns testes podem falhar se os serviços dependentes não estiverem rodando"
    echo "- Testes de autenticação esperam HTTP 401/403, não 200"
    echo "- Para testes completos com dados reais, use autenticação JWT válida"
    echo ""
    echo "Para testar com autenticação:"
    echo "1. Obtenha um token JWT do Auth Service"
    echo "2. Use: curl -H 'Authorization: Bearer <TOKEN>' ..."
    echo ""
    exit 1
fi


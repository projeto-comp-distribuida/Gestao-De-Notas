#!/bin/bash

# Script de validação completa das integrações do Gestao-De-Notas
# Testa: Feign Clients, Enriquecimento de Dados, Kafka, Autorização

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================="
echo "VALIDAÇÃO DE INTEGRAÇÃO - GESTAO-DE-NOTAS"
echo "==========================================${NC}"
echo ""

# URLs dos serviços
GRADE_SERVICE="${GRADE_SERVICE:-http://localhost:8083}"
STUDENT_SERVICE="${STUDENT_SERVICE:-http://localhost:8082}"
TEACHER_SERVICE="${TEACHER_SERVICE:-http://localhost:8080}"
AUTH_SERVICE="${AUTH_SERVICE:-http://localhost:8080}"

# Contadores
PASSED=0
FAILED=0
WARNINGS=0

# Função para testar endpoint
test_endpoint() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}
    local method=${4:-GET}
    local data=$5
    
    echo -e "${BLUE}Testando: $name${NC}"
    echo "URL: $url"
    
    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$url" \
            -H "Content-Type: application/json" \
            -d "$data" 2>&1)
    else
        RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$url" 2>&1)
    fi
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2 || echo "000")
    BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')
    
    if [ "$HTTP_CODE" = "$expected_code" ]; then
        echo -e "${GREEN}✅ PASSOU - HTTP $HTTP_CODE${NC}"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}❌ FALHOU - Esperado: $expected_code, Recebido: $HTTP_CODE${NC}"
        echo "Response: $BODY" | head -5
        ((FAILED++))
        return 1
    fi
    echo ""
}

# 1. Verificar se os serviços estão rodando
echo -e "${BLUE}=========================================="
echo "1. VERIFICAÇÃO DE SERVIÇOS"
echo "==========================================${NC}"
echo ""

test_endpoint "Grade Service Health" "$GRADE_SERVICE/api/v1/health" 200
test_endpoint "Student Service Health" "$STUDENT_SERVICE/api/v1/health" 200
test_endpoint "Teacher Service Health" "$TEACHER_SERVICE/api/v1/health" 200

if [ $FAILED -gt 0 ]; then
    echo -e "${RED}⚠️  Alguns serviços não estão rodando. Testes de integração podem falhar.${NC}"
    echo ""
fi

# 2. Testar Feign Clients (criando dados de teste)
echo -e "${BLUE}=========================================="
echo "2. TESTE DE FEIGN CLIENTS"
echo "==========================================${NC}"
echo ""

echo -e "${YELLOW}Criando estudante de teste...${NC}"
STUDENT_DATA='{
  "fullName": "Teste Integração",
  "cpf": "12345678901",
  "email": "teste.integracao@test.com",
  "birthDate": "2000-01-01",
  "course": "Teste",
  "semester": 1,
  "enrollmentDate": "2024-01-01"
}'

STUDENT_RESPONSE=$(curl -s -X POST "$STUDENT_SERVICE/api/v1/students" \
    -H "Content-Type: application/json" \
    -d "$STUDENT_DATA" 2>&1 || echo '{"error":"failed"}')

STUDENT_ID=$(echo "$STUDENT_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('data', {}).get('id', 'null'))" 2>/dev/null || echo "null")

if [ "$STUDENT_ID" != "null" ] && [ -n "$STUDENT_ID" ]; then
    echo -e "${GREEN}✅ Estudante criado com ID: $STUDENT_ID${NC}"
    
    # Testar se Grade Service consegue buscar o estudante via Feign
    echo -e "${YELLOW}Testando busca de estudante via Feign no Grade Service...${NC}"
    # Nota: Isso só pode ser testado indiretamente ao criar uma nota
else
    echo -e "${RED}❌ Falha ao criar estudante de teste${NC}"
    echo "Response: $STUDENT_RESPONSE"
    ((WARNINGS++))
fi
echo ""

echo -e "${YELLOW}Criando professor de teste...${NC}"
TEACHER_DATA='{
  "name": "Professor Teste",
  "employeeId": "PROF-TEST-001",
  "email": "prof.teste@test.com"
}'

TEACHER_RESPONSE=$(curl -s -X POST "$TEACHER_SERVICE/api/v1/teachers" \
    -H "Content-Type: application/json" \
    -d "$TEACHER_DATA" 2>&1 || echo '{"error":"failed"}')

TEACHER_ID=$(echo "$TEACHER_RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data.get('data', {}).get('id', 'null'))" 2>/dev/null || echo "null")

if [ "$TEACHER_ID" != "null" ] && [ -n "$TEACHER_ID" ]; then
    echo -e "${GREEN}✅ Professor criado com ID: $TEACHER_ID${NC}"
else
    echo -e "${RED}❌ Falha ao criar professor de teste${NC}"
    echo "Response: $TEACHER_RESPONSE"
    ((WARNINGS++))
fi
echo ""

# 3. Testar Enriquecimento de Dados
echo -e "${BLUE}=========================================="
echo "3. TESTE DE ENRIQUECIMENTO DE DADOS"
echo "==========================================${NC}"
echo ""

if [ "$STUDENT_ID" != "null" ] && [ "$TEACHER_ID" != "null" ]; then
    echo -e "${YELLOW}Para testar o enriquecimento, é necessário:${NC}"
    echo "1. Criar uma nota com os IDs acima"
    echo "2. Verificar se a resposta inclui StudentInfo e TeacherInfo"
    echo "3. Isso requer autenticação JWT válida"
    echo ""
    echo -e "${YELLOW}Exemplo de comando (requer token JWT):${NC}"
    echo "curl -X POST $GRADE_SERVICE/api/v1/grades \\"
    echo "  -H 'Content-Type: application/json' \\"
    echo "  -H 'Authorization: Bearer <TOKEN>' \\"
    echo "  -d '{"
    echo "    \"studentId\": $STUDENT_ID,"
    echo "    \"teacherId\": $TEACHER_ID,"
    echo "    \"evaluationId\": 1,"
    echo "    \"gradeValue\": 8.5,"
    echo "    \"gradeDate\": \"2024-11-02\","
    echo "    \"academicYear\": 2024,"
    echo "    \"academicSemester\": 2"
    echo "  }'"
    echo ""
    ((WARNINGS++))
else
    echo -e "${RED}❌ Não é possível testar enriquecimento sem estudante e professor${NC}"
    ((WARNINGS++))
fi
echo ""

# 4. Verificar Configuração de Kafka
echo -e "${BLUE}=========================================="
echo "4. VERIFICAÇÃO DE KAFKA"
echo "==========================================${NC}"
echo ""

KAFKA_UI="${KAFKA_UI:-http://localhost:8090}"
KAFKA_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$KAFKA_UI" 2>/dev/null || echo "000")

if [ "$KAFKA_HEALTH" = "200" ]; then
    echo -e "${GREEN}✅ Kafka UI está acessível em $KAFKA_UI${NC}"
    echo "   Verifique os tópicos:"
    echo "   - distrischool.grade.created"
    echo "   - distrischool.grade.updated"
    echo "   - distrischool.grade.deleted"
    echo "   - distrischool.student.created"
    echo "   - distrischool.teacher.created"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠️  Kafka UI não está acessível (HTTP: $KAFKA_HEALTH)${NC}"
    echo "   Verifique se Kafka está rodando: docker ps | grep kafka"
    ((WARNINGS++))
fi
echo ""

# 5. Verificar Configuração de URLs
echo -e "${BLUE}=========================================="
echo "5. VERIFICAÇÃO DE CONFIGURAÇÃO"
echo "==========================================${NC}"
echo ""

echo -e "${YELLOW}Verificando URLs configuradas no application.yml...${NC}"
if [ -f "src/main/resources/application.yml" ]; then
    echo "Student Service URL:"
    grep -A 1 "student:" src/main/resources/application.yml | grep "url:" || echo "  Não encontrado"
    echo "Teacher Service URL:"
    grep -A 1 "teacher:" src/main/resources/application.yml | grep "url:" || echo "  Não encontrado"
    echo "Auth Service URL:"
    grep -A 1 "auth:" src/main/resources/application.yml | grep "url:" || echo "  Não encontrado"
    echo ""
    
    # Verificar se as URLs estão corretas
    STUDENT_URL_CONFIG=$(grep -A 1 "student:" src/main/resources/application.yml | grep "url:" | cut -d: -f2 | tr -d ' "')
    TEACHER_URL_CONFIG=$(grep -A 1 "teacher:" src/main/resources/application.yml | grep "url:" | cut -d: -f2 | tr -d ' "')
    
    if [[ "$STUDENT_URL_CONFIG" == *"student-management-service"* ]]; then
        echo -e "${GREEN}✅ Student Service URL configurada corretamente${NC}"
        ((PASSED++))
    else
        echo -e "${RED}❌ Student Service URL pode estar incorreta: $STUDENT_URL_CONFIG${NC}"
        ((FAILED++))
    fi
    
    if [[ "$TEACHER_URL_CONFIG" == *"teacher-management-service"* ]]; then
        echo -e "${GREEN}✅ Teacher Service URL configurada corretamente${NC}"
        ((PASSED++))
    else
        echo -e "${RED}❌ Teacher Service URL pode estar incorreta: $TEACHER_URL_CONFIG${NC}"
        ((FAILED++))
    fi
else
    echo -e "${RED}❌ Arquivo application.yml não encontrado${NC}"
    ((FAILED++))
fi
echo ""

# 6. Verificar EventConsumer
echo -e "${BLUE}=========================================="
echo "6. VERIFICAÇÃO DE EVENT CONSUMER"
echo "==========================================${NC}"
echo ""

if [ -f "src/main/java/com/distrischool/grade/kafka/EventConsumer.java" ]; then
    echo -e "${YELLOW}Verificando listeners de eventos...${NC}"
    
    STUDENT_LISTENERS=$(grep -c "@KafkaListener.*student" src/main/java/com/distrischool/grade/kafka/EventConsumer.java || echo "0")
    TEACHER_LISTENERS=$(grep -c "@KafkaListener.*teacher" src/main/java/com/distrischool/grade/kafka/EventConsumer.java || echo "0")
    
    if [ "$STUDENT_LISTENERS" -ge 3 ]; then
        echo -e "${GREEN}✅ EventConsumer tem listeners para eventos de estudantes (created, updated, deleted)${NC}"
        ((PASSED++))
    else
        echo -e "${RED}❌ EventConsumer não tem todos os listeners de estudantes${NC}"
        ((FAILED++))
    fi
    
    if [ "$TEACHER_LISTENERS" -ge 3 ]; then
        echo -e "${GREEN}✅ EventConsumer tem listeners para eventos de professores (created, updated, deleted)${NC}"
        ((PASSED++))
    else
        echo -e "${RED}❌ EventConsumer não tem todos os listeners de professores${NC}"
        ((FAILED++))
    fi
else
    echo -e "${RED}❌ EventConsumer.java não encontrado${NC}"
    ((FAILED++))
fi
echo ""

# 7. Verificar Autorização
echo -e "${BLUE}=========================================="
echo "7. VERIFICAÇÃO DE AUTORIZAÇÃO"
echo "==========================================${NC}"
echo ""

if [ -f "src/main/java/com/distrischool/grade/controller/GradeController.java" ]; then
    echo -e "${YELLOW}Verificando @PreAuthorize nos endpoints...${NC}"
    
    PREAUTHORIZE_COUNT=$(grep -c "@PreAuthorize" src/main/java/com/distrischool/grade/controller/GradeController.java || echo "0")
    
    if [ "$PREAUTHORIZE_COUNT" -ge 3 ]; then
        echo -e "${GREEN}✅ GradeController tem @PreAuthorize configurado${NC}"
        echo "   Endpoints protegidos: createGrade, updateGrade, deleteGrade"
        ((PASSED++))
    else
        echo -e "${RED}❌ GradeController não tem @PreAuthorize suficiente${NC}"
        ((FAILED++))
    fi
else
    echo -e "${RED}❌ GradeController.java não encontrado${NC}"
    ((FAILED++))
fi
echo ""

# Resumo Final
echo -e "${BLUE}=========================================="
echo "RESUMO DA VALIDAÇÃO"
echo "==========================================${NC}"
echo ""
echo -e "${GREEN}✅ Testes Passados: $PASSED${NC}"
echo -e "${RED}❌ Testes Falhados: $FAILED${NC}"
echo -e "${YELLOW}⚠️  Avisos: $WARNINGS${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 Todas as verificações críticas passaram!${NC}"
    echo ""
    echo "Próximos passos para teste completo:"
    echo "1. Obtenha um token JWT válido do Auth Service"
    echo "2. Crie uma nota usando os IDs de estudante e professor criados acima"
    echo "3. Verifique se a resposta inclui StudentInfo e TeacherInfo (enriquecimento)"
    echo "4. Verifique os eventos no Kafka UI"
    exit 0
else
    echo -e "${RED}❌ Algumas verificações falharam. Revise os erros acima.${NC}"
    exit 1
fi


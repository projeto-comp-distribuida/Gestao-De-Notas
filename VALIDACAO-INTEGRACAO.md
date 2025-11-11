# Validação de Integração - Gestao-De-Notas

Este documento descreve como validar que todas as integrações estão funcionando corretamente.

## 🎯 O que é validado?

1. **Serviços Rodando**: Verifica se todos os microserviços estão acessíveis
2. **Feign Clients**: Testa comunicação com Student e Teacher services
3. **Enriquecimento de Dados**: Valida se GradeResponseDTO inclui StudentInfo e TeacherInfo
4. **Kafka**: Verifica se eventos estão sendo publicados e consumidos
5. **Autorização**: Confirma que @PreAuthorize está configurado
6. **Configuração**: Valida URLs e configurações no application.yml

## 🚀 Como executar

### Opção 1: Script Automatizado (Recomendado)

```bash
cd Gestao-De-Notas
./validar-integracao.sh
```

O script irá:
- ✅ Verificar se os serviços estão rodando
- ✅ Criar dados de teste (estudante e professor)
- ✅ Validar configurações
- ✅ Verificar EventConsumer e autorização
- ✅ Gerar relatório de validação

### Opção 2: Validação Manual

#### 1. Verificar Serviços

```bash
# Grade Service
curl http://localhost:8083/api/v1/health

# Student Service
curl http://localhost:8082/api/v1/health

# Teacher Service
curl http://localhost:8080/api/v1/health
```

#### 2. Testar Feign Clients

**Criar um estudante:**
```bash
curl -X POST http://localhost:8082/api/v1/students \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "João Silva",
    "cpf": "12345678901",
    "email": "joao@test.com",
    "birthDate": "2000-01-01",
    "course": "Teste",
    "semester": 1,
    "enrollmentDate": "2024-01-01"
  }'
```

**Anote o ID retornado** (ex: `{"data": {"id": 1, ...}}`)

**Criar um professor:**
```bash
curl -X POST http://localhost:8080/api/v1/teachers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Professor Teste",
    "employeeId": "PROF-001",
    "email": "prof@test.com"
  }'
```

**Anote o ID retornado**

#### 3. Testar Enriquecimento de Dados

**Criar uma nota** (requer token JWT):
```bash
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <SEU_TOKEN_JWT>" \
  -d '{
    "studentId": 1,
    "teacherId": 1,
    "evaluationId": 1,
    "gradeValue": 8.5,
    "gradeDate": "2024-11-02",
    "academicYear": 2024,
    "academicSemester": 2
  }'
```

**Verificar resposta** - deve incluir:
```json
{
  "data": {
    "id": 1,
    "studentId": 1,
    "teacherId": 1,
    "gradeValue": 8.5,
    "student": {
      "id": 1,
      "fullName": "João Silva",
      "email": "joao@test.com",
      "registrationNumber": "...",
      "course": "Teste"
    },
    "teacher": {
      "id": 1,
      "name": "Professor Teste",
      "email": "prof@test.com",
      "employeeId": "PROF-001"
    }
  }
}
```

✅ **Se `student` e `teacher` estão presentes**, o enriquecimento está funcionando!

#### 4. Verificar Kafka

**Verificar se eventos foram publicados:**
```bash
# Acesse Kafka UI (se disponível)
http://localhost:8090

# Ou verifique logs
docker logs kafka | grep "grade.created"
```

**Eventos esperados:**
- `distrischool.grade.created` - Quando nota é criada
- `distrischool.grade.updated` - Quando nota é atualizada
- `distrischool.grade.deleted` - Quando nota é deletada

#### 5. Testar Autorização

**Tentar criar nota sem token:**
```bash
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -d '{...}'
```

**Resultado esperado:** HTTP 401 ou 403 (não autorizado)

**Tentar criar nota com token de usuário sem role TEACHER/ADMIN:**
```bash
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_SEM_ROLE>" \
  -d '{...}'
```

**Resultado esperado:** HTTP 403 (forbidden)

## ✅ Checklist de Validação

- [ ] Todos os serviços estão rodando e respondendo
- [ ] Feign Client consegue buscar estudante do Student Service
- [ ] Feign Client consegue buscar professor do Teacher Service
- [ ] GradeResponseDTO inclui StudentInfo (enriquecimento funcionando)
- [ ] GradeResponseDTO inclui TeacherInfo (enriquecimento funcionando)
- [ ] Evento `grade.created` é publicado no Kafka
- [ ] EventConsumer recebe eventos de `student.created`
- [ ] EventConsumer recebe eventos de `teacher.created`
- [ ] Endpoint `POST /grades` requer autenticação (401/403 sem token)
- [ ] Endpoint `POST /grades` requer role TEACHER ou ADMIN (403 com role errada)
- [ ] URLs no application.yml estão corretas

## 🐛 Troubleshooting

### Problema: Feign Client retorna 404

**Verificar:**
1. Student/Teacher Service está rodando?
2. URL no `application.yml` está correta?
3. Porta do serviço está correta?

**Solução:**
```yaml
# application.yml
microservice:
  student:
    url: http://student-management-service-dev:8080
  teacher:
    url: http://teacher-management-service-dev:8080
```

### Problema: Enriquecimento não funciona (student/teacher null)

**Verificar:**
1. Feign Client está conseguindo buscar os dados?
2. Verificar logs: `docker logs grade-management-service-dev | grep "Estudante\|Professor"`
3. Student/Teacher Service está retornando dados corretos?

**Solução:**
- O enriquecimento é opcional (não falha se não conseguir buscar)
- Verifique se os IDs de studentId e teacherId são válidos
- Verifique se os serviços estão acessíveis

### Problema: Kafka não recebe eventos

**Verificar:**
1. Kafka está rodando? `docker ps | grep kafka`
2. Tópicos existem? Acesse Kafka UI
3. Verificar logs: `docker logs kafka`

**Solução:**
```bash
# Reiniciar Kafka
docker-compose restart kafka

# Verificar tópicos
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Problema: Autorização não funciona

**Verificar:**
1. SecurityConfig está configurado?
2. JWT está sendo validado?
3. Roles estão sendo extraídas do JWT?

**Solução:**
- Verifique `SecurityConfig.java`
- Verifique se o token JWT contém as claims `roles` ou `permissions`
- Teste com um token válido do Auth Service

## 📊 Interpretando Resultados

### ✅ Tudo Funcionando
- Todos os serviços respondem
- Feign Clients funcionam
- Enriquecimento inclui student e teacher
- Kafka publica eventos
- Autorização bloqueia requisições não autorizadas

### ⚠️ Parcialmente Funcionando
- Serviços respondem, mas algumas integrações falham
- Verifique logs para identificar o problema
- Pode ser problema de configuração ou rede

### ❌ Não Funcionando
- Serviços não respondem
- Verifique se estão rodando
- Verifique configurações de URL
- Verifique logs de erro

## 🔍 Logs Úteis

```bash
# Logs do Grade Service
docker logs grade-management-service-dev -f | grep -E "Estudante|Professor|Evento|Feign|Error"

# Logs do Student Service
docker logs student-management-service-dev -f

# Logs do Teacher Service
docker logs teacher-management-service-dev -f

# Logs do Kafka
docker logs kafka -f
```

## 📝 Notas Importantes

1. **Enriquecimento é opcional**: Se não conseguir buscar dados de student/teacher, a nota ainda é criada, mas sem os dados enriquecidos
2. **Autorização requer JWT válido**: Obtenha um token do Auth Service para testes completos
3. **Kafka é assíncrono**: Eventos podem demorar alguns segundos para aparecer
4. **Circuit Breaker**: Em caso de falhas repetidas, o Circuit Breaker pode abrir e bloquear requisições temporariamente


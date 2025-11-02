# Testes de Integração - Grade Management Service

Este documento descreve como testar a integração do microserviço de gestão de notas com os microserviços de estudantes e professores, além dos testes de Kafka.

## 📋 Pré-requisitos

1. Docker e Docker Compose instalados
2. Todos os microserviços rodando:
   - Student Management Service (porta 8082)
   - Teacher Management Service (porta 8080)
   - Grade Management Service (porta 8083)
   - Kafka (porta 9092)
   - PostgreSQL (portas diversas)
   - Redis (porta 6379)

## 🚀 Iniciando os Serviços

```bash
# No diretório Gestao-De-Notas
docker-compose up -d

# Ou inicie cada serviço individualmente nos seus respectivos diretórios
cd ../Gestao-de-Alunos && docker-compose up -d
cd ../Gestao-De-Professores && docker-compose up -d
cd ../Gestao-De-Notas && docker-compose up -d
```

## ✅ Testes de Integração

### 1. Verificar Health Check

```bash
# Grade Management Service
curl http://localhost:8083/api/v1/health

# Student Service
curl http://localhost:8082/api/v1/health

# Teacher Service
curl http://localhost:8080/api/v1/health
```

**Resultado esperado**: Status 200 com JSON contendo informações do serviço

### 2. Testar Integração com Student Service

#### 2.1. Criar um estudante primeiro

```bash
curl -X POST http://localhost:8082/api/v1/students \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_JWT>" \
  -d '{
    "fullName": "João Silva",
    "cpf": "12345678901",
    "email": "joao.silva@email.com",
    "birthDate": "2000-01-15",
    "course": "Ciência da Computação",
    "semester": 3,
    "enrollmentDate": "2023-01-01"
  }'
```

**Anote o ID do estudante retornado** (ex: `{"data": {"id": 1, ...}}`)

#### 2.2. Verificar se Grade Service consegue buscar o estudante

```bash
# O Grade Service valida automaticamente ao criar uma nota
# Mas você pode testar diretamente o Feign Client através de um endpoint de teste
# (seria necessário criar um endpoint de teste ou verificar os logs)
```

### 3. Testar Integração com Teacher Service

#### 3.1. Criar um professor primeiro

```bash
curl -X POST http://localhost:8080/api/v1/teachers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_JWT>" \
  -d '{
    "name": "Maria Santos",
    "employeeId": "EMP001",
    "email": "maria.santos@email.com",
    "qualification": "Doutora em Matemática"
  }'
```

**Anote o ID do professor retornado**

### 4. Testar Criação de Nota com Integração

#### 4.1. Criar uma avaliação primeiro (se necessário)

Você precisa ter um `evaluationId` válido. Por enquanto, você pode usar um ID fictício para testar a validação.

#### 4.2. Criar uma nota (com validações automáticas)

```bash
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_JWT>" \
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

**Cenários de teste**:

1. **Sucesso** (studentId e teacherId existem):
   - Status: 201 Created
   - Response: JSON com a nota criada
   - Evento Kafka publicado no tópico `distrischool.grade.created`

2. **Falha - Estudante não existe**:
   - Status: 400 Bad Request
   - Mensagem: "Estudante não encontrado com ID: X"

3. **Falha - Professor não existe**:
   - Status: 400 Bad Request
   - Mensagem: "Professor não encontrado com ID: X"

### 5. Testar Eventos Kafka

#### 5.1. Verificar tópicos Kafka

Acesse o Kafka UI: http://localhost:8090

Você deve ver os seguintes tópicos:
- `distrischool.grade.created`
- `distrischool.grade.updated`
- `distrischool.grade.deleted`
- `distrischool.student.created` (para consumo)
- `distrischool.student.updated` (para consumo)
- `distrischool.student.deleted` (para consumo)
- `distrischool.teacher.created` (para consumo)

#### 5.2. Publicar evento manualmente (opcional)

```bash
# Usando kafka-console-producer (se disponível)
docker exec -it kafka kafka-console-producer \
  --broker-list localhost:9092 \
  --topic distrischool.grade.created

# Então digite:
{"eventId":"test-123","eventType":"grade.created","source":"test","timestamp":"2024-11-02T10:00:00","data":{"gradeId":1,"studentId":1}}
```

#### 5.3. Verificar consumo de eventos

Verifique os logs do Grade Management Service:

```bash
docker logs grade-management-service-dev -f | grep "Evento recebido"
```

Você deve ver mensagens como:
```
Evento recebido - Student Created: <event-id>
Estudante criado - ID: 1
```

### 6. Testar Circuit Breaker (Resilience4j)

#### 6.1. Simular falha do Student Service

1. Pare o Student Service:
```bash
docker stop student-management-service-dev
```

2. Tente criar uma nota:
```bash
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_JWT>" \
  -d '{"studentId": 1, "teacherId": 1, ...}'
```

3. Verifique o comportamento:
   - Primeiras tentativas: Retry automático
   - Após várias falhas: Circuit Breaker abre
   - Mensagem de erro apropriada retornada

4. Reinicie o Student Service:
```bash
docker start student-management-service-dev
```

5. Após alguns segundos, o Circuit Breaker deve fechar e as requisições voltam a funcionar

## 📊 Verificação de Métricas

### Prometheus Metrics

```bash
curl http://localhost:8083/actuator/prometheus | grep grade
```

Você deve ver métricas como:
- `grades_create_seconds_count`
- `grades_get_seconds_count`
- `grades_update_seconds_count`

### Health Indicators

```bash
curl http://localhost:8083/actuator/health | jq
```

Verifique:
- `status`: UP
- `components.kafka.status`: UP
- `components.db.status`: UP
- `components.redis.status`: UP
- `components.circuitBreakers.status`: UP

## 🔍 Logs de Debug

Para ver logs detalhados da integração:

```bash
# Logs do Grade Service
docker logs grade-management-service-dev -f | grep -E "Estudante|Professor|Evento|Feign"

# Logs do Student Service
docker logs student-management-service-dev -f

# Logs do Kafka
docker logs kafka -f
```

## ✅ Checklist de Testes

- [ ] Health check responde corretamente
- [ ] Student Service está acessível via Feign
- [ ] Teacher Service está acessível via Feign
- [ ] Validação de estudante funciona (estudante existe)
- [ ] Validação de estudante funciona (estudante não existe)
- [ ] Validação de professor funciona (professor existe)
- [ ] Validação de professor funciona (professor não existe)
- [ ] Criação de nota publica evento Kafka
- [ ] Atualização de nota publica evento Kafka
- [ ] Deleção de nota publica evento Kafka
- [ ] Consumo de eventos de estudantes funciona
- [ ] Consumo de eventos de professores funciona
- [ ] Circuit Breaker funciona em caso de falha
- [ ] Retry funciona em caso de falha temporária
- [ ] Cache funciona corretamente
- [ ] Métricas Prometheus estão sendo coletadas

## 🐛 Troubleshooting

### Problema: Feign Client retorna 404

**Solução**: Verifique se a URL do serviço está correta em `application.yml`:
```yaml
microservice:
  student:
    url: http://student-management-service-dev:8080
  teacher:
    url: http://microservice-template-dev:8080
```

### Problema: Kafka não está recebendo eventos

**Solução**: 
1. Verifique se Kafka está rodando: `docker ps | grep kafka`
2. Verifique se o tópico existe no Kafka UI
3. Verifique os logs: `docker logs kafka`

### Problema: Circuit Breaker não funciona

**Solução**: 
1. Verifique se Resilience4j está configurado em `application.yml`
2. Verifique se `@CircuitBreaker` está sendo usado nos métodos corretos
3. Verifique os logs do Resilience4j

## 📝 Notas

- As integrações são **síncronas** (via Feign) para validações
- As notificações são **assíncronas** (via Kafka) para eventos
- Circuit Breaker protege contra falhas em cascata
- Retry tenta novamente em caso de falhas temporárias
- Cache reduz a carga nos serviços externos


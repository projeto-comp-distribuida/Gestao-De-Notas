# Como Validar que Tudo Está Funcionando

## 🎯 Resumo

Para ter **certeza** de que as integrações estão funcionando, você precisa:

1. ✅ **Rodar os testes unitários** - Validam a lógica de enriquecimento
2. ✅ **Executar o script de validação** - Verifica configurações e serviços
3. ✅ **Testar manualmente** - Cria dados reais e verifica o comportamento
4. ✅ **Verificar logs** - Confirma que Feign e Kafka estão funcionando

## 📋 Passo a Passo Completo

### 1. Rodar Testes Unitários

```bash
cd Gestao-De-Notas
./mvnw test
```

**O que valida:**
- ✅ Enriquecimento de dados funciona quando Feign retorna dados
- ✅ Enriquecimento é opcional (não falha se serviço não responder)
- ✅ Tratamento de erros (404, timeout, etc.)

### 2. Executar Script de Validação

```bash
cd Gestao-De-Notas
./validar-integracao.sh
```

**O que valida:**
- ✅ Serviços estão rodando
- ✅ URLs configuradas corretamente
- ✅ EventConsumer tem todos os listeners
- ✅ @PreAuthorize está configurado

### 3. Teste Manual Completo

#### 3.1. Iniciar todos os serviços

```bash
# Terminal 1: Student Service
cd Gestao-de-Alunos
./mvnw spring-boot:run

# Terminal 2: Teacher Service
cd Gestao-De-Professores
./mvnw spring-boot:run

# Terminal 3: Grade Service
cd Gestao-De-Notas
./mvnw spring-boot:run

# Terminal 4: Kafka (se usar Docker)
docker-compose up -d kafka
```

#### 3.2. Criar dados de teste

**Criar estudante:**
```bash
curl -X POST http://localhost:8082/api/v1/students \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "João Silva",
    "cpf": "12345678901",
    "email": "joao@test.com",
    "birthDate": "2000-01-01",
    "course": "Ciência da Computação",
    "semester": 3,
    "enrollmentDate": "2024-01-01"
  }'
```

**Anote o ID retornado** (ex: `{"data": {"id": 1, ...}}`)

**Criar professor:**
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

#### 3.3. Criar nota e verificar enriquecimento

**Criar nota** (substitua `STUDENT_ID` e `TEACHER_ID` pelos IDs acima):
```bash
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_JWT>" \
  -d '{
    "studentId": STUDENT_ID,
    "teacherId": TEACHER_ID,
    "evaluationId": 1,
    "gradeValue": 8.5,
    "gradeDate": "2024-11-02",
    "academicYear": 2024,
    "academicSemester": 2
  }'
```

**Verificar resposta** - Deve incluir:
```json
{
  "data": {
    "id": 1,
    "studentId": 1,
    "teacherId": 1,
    "gradeValue": 8.5,
    "student": {          // ← ENRIQUECIMENTO
      "id": 1,
      "fullName": "João Silva",
      "email": "joao@test.com",
      "registrationNumber": "...",
      "course": "Ciência da Computação"
    },
    "teacher": {          // ← ENRIQUECIMENTO
      "id": 1,
      "name": "Professor Teste",
      "email": "prof@test.com",
      "employeeId": "PROF-001"
    }
  }
}
```

✅ **Se `student` e `teacher` estão presentes**, o enriquecimento está funcionando!

#### 3.4. Verificar logs do Grade Service

```bash
# Ver logs do Grade Service
# Procure por:
# - "Enriquecendo com dados do estudante"
# - "Enriquecendo com dados do professor"
# - "Evento publicado: grade.created"
```

**Logs esperados:**
```
INFO  - Criando nota para estudante: 1, professor: 1
DEBUG - Buscando dados do estudante 1 via Feign
DEBUG - Buscando dados do professor 1 via Feign
INFO  - Nota criada com sucesso: 1
INFO  - Evento publicado: grade.created
```

#### 3.5. Verificar eventos Kafka

```bash
# Verificar se evento foi publicado
docker logs kafka | grep "grade.created"

# Ou acesse Kafka UI
http://localhost:8090
```

**Verificar tópico:** `distrischool.grade.created`

### 4. Testar Cenários de Erro

#### 4.1. Estudante não existe

```bash
# Tentar criar nota com studentId inexistente
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "studentId": 99999,  # ID que não existe
    "teacherId": 1,
    ...
  }'
```

**Resultado esperado:**
- ✅ Nota NÃO é criada
- ✅ Retorna erro 400: "Estudante não encontrado"
- ✅ Log mostra: "Estudante 99999 não encontrado"

#### 4.2. Professor não existe

```bash
# Tentar criar nota com teacherId inexistente
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "studentId": 1,
    "teacherId": 99999,  # ID que não existe
    ...
  }'
```

**Resultado esperado:**
- ✅ Nota NÃO é criada
- ✅ Retorna erro 400: "Professor não encontrado"
- ✅ Log mostra: "Professor 99999 não encontrado"

#### 4.3. Student Service offline

**Parar Student Service:**
```bash
# Parar o serviço
# Ctrl+C no terminal do Student Service
```

**Criar nota:**
```bash
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{...}'
```

**Resultado esperado:**
- ✅ Nota NÃO é criada (validação falha)
- ✅ Retorna erro 400 ou 500
- ✅ Log mostra erro de conexão com Feign

### 5. Verificar Autorização

#### 5.1. Sem token

```bash
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -d '{...}'
```

**Resultado esperado:** HTTP 401 ou 403

#### 5.2. Com token sem role TEACHER/ADMIN

```bash
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_SEM_ROLE>" \
  -d '{...}'
```

**Resultado esperado:** HTTP 403 Forbidden

#### 5.3. Com token com role TEACHER

```bash
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_TEACHER>" \
  -d '{...}'
```

**Resultado esperado:** HTTP 200 ou 201 (sucesso)

## ✅ Checklist Final

Marque cada item após validar:

- [ ] Testes unitários passam (`./mvnw test`)
- [ ] Script de validação passa (`./validar-integracao.sh`)
- [ ] Todos os serviços estão rodando
- [ ] Estudante pode ser criado no Student Service
- [ ] Professor pode ser criado no Teacher Service
- [ ] Nota pode ser criada no Grade Service
- [ ] Resposta da nota inclui `student` (enriquecimento)
- [ ] Resposta da nota inclui `teacher` (enriquecimento)
- [ ] Evento `grade.created` aparece no Kafka
- [ ] Validação de estudante inexistente funciona
- [ ] Validação de professor inexistente funciona
- [ ] Autorização bloqueia requisições sem token
- [ ] Autorização bloqueia requisições sem role adequada
- [ ] Logs mostram chamadas Feign bem-sucedidas

## 🎯 Como Ter Certeza Absoluta

Para ter **100% de certeza**, execute este teste completo:

```bash
# 1. Rodar testes
cd Gestao-De-Notas
./mvnw test

# 2. Iniciar serviços
# (em terminais separados)

# 3. Executar script de validação
./validar-integracao.sh

# 4. Criar dados e testar manualmente
# (seguir passo 3 acima)

# 5. Verificar logs
docker logs grade-management-service-dev | grep -E "Estudante|Professor|Evento|Feign"
```

**Se todos os passos passarem**, você tem certeza de que está funcionando! ✅

## 🐛 Se Algo Falhar

1. **Verifique logs** - Sempre o primeiro passo
2. **Verifique configurações** - URLs, portas, tópicos Kafka
3. **Verifique serviços** - Todos estão rodando?
4. **Verifique rede** - Serviços conseguem se comunicar?
5. **Verifique autenticação** - Token JWT é válido?

## 📊 Métricas de Sucesso

- ✅ **100% dos testes passam**
- ✅ **Script de validação retorna 0 erros**
- ✅ **Enriquecimento funciona em 100% dos casos quando serviços estão online**
- ✅ **Autorização bloqueia 100% das requisições não autorizadas**
- ✅ **Eventos Kafka são publicados em 100% das operações**


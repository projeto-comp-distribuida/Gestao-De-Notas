# Como Testar o Serviço de Notas SEM Autenticação no Postman

## 🚀 Passo 1: Desabilitar Autenticação

### Opção 1: Variável de Ambiente (Recomendado)

```bash
export SECURITY_DISABLE=true
cd Gestao-De-Notas
mvn spring-boot:run
```

### Opção 2: application.yml

Adicione no `application.yml`:

```yaml
security:
  disable: true
```

### Opção 3: Argumento JVM

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--security.disable=true"
```

## 📋 Endpoints Disponíveis

### Base URL
```
http://localhost:8083
```

### 1. Health Check (Sempre Público)
```
GET http://localhost:8083/api/v1/health
```

**Resposta esperada:**
```json
{
  "status": "UP",
  "service": "grade-management-service",
  "version": "1.0.0"
}
```

### 2. Listar Todas as Notas
```
GET http://localhost:8083/api/v1/grades?page=0&size=20
```

**Parâmetros opcionais:**
- `page`: número da página (default: 0)
- `size`: tamanho da página (default: 20)
- `sortBy`: campo para ordenar (default: id)
- `direction`: ASC ou DESC (default: ASC)

**Resposta esperada:**
```json
{
  "success": true,
  "data": {
    "content": [],
    "totalElements": 0,
    "totalPages": 0
  }
}
```

### 3. Buscar Nota por ID
```
GET http://localhost:8083/api/v1/grades/1
```

**Resposta esperada (se existir):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "studentId": 1,
    "teacherId": 1,
    "evaluationId": 1,
    "gradeValue": 8.5,
    "gradeDate": "2024-11-02",
    "academicYear": 2024,
    "academicSemester": 2,
    "status": "CONFIRMED"
  }
}
```

### 4. Buscar Notas por Estudante
```
GET http://localhost:8083/api/v1/grades/student/1?page=0&size=20
```

**Parâmetros:**
- `1` = ID do estudante
- `page` e `size` opcionais

### 5. Buscar Notas por Avaliação
```
GET http://localhost:8083/api/v1/grades/evaluation/1?page=0&size=20
```

### 6. Calcular Média do Estudante
```
GET http://localhost:8083/api/v1/grades/student/1/average?academicYear=2024&academicSemester=2
```

**Parâmetros obrigatórios:**
- `academicYear`: ano letivo (ex: 2024)
- `academicSemester`: semestre (1 ou 2)

### 7. Criar Nota
```
POST http://localhost:8083/api/v1/grades
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "studentId": 1,
  "teacherId": 1,
  "evaluationId": 1,
  "gradeValue": 8.5,
  "gradeDate": "2024-11-02",
  "academicYear": 2024,
  "academicSemester": 2,
  "notes": "Nota da primeira avaliação"
}
```

**⚠️ IMPORTANTE**: Para criar nota, você precisa:
- Student Service rodando (valida se estudante existe)
- Teacher Service rodando (valida se professor existe)

**Resposta esperada:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "studentId": 1,
    "teacherId": 1,
    "evaluationId": 1,
    "gradeValue": 8.5,
    "gradeDate": "2024-11-02",
    "academicYear": 2024,
    "academicSemester": 2,
    "status": "REGISTERED",
    "student": {
      "id": 1,
      "fullName": "João Silva",
      "email": "joao@test.com"
    },
    "teacher": {
      "id": 1,
      "name": "Professor Teste",
      "email": "prof@test.com"
    }
  }
}
```

### 8. Atualizar Nota
```
PUT http://localhost:8083/api/v1/grades/1
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "studentId": 1,
  "teacherId": 1,
  "evaluationId": 1,
  "gradeValue": 9.0,
  "gradeDate": "2024-11-02",
  "academicYear": 2024,
  "academicSemester": 2,
  "notes": "Nota atualizada"
}
```

### 9. Deletar Nota (Soft Delete)
```
DELETE http://localhost:8083/api/v1/grades/1
```

## 📝 Collection do Postman

### Criar Collection Manualmente

1. **Abrir Postman**
2. **Criar nova Collection**: "Grade Service - Sem Auth"
3. **Adicionar variável de ambiente**:
   - `base_url`: `http://localhost:8083`

### Requests para Adicionar

#### 1. Health Check
- **Method**: GET
- **URL**: `{{base_url}}/api/v1/health`

#### 2. Listar Notas
- **Method**: GET
- **URL**: `{{base_url}}/api/v1/grades?page=0&size=20`

#### 3. Buscar Nota por ID
- **Method**: GET
- **URL**: `{{base_url}}/api/v1/grades/1`

#### 4. Buscar Notas por Estudante
- **Method**: GET
- **URL**: `{{base_url}}/api/v1/grades/student/1`

#### 5. Calcular Média
- **Method**: GET
- **URL**: `{{base_url}}/api/v1/grades/student/1/average?academicYear=2024&academicSemester=2`

#### 6. Criar Nota
- **Method**: POST
- **URL**: `{{base_url}}/api/v1/grades`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):
```json
{
  "studentId": 1,
  "teacherId": 1,
  "evaluationId": 1,
  "gradeValue": 8.5,
  "gradeDate": "2024-11-02",
  "academicYear": 2024,
  "academicSemester": 2
}
```

#### 7. Atualizar Nota
- **Method**: PUT
- **URL**: `{{base_url}}/api/v1/grades/1`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):
```json
{
  "studentId": 1,
  "teacherId": 1,
  "evaluationId": 1,
  "gradeValue": 9.0,
  "gradeDate": "2024-11-02",
  "academicYear": 2024,
  "academicSemester": 2
}
```

#### 8. Deletar Nota
- **Method**: DELETE
- **URL**: `{{base_url}}/api/v1/grades/1`

## ⚠️ Validações que Ainda Funcionam

Mesmo sem autenticação, o serviço ainda valida:

1. **Nota entre 0 e 10**
   - Se enviar `gradeValue: 15` → Erro 400

2. **Campos obrigatórios**
   - Se faltar `studentId`, `teacherId`, etc → Erro 400

3. **Estudante existe** (se Student Service estiver rodando)
   - Se `studentId: 999` não existir → Erro 400

4. **Professor existe** (se Teacher Service estiver rodando)
   - Se `teacherId: 999` não existir → Erro 400

5. **Duplicatas**
   - Se já existir nota para mesmo aluno + mesma avaliação → Erro 400

## 🧪 Teste Rápido

### 1. Verificar se está rodando
```bash
curl http://localhost:8083/api/v1/health
```

### 2. Listar notas (deve retornar vazio se não houver dados)
```bash
curl http://localhost:8083/api/v1/grades
```

### 3. Criar nota (requer Student e Teacher Services)
```bash
curl -X POST http://localhost:8083/api/v1/grades \
  -H "Content-Type: application/json" \
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

## 📊 Códigos de Resposta

- **200 OK**: Sucesso
- **201 Created**: Nota criada
- **400 Bad Request**: Erro de validação
- **404 Not Found**: Recurso não encontrado
- **500 Internal Server Error**: Erro no servidor

## 🔍 Troubleshooting

### Problema: Retorna 401/403 mesmo com SECURITY_DISABLE=true

**Solução**: Verifique se a variável está sendo lida:
```bash
# Verificar variável
echo $SECURITY_DISABLE

# Reiniciar o serviço após definir a variável
```

### Problema: Não consegue criar nota (erro 400)

**Possíveis causas**:
1. Student Service não está rodando
2. Teacher Service não está rodando
3. Estudante/Professor não existe
4. Dados inválidos (nota > 10, campos faltando, etc)

**Solução**: Verifique os logs do serviço para ver o erro específico

### Problema: Serviço não inicia

**Possíveis causas**:
1. PostgreSQL não está rodando
2. Banco de dados não existe
3. Porta 8083 já está em uso

**Solução**: 
```bash
# Verificar PostgreSQL
docker ps | grep postgres

# Verificar porta
lsof -i :8083
```


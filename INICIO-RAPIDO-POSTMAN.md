# Início Rápido - Testar no Postman SEM Autenticação

## 🚀 Passo a Passo

### 1. Desabilitar Autenticação

```bash
export SECURITY_DISABLE=true
cd Gestao-De-Notas
mvn spring-boot:run
```

### 2. Importar Collection no Postman

1. Abra o Postman
2. Clique em **Import**
3. Selecione o arquivo: `Grade-Service-SEM-AUTH.postman_collection.json`
4. A collection será importada com todos os endpoints

### 3. Configurar Variável

1. Na collection importada, clique em **Variables**
2. Verifique se `base_url` está configurado como: `http://localhost:8083`
3. Se não estiver, adicione a variável

### 4. Testar!

#### ✅ Teste 1: Health Check
- Selecione: **Health Check**
- Clique em **Send**
- Deve retornar: `{"status":"UP",...}`

#### ✅ Teste 2: Listar Notas
- Selecione: **Listar Todas as Notas**
- Clique em **Send**
- Deve retornar lista vazia (se não houver notas)

#### ✅ Teste 3: Criar Nota
- Selecione: **Criar Nota**
- **IMPORTANTE**: Altere os IDs no body:
  - `studentId`: Use um ID de estudante que existe (se Student Service estiver rodando)
  - `teacherId`: Use um ID de professor que existe (se Teacher Service estiver rodando)
- Clique em **Send**
- Se Student/Teacher Services estiverem rodando: ✅ Nota criada
- Se não estiverem: ❌ Erro 400 (validação falha)

## 📋 Endpoints na Collection

1. **Health Check** - GET `/api/v1/health`
2. **Listar Todas as Notas** - GET `/api/v1/grades`
3. **Buscar Nota por ID** - GET `/api/v1/grades/1`
4. **Buscar Notas por Estudante** - GET `/api/v1/grades/student/1`
5. **Buscar Notas por Avaliação** - GET `/api/v1/grades/evaluation/1`
6. **Calcular Média** - GET `/api/v1/grades/student/1/average`
7. **Criar Nota** - POST `/api/v1/grades`
8. **Atualizar Nota** - PUT `/api/v1/grades/1`
9. **Deletar Nota** - DELETE `/api/v1/grades/1`

## ⚠️ Importante

- **Sem Student/Teacher Services**: Não consegue criar notas (validação falha)
- **Sem PostgreSQL**: Serviço não inicia
- **Com SECURITY_DISABLE=true**: Todos os endpoints funcionam sem token

## 🧪 Exemplo de Body para Criar Nota

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

## 📊 Códigos de Resposta

- **200 OK**: Sucesso
- **201 Created**: Nota criada
- **400 Bad Request**: Erro de validação (estudante/professor não existe, nota inválida, etc)
- **404 Not Found**: Recurso não encontrado
- **500 Internal Server Error**: Erro no servidor


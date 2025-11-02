# Guia de Uso do Postman - Grade Management Service

## 📥 Como Importar

### Método 1: Importar Coleção YAML
1. Abra o Postman
2. Clique em **Import**
3. Selecione o arquivo `DistriSchool-Grade-Management-Service.postman_collection.yaml`
4. Clique em **Import**

### Método 2: Importar Ambiente
1. Abra o Postman
2. Clique em **Import**
3. Selecione o arquivo `DistriSchool-Grade-Management-Service.postman_environment.json`
4. Clique em **Import**
5. Selecione o ambiente **"Grade Management Service - Local"** no dropdown de ambientes

## 🔧 Configuração Inicial

### 1. Configurar Variáveis de Ambiente

Certifique-se de que as seguintes variáveis estão configuradas:

- `baseUrl`: `http://localhost:8083` (ou a URL do seu serviço)
- `token`: Seu token JWT do Auth0 (obtenha via login)
- `studentId`: ID de um estudante existente (ex: `1`)
- `teacherId`: ID de um professor existente (ex: `1`)
- `evaluationId`: ID de uma avaliação existente (ex: `1`)

### 2. Obter Token JWT

Para obter um token JWT válido:

1. Faça login no Auth Service:
   ```
   POST http://localhost:8080/api/v1/auth/login
   {
     "email": "admin@email.com",
     "password": "senha123"
   }
   ```

2. Copie o `access_token` da resposta

3. Cole no campo `token` do ambiente Postman

### 3. Verificar IDs Existentes

Antes de criar notas, você precisa ter:
- Um estudante criado no Student Service (anote o ID)
- Um professor criado no Teacher Service (anote o ID)
- Uma avaliação criada (anote o ID)

## 📚 Estrutura da Coleção

A coleção está organizada em 4 seções:

### 1. Health Checks
- Health Check básico
- Health Info
- Actuator Health
- Actuator Metrics
- Actuator Prometheus

### 2. Grades (Notas)
- Criar Nota (POST)
- Buscar Nota por ID (GET)
- Listar Todas as Notas (GET)
- Buscar Notas por Estudante (GET)
- Buscar Notas por Avaliação (GET)
- Calcular Média do Estudante (GET)
- Atualizar Nota (PUT)
- Deletar Nota (DELETE)

### 3. Testes de Validação
- Criar Nota com Valor Inválido (> 10)
- Criar Nota com Valor Inválido (< 0)
- Criar Nota sem Campos Obrigatórios
- Criar Nota com Estudante Inexistente
- Criar Nota com Professor Inexistente
- Criar Nota Duplicada
- Buscar Nota Inexistente

### 4. Testes de Autenticação
- Listar Notas sem Token
- Criar Nota sem Token

## 🧪 Como Testar

### Passo 1: Verificar Health Check

1. Selecione **Health Checks > Health Check**
2. Clique em **Send**
3. Deve retornar `200 OK` com informações do serviço

### Passo 2: Criar uma Nota

1. Certifique-se de ter:
   - Token JWT válido no ambiente
   - IDs válidos de student, teacher e evaluation

2. Selecione **Grades > Criar Nota**
3. Verifique o body da requisição
4. Clique em **Send**
5. Se sucesso, deve retornar `201 Created` com a nota criada
6. O `gradeId` será automaticamente atualizado no ambiente

### Passo 3: Buscar a Nota Criada

1. Selecione **Grades > Buscar Nota por ID**
2. O `{{gradeId}}` será usado automaticamente
3. Clique em **Send**
4. Deve retornar `200 OK` com os dados da nota

### Passo 4: Testar Validações

1. Selecione **Testes de Validação > Criar Nota com Valor Inválido (> 10)**
2. Clique em **Send**
3. Deve retornar `400 Bad Request` com mensagem de erro

### Passo 5: Testar Autenticação

1. Remova ou deixe vazio o campo `token` no ambiente
2. Selecione **Testes de Autenticação > Listar Notas sem Token**
3. Clique em **Send**
4. Deve retornar `401 Unauthorized`

## 📝 Variáveis Automáticas

A coleção atualiza automaticamente:
- `gradeId`: Após criar uma nota com sucesso, o ID é salvo no ambiente

## 🔄 Fluxo de Teste Completo

### 1. Preparação
```
1. Verificar Health Check
2. Obter Token JWT
3. Criar/Verificar Estudante (Student Service)
4. Criar/Verificar Professor (Teacher Service)
```

### 2. Teste de Criação
```
1. Criar Nota (deve validar student e teacher)
2. Verificar se evento Kafka foi publicado
3. Buscar Nota criada
```

### 3. Teste de Consultas
```
1. Listar Todas as Notas
2. Buscar Notas por Estudante
3. Buscar Notas por Avaliação
4. Calcular Média do Estudante
```

### 4. Teste de Atualização
```
1. Atualizar Nota
2. Verificar mudanças
3. Verificar evento Kafka publicado
```

### 5. Teste de Deleção
```
1. Deletar Nota (soft delete)
2. Tentar buscar nota deletada (deve retornar 404)
3. Verificar evento Kafka publicado
```

## 💡 Dicas

1. **Variáveis de Ambiente**: Use as variáveis `{{baseUrl}}`, `{{token}}`, etc. para facilitar testes em diferentes ambientes

2. **Testes Automáticos**: Cada requisição tem testes automáticos (veja aba "Tests")

3. **Runner do Postman**: Use o Collection Runner para executar todos os testes automaticamente

4. **Monitoramento**: Após criar/atualizar/deletar notas, verifique o Kafka UI para ver os eventos publicados

5. **Logs**: Acompanhe os logs do serviço para ver as validações de integração funcionando

## 🐛 Troubleshooting

### Erro 401 Unauthorized
- Verifique se o token JWT está válido
- Verifique se o token não expirou
- Obtenha um novo token

### Erro 400 Bad Request - Estudante não encontrado
- Verifique se o `studentId` existe no Student Service
- Crie um estudante primeiro

### Erro 400 Bad Request - Professor não encontrado
- Verifique se o `teacherId` existe no Teacher Service
- Crie um professor primeiro

### Erro 404 Not Found
- Verifique se o serviço está rodando
- Verifique se a URL está correta
- Verifique se o ID existe

### Erro de Conexão
- Verifique se o serviço está rodando em `http://localhost:8083`
- Verifique se não há firewall bloqueando
- Verifique os logs do serviço


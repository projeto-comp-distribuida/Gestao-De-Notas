# Requisitos Básicos - Serviço de Notas

## 🎯 O Básico para Funcionar

### 1. **Infraestrutura (Obrigatório)**

#### ✅ PostgreSQL
- **Porta**: 5432 (padrão)
- **Banco de dados**: `distrischool_grades`
- **Usuário**: `distrischool`
- **Senha**: `distrischool123`
- **Função**: Armazenar notas, avaliações e assessments

#### ✅ Redis (Opcional mas recomendado)
- **Porta**: 6379 (padrão)
- **Função**: Cache para melhorar performance
- **Nota**: O serviço funciona sem Redis, mas será mais lento

#### ✅ Kafka (Opcional mas recomendado)
- **Porta**: 9092 (padrão)
- **Função**: Publicar eventos de criação/atualização/deleção de notas
- **Nota**: O serviço funciona sem Kafka, mas não publicará eventos

### 2. **Serviços Externos (Obrigatórios para validação)**

#### ✅ Student Service
- **URL**: `http://student-management-service-dev:8080` (ou configurável)
- **Função**: Validar se estudante existe antes de criar nota
- **Nota**: Se não estiver disponível, criação de nota falhará

#### ✅ Teacher Service
- **URL**: `http://teacher-management-service-dev:8080` (ou configurável)
- **Função**: Validar se professor existe antes de criar nota
- **Nota**: Se não estiver disponível, criação de nota falhará

#### ✅ Auth Service (Obrigatório para autenticação)
- **URL**: `http://auth-service-dev:8080` (ou configurável)
- **Função**: Validar JWT tokens
- **Nota**: Sem Auth Service, endpoints protegidos retornarão 401/403

### 3. **Configurações Mínimas**

#### Variáveis de Ambiente Essenciais

```bash
# Banco de dados
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/distrischool_grades
SPRING_DATASOURCE_USERNAME=distrischool
SPRING_DATASOURCE_PASSWORD=distrischool123

# Auth0 (para autenticação)
AUTH0_DOMAIN=your-tenant.auth0.com
AUTH0_ISSUER_URI=https://your-tenant.auth0.com/
AUTH0_AUDIENCE=your-api-identifier

# URLs dos serviços (opcional - tem defaults)
AUTH_SERVICE_URL=http://auth-service-dev:8080
STUDENT_SERVICE_URL=http://student-management-service-dev:8080
TEACHER_SERVICE_URL=http://teacher-management-service-dev:8080

# Kafka (opcional)
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis (opcional)
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# Desabilitar segurança (apenas para testes)
SECURITY_DISABLE=false
```

### 4. **Requisitos de Software**

- **Java**: 17+ (obrigatório)
- **Maven**: 3.8+ (para build)
- **Docker**: (opcional, para rodar infraestrutura)

## 📊 Resumo: O Que É Essencial vs Opcional

| Componente | Status | O Que Acontece Se Não Estiver |
|-----------|--------|-------------------------------|
| **PostgreSQL** | 🔴 **Obrigatório** | Serviço não inicia |
| **Student Service** | 🟡 **Recomendado** | Não consegue criar notas (validação falha) |
| **Teacher Service** | 🟡 **Recomendado** | Não consegue criar notas (validação falha) |
| **Auth Service** | 🟡 **Recomendado** | Endpoints protegidos retornam 401/403 |
| **Redis** | 🟢 **Opcional** | Funciona, mas sem cache (mais lento) |
| **Kafka** | 🟢 **Opcional** | Funciona, mas não publica eventos |

## 🚀 Setup Mínimo para Testar

### Opção 1: Apenas PostgreSQL (Mínimo Absoluto)

```bash
# 1. Iniciar PostgreSQL
docker run -d \
  --name postgres-grades \
  -e POSTGRES_DB=distrischool_grades \
  -e POSTGRES_USER=distrischool \
  -e POSTGRES_PASSWORD=distrischool123 \
  -p 5432:5432 \
  postgres:15

# 2. Desabilitar segurança (para testes sem Auth)
export SECURITY_DISABLE=true

# 3. Rodar o serviço
cd Gestao-De-Notas
mvn spring-boot:run
```

**Limitações**:
- ❌ Não consegue criar notas (precisa validar student/teacher)
- ✅ Consegue listar notas (se já existirem)
- ✅ Health check funciona

### Opção 2: PostgreSQL + Student/Teacher Services (Funcional)

```bash
# 1. Iniciar PostgreSQL
docker run -d --name postgres-grades \
  -e POSTGRES_DB=distrischool_grades \
  -e POSTGRES_USER=distrischool \
  -e POSTGRES_PASSWORD=distrischool123 \
  -p 5432:5432 postgres:15

# 2. Iniciar Student Service (em outro terminal)
cd Gestao-de-Alunos
mvn spring-boot:run

# 3. Iniciar Teacher Service (em outro terminal)
cd Gestao-De-Professores
mvn spring-boot:run

# 4. Desabilitar segurança (para testes)
export SECURITY_DISABLE=true

# 5. Rodar Grade Service
cd Gestao-De-Notas
mvn spring-boot:run
```

**Funcionalidades**:
- ✅ Consegue criar notas (valida student e teacher)
- ✅ Consegue listar notas
- ✅ Consegue buscar notas por estudante
- ❌ Endpoints protegidos não funcionam (sem Auth Service)

### Opção 3: Setup Completo (Produção)

```bash
# 1. Iniciar toda infraestrutura
docker-compose up -d postgres redis kafka

# 2. Iniciar todos os serviços
# - Auth Service
# - Student Service
# - Teacher Service
# - Grade Service

# 3. Configurar variáveis de ambiente
export AUTH0_DOMAIN=your-tenant.auth0.com
export AUTH0_AUDIENCE=your-api-identifier
export SECURITY_DISABLE=false
```

**Funcionalidades**:
- ✅ Tudo funciona
- ✅ Autenticação completa
- ✅ Cache Redis
- ✅ Eventos Kafka

## 📝 Checklist Rápido

Para o serviço funcionar **básico**:

- [ ] PostgreSQL rodando na porta 5432
- [ ] Banco `distrischool_grades` criado
- [ ] Java 17 instalado
- [ ] `SECURITY_DISABLE=true` (se não tiver Auth Service)

Para o serviço funcionar **completo**:

- [ ] PostgreSQL rodando
- [ ] Student Service rodando
- [ ] Teacher Service rodando
- [ ] Auth Service rodando (ou `SECURITY_DISABLE=true`)
- [ ] Redis rodando (opcional)
- [ ] Kafka rodando (opcional)

## 🔧 Comando Rápido para Testar

```bash
# Verificar se está funcionando
curl http://localhost:8083/api/v1/health

# Se retornar JSON com status, está funcionando! ✅
```

## ⚠️ Notas Importantes

1. **Java 17 é obrigatório** - Não funciona com Java 25 ou outras versões
2. **PostgreSQL é obrigatório** - O serviço não inicia sem banco
3. **Student/Teacher Services** - Necessários para criar notas (validação)
4. **Auth Service** - Necessário para endpoints protegidos funcionarem
5. **Redis e Kafka** - Opcionais, mas melhoram performance e integração


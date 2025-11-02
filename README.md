# Grade Management Service - DistriSchool

Microserviço de gestão de notas e avaliações para o sistema DistriSchool.

## 📋 Visão Geral

Este microserviço é responsável por:
- Gerenciar notas de estudantes
- Gerenciar avaliações/exames
- Gerenciar avaliações consolidadas (assessments)
- Validar integridade referencial com estudantes e professores
- Publicar eventos via Kafka
- Consumir eventos de outros microserviços

## 🏗️ Arquitetura

### Entidades Principais

- **Grade**: Representa uma nota individual de um estudante em uma avaliação
- **Evaluation**: Representa uma avaliação/exame atribuída a uma disciplina
- **Assessment**: Representa a avaliação consolidada final de um estudante em uma disciplina

### Integrações

#### Integração Síncrona (Feign)

- **StudentServiceClient**: Comunicação com microserviço de estudantes
  - Validação de existência de estudantes antes de criar notas
  - URL configurável: `microservice.student.url`

- **TeacherServiceClient**: Comunicação com microserviço de professores
  - Validação de existência de professores antes de criar notas
  - URL configurável: `microservice.teacher.url`

#### Integração Assíncrona (Kafka)

**Tópicos Publicados**:
- `distrischool.grade.created` - Quando uma nota é criada
- `distrischool.grade.updated` - Quando uma nota é atualizada
- `distrischool.grade.deleted` - Quando uma nota é deletada

**Tópicos Consumidos**:
- `distrischool.student.created` - Quando um estudante é criado
- `distrischool.student.updated` - Quando um estudante é atualizado
- `distrischool.student.deleted` - Quando um estudante é deletado
- `distrischool.teacher.created` - Quando um professor é criado

## 🚀 Como Executar

### Pré-requisitos

- Java 17+
- Maven 3.8+
- Docker e Docker Compose
- PostgreSQL 15+
- Redis 7+
- Apache Kafka

### Desenvolvimento Local

```bash
# 1. Clone o repositório (se necessário)
git clone <repository-url>
cd Gestao-De-Notas

# 2. Inicie os serviços de infraestrutura
docker-compose up -d postgres redis zookeeper kafka

# 3. Execute a aplicação
mvn spring-boot:run

# Ou usando Docker
docker-compose up -d grade-management-service-dev
```

### Configuração

As configurações principais estão em `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: grade-management-service
  datasource:
    url: jdbc:postgresql://localhost:5432/distrischool_grades
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092

microservice:
  student:
    url: http://student-management-service-dev:8080
  teacher:
    url: http://microservice-template-dev:8080
```

## 📡 Endpoints REST

### Health Check

```
GET /api/v1/health
GET /api/v1/health/info
```

### Grades (Notas)

```
POST   /api/v1/grades              - Criar nota
GET    /api/v1/grades              - Listar notas (paginação)
GET    /api/v1/grades/{id}         - Buscar nota por ID
PUT    /api/v1/grades/{id}         - Atualizar nota
DELETE /api/v1/grades/{id}        - Deletar nota
GET    /api/v1/grades/student/{studentId} - Buscar notas de um estudante
GET    /api/v1/grades/evaluation/{evaluationId} - Buscar notas de uma avaliação
GET    /api/v1/grades/student/{studentId}/average - Calcular média de um estudante
```

### Exemplo de Requisição

```bash
# Criar nota
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

## 🔐 Segurança

- Autenticação OAuth2 com Auth0
- JWT tokens obrigatórios (exceto endpoints de health)
- Validação de roles via SecurityContext

Para desabilitar segurança em desenvolvimento:
```yaml
security:
  disable: true
```

## 📊 Monitoramento

### Actuator Endpoints

```
GET /actuator/health       - Health check detalhado
GET /actuator/metrics     - Métricas da aplicação
GET /actuator/prometheus  - Métricas Prometheus
GET /actuator/info        - Informações da aplicação
```

### Logs

Os logs estão configurados para incluir:
- Requests HTTP
- Validações de integração
- Eventos Kafka
- Erros e exceções

## 🧪 Testes

### Testes de Integração

Execute o script de teste:

```bash
./test-integration.sh
```

Veja [INTEGRATION_TEST.md](./INTEGRATION_TEST.md) para detalhes completos.

### Testes Unitários

```bash
mvn test
```

## 🔄 Fluxo de Integração

### Criação de Nota

1. Cliente faz POST `/api/v1/grades`
2. GradeService valida dados da requisição
3. GradeService valida existência do estudante (via Feign → Student Service)
4. GradeService valida existência do professor (via Feign → Teacher Service)
5. GradeService verifica se já existe nota para a avaliação
6. GradeService salva a nota no banco
7. GradeService publica evento Kafka `grade.created`
8. EventConsumer do Student Service recebe evento (se configurado)
9. Retorna resposta ao cliente

### Consumo de Eventos

1. Student Service publica evento `student.created`
2. EventConsumer do Grade Service recebe evento
3. EventConsumer processa evento (logs, sincronização, etc.)

## 🛠️ Tecnologias

- **Spring Boot 3.2.0**
- **Spring Data JPA** - Persistência
- **PostgreSQL** - Banco de dados
- **Redis** - Cache
- **Apache Kafka** - Mensageria
- **Spring Cloud OpenFeign** - Comunicação entre serviços
- **Resilience4j** - Circuit Breaker e Retry
- **Flyway** - Migrações de banco
- **Auth0** - Autenticação
- **Prometheus** - Métricas

## 📚 Documentação Adicional

- [INTEGRATION_TEST.md](./INTEGRATION_TEST.md) - Guia completo de testes de integração
- [DEVELOPMENT.md](./DEVELOPMENT.md) - Guia de desenvolvimento
- [TEMPLATE_USAGE.md](./TEMPLATE_USAGE.md) - Documentação do template

## 🤝 Contribuindo

1. Siga os padrões de código estabelecidos
2. Adicione testes para novas funcionalidades
3. Atualize a documentação
4. Siga os padrões de commit (Conventional Commits)

## 📝 Licença

Este projeto faz parte do DistriSchool e segue a licença do projeto principal.

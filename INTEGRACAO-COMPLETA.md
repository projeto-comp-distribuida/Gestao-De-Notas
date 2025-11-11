# Integração Completa - Sistema de Notas

## Visão Geral

O sistema de notas (Gestao-De-Notas) está integrado com os seguintes serviços:
- **Auth Service**: Autenticação e autorização
- **Student Service**: Gestão de estudantes
- **Teacher Service**: Gestão de professores

## Integrações Síncronas (Feign)

### 1. Student Service Client
- **URL**: `http://student-management-service-dev:8080`
- **Endpoints**:
  - `GET /api/v1/students/{id}` - Busca estudante por ID
- **Uso**: Validação de existência de estudantes antes de criar notas

### 2. Teacher Service Client
- **URL**: `http://teacher-management-service-dev:8080`
- **Endpoints**:
  - `GET /api/v1/teachers/{id}` - Busca professor por ID
- **Uso**: Validação de existência de professores antes de criar notas

### 3. Auth Service Client
- **URL**: `http://auth-service-dev:8080`
- **Endpoints**:
  - `GET /api/v1/users/{userId}` - Busca usuário por ID
  - `GET /api/v1/users/{userId}/has-role?role={role}` - Verifica role do usuário
  - `GET /api/v1/users/auth0/{auth0Id}` - Busca usuário por Auth0 ID
- **Uso**: Validação de autorização e permissões (preparado para uso futuro)

## Integrações Assíncronas (Kafka)

### Eventos Publicados

#### Grade Events
- **distrischool.grade.created** - Quando uma nota é criada
- **distrischool.grade.updated** - Quando uma nota é atualizada
- **distrischool.grade.deleted** - Quando uma nota é deletada

### Eventos Consumidos

#### Student Events
- **distrischool.student.created** - Quando um estudante é criado
- **distrischool.student.updated** - Quando um estudante é atualizado
- **distrischool.student.deleted** - Quando um estudante é deletado

#### Teacher Events
- **distrischool.teacher.created** - Quando um professor é criado
- **distrischool.teacher.updated** - Quando um professor é atualizado
- **distrischool.teacher.deleted** - Quando um professor é deletado

## Autenticação e Autorização

### Spring Security
- **JWT Validation**: Tokens JWT do Auth0 são validados automaticamente
- **Role-Based Access Control**: Usa `@PreAuthorize` para controlar acesso
- **Roles Suportadas**: `ADMIN`, `TEACHER`, `STUDENT`, `PARENT`

### Endpoints Protegidos

#### Requerem Role TEACHER ou ADMIN:
- `POST /api/v1/grades` - Criar nota
- `PUT /api/v1/grades/{id}` - Atualizar nota
- `DELETE /api/v1/grades/{id}` - Deletar nota

#### Públicos (após autenticação):
- `GET /api/v1/grades` - Listar notas
- `GET /api/v1/grades/{id}` - Buscar nota por ID
- `GET /api/v1/grades/student/{studentId}` - Buscar notas por aluno
- `GET /api/v1/grades/evaluation/{evaluationId}` - Buscar notas por avaliação
- `GET /api/v1/grades/student/{studentId}/average` - Calcular média

## Enriquecimento de Dados

O `GradeResponseDTO` é enriquecido automaticamente com:
- **StudentInfo**: Nome, email, matrícula, curso do estudante
- **TeacherInfo**: Nome, email, matrícula do professor

Os dados são buscados via Feign dos serviços correspondentes de forma opcional (não falha se não conseguir buscar).

## Validações

### Validações de Negócio
- Nota deve estar entre 0 e 10
- Não pode existir duas notas para o mesmo aluno na mesma avaliação
- Estudante deve existir no Student Service
- Professor deve existir no Teacher Service

### Validações de Autorização
- Apenas professores e admins podem criar/atualizar/deletar notas
- Validação feita via Spring Security `@PreAuthorize`

## Configuração

### application.yml
```yaml
microservice:
  auth:
    url: ${AUTH_SERVICE_URL:http://auth-service-dev:8080}
  student:
    url: ${STUDENT_SERVICE_URL:http://student-management-service-dev:8080}
  teacher:
    url: ${TEACHER_SERVICE_URL:http://teacher-management-service-dev:8080}
  kafka:
    topics:
      grade-created: distrischool.grade.created
      grade-updated: distrischool.grade.updated
      grade-deleted: distrischool.grade.deleted
      student-created: distrischool.student.created
      student-updated: distrischool.student.updated
      student-deleted: distrischool.student.deleted
      teacher-created: distrischool.teacher.created
      teacher-updated: distrischool.teacher.updated
      teacher-deleted: distrischool.teacher.deleted
```

## Circuit Breaker

Resilience4j está configurado para:
- Circuit Breaker: Protege contra falhas em cascata
- Retry: Tenta novamente em caso de falhas temporárias
- Timeout: Evita requisições muito lentas

## Cache

Redis está configurado para cachear:
- Notas por ID
- Listas de notas (invalidado ao criar/atualizar/deletar)

## Próximos Passos

1. ✅ Integração com Student Service
2. ✅ Integração com Teacher Service
3. ✅ Integração com Auth Service (preparado)
4. ✅ Eventos Kafka
5. ✅ Validação de autorização
6. ✅ Enriquecimento de dados
7. ⏳ Testes de integração end-to-end
8. ⏳ Monitoramento e métricas


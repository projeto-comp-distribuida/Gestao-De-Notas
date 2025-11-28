# API Documentation - Gestão de Notas (Grade Management)

Esta documentação descreve todos os endpoints disponíveis no microserviço de Gestão de Notas para integração com o frontend.

## 📋 Índice

- [Base URL e Autenticação](#base-url-e-autenticação)
- [Estrutura de Resposta](#estrutura-de-resposta)
- [Endpoints](#endpoints)
  - [CRUD de Notas](#crud-de-notas)
  - [Consultas e Relatórios](#consultas-e-relatórios)
- [DTOs (Data Transfer Objects)](#dtos-data-transfer-objects)
- [Tratamento de Erros](#tratamento-de-erros)
- [Paginação](#paginação)

---

## Base URL e Autenticação

### Base URL

```
Desenvolvimento: http://localhost:8083/api/v1
Produção: https://api.distrischool.com/api/v1
```

### Autenticação

Todos os endpoints (exceto health checks) requerem autenticação via **JWT Bearer Token**.

**Header obrigatório:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Header opcional (para identificar o usuário que está fazendo a requisição):**
```
X-User-Id: <user_id>
```

> **Nota:** Se o header `X-User-Id` não for fornecido, o sistema utilizará o `subject` do JWT token para identificar o usuário.

---

## Estrutura de Resposta

Todas as respostas da API seguem o formato padronizado `ApiResponse<T>`:

```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string; // ISO 8601 format (LocalDateTime)
}
```

### Exemplo de Resposta de Sucesso

```json
{
  "success": true,
  "message": "Nota criada com sucesso",
  "data": {
    "id": 1,
    "studentId": 123,
    "teacherId": 456,
    "classId": 789,
    "evaluationId": 10,
    "gradeValue": 8.5,
    "gradeDate": "2024-11-02",
    "notes": "Bom desempenho",
    "status": "REGISTERED",
    "isAutomatic": false,
    "postedAt": "2024-11-02T10:30:00",
    "academicYear": 2024,
    "academicSemester": 2,
    "createdAt": "2024-11-02T10:30:00",
    "updatedAt": "2024-11-02T10:30:00",
    "createdBy": "user123",
    "updatedBy": "user123"
  },
  "timestamp": "2024-11-02T10:30:00"
}
```

### Exemplo de Resposta de Erro

```json
{
  "success": false,
  "message": "Erro de validação",
  "data": {
    "gradeValue": "Nota deve ser menor ou igual a 10",
    "academicYear": "Ano letivo é obrigatório"
  },
  "timestamp": "2024-11-02T10:30:00"
}
```

---

## Endpoints

### CRUD de Notas

#### 1. Criar Nota

Cria uma nova nota para um aluno em uma avaliação.

**Endpoint:** `POST /api/v1/grades`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <user_id> (opcional)
```

**Request Body:**
```json
{
  "studentId": 123,
  "teacherId": 456,
  "classId": 789,
  "evaluationId": 10,
  "gradeValue": 8.5,
  "gradeDate": "2024-11-02",
  "notes": "Bom desempenho na avaliação",
  "status": "REGISTERED",
  "isAutomatic": false,
  "academicYear": 2024,
  "academicSemester": 2
}
```

**Campos Obrigatórios:**
- `studentId` (Long): ID do aluno
- `teacherId` (Long): ID do professor
- `classId` (Long): ID da turma
- `evaluationId` (Long): ID da avaliação
- `gradeValue` (BigDecimal): Valor da nota (0.0 a 10.0, máximo 2 casas decimais)
- `gradeDate` (LocalDate): Data da avaliação (formato: YYYY-MM-DD)
- `academicYear` (Integer): Ano letivo (mínimo: 2000)
- `academicSemester` (Integer): Semestre letivo (1 ou 2)

**Campos Opcionais:**
- `notes` (String): Observações sobre a nota
- `status` (GradeStatus): Status da nota (padrão: REGISTERED)
- `isAutomatic` (Boolean): Indica se a nota foi gerada automaticamente

**Validações:**
- `gradeValue`: Deve estar entre 0.0 e 10.0
- `gradeValue`: Máximo 2 casas decimais
- `academicSemester`: Deve ser 1 ou 2
- `academicYear`: Deve ser >= 2000

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Nota criada com sucesso",
  "data": { /* GradeResponseDTO */ }
}
```

**Possíveis Erros:**
- `400 Bad Request`: Erro de validação
- `404 Not Found`: Aluno, professor, turma ou avaliação não encontrados
- `409 Conflict`: Já existe uma nota para este aluno nesta avaliação
- `401 Unauthorized`: Token inválido ou ausente

---

#### 2. Buscar Nota por ID

Retorna os detalhes de uma nota específica.

**Endpoint:** `GET /api/v1/grades/{id}`

**Path Parameters:**
- `id` (Long): ID da nota

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Operação realizada com sucesso",
  "data": { /* GradeResponseDTO */ }
}
```

**Possíveis Erros:**
- `404 Not Found`: Nota não encontrada
- `401 Unauthorized`: Token inválido ou ausente

---

#### 3. Listar Todas as Notas

Lista todas as notas com paginação e ordenação.

**Endpoint:** `GET /api/v1/grades`

**Query Parameters:**
- `page` (int, padrão: 0): Número da página (começa em 0)
- `size` (int, padrão: 20): Tamanho da página
- `sortBy` (string, padrão: "id"): Campo para ordenação
- `direction` (string, padrão: "ASC"): Direção da ordenação (ASC ou DESC)

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Exemplo de Requisição:**
```
GET /api/v1/grades?page=0&size=20&sortBy=gradeDate&direction=DESC
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Operação realizada com sucesso",
  "data": {
    "content": [ /* Array de GradeResponseDTO */ ],
    "pageable": { /* Pageable object */ },
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0,
    "first": true,
    "last": false,
    "numberOfElements": 20
  }
}
```

**Possíveis Erros:**
- `401 Unauthorized`: Token inválido ou ausente

---

#### 4. Atualizar Nota

Atualiza uma nota existente.

**Endpoint:** `PUT /api/v1/grades/{id}`

**Path Parameters:**
- `id` (Long): ID da nota a ser atualizada

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <user_id> (opcional)
```

**Request Body:**
```json
{
  "studentId": 123,
  "teacherId": 456,
  "classId": 789,
  "evaluationId": 10,
  "gradeValue": 9.0,
  "gradeDate": "2024-11-02",
  "notes": "Nota atualizada após revisão",
  "status": "CONFIRMED",
  "isAutomatic": false,
  "academicYear": 2024,
  "academicSemester": 2
}
```

> **Nota:** Todos os campos devem ser enviados, mesmo que não tenham sido alterados.

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Nota atualizada com sucesso",
  "data": { /* GradeResponseDTO */ }
}
```

**Possíveis Erros:**
- `400 Bad Request`: Erro de validação
- `404 Not Found`: Nota não encontrada
- `401 Unauthorized`: Token inválido ou ausente

---

#### 5. Deletar Nota

Realiza soft delete de uma nota (não remove fisicamente do banco).

**Endpoint:** `DELETE /api/v1/grades/{id}`

**Path Parameters:**
- `id` (Long): ID da nota a ser deletada

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <user_id> (opcional)
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Nota deletada com sucesso",
  "data": null
}
```

**Possíveis Erros:**
- `404 Not Found`: Nota não encontrada
- `401 Unauthorized`: Token inválido ou ausente

---

### Consultas e Relatórios

#### 6. Buscar Notas por Aluno

Retorna todas as notas de um aluno específico.

**Endpoint:** `GET /api/v1/grades/student/{studentId}`

**Path Parameters:**
- `studentId` (Long): ID do aluno

**Query Parameters:**
- `page` (int, padrão: 0): Número da página
- `size` (int, padrão: 20): Tamanho da página

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Exemplo de Requisição:**
```
GET /api/v1/grades/student/123?page=0&size=20
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Operação realizada com sucesso",
  "data": {
    "content": [ /* Array de GradeResponseDTO ordenado por gradeDate DESC */ ],
    "totalElements": 15,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

> **Nota:** As notas são ordenadas por `gradeDate` em ordem decrescente (mais recente primeiro).

**Possíveis Erros:**
- `404 Not Found`: Aluno não encontrado
- `401 Unauthorized`: Token inválido ou ausente

---

#### 7. Buscar Notas por Avaliação

Retorna todas as notas de uma avaliação específica.

**Endpoint:** `GET /api/v1/grades/evaluation/{evaluationId}`

**Path Parameters:**
- `evaluationId` (Long): ID da avaliação

**Query Parameters:**
- `page` (int, padrão: 0): Número da página
- `size` (int, padrão: 20): Tamanho da página

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Exemplo de Requisição:**
```
GET /api/v1/grades/evaluation/10?page=0&size=20
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Operação realizada com sucesso",
  "data": {
    "content": [ /* Array de GradeResponseDTO ordenado por gradeValue ASC */ ],
    "totalElements": 30,
    "totalPages": 2,
    "size": 20,
    "number": 0
  }
}
```

> **Nota:** As notas são ordenadas por `gradeValue` em ordem crescente (menor para maior).

**Possíveis Erros:**
- `404 Not Found`: Avaliação não encontrada
- `401 Unauthorized`: Token inválido ou ausente

---

#### 8. Calcular Média do Aluno

Calcula a média de um aluno em um período letivo específico.

**Endpoint:** `GET /api/v1/grades/student/{studentId}/average`

**Path Parameters:**
- `studentId` (Long): ID do aluno

**Query Parameters:**
- `academicYear` (Integer, obrigatório): Ano letivo
- `academicSemester` (Integer, obrigatório): Semestre letivo (1 ou 2)

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Exemplo de Requisição:**
```
GET /api/v1/grades/student/123/average?academicYear=2024&academicSemester=2
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Média calculada com sucesso",
  "data": 8.25
}
```

> **Nota:** O valor retornado é um `BigDecimal` representando a média aritmética de todas as notas do aluno no período especificado.

**Possíveis Erros:**
- `400 Bad Request`: Parâmetros obrigatórios ausentes ou inválidos
- `404 Not Found`: Aluno não encontrado
- `401 Unauthorized`: Token inválido ou ausente

---

#### 9. Listar Notas da Turma

Retorna um resumo detalhado das notas de uma turma, agrupadas por aluno.

**Endpoint:** `GET /api/v1/grades/classes/{classId}/grades`

**Path Parameters:**
- `classId` (Long): ID da turma

**Query Parameters:**
- `academicYear` (Integer, opcional): Filtrar por ano letivo
- `academicSemester` (Integer, opcional): Filtrar por semestre letivo
- `maxGradesPerStudent` (int, padrão: 3): Número máximo de notas por aluno a serem retornadas

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Exemplo de Requisição:**
```
GET /api/v1/grades/classes/789/grades?academicYear=2024&academicSemester=2&maxGradesPerStudent=5
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Notas da turma recuperadas com sucesso",
  "data": {
    "classId": 789,
    "className": "Turma A - 3º Ano",
    "classCode": "3A-2024",
    "period": "Manhã",
    "academicYear": "2024",
    "totalStudents": 30,
    "studentsWithGrades": 28,
    "maxGradesPerStudent": 5,
    "classAverage": 7.85,
    "students": [
      {
        "studentId": 123,
        "average": 8.5,
        "grades": [
          {
            "gradeId": 1,
            "evaluationId": 10,
            "gradeValue": 9.0,
            "gradeDate": "2024-11-02",
            "academicYear": 2024,
            "academicSemester": 2
          },
          {
            "gradeId": 2,
            "evaluationId": 11,
            "gradeValue": 8.0,
            "gradeDate": "2024-11-15",
            "academicYear": 2024,
            "academicSemester": 2
          }
        ]
      }
    ]
  }
}
```

**Possíveis Erros:**
- `404 Not Found`: Turma não encontrada
- `401 Unauthorized`: Token inválido ou ausente

---

#### 10. Calcular Média da Turma

Calcula a média consolidada de uma turma.

**Endpoint:** `GET /api/v1/grades/classes/{classId}/average`

**Path Parameters:**
- `classId` (Long): ID da turma

**Query Parameters:**
- `academicYear` (Integer, opcional): Filtrar por ano letivo
- `academicSemester` (Integer, opcional): Filtrar por semestre letivo
- `maxGradesPerStudent` (int, padrão: 3): Número máximo de notas por aluno a serem consideradas no cálculo

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Exemplo de Requisição:**
```
GET /api/v1/grades/classes/789/average?academicYear=2024&academicSemester=2&maxGradesPerStudent=3
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Média da turma calculada com sucesso",
  "data": 7.85
}
```

**Possíveis Erros:**
- `404 Not Found`: Turma não encontrada
- `401 Unauthorized`: Token inválido ou ausente

---

#### 11. Calcular Média Global (Todas as Turmas)

Calcula a média global considerando todas as turmas do sistema.

**Endpoint:** `GET /api/v1/grades/classes/average`

**Query Parameters:**
- `academicYear` (Integer, opcional): Filtrar por ano letivo
- `academicSemester` (Integer, opcional): Filtrar por semestre letivo
- `maxGradesPerStudent` (int, padrão: 3): Número máximo de notas por aluno a serem consideradas no cálculo

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Exemplo de Requisição:**
```
GET /api/v1/grades/classes/average?academicYear=2024&academicSemester=2
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Média global calculada com sucesso",
  "data": 7.92
}
```

**Possíveis Erros:**
- `401 Unauthorized`: Token inválido ou ausente

---

## DTOs (Data Transfer Objects)

### GradeRequestDTO

DTO usado para criar e atualizar notas.

```typescript
interface GradeRequestDTO {
  studentId: number;           // Obrigatório
  teacherId: number;           // Obrigatório
  classId: number;            // Obrigatório
  evaluationId: number;       // Obrigatório
  gradeValue: number;          // Obrigatório, 0.0 a 10.0, máximo 2 casas decimais
  gradeDate: string;          // Obrigatório, formato: "YYYY-MM-DD"
  notes?: string;             // Opcional
  status?: GradeStatus;       // Opcional, padrão: "REGISTERED"
  isAutomatic?: boolean;      // Opcional
  academicYear: number;       // Obrigatório, mínimo: 2000
  academicSemester: number;   // Obrigatório, 1 ou 2
}
```

### GradeResponseDTO

DTO retornado nas respostas da API.

```typescript
interface GradeResponseDTO {
  id: number;
  studentId: number;
  teacherId: number;
  classId: number;
  evaluationId: number;
  gradeValue: number;
  gradeDate: string;          // "YYYY-MM-DD"
  notes?: string;
  status: GradeStatus;
  isAutomatic: boolean;
  postedAt?: string;          // "YYYY-MM-DDTHH:mm:ss"
  academicYear: number;
  academicSemester: number;
  createdAt: string;          // "YYYY-MM-DDTHH:mm:ss"
  updatedAt: string;          // "YYYY-MM-DDTHH:mm:ss"
  createdBy: string;
  updatedBy: string;
}
```

### GradeStatus (Enum)

Valores possíveis para o status da nota:

```typescript
enum GradeStatus {
  REGISTERED = "REGISTERED",   // Nota registrada
  PENDING = "PENDING",         // Aguardando confirmação
  CONFIRMED = "CONFIRMED",     // Confirmada
  DISPUTED = "DISPUTED",       // Em disputa/recurso
  CANCELLED = "CANCELLED"      // Cancelada
}
```

### ClassGradeSummaryDTO

DTO retornado no endpoint de listagem de notas da turma.

```typescript
interface ClassGradeSummaryDTO {
  classId: number;
  className: string;
  classCode: string;
  period: string;
  academicYear: string;
  totalStudents: number;
  studentsWithGrades: number;
  maxGradesPerStudent: number;
  classAverage: number;
  students: StudentClassGradeDTO[];
}

interface StudentClassGradeDTO {
  studentId: number;
  average: number;
  grades: GradeSnapshotDTO[];
}

interface GradeSnapshotDTO {
  gradeId: number;
  evaluationId: number;
  gradeValue: number;
  gradeDate: string;          // "YYYY-MM-DD"
  academicYear: number;
  academicSemester: number;
}
```

### Page<T> (Spring Data)

Estrutura de paginação retornada pelos endpoints de listagem.

```typescript
interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
    };
  };
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}
```

---

## Tratamento de Erros

### Códigos de Status HTTP

| Código | Descrição |
|--------|-----------|
| `200` | Sucesso |
| `201` | Criado com sucesso |
| `400` | Erro de validação ou requisição inválida |
| `401` | Não autenticado (token ausente ou inválido) |
| `403` | Não autorizado (sem permissão) |
| `404` | Recurso não encontrado |
| `409` | Conflito (ex: nota duplicada) |
| `500` | Erro interno do servidor |

### Formato de Erro

Todos os erros retornam o formato `ApiResponse`:

```json
{
  "success": false,
  "message": "Mensagem de erro descritiva",
  "data": null,  // ou objeto com detalhes do erro
  "timestamp": "2024-11-02T10:30:00"
}
```

### Erros de Validação (400)

Quando há erros de validação, o campo `data` contém um objeto com os campos inválidos:

```json
{
  "success": false,
  "message": "Erro de validação",
  "data": {
    "gradeValue": "Nota deve ser menor ou igual a 10",
    "academicYear": "Ano letivo é obrigatório",
    "academicSemester": "Semestre deve ser 1 ou 2"
  },
  "timestamp": "2024-11-02T10:30:00"
}
```

### Erro de Recurso Não Encontrado (404)

```json
{
  "success": false,
  "message": "Nota com ID 123 não encontrada",
  "data": null,
  "timestamp": "2024-11-02T10:30:00"
}
```

### Erro de Conflito (409)

```json
{
  "success": false,
  "message": "Já existe uma nota para o aluno 123 na avaliação 10",
  "data": null,
  "timestamp": "2024-11-02T10:30:00"
}
```

---

## Paginação

Todos os endpoints de listagem suportam paginação através dos query parameters:

- `page`: Número da página (começa em 0)
- `size`: Tamanho da página (número de itens por página)
- `sortBy`: Campo para ordenação (padrão: "id")
- `direction`: Direção da ordenação - "ASC" ou "DESC" (padrão: "ASC")

### Exemplo de Uso

```typescript
// Buscar segunda página com 10 itens, ordenado por data decrescente
const response = await fetch(
  '/api/v1/grades?page=1&size=10&sortBy=gradeDate&direction=DESC',
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);
```

### Campos Ordenáveis

Para o endpoint `GET /api/v1/grades`, os seguintes campos podem ser usados em `sortBy`:
- `id`
- `gradeValue`
- `gradeDate`
- `createdAt`
- `updatedAt`
- `academicYear`
- `academicSemester`

---

## Exemplos de Integração

### Exemplo em TypeScript/React

```typescript
// types.ts
interface GradeRequest {
  studentId: number;
  teacherId: number;
  classId: number;
  evaluationId: number;
  gradeValue: number;
  gradeDate: string;
  notes?: string;
  status?: 'REGISTERED' | 'PENDING' | 'CONFIRMED' | 'DISPUTED' | 'CANCELLED';
  isAutomatic?: boolean;
  academicYear: number;
  academicSemester: number;
}

interface GradeResponse {
  id: number;
  studentId: number;
  teacherId: number;
  classId: number;
  evaluationId: number;
  gradeValue: number;
  gradeDate: string;
  notes?: string;
  status: string;
  isAutomatic: boolean;
  postedAt?: string;
  academicYear: number;
  academicSemester: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

// api.ts
const API_BASE_URL = 'http://localhost:8083/api/v1';

async function createGrade(
  grade: GradeRequest,
  token: string
): Promise<ApiResponse<GradeResponse>> {
  const response = await fetch(`${API_BASE_URL}/grades`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(grade)
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Erro ao criar nota');
  }

  return response.json();
}

async function getGradesByStudent(
  studentId: number,
  token: string,
  page: number = 0,
  size: number = 20
): Promise<ApiResponse<Page<GradeResponse>>> {
  const response = await fetch(
    `${API_BASE_URL}/grades/student/${studentId}?page=${page}&size=${size}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Erro ao buscar notas');
  }

  return response.json();
}

async function calculateStudentAverage(
  studentId: number,
  academicYear: number,
  academicSemester: number,
  token: string
): Promise<ApiResponse<number>> {
  const response = await fetch(
    `${API_BASE_URL}/grades/student/${studentId}/average?academicYear=${academicYear}&academicSemester=${academicSemester}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Erro ao calcular média');
  }

  return response.json();
}
```

### Exemplo em JavaScript (Fetch API)

```javascript
// Criar nota
async function createGrade(gradeData, token) {
  try {
    const response = await fetch('http://localhost:8083/api/v1/grades', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(gradeData)
    });

    const result = await response.json();
    
    if (!result.success) {
      throw new Error(result.message);
    }

    return result.data;
  } catch (error) {
    console.error('Erro ao criar nota:', error);
    throw error;
  }
}

// Buscar notas do aluno
async function getStudentGrades(studentId, token, page = 0, size = 20) {
  try {
    const response = await fetch(
      `http://localhost:8083/api/v1/grades/student/${studentId}?page=${page}&size=${size}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    );

    const result = await response.json();
    
    if (!result.success) {
      throw new Error(result.message);
    }

    return result.data;
  } catch (error) {
    console.error('Erro ao buscar notas:', error);
    throw error;
  }
}
```

---

## Notas Importantes

1. **Autenticação**: Todos os endpoints (exceto health checks) requerem um JWT token válido no header `Authorization`.

2. **Validação**: O backend valida todos os dados antes de processar. Certifique-se de validar no frontend também para melhor UX.

3. **Formato de Data**: Use o formato ISO 8601 para datas:
   - Data apenas: `"YYYY-MM-DD"` (ex: `"2024-11-02"`)
   - Data e hora: `"YYYY-MM-DDTHH:mm:ss"` (ex: `"2024-11-02T10:30:00"`)

4. **Precisão Decimal**: Valores de nota (`gradeValue`) suportam até 2 casas decimais.

5. **Soft Delete**: Quando uma nota é deletada, ela não é removida fisicamente do banco, apenas marcada como deletada. Ela não aparecerá em consultas normais.

6. **Cache**: Alguns endpoints utilizam cache Redis para melhor performance. Mudanças podem levar alguns segundos para refletir.

7. **Eventos Kafka**: A criação, atualização e exclusão de notas geram eventos Kafka que podem ser consumidos por outros serviços.

---

## Suporte

Para dúvidas ou problemas com a integração, entre em contato com a equipe de backend ou consulte a documentação técnica do projeto.

**Última atualização:** Novembro 2024


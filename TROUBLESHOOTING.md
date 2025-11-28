# Troubleshooting - DistriSchool

Este documento contém soluções para problemas comuns encontrados durante o desenvolvimento.

## Problemas Identificados

### 1. Erro: `database "distrischool" does not exist`

**Sintoma:**
```
postgres-distrischool | FATAL:  database "distrischool" does not exist
```

**Causa:**
O healthcheck do PostgreSQL no `docker-compose.yml` está usando `pg_isready -U distrischool` sem especificar um banco de dados. Quando o PostgreSQL tenta verificar a conexão, ele pode tentar conectar ao banco padrão do usuário (que seria "distrischool"), mas esse banco não existe. Os bancos criados são:
- `distrischool_auth`
- `distrischool_students`
- `distrischool_teachers`
- `distrischool_schedules`
- `distrischool_grades`

**Solução:**
1. **Corrigir o healthcheck no `docker-compose.yml`:**
   O healthcheck deve especificar o banco `postgres` que sempre existe:
   ```yaml
   healthcheck:
     test: ["CMD-SHELL", "pg_isready -U distrischool -d postgres"]
     interval: 10s
     timeout: 5s
     retries: 5
   ```

2. Verificar se todos os bancos foram criados:
```bash
docker exec -i postgres-distrischool psql -U distrischool -d postgres -c "SELECT datname FROM pg_database WHERE datname LIKE 'distrischool%';"
```

3. Se algum banco não existir, execute o script de inicialização:
```bash
./scripts/init-databases.sh
```

4. Verificar a configuração do serviço no `docker-compose.yml` para garantir que está usando o banco correto (com sufixo).

### 2. Erro Auth0: `The connection does not exist`

**Sintoma:**
```
microservice-auth-dev | Erro ao criar usuário no Auth0: 400 Bad Request: 
"{"statusCode":400,"error":"Bad Request","message":"The connection does not exist.","errorCode":"inexistent_connection"}"
```

**Causa:**
A conexão `Username-Password-Authentication` não existe no seu tenant do Auth0, ou o nome está incorreto.

**Solução:**

#### Opção 1: Criar a conexão no Auth0 Dashboard

1. Acesse o [Auth0 Dashboard](https://manage.auth0.com/)
2. Vá em **Authentication** > **Database** > **Create Database Connection**
3. Selecione **Username-Password-Authentication** (ou crie uma nova)
4. Configure a conexão:
   - **Name**: `Username-Password-Authentication` (ou o nome que você preferir)
   - **Enabled Database**: Ative
   - **Password Policy**: Configure conforme necessário
5. Salve a conexão

#### Opção 2: Usar uma conexão existente

Se você já tem uma conexão com outro nome:

1. Identifique o nome da conexão no Auth0 Dashboard
2. Configure a variável de ambiente no `docker-compose.yml` ou no arquivo `.env`:
```yaml
AUTH0_CONNECTION: "nome-da-sua-conexao"
```

#### Opção 3: Desabilitar Auth0 temporariamente (apenas para desenvolvimento)

Se você não precisa do Auth0 no momento:

1. Configure no `docker-compose.yml`:
```yaml
AUTH0_ENABLED: "false"
```

**Nota:** Isso desabilitará a criação de usuários no Auth0, mas o registro ainda funcionará localmente.

### 3. Verificar configurações do Auth0

Para verificar se as configurações do Auth0 estão corretas:

1. **Verificar variáveis de ambiente:**
```bash
docker exec microservice-auth-dev env | grep AUTH0
```

2. **Verificar se o Management API Client está configurado:**
   - No Auth0 Dashboard, vá em **Applications** > **APIs** > **Auth0 Management API**
   - Verifique se você tem um Machine to Machine Application configurado
   - O `AUTH0_CLIENT_ID` e `AUTH0_CLIENT_SECRET` devem ser do M2M Application

3. **Verificar permissões do Management API:**
   - O M2M Application precisa ter permissões:
     - `create:users`
     - `read:users`
     - `update:users`
     - `delete:users`

### 4. Scripts úteis

#### Verificar status dos bancos de dados
```bash
docker exec -i postgres-distrischool psql -U distrischool -d postgres -c "\l" | grep distrischool
```

#### Recriar todos os bancos de dados
```bash
# Parar os serviços
docker-compose down

# Remover o volume do PostgreSQL (CUIDADO: isso apaga todos os dados!)
docker volume rm gestao-de-notas_postgres_data

# Subir novamente
docker-compose up -d postgres

# Aguardar o PostgreSQL inicializar
sleep 5

# Executar script de inicialização
./scripts/init-databases.sh
```

#### Ver logs de um serviço específico
```bash
docker logs -f microservice-auth-dev
docker logs -f grade-management-service-dev
docker logs -f postgres-distrischool
```

#### Verificar conexões do Kafka
```bash
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### 5. Problemas comuns do Kafka

Se os consumidores não estão recebendo mensagens:

1. Verificar se os tópicos foram criados:
```bash
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

2. Verificar se os grupos de consumidores estão ativos:
```bash
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

3. Verificar mensagens em um tópico:
```bash
docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic distrischool.student.created --from-beginning
```

## Checklist de Verificação

Antes de reportar um problema, verifique:

- [ ] Todos os containers estão rodando: `docker ps`
- [ ] Todos os bancos de dados foram criados: `./scripts/init-databases.sh`
- [ ] As variáveis de ambiente do Auth0 estão configuradas
- [ ] A conexão do Auth0 existe no Dashboard
- [ ] O Kafka está funcionando: `docker logs kafka`
- [ ] As portas não estão em conflito: `netstat -tuln | grep -E '8080|8081|8082|8083|8084'`

## Suporte

Se o problema persistir:
1. Verifique os logs completos: `docker-compose logs`
2. Verifique a documentação do projeto
3. Consulte a documentação do Auth0: https://auth0.com/docs


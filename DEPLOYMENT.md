# Guia de Deploy para Produção

Este documento descreve como fazer o deploy da aplicação Physio Manager em produção usando Docker.

## Pré-requisitos

- Docker 20.10+ e Docker Compose 2.0+
- Acesso ao servidor de produção
- Variáveis de ambiente configuradas (veja seção abaixo)

## Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto baseado no arquivo `env.example`:

```bash
# Copie o exemplo
cp env.example .env

# Edite com suas configurações
nano .env  # ou use seu editor preferido
```

**Variáveis obrigatórias:**
- `SPRING_DATASOURCE_URL` - URL completa do banco PostgreSQL externo
- `SPRING_DATASOURCE_USERNAME` - Usuário do banco
- `SPRING_DATASOURCE_PASSWORD` - Senha do banco
- `API_SECURITY_TOKEN_SECRET` - Token secreto para JWT (mínimo 32 caracteres)

**Exemplo mínimo de `.env`:**
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://seu-servidor-postgres:5432/fisio
SPRING_DATASOURCE_USERNAME=seu_usuario
SPRING_DATASOURCE_PASSWORD=sua_senha
API_SECURITY_TOKEN_SECRET=seu_token_secreto_muito_longo_e_seguro_aqui_minimo_32_caracteres
```

**Nota:** Todas as outras variáveis têm valores padrão e são opcionais. Veja `.env.example` para todas as opções disponíveis.

**⚠️ IMPORTANTE:** Nunca commite o arquivo `.env` no repositório. Ele já está no `.gitignore`.

## Build da Imagem Docker

### Opção 1: Build local

```bash
docker build -t physio-manager:latest .
```

### Opção 2: Build com docker-compose

```bash
docker-compose -f docker-compose.prod.yml build
```

## Deploy com Docker Compose

### 1. Configure as variáveis de ambiente

Crie o arquivo `.env` conforme descrito acima.

### 2. Inicie os serviços

```bash
docker-compose -f docker-compose.prod.yml up -d
```

### 3. Verifique os logs

```bash
# Logs da aplicação
docker-compose -f docker-compose.prod.yml logs -f physio-manager

# Logs do banco de dados
docker-compose -f docker-compose.prod.yml logs -f postgres
```

### 4. Verifique o status dos containers

```bash
docker-compose -f docker-compose.prod.yml ps
```

## Deploy Standalone (sem docker-compose)

Se preferir executar apenas o container da aplicação:

```bash
docker run -d \
  --name physio-manager-prod \
  -p 8080:8080 \
  --env-file .env \
  physio-manager:latest
```

Ou passando as variáveis diretamente:

```bash
docker run -d \
  --name physio-manager-prod \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://seu-servidor-postgres:5432/fisio \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=senha \
  -e API_SECURITY_TOKEN_SECRET=seu_token_secreto \
  -e SPRING_PROFILES_ACTIVE=prod \
  physio-manager:latest
```

## Verificação de Saúde

O healthcheck do Docker verifica se a aplicação está respondendo através do endpoint do Swagger:

```bash
# Verificar manualmente
curl http://localhost:8080/swagger-ui.html

# Ou verificar qualquer endpoint da API
curl http://localhost:8080/api/v1/agendamentos
```

**Dica:** Para um healthcheck mais robusto em produção, considere adicionar o Spring Boot Actuator ao `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Otimizações de Produção

### Memória JVM

Ajuste as variáveis `JAVA_OPTS` conforme o tamanho do servidor:

- **Servidor pequeno (2GB RAM):** `-Xmx512m -Xms256m`
- **Servidor médio (4GB RAM):** `-Xmx1024m -Xms512m`
- **Servidor grande (8GB+ RAM):** `-Xmx2048m -Xms1024m`

### Connection Pool

Ajuste o tamanho do pool de conexões no `docker-compose.prod.yml`:

```yaml
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=10
```

## Backup do Banco de Dados

Como o banco de dados está em um servidor externo, você precisará fazer backup diretamente no servidor:

### Criar backup (no servidor do banco)

```bash
pg_dump -h seu-servidor-postgres -U seu_usuario -d fisio > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Restaurar backup (no servidor do banco)

```bash
psql -h seu-servidor-postgres -U seu_usuario -d fisio < backup_20250101_120000.sql
```

**Nota:** Ajuste os parâmetros de conexão conforme sua configuração.

## Atualização da Aplicação

### 1. Pare os containers

```bash
docker-compose -f docker-compose.prod.yml down
```

### 2. Faça backup do banco (recomendado)

```bash
# Execute no servidor do banco de dados
pg_dump -h seu-servidor-postgres -U seu_usuario -d fisio > backup_pre_update.sql
```

### 3. Rebuild da imagem

```bash
docker-compose -f docker-compose.prod.yml build --no-cache
```

### 4. Inicie novamente

```bash
docker-compose -f docker-compose.prod.yml up -d
```

## Monitoramento

### Logs em tempo real

```bash
docker-compose -f docker-compose.prod.yml logs -f
```

### Uso de recursos

```bash
docker stats physio-manager-prod
```

### Verificar saúde dos containers

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

## Troubleshooting

### Container não inicia

1. Verifique os logs: `docker-compose -f docker-compose.prod.yml logs physio-manager`
2. Verifique as variáveis de ambiente no `.env`
3. Verifique se a URL do banco está correta e acessível

### Erro de conexão com banco

1. Verifique se o servidor PostgreSQL está acessível e rodando
2. Verifique as credenciais no `.env` (SPRING_DATASOURCE_URL, USERNAME, PASSWORD)
3. Teste a conexão do servidor onde o Docker está rodando:
   ```bash
   # Teste de conectividade
   telnet seu-servidor-postgres 5432
   
   # Ou com psql (se instalado)
   psql -h seu-servidor-postgres -U seu_usuario -d fisio
   ```
4. Verifique firewall/security groups que podem estar bloqueando a conexão

### Aplicação lenta

1. Ajuste `JAVA_OPTS` para mais memória
2. Aumente o pool de conexões
3. Verifique o uso de recursos: `docker stats`

## Segurança

- ✅ A aplicação roda como usuário não-privilegiado (`physio`)
- ✅ Variáveis sensíveis via `.env` (não commitadas)
- ✅ Healthcheck configurado
- ⚠️ Configure HTTPS em produção (use um reverse proxy como Nginx)
- ⚠️ Configure firewall para expor apenas portas necessárias
- ⚠️ Use senhas fortes para o banco de dados

## Próximos Passos

1. Configure um reverse proxy (Nginx/Traefik) para HTTPS
2. Configure monitoramento (Prometheus/Grafana)
3. Configure backup automático do banco de dados
4. Configure log aggregation (ELK Stack ou similar)

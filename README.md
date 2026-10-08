# Acompanhamento de Treino

API + web para registrar treinos de musculação, acompanhar progressão de carga (fórmula de Epley para 1RM estimado) e gerenciar exercícios personalizados.

## Stack

- Backend: Java 21, Spring Boot 3.3.x, Maven (wrapper), Spring Web, Data JPA, Validation, Security + JWT (jjwt), PostgreSQL 16, Flyway, Springdoc OpenAPI, Actuator (health). Sem Lombok.
- Frontend: React 18 + Vite 5 + TypeScript 5, React Router 6, TanStack Query 5, React Hook Form + Zod, Recharts 2, CSS puro mobile-first.
- Testes: JUnit 5, Mockito, Testcontainers (Postgres), MockMvc.

## Pré-requisitos

- Java 21
- Node 20
- Docker (com plugin Compose: `docker compose`)
- Git

Copie as variáveis de ambiente a partir do exemplo (nunca commite o `.env` real):

```bash
cp .env.example .env
```

Valores de `.env.example` são defaults locais (`treino`/`treino`/`treino`) — sem segredos reais.

## Como rodar (Fase 1)

A Fase 1 cobre somente o banco via Compose. A API roda localmente com o Maven.

1. Suba o banco:

```bash
docker compose up -d db
```

2. Rode a API:

```bash
cd backend
./mvnw spring-boot:run
```

No Windows (PowerShell), se o wrapper não executar diretamente:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

3. Verifique a saúde da API:

```bash
curl http://localhost:8080/health
```

Resposta esperada: `{"status":"UP"}` (retornado pelo `HealthController`, não pelo Actuator).

O backend lê a conexão do banco das variáveis `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` (defaults em `backend/src/main/resources/application.yml` apontam para `jdbc:postgresql://localhost:5432/treino`, compatível com o serviço `db` do Compose que publica `5432:5432`).

Para parar o banco:

```bash
docker compose down
```

## Nota sobre Compose (Fase 9)

Na Fase 1 o `docker-compose.yml` contém **somente o serviço `db`** (Postgres 16). Os serviços `api` e `web` serão adicionados na Fase 9. Não há `Dockerfile` da API/web nesta fase.

# Fase 9 — Docker, CI e entrega: 1 comando para subir tudo

> O que esta fase fez: criou os `Dockerfile`s multi-stage (backend + frontend), o `docker-compose.yml` completo (`db` + `api` + `web` com healthchecks e dependências), os workflows GitHub Actions (backend `./mvnw verify`, frontend `npm run build`) e o `README` final. Entrega: `docker compose up --build` sobe o sistema inteiro.

## 1. Analogia simples: marmitas prontas e esteira de qualidade

O `Dockerfile` multi-stage é a cozinha que cozinha e embala a marmita, mas entrega só a marmita (imagem final enxuta, sem o fogão do build). O compose é o entregador que leva as três marmitas juntas (banco, API, web) na ordem certa. O GitHub Actions é o fiscal da esteira: cada push rebuilda e testa tudo do zero, e só passa o que está verde.

## 2. O que a fase construiu

- `backend/Dockerfile` — stage 1 Maven/TEMURIN builda o jar; stage 2 JRE + non-root roda.
- `frontend/Dockerfile` — stage 1 Node builda o SPA; stage 2 nginx serve o `dist`.
- `frontend/nginx.conf` — fallback SPA (`try_files $uri $uri/ /index.html`), cache de assets, gzip.
- `docker-compose.yml` — `db` (postgres:16 + volume), `api` (porta 8080), `web` (porta 80).
- `.github/workflows/backend.yml` + `frontend.yml` — CI separada por caminho alterado.

## 3. Conceitos-chave

### 3.1 Multi-stage: imagem final sem o entulho do build

O jar é compilado num stage com JDK + Maven e copiado para um runtime só com JRE, rodando como `appuser` (não root). O front segue a mesma ideia: Node compila, nginx serve (`backend/Dockerfile:2-20`, `frontend/Dockerfile:1-16`):

```dockerfile
# backend/Dockerfile:17-20 — só o jar vai para a imagem final
COPY --from=build /app/target/treino-*.jar /app/app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

### 3.2 Compose: ordem, saúde e segredo obrigatório

`depends_on` com `condition: service_healthy` garante Postgres pronto antes da API e API saudável antes da web (`docker-compose.yml:39-41`, `61-62`). E o `JWT_SECRET` não tem default de propósito — o compose falha na validação sem ele:

```yaml
# docker-compose.yml:35 — sem segredo, sem subida
JWT_SECRET: ${JWT_SECRET:?defina JWT_SECRET no arquivo .env (minimo 32 chars, sem default por seguranca)}
```

Gere com `openssl rand -base64 48`; o `.env` real nunca é commitado (só o `.env.example`).

### 3.3 Actions: CI por caminho + Testcontainers sem esforço

Cada workflow só dispara quando sua pasta muda (`backend.yml:3-11`), usa cache de dependências e roda build+testes do zero. O teste de integração funciona no CI sem setup extra porque o runner já tem Docker e o Testcontainers sobe o Postgres sozinho (`backend.yml:15-19`, `28-32`):

```yaml
# backend.yml:29-32 — build + testes com Testcontainers incluso
working-directory: backend
run: |
  chmod +x mvnw
  ./mvnw -B verify
```

## 4. Auto-teste (2 perguntas com respostas)

**1. Por que o nginx precisa do `try_files $uri $uri/ /index.html` (`nginx.conf:8-11`)?**
Porque as rotas (`/historico`, `/evolucao`) só existem no React Router, no navegador. Sem o fallback, recarregar `/historico` pediria esse arquivo ao nginx e daria 404.

**2. Para que serve o healthcheck da API no compose (`docker-compose.yml:42-47`)?**
Para a web só subir quando a API responde `/actuator/health` de verdade — sem ele, o nginx poderia receber tráfego com o Spring ainda inicializando (tela de erro nos primeiros segundos).

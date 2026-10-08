# AGENTS.md — Convenções do projeto (fonte de verdade para subagentes)

> `ponytail` está ativo globalmente, mas **estas convenções têm precedência** sobre "menor diff" e "stdlib primeiro". Código didático, explícito e em camadas > atalhos.

## Stack fixa
- Backend: Java 21, Spring Boot 3.3.x, Maven (wrapper), Web, Data JPA, Validation, Security + JWT (jjwt), PostgreSQL 16, Flyway, Springdoc OpenAPI, Actuator (health). **Sem Lombok.**
- Frontend: React 18 + Vite 5 + TypeScript 5, React Router 6, TanStack Query 5, React Hook Form + Zod, Recharts 2, CSS puro mobile-first (sem Tailwind).
- Testes: JUnit 5, Mockito, Testcontainers (Postgres), MockMvc.

## Estrutura
- Monorepo: `backend/`, `frontend/`, `docs/`, `.opencode/agent/`, `docker-compose.yml`, `README.md`, `AGENTS.md`, `APRENDIZADOS.md`.
- Pacote base: `br.com.treino`. Subpacotes: `controller, service, repository, domain, dto, config, security, exception`.
- Nomes de classes/métodos/variáveis em **inglês**; mensagens de erro e validação em **português**. Exceção aceita: entidades do domínio (`Ficha`, `Treino`, `Serie`, `MedidaCorporal`) usam linguagem ubíqua em PT por decisão didática.

## Regras obrigatórias (reviewer barra se violar)
1. **Nunca expor entidade JPA na API.** Sempre DTOs como `records`. Mapeamento manual explícito no service (sem MapStruct/ModelMapper).
2. **Injeção por construtor** (sem `@Autowired` em campo).
3. **Schema só por Flyway** (`src/main/resources/db/migration/V*.sql`). `spring.jpa.hibernate.ddl-auto=validate`. Nomes de migration: `V1__create_schema.sql`, `V2__seed_exercicios.sql`.
4. Carga/peso com `BigDecimal` no Java e `numeric(6,2)` no Postgres.
5. **Transacional:** registro de treino com `@Transactional` no service; qualquer série inválida → rollback total.
6. **Isolamento por usuário:** toda query filtra por `usuarioId` do JWT; recurso de outro usuário retorna **404** (não 403, para não vazar existência).
7. **Erros em JSON padronizado** via `@RestControllerAdvice`: `{timestamp, status, mensagem, caminho, errosCampos[]}`. Bean Validation com mensagens em português.
8. **Segurança:** senha BCrypt; JWT `Authorization: Bearer`, expiração 1h, chave de `JWT_SECRET` (env, min 32 chars, sem default — fail-fast). Públicos: `/auth/**`, `/health`, `/v3/api-docs/**`, `/swagger-ui/**`. CORS só via env (`http://localhost:5173` no dev, `http://localhost` no compose).
9. **Validações de treino:** `repeticoes > 0`, `carga >= 0`, exercício existente e acessível (global ou do usuário), `data <= hoje`.
10. **Segredos só via ambiente.** Nunca hardcoded. `.env` real nunca commitado; manter `.env.example`.
11. **Sem placeholders.** Skill `full-output-enforcement`: proibido `// resto do código aqui`, `...`. Todo arquivo entregue completo e compilável.
12. Comentários curtos só em conceitos importantes (transação, segurança, N+1, Epley, isolamento). Sem comentário óbvio.

## Comandos
- Backend: `cd backend; ./mvnw spring-boot:run` (local), `./mvnw verify` (build+testes), `./mvnw test -Dtest=NomeTest`.
- Frontend: `npm run dev`, `npm run build`, `npx openapi-typescript http://localhost:8080/v3/api-docs -o src/api/types.ts`.
- Infra: `docker compose up --build` (sobe db+api+web), `docker compose up -d db` (só banco local).

## Delegação (orquestrador)
- Briefings autocontidos: ID do backlog, contexto, pastas permitidas, aceite testável.
- Escopos disjuntos para paralelizar; nunca dois agentes nos mesmos arquivos.
- Todo agente devolve: o que fez, arquivos alterados, como testar, decisões, pendências.

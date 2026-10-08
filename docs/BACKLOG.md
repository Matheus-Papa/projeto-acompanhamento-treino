# BACKLOG — Fonte única da verdade

> Atualizado em tempo real pelo orquestrador: `TODO → IN_PROGRESS → IN_REVIEW → DONE/BLOCKED`.
> Cada tarefa é pequena (uma sessão de subagente) com aceite testável.

## Épico F1 — Fundação, esqueleto Boot, /health

- [x] **F1-01** Estrutura monorepo + docs base | dep: — | resp: orquestrador | aceite: pastas `backend/ frontend/ docs/ .opencode/agent/`, arquivos `.gitignore README.md AGENTS.md APRENDIZADOS.md docker-compose.yml .env.example` existem | status: DONE
- [x] **F1-02** Criar 8 subagentes em `.opencode/agent/` | dep: F1-01 | resp: orquestrador | aceite: 8 arquivos `.md` com frontmatter `description, mode: subagent, permission` válido | status: DONE
- [x] **F1-03** Esqueleto Spring Boot + `GET /health` | dep: F1-01 | resp: backend-dev | toque: `backend/**` | aceite: `./mvnw spring-boot:run` sobe; `curl localhost:8080/health → {"status":"UP"}`; `ddl-auto=validate`, actuator, springdoc no `pom.xml` | status: DONE (código pronto; execução pendente de Java 21 — ver Riscos)
- [x] **F1-04** docker-compose db + wiring local | dep: F1-03 | resp: devops | toque: `docker-compose.yml, backend/src/main/resources/application*.yml` | aceite: `docker compose up -d db` conecta; API usa env `SPRING_DATASOURCE_URL` | status: DONE (validação estática; daemon Docker ausente — ver Riscos)
- [x] **F1-05** Review + explicação + commit F1 | dep: F1-02,F1-04 | resp: reviewer → explainer | aceite: 0 issues alta/média; `docs/fase-1-explicacao.md` existe; commit `chore(f1): fundacao` | status: DONE

## Épico F2 — Migrations, entidades, seed — DONE (review consolidado com F5; commit backend conjunto em modo contínuo)

- [x] **F2-01** Migration V1 tabelas + índices + FKs | dep: F1-04 | resp: db-designer | aceite: `V1__create_schema.sql` cria 8 tabelas + índices `treino(usuario_id,data)`, `serie(exercicio_id)`, `serie(treino_id)`; migrate verde | status: DONE
- [x] **F2-02** Migration V2 seed 30 globais | dep: F2-01 | resp: db-designer | aceite: `V2__seed_exercicios.sql` com 30 `INSERT` (`usuario_id NULL`), idempotente | status: DONE
- [x] **F2-03** Entidades JPA + repositories | dep: F2-01 | resp: db-designer | aceite: 7 entidades sem Lombok, `BigDecimal`, `LAZY`, queries com `usuarioId`; sem entidade exposta | status: DONE (+ fix review: LEFT JOIN nos acessíveis, CHECKs ordem/numero)
- [x] **F2-04** Review + explicação + commit F2 | dep: F2-02,F2-03 | resp: reviewer → explainer | aceite: sem N+1/`ddl-auto=update`; `docs/fase-2-explicacao.md` | status: DONE (review consolidado F2-F6; explicações geradas no fechamento)

## Épico F3 — Exercícios, fichas, erros, Swagger

- [x] **F3-01** DTOs records + handler de erros | dep: F2-03 | resp: backend-dev | toque: `dto/, exception/` | aceite: 404 → `{timestamp,status,mensagem,caminho,errosCampos}`; 400 validação lista campos em português | status: DONE
- [x] **F3-02** CRUD `/exercicios` | dep: F3-01 | resp: backend-dev | aceite: vê `globais + próprios`; editar global alheio → 404 | status: DONE
- [x] **F3-03** CRUD `/fichas` + itens ordenados | dep: F3-01 | resp: backend-dev | aceite: `POST /fichas {nome, itens[]}` valida ordem única e exercício acessível; GET ordenado | status: DONE
- [x] **F3-04** Swagger / OpenAPI | dep: F3-02,F3-03 | resp: backend-dev | aceite: `/swagger-ui.html` e `/v3/api-docs` 200 | status: DONE
- [x] **F3-05** Review + explicação + commit F3 | dep: F3-04 | resp: reviewer → explainer | aceite: `docs/fase-3-explicacao.md` | status: DONE

## Épico F4 — Treinos + histórico

- [x] **F4-01** `POST /treinos` transacional + PRs na resposta | dep: F3-03 (F6 religável) | resp: backend-dev | aceite: `@Transactional`; série inválida → rollback total; resposta inclui `prs[]` | status: DONE
- [x] **F4-02** `GET /treinos` paginado + `GET /treinos/{id}` | dep: F4-01 | resp: backend-dev | aceite: `?de=&ate=&page=&size=` filtra por dono/período; recurso alheio → 404 | status: DONE
- [x] **F4-03** `GET /exercicios/{id}/historico` | dep: F4-01 | resp: backend-dev | aceite: por treino `{data, melhorSerie, volume, epley}` ordenado | status: DONE
- [x] **F4-04** Review + explicação + commit F4 | dep: F4-02,F4-03 | resp: reviewer → explainer | aceite: `docs/fase-4-explicacao.md` | status: DONE

## Épico F5 — Estatísticas

- [x] **F5-01** Util Epley + `GET /exercicios/{id}/recorde` | dep: F4-01 | resp: backend-dev | aceite: `{maxCarga, maxEpley, emTreinoId}`; `reps=1 → epley=carga` | status: DONE
- [x] **F5-02** `GET /estatisticas/volume-semanal` | dep: F4-01 | resp: backend-dev | aceite: `?de=&ate= → [{semanaSegunda, volume}]`, `volume=Σ reps*carga` | status: DONE
- [x] **F5-03** Review + `ponytail-audit` + explicação + commit F5 | dep: F5-01,F5-02 | resp: reviewer → explainer | aceite: `docs/fase-5-explicacao.md` | status: DONE

## Épico F6 — Segurança

- [x] **F6-01** `POST /auth/register|login` + JWT + BCrypt | dep: F2-01 | resp: security-engineer | toque: `security/, AuthController, domain/Usuario` | aceite: senha hasheada; login retorna `{token, expiresIn:3600}`; sem token → 401 (exceto `/auth/**`, `/health`, swagger) | status: DONE
- [x] **F6-02** Isolamento por usuário + CORS | dep: F6-01 | resp: security-engineer | aceite: tudo filtra por `usuarioId` do JWT; alheio → 404; CORS só `http://localhost:5173` via env | status: DONE
- [x] **F6-03** Review + explicação + commit F6 (+ religar F4) | dep: F6-02 | resp: reviewer → explainer | aceite: `JWT_SECRET` só via env; `docs/fase-6-explicacao.md` | status: DONE

## Épico F7 — Testes

- [x] **F7-01** Unitários (Epley, PR, regras) | dep: F5-02 | resp: test-writer | toque: `backend/src/test/**` (só toca `src/main` com aprovação) | aceite: `mvn test -Dtest=CalculoTreinoTest,DeteccaoPrTest` verde | status: DONE
- [x] **F7-02** Integração Testcontainers | dep: F7-01 | resp: test-writer | aceite: `./mvnw verify` verde: rollback, histórico, volume, login, isolamento A×B | status: DONE
- [x] **F7-03** Review + audit + explicação + commit F7 | dep: F7-02 | resp: reviewer → explainer | aceite: `docs/fase-7-explicacao.md` | status: DONE

## Épico F8 — Frontend React

- [x] **F8-01** Setup + auth + tipos OpenAPI | dep: F3-04,F6-01 | resp: frontend-dev | toque: `frontend/**` | aceite: `npm run dev` abre; JWT guardado, 401 → logout; tipos gerados | status: DONE
- [x] **F8-02** Exercícios + fichas | dep: F8-01 | resp: frontend-dev | aceite: lista exercícios, CRUD fichas, loading+erro via Query | status: DONE
- [x] **F8-03** Registrar treino (principal, 1 mão) | dep: F8-02 | resp: frontend-dev | aceite: inputs grandes, copiar série, timer, finalizar rápido, exibe PRs | status: DONE
- [x] **F8-04** Histórico + evolução + stats + medidas | dep: F8-03 | resp: frontend-dev | aceite: histórico paginado, Recharts, volume semanal, medidas | status: DONE
- [x] **F8-05** Review + explicação + commit F8 | dep: F8-04 | resp: reviewer → explainer | aceite: usável no celular; `docs/fase-8-explicacao.md` | status: DONE

## Épico F9 — Docker, CI, README

- [x] **F9-01** Dockerfiles + compose completo | dep: F8-01 | resp: devops | toque: `backend/Dockerfile, frontend/Dockerfile, docker-compose.yml` | aceite: `docker compose up --build` sobe `db+api+web` | status: DONE
- [x] **F9-02** GitHub Actions | dep: F9-01 | resp: devops | toque: `.github/workflows/*` | aceite: workflows backend (build+test) e frontend (build) verdes | status: DONE
- [x] **F9-03** README + ER Mermaid + limpeza | dep: F9-02 | resp: orquestrador | aceite: README com stack/como rodar/decisões/ER; sem segredo; sem TODO | status: DONE
- [x] **F9-04** Review final + audit + explicação + commit | dep: F9-03 | resp: reviewer → explainer | aceite: `docs/fase-9-explicacao.md` | status: DONE

## Definition of Done global

Build verde (`./mvnw verify` + `npm run build`), testes passando, sem TODOs esquecidos, sem segredos no código, `README/AGENTS.md` atualizados quando relevante, review sem pendência alta/média, `docs/fase-N-explicacao.md` criada, commit Conventional Commits por fase.

## Riscos e decisões em aberto

1. **Java 21 ausente na máquina (só Java 8) + sem Maven/Docker** — build local bloqueado até instalar Temurin 21 + Docker Desktop. Código pode ser escrito sem build; verificação fica pendente.
2. Testcontainers exige Docker — sem Docker, F7-02 não roda local; CI cobre.
3. `numeric(6,2)` limita a 9999.99 kg — aceito e documentado.
4. `data` como `LocalDate`, validação `!data.isAfter(hoje)` — evita fuso.
5. Tipos TS via `openapi-typescript`; fallback manual se falhar no Windows.
6. F4 pode iniciar com usuário mockado e religar ao JWT na F6-03.

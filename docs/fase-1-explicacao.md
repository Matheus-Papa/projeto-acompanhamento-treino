# Fase 1 — Fundação, esqueleto Spring Boot e `/health`

> O que esta fase fez: criou o monorepo (`backend/`, `frontend/`, `docs/`), o esqueleto da API Spring Boot 3.3.5 com `GET /health → {"status":"UP"}`, a configuração base (`application.yml`), o `pom.xml` sem Lombok, o `docker-compose.yml` só com o banco Postgres 16, o `.env.example`, o `README.md`, o `AGENTS.md` e o `BACKLOG` com 8 agentes. Nada de regra de negócio ainda — só o terreno pronto para construir.

## 1. Analogia simples: o esqueleto + o healthcheck

Imagine que você vai abrir uma academia.

Antes de comprar halteres, contratar professores ou vender planos, você precisa de três coisas:

1. **O prédio com fundação, energia e água ligadas** — é o esqueleto Spring Boot. Ainda não tem aula acontecendo, mas as paredes estão de pé, a porta abre e a luz acende.
2. **O endereço e a planta do prédio** — é o monorepo: `backend/` (a sala de musculação), `frontend/` (a recepção, ainda vazia nesta fase), `docs/` (o manual de operação), `docker-compose.yml` (o contrato com a companhia de água/luz, ou seja, o banco de dados).
3. **Bater na porta e ouvir "estamos abertos!"** — é o healthcheck. O `GET /health` que responde `{"status":"UP"}` não mede pressão de ninguém nem registra treino. Ele só prova uma coisa valiosa: a aplicação subiu, o Spring montou o contexto, o roteamento HTTP funciona e dá para seguir para a próxima fase com confiança.

Por isso a Fase 1 é pequena de propósito. Se o esqueleto não sobe ou o `/health` não responde, não adianta discutir entidade JPA, JWT ou cálculo de 1RM. Primeiro garanta o prédio de pé.

O ponto de entrada mínimo é este:

```java
// backend/src/main/java/br/com/treino/TreinoApplication.java
@SpringBootApplication
public class TreinoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TreinoApplication.class, args);
    }
}
```

E a "batida na porta" é esta:

```java
// backend/src/main/java/br/com/treino/controller/HealthController.java
@GetMapping("/health")
public HealthResponse health() {
    return new HealthResponse("UP");
}
```

Teste manual da fase, conforme o `README.md`:

```bash
curl http://localhost:8080/health
# esperado: {"status":"UP"}
```

## 2. Conceitos-chave

### 3.1 Starter parent e BOM — a "lista de compras combinada"

O `spring-boot-starter-parent` traz um BOM: tabela de versões testadas juntas para o Boot 3.3.5. Declaramos `spring-boot-starter-web` sem versão e herdamos a compatível. Só pinamos o que está fora do BOM (Springdoc 2.6.0, jjwt 0.12.6). Starter = kit pronto; parent/BOM = garantia de compatibilidade.

### 3.2 `ddl-auto: validate` — quem manda no banco?

`create` apaga dados; `update` altera o schema sozinho (invisível, diverge entre ambientes); `validate` **não muda nada**, só confere se as entidades batem com o banco real e falha rápido se divergir. Regra do projeto: schema só por Flyway, Hibernate só valida (`application.yml`).

### 3.3 Flyway como "controle de versão do banco"

Migrations numeradas (`V1__create_schema.sql`, `V2__seed_exercicios.sql` na Fase 2), aplicadas em ordem, uma vez só, em todo ambiente. Sem Flyway cada dev tem um banco diferente; com Flyway o banco evolui como commits.

### 3.4 Actuator vs controller custom

`GET /health` (HealthController) = cartaz "ABERTO" na porta, contrato público simples. `GET /actuator/health` = sala de máquinas para monitoramento de plataforma. Actuator configurado para expor só `health`, sem detalhes.

### 3.5 Variáveis de ambiente — segredos fora do código

Código lê `${VAR:default}`; segredo real só no ambiente. `.env.example` versiona só nomes com valores fake locais. `.env` real nunca commitado.

### 3.6 Compose com só o banco

Fase 1: só serviço `db` (postgres:16, volume `pgdata`, healthcheck `pg_isready`). API roda no Maven local. `api`/`web` chegam na Fase 9. A cola: compose publica `5432:5432` e o default do `application.yml` aponta para `localhost:5432/treino`.

## 3. Auto-teste (3 perguntas com respostas)

**1. Por que `validate` em vez de `update`?**
Porque `update` muda o schema sozinho, sem revisão nem histórico. `validate` falha na subida se entidade divergir do banco Flyway — explícito e seguro.

**2. Diferença entre `/health` e `/actuator/health`?**
Custom = contrato público simples para humanos/scripts. Actuator = endpoint operacional para plataforma/monitoramento.

**3. Onde vai a senha real do banco?**
Só em variável de ambiente (`.env` local não commitado, secrets em produção). `.env.example` tem só nomes + fakes locais.

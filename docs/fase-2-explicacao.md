# Fase 2 — Banco: migrations Flyway, entidades e seed

> O que esta fase fez: criou `V1__create_schema.sql` (8 tabelas + índices + CHECKs + FKs), `V2__seed_exercicios.sql` (30 exercícios globais idempotentes), 7 entidades JPA sem Lombok (`Usuario`, `Exercicio`, `Ficha`, `FichaExercicio`, `Treino`, `Serie`, `MedidaCorporal`) com `BigDecimal` e `LAZY`, e os repositories com queries filtradas por `usuarioId`. Nenhum endpoint ainda — só o terreno do banco.

## 1. Analogia simples: a planta registrada em cartório

Pense nas migrations como a planta da academia registrada em cartório: cada versão (`V1`, `V2`) é uma reforma carimbada, aplicada uma única vez em todo ambiente. Sem isso, cada desenvolvedor teria uma academia diferente. O seed `V2` é a compra inicial de 30 halteres padrão (exercícios globais com `usuario_id NULL`) que toda unidade recebe igual.

## 2. O que a fase construiu

- `backend/src/main/resources/db/migration/V1__create_schema.sql` — 8 tabelas, índices de isolamento e histórico, CHECKs de domínio.
- `backend/src/main/resources/db/migration/V2__seed_exercicios.sql` — 30 `INSERT`s globais idempotentes.
- `backend/src/main/java/br/com/treino/domain/` — 7 entidades (`Treino`, `Serie`, `Exercicio`, `Ficha`, `FichaExercicio`, `MedidaCorporal`, `Usuario`).
- `backend/src/main/java/br/com/treino/repository/` — `ExercicioRepository`, `TreinoRepository`, `SerieRepository` e demais, sempre com filtro por dono.

## 3. Conceitos-chave

### 3.1 Flyway como versionamento do banco

Cada migration roda uma vez, em ordem. O `V2` é idempotente graças ao `WHERE NOT EXISTS`, então reexecutar não duplica:

```sql
-- V2__seed_exercicios.sql:5-9 (amostra; o arquivo lista 30 linhas no mesmo formato)
INSERT INTO exercicio (id, nome, grupo_muscular, equipamento)
SELECT v.id, v.nome, v.grupo_muscular, v.equipamento
FROM (VALUES
    ('11111111-1111-4111-8111-000000000001'::uuid, 'Supino reto com barra', 'peito', 'barra'),
    ('11111111-1111-4111-8111-000000000002'::uuid, 'Supino inclinado com halteres', 'peito', 'halter'),
-- V2__seed_exercicios.sql:39-44 (idempotência completa)
WHERE NOT EXISTS (
    SELECT 1
    FROM exercicio e
    WHERE e.usuario_id IS NULL
      AND e.nome = v.nome
);
```

E o Hibernate nunca altera o schema sozinho — só valida contra o Flyway (`application.yml:15` → `ddl-auto: validate`).

### 3.2 Índices: isolar por usuário e acelerar histórico

Toda listagem filtra por `usuario_id`; sem índice, cada query varre a tabela inteira. O índice composto cobre o filtro mais comum (meus treinos por data):

```sql
-- V1__create_schema.sql:60 e 75-76
CREATE INDEX IF NOT EXISTS idx_treino_usuario_data ON treino (usuario_id, data);
CREATE INDEX IF NOT EXISTS idx_serie_treino ON serie (treino_id);
CREATE INDEX IF NOT EXISTS idx_serie_exercicio ON serie (exercicio_id);
```

### 3.3 LAZY vs N+1: preguiça controlada

Todas as associações são `LAZY` — o JPA só busca o relacionado quando o código toca nele (`Treino.java:29`, `Treino.java:48`). Isso evita carregar a academia inteira num `findById`, mas cobra 1 query extra por acesso (problema N+1). A saída nos pontos quentes é `JOIN FETCH`, que traz tudo numa query só:

```java
// SerieRepository.java:15-16
@Query("SELECT s FROM Serie s JOIN FETCH s.treino t WHERE t.usuario.id = :usuarioId AND s.exercicio.id = :exercicioId ORDER BY t.data ASC, s.numero ASC")
List<Serie> historicoPorExercicio(@Param("usuarioId") UUID usuarioId, @Param("exercicioId") UUID exercicioId);
```

Nos pontos frios (poucas séries por treino), o N+1 é aceito e documentado no código.

## 4. Auto-teste (2 perguntas com respostas)

**1. Por que `carga_kg NUMERIC(6,2)` no banco e `BigDecimal` no Java (`Serie.java:39-40`, `V1:68`)?**
Porque `double`/`float` arredondam em binário (0.1 + 0.2 ≠ 0.3) — inaceitável para carga e volume. `BigDecimal` + `numeric(6,2)` dá exatidão decimal com 2 casas, até 9999.99 kg.

**2. O que acontece se dois devs criarem migrations `V3` diferentes?**
O Flyway usa checksum: na segunda máquina a migration com mesmo número e conteúdo diferente falha na validação. Por isso migration aplicada nunca se edita — cria-se uma nova.

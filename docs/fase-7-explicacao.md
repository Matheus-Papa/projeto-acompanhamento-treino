# Fase 7 — Testes: unitários, integração real e isolamento

> O que esta fase fez: criou `CalculoTreinoTest` (matemática pura), `DeteccaoPrTest` (PRs com repositories mockados), `TreinoIntegrationTest` (rollback, histórico e volume contra Postgres de verdade) e `SegurancaIsolamentoTest` (401/404 via HTTP com MockMvc). Base comum: `AbstractIntegrationTest` com Testcontainers. Rode com `./mvnw verify`.

## 1. Analogia simples: simulador, pista de teste e fiscal

O teste unitário é o simulador de direção: barato, rápido, sem carro real (Epley sem Spring nem banco). O mock é o manequim de crash-test: finge ser o banco para testar só a lógica de PRs. A integração é a pista de teste com carro real (Postgres em container). E o MockMvc é o fiscal que bate na porta da API como um cliente HTTP de verdade.

## 2. O que a fase construiu

- `backend/src/test/java/br/com/treino/service/CalculoTreinoTest.java` — 7 testes puros de Epley/volume, sem Spring.
- `backend/src/test/java/br/com/treino/service/DeteccaoPrTest.java` — 6 testes de PR via `TreinoService` com Mockito, sem banco.
- `backend/src/test/java/br/com/treino/TreinoIntegrationTest.java` — 5 cenários fim a fim no container (rollback, histórico, volume).
- `backend/src/test/java/br/com/treino/SegurancaIsolamentoTest.java` — isolamento A×B e 401 via MockMvc.
- `backend/src/test/java/br/com/treino/AbstractIntegrationTest.java` — Postgres 16 em container + Flyway V1/V2 automáticos.

## 3. Conceitos-chave

### 3.1 Testcontainers: banco real, descartável

Cada classe de teste sobe um Postgres 16 em Docker e injeta a URL por `DynamicPropertySource`; o Flyway migra schema + seed sozinho (`AbstractIntegrationTest.java:19-29`). Nada de H2 com dialeto diferente — o teste roda contra o mesmo banco da produção:

```java
// AbstractIntegrationTest.java:19-20
@Container
private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");
```

### 3.2 Mock de PR: testar lógica sem banco

Com repositories mockados, o teste injeta histórico falso e verifica o tipo de PR — inclusive o caso sutil "carga maior, Epley menor → só CARGA" (`DeteccaoPrTest.java:121-136`). E `verify(..., never()).save(...)` prova que dado inválido nem chega a persistir (`DeteccaoPrTest.java:166-168`):

```java
// DeteccaoPrTest.java:166-168
assertThrows(ConflictException.class, () -> treinoService.registrar(request));
verify(treinoRepository, never()).save(any(Treino.class));
```

O service lê o dono do `SecurityContext`, então cada teste se autentica com um UUID antes e limpa depois (`DeteccaoPrTest.java:75-90`).

### 3.3 Rollback-count: provar o "tudo ou nada" com números

O teste de integração **não** usa `@Transactional` de propósito: com commits reais, `count() == 0` após a exceção prova que nada ficou no banco (`TreinoIntegrationTest.java:37-39`, `123-125`). O isolamento A×B vai pelo HTTP: o dono enxerga o treino (200) e o outro usuário recebe 404 (`SegurancaIsolamentoTest.java:39-49`):

```java
// SegurancaIsolamentoTest.java:39-44
mvc.perform(get("/treinos/" + treinoId).header("Authorization", "Bearer " + tokenA))
        .andExpect(status().isOk());
mvc.perform(get("/treinos/" + treinoId).header("Authorization", "Bearer " + tokenB))
        .andExpect(status().isNotFound());

```java
// TreinoIntegrationTest.java:123-125
assertThrows(ConflictException.class, () -> treinoService.registrar(request));
assertEquals(0, treinoRepository.count());
assertEquals(0, serieRepository.count());
```

## 4. Auto-teste (2 perguntas com respostas)

**1. Por que não anotar o teste de integração com `@Transactional`?**
Porque o Spring desfaria tudo no fim do teste de qualquer jeito — o `count() == 0` passaria mesmo sem rollback real, e o teste provaria nada. Sem a anotação, só o rollback de verdade zera as tabelas.

**2. Quando usar MockMvc e quando chamar o service direto?**
Service direto para regras e dados (rollback, cálculos, agrupamentos). MockMvc para o que só existe no HTTP: status 401/404, envelope de erro, header `Bearer` — o que o fiscal da porta vê.

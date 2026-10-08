# Fase 5 — Estatísticas: Epley, recordes e volume semanal

> O que esta fase fez: criou a classe pura `CalculoTreino` (Epley + volume), o `GET /exercicios/{id}/recorde` (`{maxCarga, maxEpley, emTreinoId, emData}`) e o `GET /estatisticas/volume-semanal` (`[{semanaSegunda, volume}]`, semanas ISO segunda→domingo). Tudo `@Transactional(readOnly = true)` e isolado por usuário.

## 1. Analogia simples: a tabela de equivalência da academia

Na parede da academia há uma tabela: "se você faz 10 reps com 80 kg, equivale a ~107 kg em 1 rep". Essa tabela é a fórmula de Epley. O recorde é o troféu na estante (maior carga e maior equivalência). O volume semanal é o caderno do professor somando `reps × carga` de segunda a domingo.

## 2. O que a fase construiu

- `backend/src/main/java/br/com/treino/service/CalculoTreino.java` — `epley(carga, reps)` e `volume(reps, carga)`, puros e sem estado.
- `backend/src/main/java/br/com/treino/service/EstatisticaService.java` — `recordePorExercicio()` e `volumeSemanal()`.
- `backend/src/main/java/br/com/treino/controller/EstatisticaController.java` — `GET /estatisticas/volume-semanal`.
- Recorde pendurado em `ExercicioController.java:79-82` (`GET /exercicios/{id}/recorde`).
- DTOs `RecordeResponse` e `VolumeSemanalResponse`.

## 3. Conceitos-chave

### 3.1 Epley: 1 repetição é a própria carga

Estimativa de 1RM = `carga × (1 + reps/30)`, com 2 casas (`CalculoTreino.java:12-28`). O caso `reps == 1` retorna a carga sem extrapolar — sem isso, a fórmula inventaria força que não foi demonstrada:

```java
// CalculoTreino.java:21-23
if (reps == 1) {
    return carga.setScale(2, RoundingMode.HALF_UP);
}
```

Exemplo do teste: 80 kg × 10 reps → `80 × (1 + 10/30)` = 106.67 (`TreinoIntegrationTest.java:164`).

### 3.2 Recorde: dois máximos independentes + desempate pelo recente

Carga máxima e Epley máximo podem vir de séries diferentes (80×10 tem Epley maior que 90×1). O empate no Epley fica com o treino mais recente via `>=` (`EstatisticaService.java:59-62`), e sem histórico a resposta é 404, não zero:

```java
// EstatisticaService.java:46-49
List<Serie> historico = serieRepository.historicoPorExercicio(usuarioId, exercicioId);
if (historico.isEmpty()) {
    throw new NotFoundException("Sem treinos para este exercício");
}
```

### 3.3 Semana ISO e volume: segunda-feira como chave

Cada treino cai na segunda da sua semana (`with(DayOfWeek.MONDAY)`), e um `TreeMap` já ordena as semanas; só semanas com treino aparecem (`EstatisticaService.java:77-91`). Volume do treino = Σ `reps × carga` (`CalculoTreino.java:30-38`):

```java
// EstatisticaService.java:86-87
LocalDate segunda = treino.getData().with(DayOfWeek.MONDAY);
porSemana.merge(segunda, volumeTreino, BigDecimal::add);
```

## 4. Auto-teste (2 perguntas com respostas)

**1. 90 kg × 1 é PR de carga contra um histórico de 80 kg × 10, mas é PR de Epley?**
Não: Epley novo = 90.00, anterior = 106.67. Por isso o sistema tem dois tipos (`CARGA` vs `E1RM`) — caso coberto em `DeteccaoPrTest.java:121-136`.

**2. Por que o padrão do volume semanal é 56 dias (`EstatisticaService.java:73`)?**
Oito semanas é a janela útil de um gráfico de evolução: curta o bastante para a query N+1 continuar barata, longa o bastante para mostrar tendência.

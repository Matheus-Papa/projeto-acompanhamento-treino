# Fase 4 — Treinos: registro transacional, histórico e paginação

> O que esta fase fez: criou `POST /treinos` (transacional, com detecção de PRs na resposta), `GET /treinos` paginado com filtro de período, `GET /treinos/{id}` e `GET /exercicios/{id}/historico` (por treino, com melhor série, volume e Epley). Validações: `repeticoes > 0`, `carga >= 0`, exercício acessível, `data <= hoje`.

## 1. Analogia simples: a prova com grampeador

Registrar um treino é entregar uma prova grampeada: ou o professor recebe **todas** as páginas, ou não recebe nenhuma. Se a questão 7 (uma série) está inválida, ele devolve a prova inteira — não guarda metade. Esse "tudo ou nada" é o `@Transactional` com rollback total.

## 2. O que a fase construiu

- `backend/src/main/java/br/com/treino/service/TreinoService.java` — `registrar()`, `listar()`, `buscarPorId()`, `historicoPorExercicio()`, `detectarPrs()`, `gerarSeriesDaFicha()`.
- `backend/src/main/java/br/com/treino/controller/TreinoController.java` — `POST /treinos` (201 + `Location`), `GET /treinos` (`?de=&ate=&page=&size=`), `GET /treinos/{id}`.
- Rota de histórico pendurada em `ExercicioController.java:74-77` (`GET /exercicios/{id}/historico`).
- `SerieRepository.historicoPorExercicio` com `JOIN FETCH` para ler a data sem N+1.

## 3. Conceitos-chave

### 3.1 @Transactional: qualquer série inválida apaga tudo

O método valida **antes** de persistir e roda numa transação única; exceção no meio desfaz treino + séries (`TreinoService.java:72-74` e `191-207`):

```java
// TreinoService.java:72-74
// Transação única: qualquer série inválida desfaz treino + séries (rollback total).
@Transactional
public TreinoResponse registrar(TreinoRequest request) {
```

A prova real está no teste: após `ConflictException`, `treinoRepository.count()` e `serieRepository.count()` voltam a 0 (`TreinoIntegrationTest.java:123-125`).

### 3.2 PRs calculados antes de salvar, em 1 query

Os recordes são detectados contra o histórico **já persistido**, antes do `save` — senão o próprio treino "bateria recorde" contra si mesmo (`TreinoService.java:101-102`). E o histórico de todos os exercícios vem numa query com `IN`, agrupado em memória (`SerieRepository.java:19-21`, `TreinoService.java:211-230`):

```java
// TreinoService.java:102 — PRs calculados ANTES de salvar, contra o histórico já persistido.
List<PrResponse> prs = detectarPrs(usuarioId, pedidos);
```

Tipos de PR: `CARGA` (carga máxima superada), `E1RM` (Epley superado) ou `AMBOS`; primeira carga > 0 conta como `AMBOS`, carga zero nunca gera PR.

### 3.3 Paginação e gerar-da-ficha

Listar usa `PageRequest` ordenado por data desc, com intervalo amplo quando sem filtro (`TreinoService.java:117-124`; limites `size ≤ 100` em `TreinoController.java:53`):

```java
// TreinoService.java:119-123
LocalDate inicio = de != null ? de : DATA_MINIMA_LISTAGEM;
LocalDate fim = ate != null ? ate : DATA_MAXIMA_LISTAGEM;
Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "data"));
return treinoRepository.findByUsuarioIdAndDataBetween(usuarioId, inicio, fim, pageable)
```

Enviar `fichaId` sem séries gera as séries da ficha (número sequencial global, `repsAlvo` com carga zero):

```java
// TreinoService.java:176-183
private List<SerieRequest> gerarSeriesDaFicha(Ficha ficha) {
    List<FichaExercicio> itens = fichaExercicioRepository.findByFichaIdOrderByOrdemAsc(ficha.getId());
```

## 4. Auto-teste (2 perguntas com respostas)

**1. Por que o `GET /treinos/{id}` retorna `prs` vazio (`TreinoService.java:131`)?**
Porque PR é um evento do momento do registro ("você bateu recorde **agora**"). Recalcular no GET compararia o treino contra um histórico que já o inclui — ele sempre "empataria consigo mesmo".

**2. Por que `data <= hoje` é `@PastOrPresent` no DTO (`TreinoRequest.java:19-21`) E checada no service (`TreinoService.java:76-78`)?**
Defesa em profundidade: o DTO barra na borda HTTP com 400; o service barra em chamada direta (testes, jobs) com rollback. Camadas diferentes, mesma regra.

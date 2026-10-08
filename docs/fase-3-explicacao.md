# Fase 3 — Exercícios, fichas, erros padronizados e Swagger

> O que esta fase fez: criou os DTOs como `records` com Bean Validation em português (`dto/`), o `GlobalExceptionHandler` com envelope de erro único, o CRUD de `/exercicios` (globais + próprios) e de `/fichas` (itens ordenados com ordem única), e ligou o Swagger (`/swagger-ui.html`, `/v3/api-docs`). Regra de ouro: entidade JPA nunca sai na API.

## 1. Analogia simples: cardápio, recepcionista e manual de reclamações

O DTO é o cardápio: o cliente vê nome e preço, nunca a receita secreta (a entidade). O `ControllerAdvice` é a recepcionista que transforma qualquer problema — prato errado (400), prato inexistente (404) — num formulário padrão de reclamação. E o Swagger é o cardápio ilustrado na parede: dá para ver e testar tudo sem perguntar ao cozinheiro.

## 2. O que a fase construiu

- `backend/src/main/java/br/com/treino/dto/` — `ExercicioRequest/Response`, `FichaRequest/Response`, `FichaItemRequest/Response` como `records`, mapeamento manual no service.
- `backend/src/main/java/br/com/treino/exception/GlobalExceptionHandler.java` — envelope `{timestamp, status, mensagem, caminho, errosCampos}`.
- `service/ExercicioService.java` + `controller/ExercicioController.java` — CRUD com isolamento.
- `service/FichaService.java` + `controller/FichaController.java` — ficha com itens ordenados.
- `config/OpenApiConfig.java` — Swagger ativo.

## 3. Conceitos-chave

### 3.1 Records + Bean Validation: contrato blindado em português

O `record` é imutável e carrega as regras na declaração; mensagens em português vão direto para `errosCampos` no 400 (`GlobalExceptionHandler.java:54-61`):

```java
// ExercicioRequest.java:6-16
public record ExercicioRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
    String nome,
    @NotBlank(message = "Grupo muscular é obrigatório")
    @Size(max = 60, message = "Grupo muscular deve ter no máximo 60 caracteres")
    String grupoMuscular,
    @Size(max = 60, message = "Equipamento deve ter no máximo 60 caracteres")
    String equipamento) {
}
```

### 3.2 404 em vez de 403: não vazar existência

Recurso global ou de outro usuário responde **404**, nunca 403 — assim um curioso não descobre o que existe (`ExercicioService.java:73-78`):

```java
// ExercicioService.java:74 — Isolamento: global ou de outro usuário responde 404 (nunca 403).
private Exercicio buscarProprioOu404(UUID id) {
    return exercicioRepository.findProprioPorId(id, usuarioId)
        .orElseThrow(() -> new NotFoundException("Exercício não encontrado"));
}
```

### 3.3 LEFT JOIN: o detalhe que salva os globais

Exercícios globais têm `usuario NULL`. Navegar `e.usuario.id` geraria `INNER JOIN` e os excluiria do resultado. O `LEFT JOIN` explícito mantém os globais visíveis (`ExercicioRepository.java:13-16`):

```java
// ExercicioRepository.java:15-16
@Query("SELECT e FROM Exercicio e LEFT JOIN e.usuario u WHERE u.id IS NULL OR u.id = :usuarioId ORDER BY e.nome ASC")
List<Exercicio> findAcessiveis(@Param("usuarioId") UUID usuarioId);
```

Ordem única na ficha é garantida em duas camadas: `UNIQUE (ficha_id, ordem)` no banco (`V1:44`) e checagem de duplicada no service (`FichaService.java:123-125`), que devolve 409 com mensagem clara em vez de erro críptico do banco.

## 4. Auto-teste (2 perguntas com respostas)

**1. Por que não retornar a entidade JPA direto no controller?**
Porque ela vaza o modelo interno (ex.: `senhaHash`), carrega `LAZY` fora da transação (`LazyInitializationException`) e qualquer mudança no banco quebra a API. O DTO congela o contrato.

**2. Quando um exercício global retorna 404 na edição mas 200 na leitura?**
Na leitura vale `findAcessivelPorId` (global + próprio). Na edição vale `findProprioPorId` (só próprio): global não tem dono para editar, então é 404 — ninguém altera o "haltere padrão" da academia.

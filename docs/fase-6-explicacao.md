# Fase 6 — Segurança: BCrypt, JWT, filtro, CORS e isolamento

> O que esta fase fez: criou `POST /auth/register` e `/auth/login` (BCrypt + JWT de 1h), o `JwtAuthenticationFilter` (lê `Authorization: Bearer`), o `SecurityConfig` (rotas públicas vs autenticadas, 401 em envelope), o `CorsConfig` (origem via env) e o `CurrentUser` (contrato de isolamento usado por todos os services). Sem token → 401; recurso alheio → 404.

## 1. Analogia simples: pulseira, catraca e armários

O cadastro entrega a pulseira (JWT assinado, válido 1h). A catraca (filtro) confere a pulseira a cada entrada sem guardar lista de quem está dentro (stateless). Cada armário só abre com a sua pulseira (isolamento por `usuarioId`), e se um estranho tentar abrir o seu, a academia diz "armário inexistente" (404) em vez de "é do vizinho" (403). O CORS é o segurança da porta que só deixa entrar quem vem do endereço combinado (o front em `localhost:5173`).

## 2. O que a fase construiu

- `backend/src/main/java/br/com/treino/service/AuthService.java` — register (409 se e-mail existe) e login (401 genérico "Credenciais inválidas").
- `backend/src/main/java/br/com/treino/security/JwtService.java` — gera/valida token com `subject = usuarioId`, expiração 1h.
- `backend/src/main/java/br/com/treino/security/JwtAuthenticationFilter.java` — extrai o `Bearer` e autentica no contexto.
- `backend/src/main/java/br/com/treino/config/SecurityConfig.java` — `stateless`, públicas liberadas, 401 em envelope manual.
- `config/CorsConfig.java` + `security/CurrentUser.java` — origem via env e `currentUsuarioId()`.

## 3. Conceitos-chave

### 3.1 BCrypt: hash com sal, nunca a senha

No cadastro a senha é hasheada; no login compara-se com `matches` — a senha original nunca é armazenada nem logada (`AuthService.java:35-38` e `49-51`). E a mensagem de erro é igual para e-mail inexistente ou senha errada, para não revelar quais e-mails existem:

```java
// AuthService.java:38 — cadastro guarda só o hash
passwordEncoder.encode(request.senha()));
// AuthService.java:49 — login compara sem descriptografar
if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
```

### 3.2 JWT Bearer + filtro stateless

O token vai no header `Authorization: Bearer <token>`; o filtro extrai o `usuarioId` do `subject` e o pendura no `SecurityContext` (`JwtAuthenticationFilter.java:28-35`). Token inválido não explode — limpa o contexto e o entry point devolve 401 (`JwtAuthenticationFilter.java:36-39`, `SecurityConfig.java:38-40`). A chave exige ≥ 32 chars só via env, e a aplicação nem sobe sem ela:

```java
// JwtService.java:22-25
if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
    throw new IllegalStateException(
        "JWT_SECRET inválido: defina a variável de ambiente com ao menos 32 caracteres");
}
```

Públicos: `/auth/**`, `/health`, `/actuator/health`, Swagger (`SecurityConfig.java:41-51`).

### 3.3 CORS via env + CurrentUser como contrato

A origem permitida vem de variável (`CorsConfig.java:17-23`, default `http://localhost:5173`), e todo service lê o dono da requisição de um ponto único (`CurrentUser.java:15-21`):

```java
// CurrentUser.java:15-18 — sem autenticação válida, 401
public static UUID currentUsuarioId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UUID usuarioId)) {
```

## 4. Auto-teste (2 perguntas com respostas)

**1. Por que o 401 do filtro não passa pelo `GlobalExceptionHandler` (`SecurityConfig.java:61`)?**
Porque o filtro roda antes do DispatcherServlet: a exceção nunca chega ao controller, então o handler nunca a vê. Por isso o 401 é escrito à mão no mesmo envelope JSON.

**2. Roubaram meu token: o que limita o estrago?**
A expiração de 1h (`application.yml:39`, `expiration-ms: 3600000`). Passado esse tempo o token morre sozinho — sem refresh token nem blacklist, o projeto aceita pedir login de novo.

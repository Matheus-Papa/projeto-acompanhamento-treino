# Fase 8 — Frontend React: telas, auth, gráficos e timer

> O que esta fase fez: criou o SPA React 18 + Vite + TS (`frontend/src/`): `AuthContext` + `ProtectedRoute`, cliente axios com 401→login, páginas de treino (registro com Zod + timer de descanso), histórico paginado, evolução e estatísticas com Recharts, exercícios, fichas e medidas. CSS puro mobile-first, com a tela de registro pensada para 1 mão na academia.

## 1. Analogia simples: o app é o personal no bolso

O backend é a academia; o frontend é o personal que vai com você: guarda sua pulseira (token no `localStorage`), monta a ficha do dia, cronometra seu descanso apitando no fim, mostra seu gráfico de evolução e, se a pulseira vencer (401), te leva de volta à recepção (login) sozinho.

## 2. O que a fase construiu

- `frontend/src/main.tsx` — `QueryClientProvider` + `BrowserRouter` na raiz.
- `frontend/src/auth/AuthContext.tsx` + `routes/ProtectedRoute.tsx` — login/cadastro/logout e guarda de rotas.
- `frontend/src/api/client.ts` — axios com `Bearer` automático, 401→`/login` e `mensagemErro` em português.
- `frontend/src/pages/RegistrarTreinoPage.tsx` — validação Zod, copiar série, usar-ficha-como-base, exibe PRs.
- `frontend/src/components/RestTimer.tsx` — timer com presets, beep e vibração.
- `EvolucaoPage.tsx`, `EstatisticasPage.tsx`, `HistoricoPage.tsx` — Recharts + paginação.

## 3. Conceitos-chave

### 3.1 Query + Router + Hook Form com Zod: cada um no seu papel

O TanStack Query gerencia dado do servidor (cache por `queryKey`, `isPending`/`isError` na tela); o Router gerencia URL e guarda; o Zod valida o formulário local antes do POST (`RegistrarTreinoPage.tsx:34-65`, `HistoricoPage.tsx:23-31`):

```tsx
// RegistrarTreinoPage.tsx:44-49 — mesma regra do backend, em TS
const treinoSchema = z.object({
  data: z.string().min(1, "Data é obrigatória")
    .refine((d) => d <= hojeISO(), "Data não pode ser futura"),
```

Duplicar a regra no front não é desperdício: o front valida para UX instantânea, o backend para segurança.

### 3.2 401 → login automático + tipos do OpenAPI

Dois interceptors: um anexa o `Bearer` em toda request, o outro expulsa para `/login` ao receber 401 (`client.ts:10-27`). E `mensagemErro` extrai o português do envelope do backend (`client.ts:30-39`):

```ts
// client.ts:10-16 — toda request leva o Bearer automaticamente
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

```ts
// client.ts:21-24 — sessão expirada: limpa e redireciona
if (error?.response?.status === 401 && window.location.pathname !== "/login") {
  localStorage.removeItem(TOKEN_KEY);
  window.location.href = "/login";
}
```

### 3.3 Recharts + timer: evolução visível, descanso audível

Evolução plota 1RM (linha) e volume (barras) a partir do histórico (`EvolucaoPage.tsx:115-122`); estatísticas limitam os recordes a 5 exercícios para não estourar o backend (`EstatisticasPage.tsx:55-60`). O timer usa `setInterval` com limpeza no unmount e efeito separado que dispara beep + vibração ao zerar (`RestTimer.tsx:58-75`).

## 4. Auto-teste (2 perguntas com respostas)

**1. Por que a query-key do histórico inclui filtros e página (`HistoricoPage.tsx:24`)?**
Porque a chave é o cache: filtros diferentes precisam de entradas diferentes. Sem isso, trocar o período mostraria dados velhos do filtro anterior.

**2. O que acontece se o usuário recarregar a página no meio do treino?**
O token sobrevive (`localStorage`), mas as linhas do formulário se perdem (estado em memória). Decisão aceita: rascunho persistente seria outra feature; o timer e o form recomeçam zerados.

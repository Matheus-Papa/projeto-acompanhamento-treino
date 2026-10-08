# Frontend — Acompanhamento de Treino (F8-01, F8-02 parcial)

React 18 + Vite 5 + TypeScript, React Router 6, TanStack Query 5,
React Hook Form + Zod, Axios, CSS puro mobile-first (sem Tailwind).

## Rodar

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # valida tsc + build
```

Configure a API em `.env` (veja `.env.example`):

```bash
VITE_API_URL=http://localhost:8080
```

## Tipos da API

`src/api/types.ts` é um **espelho manual** dos DTOs em
`backend/src/main/java/br/com/treino/dto/*.java`
(Auth, Exercicio, Ficha, Treino, Serie, Pr, Historico, Recorde,
VolumeSemanal, Medida, ErroPadrao) e dos paths dos controllers
(`/auth`, `/exercicios`, `/fichas`, `/treinos`, `/medidas`, `/estatisticas`).

Com a API rodando, pode ser regenerado via:

```bash
npx openapi-typescript http://localhost:8080/v3/api-docs -o src/api/types.ts
```

Nesse caso, adapte os imports que hoje usam os tipos manuais.

## Rotas

- `/login`, `/cadastro` (públicas)
- `/` (home — placeholder com links; registro completo de treino é F8-B)
- `/exercicios` (lista globais+próprios com badge "global" + criar próprio)
- `/fichas` (lista + criar com itens ordenados)
- `/fichas/:id` (editar + excluir)

Telas de histórico, gráficos, stats e medidas ficam para o F8-B.

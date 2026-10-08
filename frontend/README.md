# Frontend — Acompanhamento de Treino

React 18 + Vite 5 + TypeScript, React Router 6, TanStack Query 5,
React Hook Form + Zod, Axios, Recharts, CSS puro mobile-first (sem Tailwind).

Visão geral do projeto: veja o [README raiz](../README.md).

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

## Rotas

- `/login`, `/cadastro` (públicas; redirecionam para `/` se já logado)
- `/` (registrar treino + timer de descanso)
- `/historico` (lista paginada com "carregar mais"), `/treinos/:id` (detalhe)
- `/evolucao`, `/evolucao/:exercicioId` (recorde + gráficos Epley/volume)
- `/estatisticas` (volume semanal + recordes da amostra)
- `/medidas` (registro + gráfico de peso)
- `/exercicios` (lista com badge "global" + criar próprio)
- `/fichas`, `/fichas/:id` (lista/criar, editar + excluir)

## Tipos da API

`src/api/types.ts` espelha os DTOs do backend (`br.com.treino.dto.*`):
numéricos de `BigDecimal` (`cargaKg`, `volume`, `epley`, `maxCarga`,
`maxEpley`, `pesoKg`, `percentualGordura`) são `number` no JSON. Inputs de
formulário continuam string e usam `z.coerce` na validação.

Com a API rodando, regenere via OpenAPI:

```bash
npx openapi-typescript http://localhost:8080/v3/api-docs -o src/api/types.ts
```

Nesse caso, adapte os imports que hoje usam os tipos manuais.

## Fora de escopo (conhecido)

- Edição/exclusão de exercício (só criação; Globais vêm do backend).
- Agregado de recordes no backend (`Recordes` usa amostra de até 5 exercícios).

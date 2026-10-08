// Espelho manual dos DTOs do backend (br.com.treino.dto.*).
// Regenerar via OpenAPI quando a API estiver rodando:
// npx openapi-typescript http://localhost:8080/v3/api-docs -o src/api/types.ts
// Controllers espelhados: AuthController (/auth), ExercicioController (/exercicios),
// FichaController (/fichas), TreinoController (/treinos), MedidaController (/medidas),
// EstatisticaController (/estatisticas). Recurso de outro usuário retorna 404.

export interface AuthResponse {
  token: string;
  expiresIn: number;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface RegisterRequest {
  nome: string;
  email: string;
  senha: string;
}

export interface Exercicio {
  id: string;
  nome: string;
  grupoMuscular: string;
  equipamento: string | null;
  global: boolean;
}

export interface ExercicioRequest {
  nome: string;
  grupoMuscular: string;
  equipamento?: string;
}

export interface FichaItem {
  ordem: number;
  seriesAlvo: number;
  repsAlvo: number;
  exercicio: Exercicio;
}

export interface Ficha {
  id: string;
  nome: string;
  itens: FichaItem[];
}

export interface FichaItemRequest {
  exercicioId: string;
  ordem: number;
  seriesAlvo: number;
  repsAlvo: number;
}

export interface FichaRequest {
  nome: string;
  itens: FichaItemRequest[];
}

export interface Serie {
  id: string;
  numero: number;
  repeticoes: number;
  cargaKg: string;
  rpe: number | null;
  exercicio: Exercicio;
}

export interface SerieRequest {
  exercicioId: string;
  numero: number;
  repeticoes: number;
  cargaKg: number;
  rpe?: number;
}

export type TipoPr = "CARGA" | "E1RM" | "AMBOS";

export interface Pr {
  exercicioId: string;
  tipo: TipoPr;
}

export interface Treino {
  id: string;
  data: string;
  duracaoMin: number | null;
  observacao: string | null;
  fichaId: string | null;
  series: Serie[];
  prs: Pr[];
}

export interface TreinoRequest {
  fichaId?: string;
  data: string;
  duracaoMin?: number;
  observacao?: string;
  series?: SerieRequest[];
}

export interface TreinoResumo {
  id: string;
  data: string;
  duracaoMin: number | null;
  fichaId: string | null;
  totalSeries: number;
  volumeTotal: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface MelhorSerie {
  repeticoes: number;
  cargaKg: string;
}

export interface HistoricoItem {
  treinoId: string;
  data: string;
  melhorSerie: MelhorSerie;
  volume: string;
  epley: string;
}

export interface Recorde {
  exercicioId: string;
  maxCarga: string;
  maxEpley: string;
  emTreinoId: string;
  emData: string;
}

export interface VolumeSemanal {
  semanaSegunda: string;
  volume: string;
}

export interface Medida {
  id: string;
  data: string;
  pesoKg: string;
  percentualGordura: string | null;
}

export interface MedidaRequest {
  data: string;
  pesoKg: number;
  percentualGordura?: number;
}

export interface ErroCampo {
  campo: string;
  mensagem: string;
}

// Corpo padronizado de erro do @RestControllerAdvice do backend.
export interface ErroPadrao {
  timestamp: string;
  status: number;
  mensagem: string;
  caminho: string;
  errosCampos: ErroCampo[];
}

import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";
import { api, mensagemErro } from "../api/client";
import type { Exercicio, Ficha, Treino, TreinoRequest } from "../api/types";
import { hojeISO } from "../utils/data";
import { RestTimer } from "../components/RestTimer";

interface LinhaSerie {
  key: number;
  exercicioId: string;
  repeticoes: string;
  cargaKg: string;
}

const serieSchema = z.object({
  exercicioId: z.string().min(1, "Exercício é obrigatório"),
  repeticoes: z.coerce
    .number()
    .int("Repetições devem ser um número inteiro")
    .positive("Repetições devem ser maiores que zero"),
  cargaKg: z.coerce.number().nonnegative("Carga não pode ser negativa"),
});

const treinoSchema = z.object({
  fichaId: z.string().optional(),
  data: z
    .string()
    .min(1, "Data é obrigatória")
    .refine((d) => d <= hojeISO(), "Data não pode ser futura"),
  duracaoMin: z.preprocess(
    (v) => (v === "" || v === null || v === undefined ? undefined : v),
    z.coerce
      .number()
      .int("Duração deve ser um número inteiro")
      .positive("Duração deve ser maior que zero")
      .optional(),
  ),
  observacao: z
    .preprocess(
      (v) => (typeof v === "string" && v.trim() === "" ? undefined : v),
      z.string().max(500, "Observação deve ter no máximo 500 caracteres").optional(),
    )
    .optional(),
  series: z.array(serieSchema).min(1, "Adicione ao menos uma série"),
});

function rotuloPr(tipo: string): string {
  if (tipo === "CARGA") return "recorde de carga";
  if (tipo === "E1RM") return "recorde de 1RM (Epley)";
  return "recorde de carga + 1RM";
}

export function RegistrarTreinoPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const proximaKey = useRef(1);
  const [fichaId, setFichaId] = useState("");
  const [data, setData] = useState(hojeISO());
  const [duracao, setDuracao] = useState("");
  const [observacao, setObservacao] = useState("");
  const [linhas, setLinhas] = useState<LinhaSerie[]>([
    { key: 0, exercicioId: "", repeticoes: "10", cargaKg: "0" },
  ]);
  const [erros, setErros] = useState<Record<string, string>>({});
  const [erroApi, setErroApi] = useState("");
  const [concluido, setConcluido] = useState<Treino | null>(null);

  const exercicios = useQuery({
    queryKey: ["exercicios"],
    queryFn: async () => (await api.get<Exercicio[]>("/exercicios")).data,
  });
  const fichas = useQuery({
    queryKey: ["fichas"],
    queryFn: async () => (await api.get<Ficha[]>("/fichas")).data,
  });
  const ficha = useQuery({
    queryKey: ["ficha", fichaId],
    queryFn: async () => (await api.get<Ficha>(`/fichas/${fichaId}`)).data,
    enabled: fichaId !== "",
  });

  const finalizar = useMutation({
    mutationFn: async (corpo: TreinoRequest) => (await api.post<Treino>("/treinos", corpo)).data,
    onSuccess: (treino) => {
      setErroApi("");
      setErros({});
      setConcluido(treino);
      void queryClient.invalidateQueries({ queryKey: ["treinos"] });
      void queryClient.invalidateQueries({ queryKey: ["volume-semanal"] });
      void queryClient.invalidateQueries({ queryKey: ["recordes-destaque"] });
    },
    onError: (e) => setErroApi(mensagemErro(e, "Não foi possível registrar o treino.")),
  });

  // Auto-redirect após 4s. O cleanup cancela o timeout ao desmontar, então
  // cliques em "Ver detalhes"/"Ir para o histórico" (que navegam e desmontam)
  // nunca competem com o redirect.
  useEffect(() => {
    if (!concluido) return;
    const t = window.setTimeout(() => navigate("/historico"), 4000);
    return () => window.clearTimeout(t);
  }, [concluido, navigate]);

  function erroLinha(i: number, campo: string): string | undefined {
    return erros[`series.${i}.${campo}`];
  }

  function atualizarLinha(key: number, campo: keyof Omit<LinhaSerie, "key">, valor: string) {
    setLinhas((atual) => atual.map((l) => (l.key === key ? { ...l, [campo]: valor } : l)));
  }

  function novaLinha(base?: Partial<LinhaSerie>): LinhaSerie {
    const key = proximaKey.current++;
    return {
      key,
      exercicioId: base?.exercicioId ?? "",
      repeticoes: base?.repeticoes ?? "10",
      cargaKg: base?.cargaKg ?? "0",
    };
  }

  function adicionarSerie() {
    setLinhas((atual) => [...atual, novaLinha()]);
  }

  function copiarSerie(i: number) {
    setLinhas((atual) => {
      const origem = atual[i];
      if (!origem) return atual;
      const copia = novaLinha({ ...origem });
      return [...atual.slice(0, i + 1), copia, ...atual.slice(i + 1)];
    });
  }

  function copiarUltima() {
    if (linhas.length === 0) return;
    copiarSerie(linhas.length - 1);
  }

  function removerSerie(key: number) {
    setLinhas((atual) => (atual.length <= 1 ? atual : atual.filter((l) => l.key !== key)));
  }

  function usarFichaComoBase() {
    if (!ficha.data) return;
    if (linhas.some((l) => l.exercicioId !== "") && !window.confirm("Substituir as séries pela ficha?")) {
      return;
    }
    const itens = [...ficha.data.itens].sort((a, b) => a.ordem - b.ordem);
    const base: LinhaSerie[] = [];
    for (const item of itens) {
      for (let s = 0; s < item.seriesAlvo; s++) {
        base.push(
          novaLinha({
            exercicioId: item.exercicio.id,
            repeticoes: String(item.repsAlvo),
            cargaKg: "0",
          }),
        );
      }
    }
    if (base.length > 0) setLinhas(base);
  }

  function onFinalizar() {
    setErroApi("");
    const parse = treinoSchema.safeParse({
      fichaId: fichaId === "" ? undefined : fichaId,
      data,
      duracaoMin: duracao,
      observacao,
      series: linhas.map((l) => ({
        exercicioId: l.exercicioId,
        repeticoes: l.repeticoes,
        cargaKg: l.cargaKg,
      })),
    });
    if (!parse.success) {
      const mapa: Record<string, string> = {};
      for (const issue of parse.error.issues) {
        mapa[issue.path.join(".")] = issue.message;
      }
      setErros(mapa);
      return;
    }
    setErros({});
    const valido = parse.data;
    finalizar.mutate({
      fichaId: valido.fichaId,
      data: valido.data,
      duracaoMin: valido.duracaoMin,
      observacao: valido.observacao,
      series: valido.series.map((s, i) => ({
        exercicioId: s.exercicioId,
        numero: i + 1,
        repeticoes: s.repeticoes,
        cargaKg: s.cargaKg,
      })),
    });
  }

  function nomeExercicio(id: string): string {
    return exercicios.data?.find((e) => e.id === id)?.nome ?? "Exercício";
  }

  if (concluido) {
    return (
      <section className="card sucesso">
        <h1>Treino registrado!</h1>
        {concluido.prs.length === 0 ? (
          <p className="muted">Nenhum recorde desta vez. Bons treinos somam.</p>
        ) : (
          <>
            <p>Recordes batidos:</p>
            <ul className="lista">
              {concluido.prs.map((pr) => (
                <li key={`${pr.exercicioId}-${pr.tipo}`}>
                  <span>
                    <span className="badge-pr">PR!</span> <strong>{nomeExercicio(pr.exercicioId)}</strong>
                  </span>
                  <span className="muted">{rotuloPr(pr.tipo)}</span>
                </li>
              ))}
            </ul>
          </>
        )}
        <p className="muted">Indo para o histórico…</p>
        <div className="acoes">
          <Link className="botao" to={`/treinos/${concluido.id}`}>
            Ver detalhes
          </Link>
          <Link className="botao secundario" to="/historico">
            Ir para o histórico
          </Link>
        </div>
      </section>
    );
  }

  return (
    <div className="coluna">
      <section className="card">
        <h1>Treino de hoje</h1>
        <div className="coluna">
          <label>
            Ficha (opcional)
            <select value={fichaId} onChange={(e) => setFichaId(e.target.value)}>
              <option value="">Treino livre…</option>
              {fichas.data?.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.nome} ({f.itens.length} exercícios)
                </option>
              ))}
            </select>
          </label>
          {fichas.isPending && <p className="muted">Carregando fichas…</p>}
          {fichas.isError && <p className="erro">Não foi possível carregar as fichas.</p>}
          {fichaId !== "" && (
            <button
              type="button"
              className="secundario"
              disabled={!ficha.data || ficha.isPending}
              onClick={usarFichaComoBase}
            >
              {ficha.isPending ? "Carregando ficha…" : "Usar ficha como base"}
            </button>
          )}
          <div className="linha2">
            <label>
              Data
              <input type="date" value={data} max={hojeISO()} onChange={(e) => setData(e.target.value)} />
              {erros.data && <span className="erro">{erros.data}</span>}
            </label>
            <label>
              Duração (min, opcional)
              <input
                type="number"
                min={1}
                inputMode="numeric"
                value={duracao}
                onChange={(e) => setDuracao(e.target.value)}
              />
              {erros.duracaoMin && <span className="erro">{erros.duracaoMin}</span>}
            </label>
          </div>
          <label>
            Observação (opcional)
            <input
              type="text"
              maxLength={500}
              placeholder="Como foi o treino?"
              value={observacao}
              onChange={(e) => setObservacao(e.target.value)}
            />
            {erros.observacao && <span className="erro">{erros.observacao}</span>}
          </label>
        </div>
      </section>

      <RestTimer />

      <section className="card">
        <h2>Séries ({linhas.length})</h2>
        {exercicios.isPending && <p className="muted">Carregando exercícios…</p>}
        {exercicios.isError && <p className="erro">Não foi possível carregar os exercícios.</p>}
        {typeof erros.series === "string" && <p className="erro">{erros.series}</p>}
        <div className="coluna">
          {linhas.map((linha, i) => (
            <fieldset key={linha.key} className="serie-card">
              <legend>Série {i + 1}</legend>
              <label>
                Exercício
                <select
                  className="input-grande"
                  value={linha.exercicioId}
                  onChange={(e) => atualizarLinha(linha.key, "exercicioId", e.target.value)}
                >
                  <option value="">Selecione…</option>
                  {exercicios.data?.map((ex) => (
                    <option key={ex.id} value={ex.id}>
                      {ex.nome} ({ex.grupoMuscular})
                    </option>
                  ))}
                </select>
                {erroLinha(i, "exercicioId") && (
                  <span className="erro">{erroLinha(i, "exercicioId")}</span>
                )}
              </label>
              <div className="serie-grid serie-grid-2">
                <label>
                  Reps
                  <input
                    className="input-grande"
                    type="number"
                    min={1}
                    inputMode="numeric"
                    value={linha.repeticoes}
                    onChange={(e) => atualizarLinha(linha.key, "repeticoes", e.target.value)}
                  />
                  {erroLinha(i, "repeticoes") && (
                    <span className="erro">{erroLinha(i, "repeticoes")}</span>
                  )}
                </label>
                <label>
                  Carga (kg)
                  <input
                    className="input-grande"
                    type="number"
                    min={0}
                    step="0.5"
                    inputMode="decimal"
                    value={linha.cargaKg}
                    onChange={(e) => atualizarLinha(linha.key, "cargaKg", e.target.value)}
                  />
                  {erroLinha(i, "cargaKg") && <span className="erro">{erroLinha(i, "cargaKg")}</span>}
                </label>
              </div>
              <div className="acoes-linha">
                <button type="button" className="secundario" onClick={() => copiarSerie(i)}>
                  Copiar série anterior
                </button>
                <button
                  type="button"
                  className="perigo"
                  onClick={() => removerSerie(linha.key)}
                  disabled={linhas.length <= 1}
                >
                  Remover
                </button>
              </div>
            </fieldset>
          ))}
        </div>
        <div className="acoes">
          <button type="button" className="secundario" onClick={adicionarSerie}>
            Adicionar série
          </button>
          <button type="button" className="secundario" onClick={copiarUltima} disabled={linhas.length === 0}>
            Copiar última série
          </button>
        </div>
        <button
          type="button"
          className="botao-cheio"
          disabled={finalizar.isPending}
          onClick={onFinalizar}
        >
          {finalizar.isPending ? "Salvando…" : "Finalizar treino"}
        </button>
      </section>

      {erroApi && (
        <p className="erro" role="alert">
          {erroApi}
        </p>
      )}
    </div>
  );
}

import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { api, mensagemErro } from "../api/client";
import type { Exercicio } from "../api/types";

const schema = z.object({
  nome: z.string().min(1, "Nome é obrigatório").max(120, "Nome deve ter no máximo 120 caracteres"),
  grupoMuscular: z
    .string()
    .min(1, "Grupo muscular é obrigatório")
    .max(60, "Grupo muscular deve ter no máximo 60 caracteres"),
  equipamento: z
    .string()
    .max(60, "Equipamento deve ter no máximo 60 caracteres")
    .optional()
    .transform((v) => (v?.trim() ? v.trim() : undefined)),
});

type Form = z.input<typeof schema>;

export function ExerciciosPage() {
  const queryClient = useQueryClient();
  const [erroApi, setErroApi] = useState("");
  const [busca, setBusca] = useState("");
  const [grupoFiltro, setGrupoFiltro] = useState("todos");
  const lista = useQuery({
    queryKey: ["exercicios"],
    queryFn: async () => (await api.get<Exercicio[]>("/exercicios")).data,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Form>({ resolver: zodResolver(schema) });

  const criar = useMutation({
    mutationFn: async (dados: Form) => (await api.post<Exercicio>("/exercicios", dados)).data,
    onSuccess: () => {
      setErroApi("");
      reset();
      queryClient.invalidateQueries({ queryKey: ["exercicios"] });
    },
    onError: (e) => setErroApi(mensagemErro(e, "Não foi possível criar o exercício.")),
  });

  const grupos = useMemo(() => {
    const set = new Set<string>();
    for (const ex of lista.data ?? []) {
      if (ex.grupoMuscular.trim() !== "") set.add(ex.grupoMuscular);
    }
    return [...set].sort((a, b) => a.localeCompare(b, "pt-BR"));
  }, [lista.data]);

  const filtrados = useMemo(() => {
    const termo = busca.trim().toLowerCase();
    return (lista.data ?? []).filter((ex) => {
      if (grupoFiltro !== "todos" && ex.grupoMuscular !== grupoFiltro) return false;
      if (termo !== "" && !ex.nome.toLowerCase().includes(termo)) return false;
      return true;
    });
  }, [lista.data, busca, grupoFiltro]);

  const agrupados = useMemo(() => {
    const mapa = new Map<string, Exercicio[]>();
    for (const ex of filtrados) {
      const atual = mapa.get(ex.grupoMuscular) ?? [];
      atual.push(ex);
      mapa.set(ex.grupoMuscular, atual);
    }
    return [...mapa.entries()]
      .sort((a, b) => a[0].localeCompare(b[0], "pt-BR"))
      .map(([grupo, itens]) => ({
        grupo,
        itens: [...itens].sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR")),
      }));
  }, [filtrados]);

  const total = lista.data?.length ?? 0;
  const meus = lista.data?.filter((e) => !e.global).length ?? 0;

  return (
    <div className="coluna">
      <section className="card exercicios-topo">
        <p className="exercicios-kicker">Biblioteca</p>
        <h1>Exercícios</h1>
        <p className="muted exercicios-contador">
          {lista.isPending
            ? "Carregando exercícios…"
            : `${total} ${total === 1 ? "exercício" : "exercícios"} · ${meus} ${meus === 1 ? "meu" : "meus"}`}
        </p>
        <label className="exercicios-busca">
          <span className="exercicios-busca-rotulo">Buscar por nome</span>
          <input
            type="search"
            placeholder="Ex.: supino, agachamento…"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            autoComplete="off"
          />
        </label>
        {grupos.length > 0 && (
          <div className="chips" role="group" aria-label="Filtrar por grupo muscular">
            <button
              type="button"
              className={grupoFiltro === "todos" ? "chip chip-ativo" : "chip"}
              aria-pressed={grupoFiltro === "todos"}
              onClick={() => setGrupoFiltro("todos")}
            >
              Todos
            </button>
            {grupos.map((g) => (
              <button
                key={g}
                type="button"
                className={grupoFiltro === g ? "chip chip-ativo" : "chip"}
                aria-pressed={grupoFiltro === g}
                onClick={() => setGrupoFiltro(grupoFiltro === g ? "todos" : g)}
              >
                {g}
              </button>
            ))}
          </div>
        )}
      </section>

      <section className="card" aria-live="polite">
        <h2>Lista</h2>
        {lista.isPending && <p className="muted">Carregando…</p>}
        {lista.isError && <p className="erro">Não foi possível carregar os exercícios.</p>}
        {lista.data?.length === 0 && <p className="muted">Nenhum exercício ainda.</p>}
        {lista.data && lista.data.length > 0 && filtrados.length === 0 && (
          <p className="muted">Nada encontrado para essa busca ou filtro.</p>
        )}
        {agrupados.map((secao) => (
          <div key={secao.grupo} className="grupo-exercicios">
            <h3 className="grupo-titulo">
              {secao.grupo}
              <span className="grupo-contador">{secao.itens.length}</span>
            </h3>
            <ul className="lista lista-exercicios">
              {secao.itens.map((ex) => (
                <li key={ex.id} className="exercicio-card">
                  <div className="exercicio-linha">
                    <strong className="exercicio-nome">{ex.nome}</strong>
                    {ex.global ? (
                      <span className="badge">global</span>
                    ) : (
                      <span className="badge badge-meu">meu</span>
                    )}
                  </div>
                  <span className="muted">
                    {ex.grupoMuscular}
                    {ex.equipamento ? ` · ${ex.equipamento}` : ""}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </section>

      <section className="card form-destaque">
        <p className="exercicios-kicker">Personalizado</p>
        <h2 className="form-destaque-titulo">Novo exercício</h2>
        <p className="muted form-destaque-sub">
          Crie variações próprias para usar nas fichas e nos treinos.
        </p>
        <form onSubmit={handleSubmit((d) => criar.mutate(d))} noValidate>
          <label>
            Nome
            <input type="text" placeholder="Ex.: Supino inclinado com halter" {...register("nome")} />
            {errors.nome && <span className="erro">{errors.nome.message}</span>}
          </label>
          <label>
            Grupo muscular
            <input type="text" placeholder="Ex.: Peito" {...register("grupoMuscular")} />
            {errors.grupoMuscular && (
              <span className="erro">{errors.grupoMuscular.message}</span>
            )}
          </label>
          <label>
            Equipamento (opcional)
            <input type="text" placeholder="Ex.: Halter" {...register("equipamento")} />
            {errors.equipamento && (
              <span className="erro">{errors.equipamento.message}</span>
            )}
          </label>
          {erroApi && (
            <p className="erro" role="alert">
              {erroApi}
            </p>
          )}
          <button type="submit" disabled={criar.isPending} className="botao-cheio">
            {criar.isPending ? "Salvando…" : "Criar exercício"}
          </button>
        </form>
      </section>
    </div>
  );
}

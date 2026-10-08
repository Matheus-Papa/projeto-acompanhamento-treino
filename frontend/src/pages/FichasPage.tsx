import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useFieldArray, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link } from "react-router-dom";
import { api, mensagemErro } from "../api/client";
import type { Exercicio, Ficha } from "../api/types";
import { fichaSchema, type FichaForm } from "./fichaSchema";

export function FichasPage() {
  const queryClient = useQueryClient();
  const [erroApi, setErroApi] = useState("");

  const fichas = useQuery({
    queryKey: ["fichas"],
    queryFn: async () => (await api.get<Ficha[]>("/fichas")).data,
  });
  const exercicios = useQuery({
    queryKey: ["exercicios"],
    queryFn: async () => (await api.get<Exercicio[]>("/exercicios")).data,
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<FichaForm>({
    resolver: zodResolver(fichaSchema),
    defaultValues: { nome: "", itens: [{ exercicioId: "", ordem: 1, seriesAlvo: 3, repsAlvo: 10 }] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: "itens" });

  const criar = useMutation({
    mutationFn: async (dados: FichaForm) => (await api.post<Ficha>("/fichas", dados)).data,
    onSuccess: () => {
      setErroApi("");
      reset();
      queryClient.invalidateQueries({ queryKey: ["fichas"] });
    },
    onError: (e) => setErroApi(mensagemErro(e, "Não foi possível criar a ficha.")),
  });

  return (
    <div className="coluna">
      <section className="card">
        <h1>Fichas</h1>
        {fichas.isPending && <p className="muted">Carregando…</p>}
        {fichas.isError && <p className="erro">Não foi possível carregar as fichas.</p>}
        {fichas.data?.length === 0 && <p className="muted">Nenhuma ficha ainda.</p>}
        <ul className="lista">
          {fichas.data?.map((f) => (
            <li key={f.id}>
              <Link to={`/fichas/${f.id}`}>
                <strong>{f.nome}</strong>
              </Link>
              <span className="muted">{f.itens.length} exercícios</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <h2>Nova ficha</h2>
        <form onSubmit={handleSubmit((d) => criar.mutate(d))} noValidate>
          <label>
            Nome
            <input type="text" {...register("nome")} />
            {errors.nome && <span className="erro">{errors.nome.message}</span>}
          </label>

          {fields.map((campo, i) => (
            <fieldset key={campo.id}>
              <legend>Exercício {i + 1}</legend>
              <label>
                Exercício
                <select {...register(`itens.${i}.exercicioId`)}>
                  <option value="">Selecione…</option>
                  {exercicios.data?.map((ex) => (
                    <option key={ex.id} value={ex.id}>
                      {ex.nome} ({ex.grupoMuscular})
                    </option>
                  ))}
                </select>
                {errors.itens?.[i]?.exercicioId && (
                  <span className="erro">{errors.itens[i]?.exercicioId?.message}</span>
                )}
              </label>
              <div className="linha">
                <label>
                  Ordem
                  <input type="number" min={1} {...register(`itens.${i}.ordem`)} />
                  {errors.itens?.[i]?.ordem && (
                    <span className="erro">{errors.itens[i]?.ordem?.message}</span>
                  )}
                </label>
                <label>
                  Séries
                  <input type="number" min={1} {...register(`itens.${i}.seriesAlvo`)} />
                  {errors.itens?.[i]?.seriesAlvo && (
                    <span className="erro">{errors.itens[i]?.seriesAlvo?.message}</span>
                  )}
                </label>
                <label>
                  Reps
                  <input type="number" min={1} {...register(`itens.${i}.repsAlvo`)} />
                  {errors.itens?.[i]?.repsAlvo && (
                    <span className="erro">{errors.itens[i]?.repsAlvo?.message}</span>
                  )}
                </label>
              </div>
              <button
                type="button"
                className="secundario"
                onClick={() => remove(i)}
                disabled={fields.length <= 1}
              >
                Remover
              </button>
            </fieldset>
          ))}
          {typeof errors.itens?.message === "string" && (
            <p className="erro">{errors.itens.message}</p>
          )}

          <div className="acoes">
            <button
              type="button"
              className="secundario"
              onClick={() =>
                append({ exercicioId: "", ordem: fields.length + 1, seriesAlvo: 3, repsAlvo: 10 })
              }
            >
              Adicionar exercício
            </button>
          </div>
          {erroApi && (
            <p className="erro" role="alert">
              {erroApi}
            </p>
          )}
          <button type="submit" disabled={criar.isPending}>
            {criar.isPending ? "Salvando…" : "Criar ficha"}
          </button>
        </form>
      </section>
    </div>
  );
}

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useFieldArray, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, mensagemErro } from "../api/client";
import type { Exercicio, Ficha } from "../api/types";
import { fichaSchema, type FichaForm } from "./fichaSchema";

export function FichaDetalhePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [erroApi, setErroApi] = useState("");

  const ficha = useQuery({
    queryKey: ["ficha", id],
    queryFn: async () => (await api.get<Ficha>(`/fichas/${id}`)).data,
    enabled: Boolean(id),
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
  } = useForm<FichaForm>({ resolver: zodResolver(fichaSchema) });
  const { fields, append, remove } = useFieldArray({ control, name: "itens" });

  useEffect(() => {
    if (ficha.data) {
      reset({
        nome: ficha.data.nome,
        itens: [...ficha.data.itens]
          .sort((a, b) => a.ordem - b.ordem)
          .map((it) => ({
            exercicioId: it.exercicio.id,
            ordem: it.ordem,
            seriesAlvo: it.seriesAlvo,
            repsAlvo: it.repsAlvo,
          })),
      });
    }
  }, [ficha.data, reset]);

  const salvar = useMutation({
    mutationFn: async (dados: FichaForm) => (await api.put<Ficha>(`/fichas/${id}`, dados)).data,
    onSuccess: (dados) => {
      setErroApi("");
      queryClient.invalidateQueries({ queryKey: ["ficha", id] });
      queryClient.invalidateQueries({ queryKey: ["fichas"] });
      reset({
        nome: dados.nome,
        itens: [...dados.itens]
          .sort((a, b) => a.ordem - b.ordem)
          .map((it) => ({
            exercicioId: it.exercicio.id,
            ordem: it.ordem,
            seriesAlvo: it.seriesAlvo,
            repsAlvo: it.repsAlvo,
          })),
      });
    },
    onError: (e) => setErroApi(mensagemErro(e, "Não foi possível salvar a ficha.")),
  });

  const excluir = useMutation({
    mutationFn: async () => api.delete(`/fichas/${id}`),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["fichas"] });
      navigate("/fichas");
    },
    onError: (e) => setErroApi(mensagemErro(e, "Não foi possível excluir a ficha.")),
  });

  if (ficha.isPending) return <p className="muted">Carregando…</p>;
  if (ficha.isError) {
    return (
      <section className="card">
        <p className="erro" role="alert">
          {mensagemErro(ficha.error, "Ficha não encontrada.")}
        </p>
        <Link className="botao secundario" to="/fichas">
          Voltar
        </Link>
      </section>
    );
  }

  return (
    <div className="coluna">
      <section className="card">
        <Link className="muted" to="/fichas">
          ← Fichas
        </Link>
        <h1>Editar ficha</h1>
        <form onSubmit={handleSubmit((d) => salvar.mutate(d))} noValidate>
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
          <button type="submit" disabled={salvar.isPending}>
            {salvar.isPending ? "Salvando…" : "Salvar"}
          </button>
        </form>
      </section>

      <section className="card">
        <h2>Zona de perigo</h2>
        <button
          type="button"
          className="perigo"
          disabled={excluir.isPending}
          onClick={() => {
            if (window.confirm("Excluir esta ficha?")) excluir.mutate();
          }}
        >
          {excluir.isPending ? "Excluindo…" : "Excluir ficha"}
        </button>
      </section>
    </div>
  );
}

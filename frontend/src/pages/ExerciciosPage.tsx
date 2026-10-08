import { useState } from "react";
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

type Form = z.infer<typeof schema>;

export function ExerciciosPage() {
  const queryClient = useQueryClient();
  const [erroApi, setErroApi] = useState("");
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

  return (
    <div className="coluna">
      <section className="card">
        <h1>Exercícios</h1>
        {lista.isPending && <p className="muted">Carregando…</p>}
        {lista.isError && <p className="erro">Não foi possível carregar os exercícios.</p>}
        {lista.data?.length === 0 && <p className="muted">Nenhum exercício ainda.</p>}
        <ul className="lista">
          {lista.data?.map((ex) => (
            <li key={ex.id}>
              <strong>{ex.nome}</strong>
              <span className="muted">
                {ex.grupoMuscular}
                {ex.equipamento ? ` · ${ex.equipamento}` : ""}
              </span>
              {ex.global && <span className="badge">global</span>}
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <h2>Novo exercício</h2>
        <form onSubmit={handleSubmit((d) => criar.mutate(d))} noValidate>
          <label>
            Nome
            <input type="text" {...register("nome")} />
            {errors.nome && <span className="erro">{errors.nome.message}</span>}
          </label>
          <label>
            Grupo muscular
            <input type="text" {...register("grupoMuscular")} />
            {errors.grupoMuscular && (
              <span className="erro">{errors.grupoMuscular.message}</span>
            )}
          </label>
          <label>
            Equipamento (opcional)
            <input type="text" {...register("equipamento")} />
            {errors.equipamento && (
              <span className="erro">{errors.equipamento.message}</span>
            )}
          </label>
          {erroApi && (
            <p className="erro" role="alert">
              {erroApi}
            </p>
          )}
          <button type="submit" disabled={criar.isPending}>
            {criar.isPending ? "Salvando…" : "Criar exercício"}
          </button>
        </form>
      </section>
    </div>
  );
}

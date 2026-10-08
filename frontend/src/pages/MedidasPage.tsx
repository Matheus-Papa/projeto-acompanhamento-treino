import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { api, mensagemErro } from "../api/client";
import type { Medida } from "../api/types";
import { formatarData, hojeISO } from "../utils/data";

const schema = z.object({
  data: z
    .string()
    .min(1, "Data é obrigatória")
    .refine((d) => d <= hojeISO(), "Data não pode ser futura"),
  pesoKg: z.coerce.number().positive("Peso deve ser maior que zero"),
  percentualGordura: z.preprocess(
    (v) => (v === "" || v === null || v === undefined ? undefined : v),
    z.coerce
      .number()
      .min(0, "Percentual não pode ser negativo")
      .max(100, "Percentual deve ser no máximo 100")
      .optional(),
  ),
});

type Form = z.infer<typeof schema>;

export function MedidasPage() {
  const queryClient = useQueryClient();
  const [erroApi, setErroApi] = useState("");

  const lista = useQuery({
    queryKey: ["medidas"],
    queryFn: async () => (await api.get<Medida[]>("/medidas")).data,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Form>({
    resolver: zodResolver(schema),
    // Sem pesoKg inicial: campo vazio; o coerce do Zod converte no submit.
    defaultValues: {
      data: hojeISO(),
      percentualGordura: undefined,
    },
  });

  const criar = useMutation({
    mutationFn: async (dados: Form) => (await api.post<Medida>("/medidas", dados)).data,
    onSuccess: () => {
      setErroApi("");
      reset({ data: hojeISO(), percentualGordura: undefined });
      queryClient.invalidateQueries({ queryKey: ["medidas"] });
    },
    onError: (e) => setErroApi(mensagemErro(e, "Não foi possível registrar a medida.")),
  });

  const ordenadas = [...(lista.data ?? [])].sort((a, b) => (a.data < b.data ? -1 : 1));
  const pontos = ordenadas.map((m) => ({ data: formatarData(m.data), peso: m.pesoKg }));

  return (
    <div className="coluna">
      <section className="card">
        <h1>Medidas</h1>
        <form onSubmit={handleSubmit((d) => criar.mutate(d))} noValidate>
          <div className="linha2">
            <label>
              Data
              <input type="date" max={hojeISO()} {...register("data")} />
              {errors.data && <span className="erro">{errors.data.message}</span>}
            </label>
            <label>
              Peso (kg)
              <input
                className="input-grande"
                type="number"
                min={0}
                step="0.1"
                inputMode="decimal"
                placeholder="Ex.: 80.5"
                {...register("pesoKg")}
              />
              {errors.pesoKg && <span className="erro">{errors.pesoKg.message}</span>}
            </label>
          </div>
          <label>
            Gordura corporal % (opcional)
            <input
              type="number"
              min={0}
              max={100}
              step="0.1"
              inputMode="decimal"
              placeholder="Ex.: 18.5"
              {...register("percentualGordura")}
            />
            {errors.percentualGordura && (
              <span className="erro">{errors.percentualGordura.message}</span>
            )}
          </label>
          {erroApi && (
            <p className="erro" role="alert">
              {erroApi}
            </p>
          )}
          <button type="submit" disabled={criar.isPending}>
            {criar.isPending ? "Salvando…" : "Registrar medida"}
          </button>
        </form>
      </section>

      <section className="card">
        <h2>Peso por data</h2>
        {lista.isPending && <p className="muted">Carregando…</p>}
        {lista.isError && (
          <p className="erro" role="alert">
            {mensagemErro(lista.error, "Não foi possível carregar as medidas.")}
          </p>
        )}
        {!lista.isPending && !lista.isError && ordenadas.length === 0 && (
          <p className="muted">Nenhuma medida ainda. Registre a primeira acima.</p>
        )}
        {pontos.length > 0 && (
          <div className="chart-box">
            <ResponsiveContainer width="100%" height={240}>
              <LineChart data={pontos} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="data" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} domain={["auto", "auto"]} />
                <Tooltip />
                <Line type="monotone" dataKey="peso" name="Peso (kg)" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
        <ul className="lista">
          {[...ordenadas].reverse().map((m) => (
            <li key={m.id}>
              <strong>
                {formatarData(m.data)} · {m.pesoKg} kg
              </strong>
              {m.percentualGordura && (
                <span className="muted">{m.percentualGordura}% gordura</span>
              )}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

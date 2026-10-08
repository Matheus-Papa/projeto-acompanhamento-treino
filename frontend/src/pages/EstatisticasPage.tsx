import { useState } from "react";
import { useQueries, useQuery } from "@tanstack/react-query";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { api, mensagemErro } from "../api/client";
import type { Exercicio, Recorde, VolumeSemanal } from "../api/types";

function hojeISO(): string {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, "0");
  const dia = String(agora.getDate()).padStart(2, "0");
  return `${agora.getFullYear()}-${mes}-${dia}`;
}

function menosDias(dias: number): string {
  const d = new Date();
  d.setDate(d.getDate() - dias);
  const mes = String(d.getMonth() + 1).padStart(2, "0");
  const dia = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mes}-${dia}`;
}

function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split("-");
  return `${dia}/${mes}/${ano}`;
}

export function EstatisticasPage() {
  // Padrão: últimas 8 semanas.
  const [de, setDe] = useState(menosDias(56));
  const [ate, setAte] = useState(hojeISO());

  const volume = useQuery({
    queryKey: ["volume-semanal", de, ate],
    queryFn: async () => {
      const params: Record<string, string> = {};
      if (de) params.de = de;
      if (ate) params.ate = ate;
      return (await api.get<VolumeSemanal[]>("/estatisticas/volume-semanal", { params })).data;
    },
  });

  const exercicios = useQuery({
    queryKey: ["exercicios"],
    queryFn: async () => (await api.get<Exercicio[]>("/exercicios")).data,
  });

  // Top recordes: busca o recorde de no máximo 5 exercícios para não
  // estourar o backend com N requests (exercícios sem treino retornam 404 e são ignorados).
  const primeiros = (exercicios.data ?? []).slice(0, 5);
  const recordes = useQueries({
    queries: primeiros.map((ex) => ({
      queryKey: ["recorde", ex.id],
      queryFn: async () => (await api.get<Recorde>(`/exercicios/${ex.id}/recorde`)).data,
      retry: false,
    })),
  });
  const carregandoRecordes = recordes.some((r) => r.isPending);
  const top3 = recordes
    .map((r, i) => ({ ex: primeiros[i], rec: r.data }))
    .filter((x) => x.rec !== undefined)
    .map((x) => ({ nome: x.ex.nome, rec: x.rec as Recorde }))
    .sort((a, b) => Number(b.rec.maxEpley) - Number(a.rec.maxEpley))
    .slice(0, 3);

  const pontos = (volume.data ?? []).map((v) => ({
    semana: formatarData(v.semanaSegunda),
    volume: Number(v.volume),
  }));

  return (
    <div className="coluna">
      <section className="card">
        <h1>Estatísticas</h1>
        <div className="linha2">
          <label>
            De
            <input type="date" value={de} onChange={(e) => setDe(e.target.value)} />
          </label>
          <label>
            Até
            <input type="date" value={ate} onChange={(e) => setAte(e.target.value)} />
          </label>
        </div>
      </section>

      <section className="card">
        <h2>Volume semanal (kg)</h2>
        {volume.isPending && <p className="muted">Carregando…</p>}
        {volume.isError && (
          <p className="erro" role="alert">
            {mensagemErro(volume.error, "Não foi possível carregar o volume semanal.")}
          </p>
        )}
        {!volume.isPending && !volume.isError && pontos.length === 0 && (
          <p className="muted">Sem volume no período. Registre um treino para começar.</p>
        )}
        {pontos.length > 0 && (
          <>
            <div className="chart-box">
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={pontos} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="semana" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip />
                  <Bar dataKey="volume" name="Volume (kg)" />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <ul className="lista">
              {volume.data?.map((v) => (
                <li key={v.semanaSegunda}>
                  <strong>Semana de {formatarData(v.semanaSegunda)}</strong>
                  <span className="muted">{v.volume} kg</span>
                </li>
              ))}
            </ul>
          </>
        )}
      </section>

      <section className="card">
        <h2>Top recordes (1RM)</h2>
        {carregandoRecordes && <p className="muted">Carregando…</p>}
        {!carregandoRecordes && top3.length === 0 && (
          <p className="muted">Sem recordes ainda. Registre treinos para ver o top 3.</p>
        )}
        <ul className="lista">
          {top3.map((t) => (
            <li key={t.rec.exercicioId}>
              <strong>{t.nome}</strong>
              <span className="muted">
                1RM {t.rec.maxEpley} kg · carga {t.rec.maxCarga} kg · {formatarData(t.rec.emData)}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

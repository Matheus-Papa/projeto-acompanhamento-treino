import { useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { api, mensagemErro } from "../api/client";
import type { Exercicio, HistoricoItem, Recorde } from "../api/types";

function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split("-");
  return `${dia}/${mes}/${ano}`;
}

export function EvolucaoPage() {
  const { exercicioId } = useParams<{ exercicioId: string }>();
  const navigate = useNavigate();

  const exercicios = useQuery({
    queryKey: ["exercicios"],
    queryFn: async () => (await api.get<Exercicio[]>("/exercicios")).data,
  });
  const historico = useQuery({
    queryKey: ["historico", exercicioId],
    queryFn: async () => (await api.get<HistoricoItem[]>(`/exercicios/${exercicioId}/historico`)).data,
    enabled: Boolean(exercicioId),
  });
  const recorde = useQuery({
    queryKey: ["recorde", exercicioId],
    queryFn: async () => (await api.get<Recorde>(`/exercicios/${exercicioId}/recorde`)).data,
    enabled: Boolean(exercicioId),
  });

  const nomeSelecionado = exercicios.data?.find((e) => e.id === exercicioId)?.nome;

  const pontosEpley = (historico.data ?? []).map((h) => ({
    data: formatarData(h.data),
    epley: Number(h.epley),
  }));
  const pontosVolume = (historico.data ?? []).map((h) => ({
    data: formatarData(h.data),
    volume: Number(h.volume),
  }));

  return (
    <div className="coluna">
      <section className="card">
        <h1>Evolução</h1>
        {exercicios.isPending && <p className="muted">Carregando exercícios…</p>}
        {exercicios.isError && <p className="erro">Não foi possível carregar os exercícios.</p>}
        <label>
          Exercício
          <select
            className="input-grande"
            value={exercicioId ?? ""}
            onChange={(e) =>
              navigate(e.target.value === "" ? "/evolucao" : `/evolucao/${e.target.value}`)
            }
          >
            <option value="">Selecione…</option>
            {exercicios.data?.map((ex) => (
              <option key={ex.id} value={ex.id}>
                {ex.nome} ({ex.grupoMuscular})
              </option>
            ))}
          </select>
        </label>
      </section>

      {!exercicioId && <p className="muted">Escolha um exercício para ver a evolução.</p>}

      {exercicioId && historico.isPending && <p className="muted">Carregando evolução…</p>}
      {exercicioId && historico.isError && (
        <p className="erro" role="alert">
          {mensagemErro(historico.error, "Não foi possível carregar a evolução.")}
        </p>
      )}
      {exercicioId && historico.data?.length === 0 && (
        <p className="muted">Sem treinos com este exercício ainda.</p>
      )}

      {exercicioId && recorde.data && (
        <section className="card">
          <h2>Recorde{ nomeSelecionado ? ` · ${nomeSelecionado}` : ""}</h2>
          <div className="stats-grid">
            <div className="stat-card">
              <span className="stat-valor">{recorde.data.maxCarga} kg</span>
              <span className="muted">carga máxima</span>
            </div>
            <div className="stat-card">
              <span className="stat-valor">{recorde.data.maxEpley} kg</span>
              <span className="muted">1RM (Epley)</span>
            </div>
            <div className="stat-card">
              <span className="stat-valor">{formatarData(recorde.data.emData)}</span>
              <span className="muted">data do recorde</span>
            </div>
          </div>
          {recorde.isError && <p className="erro">Não foi possível carregar o recorde.</p>}
        </section>
      )}

      {exercicioId && pontosEpley.length > 0 && (
        <>
          <section className="card">
            <h2>1RM estimado (Epley) por data</h2>
            <div className="chart-box">
              <ResponsiveContainer width="100%" height={240}>
                <LineChart data={pontosEpley} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="data" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip />
                  <Line type="monotone" dataKey="epley" name="1RM (kg)" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </section>

          <section className="card">
            <h2>Volume por treino (kg)</h2>
            <div className="chart-box">
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={pontosVolume} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="data" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip />
                  <Bar dataKey="volume" name="Volume (kg)" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </section>
        </>
      )}
    </div>
  );
}

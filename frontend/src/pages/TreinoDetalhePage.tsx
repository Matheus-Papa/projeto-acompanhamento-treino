import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { api, mensagemErro } from "../api/client";
import type { Treino } from "../api/types";
import { formatarData } from "../utils/data";

export function TreinoDetalhePage() {
  const { id } = useParams<{ id: string }>();

  const treino = useQuery({
    queryKey: ["treino", id],
    queryFn: async () => (await api.get<Treino>(`/treinos/${id}`)).data,
    enabled: Boolean(id),
  });

  if (treino.isPending) return <p className="muted">Carregando…</p>;

  if (treino.isError) {
    return (
      <section className="card">
        <p className="erro" role="alert">
          {mensagemErro(treino.error, "Treino não encontrado.")}
        </p>
        <Link className="botao secundario" to="/historico">
          Voltar ao histórico
        </Link>
      </section>
    );
  }

  const dados = treino.data;
  const series = [...dados.series].sort((a, b) => a.numero - b.numero);
  const nomePorExercicio = new Map(series.map((s) => [s.exercicio.id, s.exercicio.nome]));

  return (
    <div className="coluna">
      <section className="card">
        <Link className="muted" to="/historico">
          ← Histórico
        </Link>
        <h1>Treino de {formatarData(dados.data)}</h1>
        {dados.duracaoMin && <p className="muted">Duração: {dados.duracaoMin} min</p>}
        {dados.observacao && <p>{dados.observacao}</p>}
        {dados.fichaId && (
          <p>
            <Link to={`/fichas/${dados.fichaId}`}>Ver ficha usada</Link>
          </p>
        )}
        {dados.prs.length > 0 && (
          <ul className="lista">
            {dados.prs.map((pr) => (
              <li key={`${pr.exercicioId}-${pr.tipo}`}>
                <span>
                  <span className="badge-pr">PR!</span>{" "}
                  <strong>{nomePorExercicio.get(pr.exercicioId) ?? "Exercício"}</strong>
                </span>
                <span className="muted">{pr.tipo === "CARGA" ? "recorde de carga" : pr.tipo === "E1RM" ? "recorde de 1RM" : "recorde de carga + 1RM"}</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card">
        <h2>Séries ({series.length})</h2>
        {series.length === 0 && <p className="muted">Nenhuma série registrada.</p>}
        <ul className="lista">
          {series.map((s) => (
            <li key={s.id}>
              <strong>
                {s.numero}. {s.exercicio.nome}
              </strong>
              <span className="muted">
                {s.repeticoes} reps × {s.cargaKg} kg
              </span>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

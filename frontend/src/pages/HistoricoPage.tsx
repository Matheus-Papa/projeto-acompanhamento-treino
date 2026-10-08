import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { api, mensagemErro } from "../api/client";
import type { Page, TreinoResumo } from "../api/types";
import { formatarData } from "../utils/data";

const TAMANHO = 10;

export function HistoricoPage() {
  const [de, setDe] = useState("");
  const [ate, setAte] = useState("");
  const [aplicados, setAplicados] = useState({ de: "", ate: "" });
  const [pagina, setPagina] = useState(0);
  const [itens, setItens] = useState<TreinoResumo[]>([]);
  const [total, setTotal] = useState(0);

  const lista = useQuery({
    queryKey: ["treinos", aplicados.de, aplicados.ate, pagina],
    queryFn: async () => {
      const params: Record<string, string | number> = { page: pagina, size: TAMANHO };
      if (aplicados.de) params.de = aplicados.de;
      if (aplicados.ate) params.ate = aplicados.ate;
      return (await api.get<Page<TreinoResumo>>("/treinos", { params })).data;
    },
  });

  useEffect(() => {
    if (!lista.data) return;
    const paginaAtual = lista.data;
    setTotal(paginaAtual.totalElements);
    if (paginaAtual.number === 0) {
      setItens(paginaAtual.content);
    } else {
      setItens((atual) => {
        const ids = new Set(atual.map((t) => t.id));
        return [...atual, ...paginaAtual.content.filter((t) => !ids.has(t.id))];
      });
    }
  }, [lista.data]);

  function aplicarFiltros(e: FormEvent) {
    e.preventDefault();
    setItens([]);
    setAplicados({ de, ate });
    setPagina(0);
  }

  function limparFiltros() {
    setDe("");
    setAte("");
    setItens([]);
    setAplicados({ de: "", ate: "" });
    setPagina(0);
  }

  const temMais = itens.length < total;

  return (
    <div className="coluna">
      <section className="card">
        <h1>Histórico</h1>
        <form onSubmit={aplicarFiltros} noValidate>
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
          <div className="acoes">
            <button type="submit">Filtrar</button>
            <button type="button" className="secundario" onClick={limparFiltros}>
              Limpar
            </button>
          </div>
        </form>
      </section>

      <section className="card">
        {lista.isPending && pagina === 0 && <p className="muted">Carregando…</p>}
        {lista.isError && (
          <>
            <p className="erro" role="alert">
              {mensagemErro(lista.error, "Não foi possível carregar o histórico.")}
            </p>
            <button type="button" className="secundario" onClick={() => lista.refetch()}>
              Tentar de novo
            </button>
          </>
        )}
        {!lista.isPending && !lista.isError && itens.length === 0 && (
          <p className="muted">Nenhum treino encontrado. Registre o primeiro na tela Treino.</p>
        )}
        <ul className="lista">
          {itens.map((t) => (
            <li key={t.id}>
              <Link to={`/treinos/${t.id}`}>
                <strong>{formatarData(t.data)}</strong>
              </Link>
              <span className="muted">
                {t.totalSeries} {t.totalSeries === 1 ? "série" : "séries"} · volume {t.volumeTotal} kg
                {t.duracaoMin ? ` · ${t.duracaoMin} min` : ""}
              </span>
            </li>
          ))}
        </ul>
        {temMais && (
          <div className="acoes">
            <button
              type="button"
              className="secundario"
              disabled={lista.isFetching}
              onClick={() => setPagina((p) => p + 1)}
            >
              {lista.isFetching ? "Carregando…" : `Carregar mais (${itens.length}/${total})`}
            </button>
          </div>
        )}
      </section>
    </div>
  );
}

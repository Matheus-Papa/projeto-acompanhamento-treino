import { Link, Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import { ProtectedRoute } from "./routes/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { CadastroPage } from "./pages/CadastroPage";
import { RegistrarTreinoPage } from "./pages/RegistrarTreinoPage";
import { HistoricoPage } from "./pages/HistoricoPage";
import { TreinoDetalhePage } from "./pages/TreinoDetalhePage";
import { EvolucaoPage } from "./pages/EvolucaoPage";
import { EstatisticasPage } from "./pages/EstatisticasPage";
import { MedidasPage } from "./pages/MedidasPage";
import { ExerciciosPage } from "./pages/ExerciciosPage";
import { FichasPage } from "./pages/FichasPage";
import { FichaDetalhePage } from "./pages/FichaDetalhePage";
import "./styles.css";

function ativo(pathname: string, base: string, exato = false): string {
  if (exato) return pathname === base ? "ativo" : "";
  return pathname === base || pathname.startsWith(`${base}/`) ? "ativo" : "";
}

const TABS = [
  { to: "/", rotulo: "Treino", icone: "🏋️", exato: true },
  { to: "/historico", rotulo: "Histórico", icone: "🕘", exato: false },
  { to: "/evolucao", rotulo: "Evolução", icone: "📈", exato: false },
  { to: "/estatisticas", rotulo: "Estatísticas", icone: "📊", exato: false },
  { to: "/medidas", rotulo: "Medidas", icone: "⚖️", exato: false },
  { to: "/exercicios", rotulo: "Exercícios", icone: "💪", exato: false },
  { to: "/fichas", rotulo: "Fichas", icone: "📋", exato: false },
];

function Topo() {
  const { token, logout } = useAuth();
  const navigate = useNavigate();
  if (!token) return null;
  function sair() {
    logout();
    navigate("/login");
  }
  return (
    <header className="topo">
      <span className="marca" aria-label="Meu Treino">
        <span className="marca-icone" aria-hidden="true">
          💪
        </span>
        Meu Treino
      </span>
      <button type="button" className="sair" onClick={sair}>
        Sair
      </button>
    </header>
  );
}

function Navegacao() {
  const { token } = useAuth();
  const { pathname } = useLocation();
  if (!token) return null;

  return (
    <nav className="tabbar" aria-label="Navegação principal">
      {TABS.map((t) => {
        const cls = t.to === "/historico"
          ? ativo(pathname, "/historico") || ativo(pathname, "/treinos")
          : ativo(pathname, t.to, t.exato);
        return (
          <Link key={t.to} to={t.to} className={cls}>
            <span className="tab-icone" aria-hidden="true">
              {t.icone}
            </span>
            {t.rotulo}
          </Link>
        );
      })}
    </nav>
  );
}

function RotaDesconhecida() {
  const { token } = useAuth();
  return <Navigate to={token ? "/" : "/login"} replace />;
}

export function App() {
  return (
    <AuthProvider>
      <div className="app">
        <Topo />
        <main className="conteudo">
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/cadastro" element={<CadastroPage />} />
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<RegistrarTreinoPage />} />
              <Route path="/historico" element={<HistoricoPage />} />
              <Route path="/treinos/:id" element={<TreinoDetalhePage />} />
              <Route path="/evolucao" element={<EvolucaoPage />} />
              <Route path="/evolucao/:exercicioId" element={<EvolucaoPage />} />
              <Route path="/estatisticas" element={<EstatisticasPage />} />
              <Route path="/medidas" element={<MedidasPage />} />
              <Route path="/exercicios" element={<ExerciciosPage />} />
              <Route path="/fichas" element={<FichasPage />} />
              <Route path="/fichas/:id" element={<FichaDetalhePage />} />
            </Route>
            <Route path="*" element={<RotaDesconhecida />} />
          </Routes>
        </main>
        <Navegacao />
      </div>
    </AuthProvider>
  );
}

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

function Navegacao() {
  const { token, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  if (!token) return null;

  function sair() {
    logout();
    navigate("/login");
  }

  return (
    <nav className="tabbar" aria-label="Navegação principal">
      <Link to="/" className={ativo(pathname, "/", true)}>
        Treino
      </Link>
      <Link to="/historico" className={ativo(pathname, "/historico") || ativo(pathname, "/treinos")}>
        Histórico
      </Link>
      <Link to="/evolucao" className={ativo(pathname, "/evolucao")}>
        Evolução
      </Link>
      <Link to="/estatisticas" className={ativo(pathname, "/estatisticas")}>
        Stats
      </Link>
      <Link to="/medidas" className={ativo(pathname, "/medidas")}>
        Medidas
      </Link>
      <Link to="/exercicios" className={ativo(pathname, "/exercicios")}>
        Exercícios
      </Link>
      <Link to="/fichas" className={ativo(pathname, "/fichas")}>
        Fichas
      </Link>
      <button type="button" className="link" onClick={sair}>
        Sair
      </button>
    </nav>
  );
}

function RotaDesconhecida() {
  const { token } = useAuth();
  return token ? <Navigate to="/" replace /> : <LoginPage />;
}

export function App() {
  return (
    <AuthProvider>
      <div className="app">
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

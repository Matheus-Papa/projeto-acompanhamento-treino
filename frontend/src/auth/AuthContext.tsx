import { createContext, useContext, useState } from "react";
import type { ReactNode } from "react";
import { api, TOKEN_KEY } from "../api/client";
import type { AuthResponse } from "../api/types";

interface AuthContexto {
  token: string | null;
  login: (email: string, senha: string) => Promise<void>;
  cadastrar: (nome: string, email: string, senha: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContexto | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));

  async function guardarToken(resposta: AuthResponse) {
    localStorage.setItem(TOKEN_KEY, resposta.token);
    setToken(resposta.token);
  }

  async function login(email: string, senha: string) {
    const { data } = await api.post<AuthResponse>("/auth/login", { email, senha });
    await guardarToken(data);
  }

  async function cadastrar(nome: string, email: string, senha: string) {
    const { data } = await api.post<AuthResponse>("/auth/register", { nome, email, senha });
    await guardarToken(data);
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
  }

  return (
    <AuthContext.Provider value={{ token, login, cadastrar, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContexto {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth deve ser usado dentro de AuthProvider");
  return ctx;
}

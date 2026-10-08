import axios from "axios";
import type { ErroPadrao } from "./types";

export const TOKEN_KEY = "treino.token";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "http://localhost:8080",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && window.location.pathname !== "/login") {
      localStorage.removeItem(TOKEN_KEY);
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);

// Extrai a mensagem em português do corpo de erro padronizado do backend.
export function mensagemErro(error: unknown, padrao: string): string {
  if (axios.isAxiosError<ErroPadrao>(error)) {
    const dados = error.response?.data;
    if (dados?.errosCampos?.length) {
      return dados.errosCampos.map((e) => e.mensagem).join(" ");
    }
    if (dados?.mensagem) return dados.mensagem;
  }
  return padrao;
}

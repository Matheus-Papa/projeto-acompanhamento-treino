import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { mensagemErro } from "../api/client";

const schema = z.object({
  nome: z.string().min(1, "Nome é obrigatório").max(120, "Nome deve ter no máximo 120 caracteres"),
  email: z.string().min(1, "E-mail é obrigatório").email("E-mail inválido").max(160),
  senha: z
    .string()
    .min(8, "Senha deve ter entre 8 e 100 caracteres")
    .max(100, "Senha deve ter entre 8 e 100 caracteres"),
});

type Form = z.infer<typeof schema>;

export function CadastroPage() {
  const { cadastrar, token } = useAuth();
  const navigate = useNavigate();
  const [erroApi, setErroApi] = useState("");
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({ resolver: zodResolver(schema) });

  async function onSubmit(dados: Form) {
    setErroApi("");
    try {
      await cadastrar(dados.nome, dados.email, dados.senha);
      navigate("/", { replace: true });
    } catch (e) {
      setErroApi(mensagemErro(e, "Não foi possível cadastrar. Tente de novo."));
    }
  }

  if (token) return <Navigate to="/" replace />;

  return (
    <section className="card">
      <h1>Criar conta</h1>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <label>
          Nome
          <input type="text" autoComplete="name" {...register("nome")} />
          {errors.nome && <span className="erro">{errors.nome.message}</span>}
        </label>
        <label>
          E-mail
          <input type="email" autoComplete="email" {...register("email")} />
          {errors.email && <span className="erro">{errors.email.message}</span>}
        </label>
        <label>
          Senha
          <input type="password" autoComplete="new-password" {...register("senha")} />
          {errors.senha && <span className="erro">{errors.senha.message}</span>}
        </label>
        {erroApi && (
          <p className="erro" role="alert">
            {erroApi}
          </p>
        )}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Cadastrando…" : "Cadastrar"}
        </button>
      </form>
      <p className="muted">
        Já tem conta? <Link to="/login">Entrar</Link>
      </p>
    </section>
  );
}

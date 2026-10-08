import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { mensagemErro } from "../api/client";

const schema = z.object({
  email: z.string().min(1, "E-mail é obrigatório").email("E-mail inválido").max(160),
  senha: z.string().min(1, "Senha é obrigatória"),
});

type Form = z.infer<typeof schema>;

export function LoginPage() {
  const { login } = useAuth();
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
      await login(dados.email, dados.senha);
      navigate("/", { replace: true });
    } catch (e) {
      setErroApi(mensagemErro(e, "Não foi possível entrar. Verifique os dados."));
    }
  }

  return (
    <section className="card">
      <h1>Entrar</h1>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <label>
          E-mail
          <input type="email" autoComplete="email" {...register("email")} />
          {errors.email && <span className="erro">{errors.email.message}</span>}
        </label>
        <label>
          Senha
          <input type="password" autoComplete="current-password" {...register("senha")} />
          {errors.senha && <span className="erro">{errors.senha.message}</span>}
        </label>
        {erroApi && (
          <p className="erro" role="alert">
            {erroApi}
          </p>
        )}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Entrando…" : "Entrar"}
        </button>
      </form>
      <p className="muted">
        Sem conta? <Link to="/cadastro">Cadastre-se</Link>
      </p>
    </section>
  );
}
